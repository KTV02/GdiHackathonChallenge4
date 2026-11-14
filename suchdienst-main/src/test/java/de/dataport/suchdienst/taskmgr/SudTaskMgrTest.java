/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.taskmgr;

import static de.dataport.suchdienst.error.model.ErrorCodeEnum.UNKNOWN_ERROR;
import static de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResultState.ACCEPTED;
import static de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResultState.IGNORED_DUE_DUPLICATE;
import static de.dataport.suchdienst.taskmgr.model.SudTaskType.IMPORT;
import static de.dataport.suchdienst.taskmgr.model.SudTaskType.OD_RE_RECALC;
import static de.dataport.suchdienst.taskmgr.model.SudTaskType.VL_RECALC;
import static de.dataport.suchdienst.taskmgr.model.SudTaskType.VL_UPDATE;
import static de.dataport.suchdienst.util.Bundeslaender.BAYERN;
import static de.dataport.suchdienst.util.Bundeslaender.BREMEN;
import static de.dataport.suchdienst.util.Bundeslaender.HAMBURG;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.MoreObjects;
import de.dataport.suchdienst.config.ThreadPoolTaskConfiguration;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.taskmgr.exception.SudTaskException;
import de.dataport.suchdienst.taskmgr.model.BundeslandTask;
import de.dataport.suchdienst.taskmgr.model.SchemeAgencyIdTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.Bundeslaender;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.Getter;
import org.apache.commons.lang3.ThreadUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class SudTaskMgrTest {

    private static final long TEST_TIMEOUT_MILLIS = 2000;

    /**
     * Dieser Anstandstest stellt einen einzigen Auftrag ein und erwartet, dass dieser abgearbeitet wird.
     */
    @Test
    @DisplayName("Einfachster Fall: Ein einzelner Task wird abgearbeitet")
    void theSimplestCase() throws InterruptedException, ExecutionException {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        var mgr = createMgr(1, List.of(testTask));
        var submissionResult = mgr.submit(testTask);
        // Der Task soll akzeptiert werden:
        assertThat(submissionResult.state(), is(ACCEPTED));
        // ... und wird erst abgearbeitet, wenn das proceed-Signal gesendet wird, vorher also
        // ist der Future noch nicht done:
        var future = submissionResult.future();
        assertThat(future.isDone(), is(false));
        // Nun den Task ablaufen lassen:
        testTask.proceed();
        // Nach der Terminierung des Tasks liefert future.get die TaskId:
        var futureResult = future.get();
        assertThat(future.isDone(), is(true));
        assertThat(testTask.wasCalled(), is(true));
        assertThat(futureResult, is(1L));
    }

    @Test
    @DisplayName(
        "Mehrere Neuberechnungs-Tasks für unterschiedliche Bundesländer bei einem Worker-Thread der Reihe nach verarbeitet."
    )
    void sequentialTaskProcessing()
        throws InterruptedException, ExecutionException, TimeoutException {
        int noOfTasks = 10;
        List<TestTask> testTasks = new ArrayList<>();
        int id = 0;
        for (var bundesland : Bundeslaender.values()) {
            testTasks.add(new TestTask(++id, VL_RECALC, bundesland));
        }
        var mgr = createMgr(1, testTasks);

        testTasks.forEach(t -> {
            var result = mgr.submit(t);
            assertThat(result.state(), is(ACCEPTED));
        });

        // Warten bis alle Tasks abgearbeitet wurden:
        testTasks.forEach(TestTask::proceed);
        waitForAll(testTasks);

        // Prüfen ob Ausführungsreihenfolge korrekt:
        assertThat(
            "Task ohne startTime gefunden",
            testTasks.stream().allMatch(t -> t.getStartTime() > 0)
        );
        Set<Long> startTimesSet = testTasks.stream().map(TestTask::getStartTime).collect(toSet());
        assertThat(
            "startTime ist nicht paarweise verschieden",
            startTimesSet.size(),
            is(testTasks.size())
        );
        for (int i = 0; i < noOfTasks - 1; i++) {
            assertThat(
                "Reihenfolge der Tasks wird nicht eingehalten",
                testTasks.get(i).getStartTime() < testTasks.get(i + 1).getStartTime()
            );
        }
    }

    @Test
    @DisplayName("Duplikate werden ignoriert")
    void ignoreDuplicates() throws InterruptedException, ExecutionException, TimeoutException {
        TestTask testTask1 = new TestTask(1, VL_RECALC, HAMBURG);
        TestTask testTask2 = new TestTask(2, VL_RECALC, BREMEN);
        TestTask duplicate1 = new TestTask(3, VL_RECALC, BREMEN);
        TestTask duplicate2 = new TestTask(4, VL_RECALC, BREMEN);
        var allTasks = List.of(testTask1, testTask2, duplicate1, duplicate2);
        var regular = List.of(testTask1, testTask2);
        var duplicates = List.of(duplicate1, duplicate2);

        var mgr = createMgr(1, allTasks);
        regular.forEach(t -> {
            var result = mgr.submit(t);
            assertThat(result.state(), is(ACCEPTED));
        });
        duplicates.forEach(t -> {
            var result = mgr.submit(t);
            assertThat(result.state(), is(IGNORED_DUE_DUPLICATE));
        });
        regular.forEach(TestTask::proceed);
        var results = waitForAll(regular);
        assertThat(results, equalTo(List.of(1L, 2L)));
    }

    /**
     * Hier werden gleichartige Tasks in die Queue gestellt, aber mit längerem Abstand, sodass sie dann
     * nicht als Duplikate erkannt werden können und folglich ausgeführt werden.
     */
    @Test
    @DisplayName("Duplikate werden ausgeführt, wenn sie langsam in die Queue gestellt werden")
    void executeDuplicates() throws InterruptedException, ExecutionException, TimeoutException {
        TestTask testTask1 = new TestTask(1, VL_RECALC, HAMBURG);
        TestTask duplicate1 = new TestTask(2, VL_RECALC, HAMBURG);
        TestTask duplicate2 = new TestTask(3, VL_RECALC, HAMBURG);
        var allTasks = List.of(testTask1, duplicate1, duplicate2);

        var mgr = createMgr(2, allTasks);
        for (var task : allTasks) {
            var result = mgr.submit(task);
            assertThat(result.state(), is(ACCEPTED));
            task.proceed();
            assertThat(result.future().get(1000, MILLISECONDS), equalTo(task.getTaskId()));
        }
    }

    /**
     * Die TaskId soll bei der Duplikat-Erkennung nicht mitspielen, ist nur ein Test-Feature.
     */
    @Test
    void testEquality() {
        var task1 = new TestTask(1, VL_RECALC, BREMEN);
        var task2 = new TestTask(2, VL_RECALC, BREMEN);
        var task3 = new TestTask(1, VL_RECALC, HAMBURG);
        var task4 = new TestTask(1, IMPORT, BREMEN);
        assertThat(task1.equals(task2), is(true));
        assertThat(task1.equals(task3), is(false));
        assertThat(task1.equals(task4), is(false));
    }

    /**
     * Hier werden zwei schnelle und ein langsamer Task eingestellt, dann auf die schnellen gewartet.
     */
    @Test
    @DisplayName("Testet das Warten auf die Beendigung bestimmter Tasks")
    void testWaitForTasks() {
        TestTask fastTask1 = new TestTask(1, VL_RECALC, HAMBURG);
        TestTask fastTask2 = new TestTask(2, VL_RECALC, BREMEN);
        TestTask slowTask = new TestTask(3, VL_RECALC, BAYERN);
        var allTasks = List.of(fastTask1, fastTask2, slowTask);
        var fastTasks = List.of(fastTask1, fastTask2);

        var mgr = createMgr(2, allTasks);
        for (var task : allTasks) {
            var result = mgr.submit(task);
            assertThat(result.state(), is(ACCEPTED));
        }

        // Nur die beiden "schnellen" anstarten:
        fastTasks.forEach(TestTask::proceed);

        // Warten auf die beiden schnellen, dies terminiert, obwohl noch nicht alle Tasks fertig sind (der langsame fehlt)
        mgr.waitForTasks(t -> ((TestTask) t).getTaskId() != 3);
        assertThat(fastTask1.getFuture().isDone(), is(true));
        assertThat(fastTask2.getFuture().isDone(), is(true));
        assertThat(slowTask.getFuture().isDone(), is(false));
    }

    @Test
    @DisplayName("Verarbeitung von vielen Tasks aller Bundesländer funktioniert")
    void testMultipleTasks() throws ExecutionException, InterruptedException, TimeoutException {
        var idGenerator = new AtomicInteger(0);
        var testTasks = Arrays.stream(SudTaskType.values())
            .flatMap(taskType ->
                Arrays.stream(Bundeslaender.values()).map(b ->
                    new TestTask(idGenerator.incrementAndGet(), taskType, b)
                )
            )
            .toList();
        var mgr = createMgr(2, testTasks);
        testTasks.forEach(mgr::submit);
        testTasks.forEach(TestTask::proceed);
        waitForAll(testTasks);
    }

    // Prioritäten aufsteigend: OD_RE_RECALC(5) < VL_RECALC(10) < VL_UPDATE(20) < IMPORT(30)
    @Test
    @DisplayName("Priorität wird beachtet")
    void testPriority() throws ExecutionException, InterruptedException, TimeoutException {
        TestTask longRunner = new TestTask(1, VL_RECALC, HAMBURG);
        var lowestPrio = new TestTask(2, OD_RE_RECALC, HAMBURG);
        var lowPrio = new TestTask(3, VL_UPDATE, HAMBURG);
        var highPrio = new TestTask(4, IMPORT, HAMBURG);
        var allTasks = List.of(longRunner, lowestPrio, lowPrio, highPrio);
        var mgr = createMgr(1, allTasks);
        mgr.submit(longRunner); // Blockiert den einzigen Worker-Thread
        // Erwartung ist, dass trotz umgekehrter Submit-Reihenfolge die Tasks in Prioritätsreihenfolge ausgeführt werden:
        mgr.submit(lowestPrio);
        mgr.submit(lowPrio);
        mgr.submit(highPrio);
        allTasks.forEach(TestTask::proceed);
        waitForAll(allTasks);
        var lowestPrioStartTime = lowestPrio.getStartTime();
        assertThat(lowestPrioStartTime, Matchers.greaterThan(0L));
        var lowPrioStartTime = lowPrio.getStartTime();
        assertThat(lowPrioStartTime, Matchers.greaterThan(0L));
        var highPrioStartTime = highPrio.getStartTime();
        assertThat(highPrioStartTime, Matchers.greaterThan(0L));
        // IMPORT startet vor VL_UPDATE, VL_UPDATE startet vor OD_RE_RECALC:
        assertThat(highPrioStartTime < lowPrioStartTime, is(true));
        assertThat(lowPrioStartTime < lowestPrioStartTime, is(true));
    }

    @Test
    @DisplayName("Snapshot wird korrekt berechnet")
    void testSnapshot() {
        TestTask longRunner = new TestTask(1, VL_RECALC, HAMBURG);
        var lowestPrio = new TestTask(2, OD_RE_RECALC, HAMBURG);
        var lowPrio = new TestTask(3, VL_UPDATE, HAMBURG);
        var highPrio = new TestTask(4, IMPORT, HAMBURG);
        var allTasks = List.of(longRunner, lowestPrio, lowPrio, highPrio);
        var mgr = createMgr(1, allTasks);
        mgr.submit(longRunner); // Blockiert den einzigen Worker-Thread
        mgr.submit(lowestPrio);
        mgr.submit(lowPrio);
        mgr.submit(highPrio);
        // Erwartung: longRunner läuft, die drei anderen warten in Prioritätsreihenfolge:
        // IMPORT(30) > VL_UPDATE(20) > OD_RE_RECALC(5)
        var snapshot = mgr.getTasksSnapshot();
        var runningTasks = snapshot.getRunningTasks();
        var waitingTasks = snapshot.getWaitingTasks();
        assertThat(runningTasks.size(), is(1));
        var runningTask = runningTasks.getFirst();
        assertThat(runningTask.getTaskType(), is(VL_RECALC));
        assertThat(waitingTasks.size(), is(3));
        assertThat(waitingTasks.get(0).getTaskType(), is(IMPORT));
        assertThat(waitingTasks.get(1).getTaskType(), is(VL_UPDATE));
        assertThat(waitingTasks.get(2).getTaskType(), is(OD_RE_RECALC));
    }

    @Test
    @DisplayName("Task wirft Exception")
    void taskThrowingException() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        testTask.throwWhenCalled(() ->
            new ImportException(ErrorCodeEnum.INVALID_FILE, "Test Msg", ExceptionType.TECHNISCH)
        );
        var mgr = createMgr(1, List.of(testTask));
        var submissionResult = mgr.submit(testTask);
        // Der Task soll akzeptiert werden:
        assertThat(submissionResult.state(), is(ACCEPTED));
        // ... und wird erst abgearbeitet, wenn das proceed-Signal gesendet wird, vorher also
        // ist der Future noch nicht done:
        var future = submissionResult.future();
        assertThat(future.isDone(), is(false));
        // Nun den Task ablaufen lassen:
        testTask.proceed();
        // Nach der Terminierung des Tasks liefert future.get die TaskId:
        var thrown = assertThrows(ExecutionException.class, future::get);
        var cause = thrown.getCause();
        assertThat(cause instanceof SudTaskException, is(true));
        var sudTaskException = (SudTaskException) cause;
        var msg = sudTaskException.getMessage();
        assertThat(msg, notNullValue());
        assertThat(sudTaskException.getCode(), is(ErrorCodeEnum.INVALID_FILE));
        assertThat(msg.contains("Test Msg"), is(true));
        assertThat(msg.contains(testTask.toString()), is(true));
    }

    @Test
    @DisplayName("Task mit generischer RuntimeException wird auf UNKNOWN_ERROR gemappt")
    void taskThrowingGenericRuntimeExceptionMapsToUnknownError() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        testTask.throwWhenCalled(() -> new IllegalArgumentException("boom"));
        var mgr = createMgr(1, List.of(testTask));
        var submissionResult = mgr.submit(testTask);
        assertThat(submissionResult.state(), is(ACCEPTED));

        var future = submissionResult.future();
        testTask.proceed();

        var thrown = assertThrows(ExecutionException.class, future::get);
        var cause = thrown.getCause();
        assertThat(cause instanceof SudTaskException, is(true));
        var sudTaskException = (SudTaskException) cause;
        assertThat(sudTaskException.getCode(), is(UNKNOWN_ERROR));
        assertThat(sudTaskException.getMessage().contains("boom"), is(true));
    }

    @Test
    @DisplayName("waitForTasks wirft nicht, wenn ein beobachteter Task fehlschlägt")
    void waitForTasksDoesNotThrowOnFailingTask() {
        TestTask failingTask = new TestTask(1, VL_RECALC, HAMBURG);
        failingTask.throwWhenCalled(() -> new IllegalStateException("expected-test-failure"));
        TestTask successfulTask = new TestTask(2, VL_RECALC, BREMEN);
        var mgr = createMgr(2, List.of(failingTask, successfulTask));

        mgr.submit(failingTask);
        mgr.submit(successfulTask);
        failingTask.proceed();
        successfulTask.proceed();

        assertDoesNotThrow(() -> mgr.waitForTasks(t -> true));
        assertThat(successfulTask.getFuture().isDone(), is(true));
        assertThat(failingTask.getFuture().isDone(), is(true));
    }

    @Test
    @DisplayName("IllegalStateException falls Mediator nicht eindeutig")
    void testNonUniqueMediatorThrowsException() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        List<SudTaskMediator<?>> mediators = List.of(testTask, testTask);
        SudTaskMgr mgr = new SudTaskMgr(
            ThreadPoolTaskConfiguration.threadPool(
                this.getClass().getSimpleName() + "-",
                1,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );
        mgr.setMediators(mediators);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            mgr.submit(testTask)
        );
        assertThat(ex.getMessage().contains(testTask.toString()), is(true));
    }

    @Test
    @DisplayName("Task bleibt wartend, wenn kein passender Mediator vorhanden ist")
    void testTaskStaysWaitingIfNoMediatorMatches() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        SudTaskMgr mgr = new SudTaskMgr(
            ThreadPoolTaskConfiguration.threadPool(
                this.getClass().getSimpleName() + "-",
                1,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );
        mgr.setMediators(List.of());

        var submissionResult = mgr.submit(testTask);

        assertThat(submissionResult.state(), is(ACCEPTED));
        assertThat(submissionResult.future().isDone(), is(false));
        assertThat(mgr.isEmpty(), is(false));
        assertThat(mgr.getTasksSnapshot().getWaitingTasks().size(), is(1));
        assertThat(mgr.getTasksSnapshot().getRunningTasks().size(), is(0));
    }

    @Test
    @DisplayName("Executor-Rejection hinterlässt keinen wartenden oder laufenden Task")
    void testExecutorRejectionCleansUpTask() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        when(executor.getCorePoolSize()).thenReturn(1);
        doThrow(new RejectedExecutionException("executor stopped"))
            .when(executor)
            .execute(any(Runnable.class));
        SudTaskMgr mgr = new SudTaskMgr(executor);
        mgr.setMediators(List.of(testTask));

        var submissionResult = mgr.submit(testTask);

        assertThat(submissionResult.future().isCompletedExceptionally(), is(true));
        assertThat(mgr.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Erneutes Submit derselben wartenden Instanz ersetzt deren Future nicht")
    void testResubmittingSameInstanceKeepsOriginalFuture()
        throws ExecutionException, InterruptedException, TimeoutException {
        TestTask blocker = new TestTask(1, VL_RECALC, HAMBURG);
        TestTask submittedTwice = new TestTask(2, VL_RECALC, BREMEN);
        var mgr = createMgr(1, List.of(blocker, submittedTwice));

        mgr.submit(blocker);
        var firstSubmission = mgr.submit(submittedTwice);
        var duplicateSubmission = mgr.submit(submittedTwice);

        assertThat(duplicateSubmission.state(), is(IGNORED_DUE_DUPLICATE));
        blocker.proceed();
        submittedTwice.proceed();
        assertThat(
            firstSubmission.future().get(TEST_TIMEOUT_MILLIS, MILLISECONDS),
            is(submittedTwice.getTaskId())
        );
    }

    @Test
    @DisplayName("IllegalStateException falls setMediators zweimal aufgerufen wird")
    void testSetMediatorsTwiceThrowsException() {
        TestTask testTask = new TestTask(1, VL_RECALC, BAYERN);
        SudTaskMgr mgr = new SudTaskMgr(
            ThreadPoolTaskConfiguration.threadPool(
                this.getClass().getSimpleName() + "-",
                1,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );

        List<SudTaskMediator<?>> testTasks = List.of(testTask);
        mgr.setMediators(testTasks);
        assertThrows(IllegalStateException.class, () -> mgr.setMediators(testTasks));
    }

    @Test
    @DisplayName(
        "isEmpty gibt true zurück wenn keine Tasks vorhanden, false sobald Tasks eingestellt sind"
    )
    void testIsEmpty() throws ExecutionException, InterruptedException, TimeoutException {
        TestTask task = new TestTask(1, VL_RECALC, HAMBURG);
        var mgr = createMgr(1, List.of(task));

        assertThat(mgr.isEmpty(), is(true));
        mgr.submit(task);
        assertThat(mgr.isEmpty(), is(false));
        task.proceed();
        waitForAll(List.of(task));
        assertThat(mgr.isEmpty(), is(true));
    }

    // --- Konflikt-Tests ---

    @Test
    @DisplayName("SchemeAgencyIdTask wird durch laufenden VlRecalcTask blockiert")
    void testSchemeAgencyIdTaskBlockedByRunningVlRecalc()
        throws ExecutionException, InterruptedException, TimeoutException {
        var vlRecalc = new VlRecalcTestTask(HAMBURG);
        var importTask = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-a");
        var mgr = createMgr(2, List.of(vlRecalc, importTask));

        mgr.submit(vlRecalc); // belegt Thread, echter VlRecalcTask läuft
        var importFuture = mgr.submit(importTask).future(); // muss warten

        vlRecalc.awaitStart();
        // importTask darf noch nicht gestartet sein:
        assertThat(importTask.wasCalled(), is(false));

        // VlRecalcTask abschließen → importTask darf jetzt starten:
        vlRecalc.proceed();
        importTask.proceed();
        waitForFutures(vlRecalc.getFuture(), importFuture);

        assertThat(importTask.wasCalled(), is(true));
        // importTask hat erst nach vlRecalc gestartet:
        assertThat(vlRecalc.getTerminationTime() <= importTask.getStartTime(), is(true));
    }

    @Test
    @DisplayName("Zwei SchemeAgencyIdTasks desselben RS blockieren sich gegenseitig")
    void testSchemeAgencyIdTasksOfSameRsBlockEachOther()
        throws ExecutionException, InterruptedException, TimeoutException {
        var import1 = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-a");
        var import2 = new SchemeAgencyIdTestTask(2, IMPORT, "rs-a", "producer-a");
        var mgr = createMgr(2, List.of(import1, import2));

        mgr.submit(import1); // belegt Thread
        mgr.submit(import2); // gleiche schemeAgencyId → muss warten

        import1.awaitStart();
        // import2 darf noch nicht gestartet sein:
        assertThat(import2.wasCalled(), is(false));

        // import1 abschließen → import2 darf jetzt starten:
        import1.proceed();
        import2.proceed();
        waitForFutures(import1.getFuture(), import2.getFuture());

        assertThat(import2.wasCalled(), is(true));
        // import2 hat erst nach import1 gestartet:
        assertThat(import1.getTerminationTime() <= import2.getStartTime(), is(true));
    }

    @Test
    @DisplayName("Zwei SchemeAgencyIdTasks unterschiedlicher RS laufen parallel")
    void testSchemeAgencyIdTasksOfDifferentRsRunInParallel()
        throws ExecutionException, InterruptedException, TimeoutException {
        var importA = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-a");
        var importB = new SchemeAgencyIdTestTask(2, IMPORT, "rs-b", "producer-b");
        var mgr = createMgr(2, List.of(importA, importB));

        mgr.submit(importA);
        mgr.submit(importB);

        importA.awaitStart();
        importB.awaitStart();
        assertThat(importA.wasCalled(), is(true));
        assertThat(importB.wasCalled(), is(true));

        importA.proceed();
        importB.proceed();
        waitForFutures(importA.getFuture(), importB.getFuture());
    }

    @Test
    @DisplayName("VlRecalcTask wird durch laufenden SchemeAgencyIdTask blockiert")
    void testVlRecalcBlockedByRunningSchemeAgencyIdTask()
        throws ExecutionException, InterruptedException, TimeoutException {
        var importTask = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-a");
        var vlRecalc = new VlRecalcTestTask(HAMBURG);
        var mgr = createMgr(2, List.of(importTask, vlRecalc));

        var importFuture = mgr.submit(importTask).future(); // belegt Thread
        mgr.submit(vlRecalc); // echter SchemeAgencyIdTask läuft → VlRecalcTask muss warten

        importTask.awaitStart();
        // vlRecalc darf noch nicht gestartet sein:
        assertThat(vlRecalc.wasCalled(), is(false));

        // importTask abschließen → vlRecalc darf jetzt starten:
        importTask.proceed();
        vlRecalc.proceed();
        waitForFutures(importFuture, vlRecalc.getFuture());

        assertThat(vlRecalc.wasCalled(), is(true));
        assertThat(importTask.getTerminationTime() <= vlRecalc.getStartTime(), is(true));
    }

    @Test
    @DisplayName("Zwei VlRecalcTasks unterschiedlicher Bundesländer blockieren sich nicht")
    void testTwoVlRecalcTasksRunInParallel()
        throws ExecutionException, InterruptedException, TimeoutException {
        var recalcHamburg = new VlRecalcTestTask(HAMBURG);
        var recalcBremen = new VlRecalcTestTask(BREMEN);
        var mgr = createMgr(2, List.of(recalcHamburg, recalcBremen));

        var futureHamburg = mgr.submit(recalcHamburg).future();
        var futureBremen = mgr.submit(recalcBremen).future();

        recalcHamburg.awaitStart();
        recalcBremen.awaitStart();
        assertThat(recalcHamburg.wasCalled(), is(true));
        assertThat(recalcBremen.wasCalled(), is(true));

        recalcHamburg.proceed();
        recalcBremen.proceed();
        waitForFutures(futureHamburg, futureBremen);
    }

    @Test
    @DisplayName("Nach Abschluss eines blockierenden Tasks werden wartende Tasks nachgezogen")
    void testBlockedTaskStartsAfterBlockerCompletes()
        throws ExecutionException, InterruptedException, TimeoutException {
        // Unterschiedliche producer, damit die Tasks nicht als Duplikate erkannt werden:
        var import1 = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-1");
        var import2 = new SchemeAgencyIdTestTask(2, IMPORT, "rs-a", "producer-2");
        var import3 = new SchemeAgencyIdTestTask(3, IMPORT, "rs-a", "producer-3");
        var mgr = createMgr(1, List.of(import1, import2, import3));

        var future1 = mgr.submit(import1).future();
        var future2 = mgr.submit(import2).future();
        var future3 = mgr.submit(import3).future();

        import1.awaitStart();
        // Nur import1 läuft; import2 und import3 warten:
        assertThat(import2.wasCalled(), is(false));
        assertThat(import3.wasCalled(), is(false));

        import1.proceed();
        import2.awaitStart();
        assertThat(import2.wasCalled(), is(true));
        assertThat(import3.wasCalled(), is(false));

        import2.proceed();
        import3.proceed();
        waitForFutures(future1, future2, future3);
        assertThat(import3.wasCalled(), is(true));
    }

    @Test
    @DisplayName(
        "Round-Robin: Ein bereits aufgestauter SchemeAgencyIdTask-Rückstau einer RS blockiert nicht dauerhaft eine später eingetroffene, leisere RS"
    )
    void testSchemeAgencyIdFairnessAcrossDifferentRs()
        throws ExecutionException, InterruptedException, TimeoutException {
        // rs-a hat bereits einen großen Rückstau, bevor rs-b überhaupt einen Task einstellt:
        var importA0 = new SchemeAgencyIdTestTask(1, IMPORT, "rs-a", "producer-1");
        var importA1 = new SchemeAgencyIdTestTask(2, IMPORT, "rs-a", "producer-2");
        var importA2 = new SchemeAgencyIdTestTask(3, IMPORT, "rs-a", "producer-3");
        var importB0 = new SchemeAgencyIdTestTask(4, IMPORT, "rs-b", "producer-b");
        var allTasks = List.of(importA0, importA1, importA2, importB0);
        // Nur ein Worker-Thread, damit die Verarbeitung strikt sequentiell ist:
        var mgr = createMgr(1, allTasks);

        mgr.submit(importA0); // belegt den einzigen Worker-Thread
        mgr.submit(importA1); // wartet (gleiche RS)
        mgr.submit(importA2); // wartet (gleiche RS)
        importA0.awaitStart();

        mgr.submit(importB0); // rs-b trifft erst jetzt ein, nachdem rs-a schon 3 Tasks eingestellt hat

        // importA0 abschließen → als nächstes darf (noch) importA1 starten, da rs-b zu diesem
        // Zeitpunkt noch nicht in der Rotation registriert war, als rs-a den Worker-Thread belegte:
        importA0.proceed();
        importA1.awaitStart();
        assertThat(importB0.wasCalled(), is(false));
        assertThat(importA2.wasCalled(), is(false));

        // importA1 abschließen → jetzt ist rs-b in der Rotation an der Reihe, NICHT importA2:
        importA1.proceed();
        importB0.awaitStart();
        assertThat(
            "importB0 muss vor importA2 starten (Round-Robin), nicht erst nach dem gesamten rs-a-Rückstau",
            importA2.wasCalled(),
            is(false)
        );

        importB0.proceed();
        importA2.proceed();
        waitForFutures(
            importA0.getFuture(),
            importA1.getFuture(),
            importB0.getFuture(),
            importA2.getFuture()
        );
        assertThat(importA2.wasCalled(), is(true));
    }

    private List<Long> waitForAll(List<TestTask> testTasks)
        throws ExecutionException, InterruptedException, TimeoutException {
        List<CompletableFuture<Long>> futures = testTasks
            .stream()
            .map(TestTask::getFuture)
            .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(
            TEST_TIMEOUT_MILLIS,
            MILLISECONDS
        );
        List<Long> ret = new ArrayList<>();
        for (var f : futures) {
            ret.add(f.get());
        }
        return ret;
    }

    private SudTaskMgr createMgr(int workerThreads, List<? extends SudTaskMediator<?>> mediators) {
        SudTaskMgr mgr = new SudTaskMgr(
            ThreadPoolTaskConfiguration.threadPool(
                this.getClass().getSimpleName() + "-",
                workerThreads,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        );
        mgr.setMediators(new ArrayList<>(mediators));
        return mgr;
    }

    /** Wartet auf eine beliebige Menge von {@link CompletableFuture}s. */
    private void waitForFutures(CompletableFuture<?>... futures)
        throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture.allOf(futures).get(TEST_TIMEOUT_MILLIS, MILLISECONDS);
    }

    /**
     * Task, der zum Testen verwendet wird. Features:
     * <ul>
     * <li>Hält in der Bearbeitung an, bis "proceed" aufgerufen wird</li>
     * <li>Sleept und simuliert damit eine geringe Ausführungszeit</li>
     * <li> Protokolliert seine Ausführungszeit um im TestCode untersuchen zu
     * können, ob Tasks aufgerufen wurden und in welcher Reihenfolge etc.</li>
     * <li>Kann optional eine Exception werfen</li>
     * <li>Ist sein eigener Mediator.
     * Das ist ein Trick, damit jeder Task auch zur Ausführung kommt. Im Produktivbetrieb werden die
     * Mediatoren dann die echte Arbeit (Callable) bekanntgeben.</li>
     * </ul>
     */
    private static class TestTask
        extends BundeslandTask<Long>
        implements Supplier<Long>, SudTaskMediator<Long>
    {

        @Getter
        private final long taskId;

        private final CountDownLatch proceedLatch = new CountDownLatch(1);
        private final long sleepMillis;

        @Getter
        private long startTime;

        @Getter
        private long terminationTime;

        private Optional<Supplier<RuntimeException>> exceptionSupplier = Optional.empty();

        private TestTask(
            long taskId,
            SudTaskType taskType,
            Bundeslaender bundesland,
            long sleepMillis
        ) {
            super(taskType, bundesland);
            this.taskId = taskId;
            this.sleepMillis = sleepMillis;
        }

        private TestTask(long taskId, SudTaskType taskType, Bundeslaender bundesland) {
            this(taskId, taskType, bundesland, 20);
        }

        @Override
        public Class<Long> getFutureType() {
            return Long.class;
        }

        public void proceed() {
            proceedLatch.countDown();
        }

        public boolean wasCalled() {
            return startTime > 0;
        }

        public void throwWhenCalled(Supplier<RuntimeException> exceptionSupplier) {
            this.exceptionSupplier = Optional.of(exceptionSupplier);
        }

        @Override
        public Supplier<Long> mediate(SudTask<Long> task) {
            if (task == this) {
                return this;
            }
            return null;
        }

        @Override
        public Class<Long> getMediatorType() {
            return Long.class;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this.getClass().getSimpleName())
                .add("taskId", taskId)
                .add("taskType", taskType)
                .add("bundesland", bundesland)
                .add("start", startTime)
                .add("termination", terminationTime)
                .toString();
        }

        @Override
        public Long get() {
            try {
                startTime = System.currentTimeMillis();
                proceedLatch.await();
                exceptionSupplier.ifPresent(sup -> {
                    throw sup.get();
                });
                ThreadUtils.sleep(Duration.ofMillis(sleepMillis));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                terminationTime = System.currentTimeMillis();
            }
            return taskId;
        }
    }

    /**
     * Analog zu {@link TestTask}, aber als {@link SchemeAgencyIdTask} – wird für Tests der
     * RS-bezogenen Konfliktlogik benötigt.
     */
    private static class SchemeAgencyIdTestTask
        extends SchemeAgencyIdTask<Long>
        implements Supplier<Long>, SudTaskMediator<Long>
    {

        @Getter
        private final long taskId;

        private final CountDownLatch proceedLatch = new CountDownLatch(1);
        private final CountDownLatch startLatch = new CountDownLatch(1);

        @Getter
        private long startTime;

        @Getter
        private long terminationTime;

        private SchemeAgencyIdTestTask(
            long taskId,
            SudTaskType taskType,
            String schemeAgencyId,
            String producer
        ) {
            super(taskType, schemeAgencyId, producer);
            this.taskId = taskId;
        }

        @Override
        public Class<Long> getFutureType() {
            return Long.class;
        }

        public void proceed() {
            proceedLatch.countDown();
        }

        public boolean wasCalled() {
            return startTime > 0;
        }

        public void awaitStart() throws InterruptedException, TimeoutException {
            boolean started = startLatch.await(TEST_TIMEOUT_MILLIS, MILLISECONDS);
            if (!started) {
                throw new TimeoutException("Task wurde nicht rechtzeitig gestartet: " + this);
            }
        }

        @Override
        public Supplier<Long> mediate(SudTask<Long> task) {
            if (task == this) {
                return this;
            }
            return null;
        }

        @Override
        public Class<Long> getMediatorType() {
            return Long.class;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this.getClass().getSimpleName())
                .add("taskId", taskId)
                .add("taskType", taskType)
                .add("schemeAgencyId", schemeAgencyId)
                .toString();
        }

        @Override
        public Long get() {
            try {
                startTime = System.currentTimeMillis();
                startLatch.countDown();
                proceedLatch.await();
                ThreadUtils.sleep(Duration.ofMillis(20));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                terminationTime = System.currentTimeMillis();
            }
            return taskId;
        }
    }

    /**
     * Steuerbarer {@link VlRecalcTask}, der tatsächlich {@code instanceof VlRecalcTask} ist.
     * Wird für Konflikt-Tests benötigt, da {@link de.dataport.suchdienst.taskmgr.service.SudTaskMgr}
     * explizit per {@code instanceof VlRecalcTask} prüft.
     */
    private static class VlRecalcTestTask
        extends VlRecalcTask
        implements Supplier<Void>, SudTaskMediator<Void>
    {

        private final CountDownLatch proceedLatch = new CountDownLatch(1);
        private final CountDownLatch startLatch = new CountDownLatch(1);

        @Getter
        private long startTime;

        @Getter
        private long terminationTime;

        private VlRecalcTestTask(Bundeslaender bundesland) {
            super(bundesland);
        }

        public void proceed() {
            proceedLatch.countDown();
        }

        public boolean wasCalled() {
            return startTime > 0;
        }

        public void awaitStart() throws InterruptedException, TimeoutException {
            boolean started = startLatch.await(TEST_TIMEOUT_MILLIS, MILLISECONDS);
            if (!started) {
                throw new TimeoutException("Task wurde nicht rechtzeitig gestartet: " + this);
            }
        }

        @Override
        public Supplier<Void> mediate(SudTask<Void> task) {
            if (task == this) {
                return this;
            }
            return null;
        }

        @Override
        public Class<Void> getMediatorType() {
            return Void.class;
        }

        @Override
        public Void get() {
            try {
                startTime = System.currentTimeMillis();
                startLatch.countDown();
                proceedLatch.await();
                ThreadUtils.sleep(Duration.ofMillis(20));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                terminationTime = System.currentTimeMillis();
            }
            return null;
        }
    }
}
