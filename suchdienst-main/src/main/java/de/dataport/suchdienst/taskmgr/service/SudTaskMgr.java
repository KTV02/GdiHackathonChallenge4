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
package de.dataport.suchdienst.taskmgr.service;

import static de.dataport.suchdienst.error.model.ErrorCodeEnum.UNKNOWN_ERROR;
import static de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResultState.ACCEPTED;
import static de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResultState.IGNORED_DUE_DUPLICATE;

import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.taskmgr.exception.SudTaskException;
import de.dataport.suchdienst.taskmgr.model.SchemeAgencyIdTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskHandle;
import de.dataport.suchdienst.taskmgr.model.SudTaskMediator;
import de.dataport.suchdienst.taskmgr.model.SudTaskSubmissionResult;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.model.out.SudTasksSnapshot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Zentraler Task-Manager für die koordinierte Abarbeitung asynchroner Operationen.
 *
 * <p>Diese Komponente verwaltet die Ausführung von {@link SudTask}s durch:
 * <ul>
 *   <li>Pufferung eingehender Tasks in einer prioritätsbasierten Warteschlange</li>
 *   <li>Konfliktbehandlung zwischen Tasks zum Schutz der Index-Datenintegrität</li>
 *   <li>Ressourcenmanagement über einen konfigurierbaren ThreadPool</li>
 *   <li>Überwachung und Logging des Task-Status</li>
 * </ul>
 *
 * <h2>Konfliktregeln</h2>
 * <p>Nicht alle Task-Kombinationen dürfen parallel laufen. Die folgende Tabelle zeigt,
 * welcher wartende Task durch welchen laufenden Task blockiert wird:
 *
 * <table border="1">
 *   <caption>Blockierungsmatrix</caption>
 *   <tr><th>Wartender Task</th><th>Blockiert durch (laufend)</th><th>Grund</th></tr>
 *   <tr>
 *     <td>{@link SchemeAgencyIdTask} (RS-A)</td>
 *     <td>beliebiger {@link VlRecalcTask}</td>
 *     <td>Ein Recalc baut den Index komplett neu auf – parallele Einzel-Änderungen würden überschrieben</td>
 *   </tr>
 *   <tr>
 *     <td>{@link SchemeAgencyIdTask} (RS-A)</td>
 *     <td>{@link SchemeAgencyIdTask} mit derselben SchemeAgencyId (RS-A)</td>
 *     <td>Dasselbe Redaktionssystem darf nicht parallel bearbeitet werden (nicht-deterministisches Ergebnis)</td>
 *   </tr>
 *   <tr>
 *     <td>{@link VlRecalcTask}</td>
 *     <td>beliebiger {@link SchemeAgencyIdTask} (egal welches RS)</td>
 *     <td>Laufende Einzel-Importe/-Updates würden den Recalc-Index mit veralteten Daten mischen</td>
 *   </tr>
 *   <tr>
 *     <td>{@link VlRecalcTask}</td>
 *     <td>anderer {@link VlRecalcTask}</td>
 *     <td>Kein Konflikt – verschiedene Bundesländer arbeiten auf getrennten Indizes</td>
 *   </tr>
 * </table>
 *
 * <h2>Architektur</h2>
 * <p>Das Mediator-Pattern entkoppelt die Konflikt- und Scheduling-Logik von der konkreten
 * Task-Ausführung. Neue Task-Typen können hinzugefügt werden, ohne den TaskMgr selbst zu ändern.
 *
 * <h2>Faire Verarbeitung von SchemeAgencyId-Tasks</h2>
 * <p>Innerhalb eines Task-Typs werden wartende {@link SchemeAgencyIdTask}s (z. B. {@code ImportTask},
 * {@code VlUpdateTask}) nicht mehr in reiner Einfüge-Reihenfolge (FIFO) über alle SchemeAgencyIds
 * hinweg abgearbeitet, sondern per Round-Robin je SchemeAgencyId (siehe {@link SudTaskQueue}). So
 * blockieren viele wartende Tasks einer SchemeAgencyId nicht mehr die zeitnahe Verarbeitung weniger
 * Tasks einer anderen SchemeAgencyId. Innerhalb einer SchemeAgencyId bleibt die Verarbeitung
 * weiterhin strikt FIFO. Task-Typen ohne SchemeAgencyId-Bezug (z. B. {@link VlRecalcTask}) sind von
 * dieser Änderung nicht betroffen und bleiben reines FIFO.
 *
 * @see SudTask
 * @see SudTaskMediator
 * @see SudTaskQueue
 * @since PV-9812
 */
@Slf4j
@Service
public class SudTaskMgr {

    // Die Liste der Mediatoren ist dafür zuständig, vor der Ausführung eines Tasks den auszuführenden Code in Form eines Callable zu bestimmen.
    // Mit der Mediator-Liste wird hier zusätzliche Flexibilität durch Indirektion eingeführt, sodass zukünftig leicht weitere TaskTypes hinzugenommen werden können.
    // Auch wird das Testen der Komponente dadurch vereinfacht (es können Dummy-Operationen definiert werden).
    private final List<SudTaskMediator<?>> mediators = new ArrayList<>();

    /** Interne Warteschlange für Task-Verwaltung mit Prioritäts- und Konfliktbehandlung. */
    private final SudTaskQueue queue = new SudTaskQueue();

    /** Thread-Pool für die asynchrone Task-Ausführung. */
    private final ThreadPoolTaskExecutor executor;

    private static final int LOG_CONTENT_DELAY_MILLIS = 5 * 60 * 1000; // 5 Minuten
    private static final int LOG_CONTENT_DELAY_COUNT = 100;

    /** Thread-sichere Zähler für kontrolliertes Logging. */
    private final AtomicLong logContentCallCount = new AtomicLong(0);
    private final AtomicLong logContentLastLogMillis = new AtomicLong(0);

    /** Konstruktor für Dependency Injection des Thread-Pool Executors. */
    public SudTaskMgr(@Qualifier("taskmgrExecutor") ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /** Beauftragt einen neuen Task zur asynchronen Ausführung. */
    public <T> SudTaskSubmissionResult<T> submit(SudTask<T> task) {
        synchronized (queue) {
            var result = submitToBuffer(task);
            processWaitingTasks();
            return result;
        }
    }

    /**
     * Konfiguriert die Task-Mediatoren einmalig während der Initialisierung, bevor Tasks eingereicht
     * werden.
     *
     * <p>Für jeden konkreten {@link SudTask}-Typ muss genau ein registrierter Mediator einen
     * Supplier liefern. Fehlt diese Zuordnung, bleibt der Task wartend; liefern mehrere Mediatoren
     * einen Supplier, wird die Verarbeitung mit einer {@link IllegalStateException} abgebrochen.
     */
    public void setMediators(List<SudTaskMediator<?>> mediators) {
        synchronized (queue) {
            if (!this.mediators.isEmpty()) {
                throw new IllegalStateException("Mediators dürfen nur einmal gesetzt werden");
            }
            this.mediators.addAll(mediators);
            log.debug("Mediators gesetzt: {}", mediators);
        }
    }

    /** Übergibt wartende Tasks zur Ausführung unter Berücksichtigung von Ressourcen und Konflikten. */
    private void processWaitingTasks() {
        if (executorIsBusy()) {
            return;
        }

        synchronized (queue) {
            int availableSlots = calculateAvailableSlots();
            executeTasksFromQueue(availableSlots);
            logTaskCounts();
        }
        logContent();
    }

    /** Berechnet die Anzahl verfügbarer Slots im Thread-Pool. */
    private int calculateAvailableSlots() {
        return Math.max(0, executor.getCorePoolSize() - queue.sizeRunningTasks());
    }

    /** Verarbeitet Tasks aus der Warteschlange für die Ausführung. */
    private void executeTasksFromQueue(int availableSlots) {
        int executedTasksCount = 0;
        Iterator<SudTaskHandle> taskIterator = queue.iteratorWaitingTasks();

        while (taskIterator.hasNext() && executedTasksCount < availableSlots) {
            SudTask<?> task = (SudTask<?>) taskIterator.next();

            if (blockingTaskIsRunning(task)) {
                continue; // Nicht zwei Tasks zum selben RS gleichzeitig ausführen
            }

            if (tryStartTask(task, taskIterator)) {
                executedTasksCount++;
            }
        }
    }

    /** Versucht einen Task zu starten und entfernt ihn vor der Executor-Übergabe aus der Queue. */
    private boolean tryStartTask(SudTask<?> task, Iterator<SudTaskHandle> taskIterator) {
        return tryStartTypedTask(task, taskIterator);
    }

    /** Bindet den Ergebnistyp von Task und Supplier für die typsichere Executor-Übergabe. */
    private <T> boolean tryStartTypedTask(SudTask<T> task, Iterator<SudTaskHandle> taskIterator) {
        Supplier<T> taskSupplier = determineSupplier(task);
        if (taskSupplier == null) {
            log.warn("Ignoriere nicht ausführbaren {}", task);
            return false;
        }

        taskIterator.remove();
        return submitToExecutor(task, taskSupplier);
    }

    /** Protokolliert aktuelle Task-Anzahlen für Debug-Zwecke. */
    private void logTaskCounts() {
        if (log.isDebugEnabled()) {
            log.debug(
                "Wartende Tasks: {}, laufende Tasks: {}",
                queue.sizeWaitingTasks(),
                queue.sizeRunningTasks()
            );
        }
    }

    /** Protokolliert regelmäßig den aktuellen Task-Status für Monitoring-Zwecke. */
    private void logContent() {
        if (!log.isInfoEnabled() || shouldSkipLogging()) {
            return;
        }

        log.info(queue.getTasksSnapshot().toString());
        logContentLastLogMillis.set(System.currentTimeMillis());
    }

    /** Prüft, ob das aktuelle Logging übersprungen werden sollte basierend auf Frequenz-Limits. */
    private boolean shouldSkipLogging() {
        long currentCallCount = logContentCallCount.incrementAndGet();
        long timeSinceLastLog = System.currentTimeMillis() - logContentLastLogMillis.get();

        return (
            currentCallCount % LOG_CONTENT_DELAY_COUNT != 0 &&
            timeSinceLastLog <= LOG_CONTENT_DELAY_MILLIS
        );
    }

    /**
     * Prüft, ob aktuell ein laufender Task den gegebenen wartenden Task blockiert.
     *
     * <p>Die Konfliktregeln sind task-typ-abhängig:
     * <ul>
     *   <li>{@link SchemeAgencyIdTask}: blockiert durch jeden laufenden {@link VlRecalcTask}
     *       (Index-Rebuild würde Einzel-Änderungen überschreiben) sowie durch jeden laufenden
     *       {@link SchemeAgencyIdTask} desselben Redaktionssystems (nicht-deterministische
     *       Ergebnisse bei paralleler Verarbeitung desselben RS).</li>
     *   <li>{@link VlRecalcTask}: blockiert durch jeden laufenden {@link SchemeAgencyIdTask}
     *       (laufende Schreiboperationen würden den neu aufgebauten Index verfälschen).</li>
     *   <li>Alle anderen Task-Typen: kein Konflikt.</li>
     * </ul>
     *
     * @param task der wartende Task, der ggf. noch nicht gestartet werden darf
     * @return {@code true} wenn der Task warten muss, {@code false} wenn er gestartet werden darf
     */
    private boolean blockingTaskIsRunning(SudTask<?> task) {
        synchronized (queue) {
            if (task instanceof SchemeAgencyIdTask<?> schTask) {
                // Einzel-Importe/-Updates dürfen nicht parallel zu einem laufenden Recalc
                // oder einem anderen Task desselben RS starten
                return hasConflictingSchemeAgencyTask(schTask);
            } else if (task instanceof VlRecalcTask) {
                // Ein Recalc darf erst starten, wenn alle laufenden Schreiboperationen abgeschlossen sind
                return hasRunningSchemeAgencyTasks();
            }
        }
        return false;
    }

    /** Prüft, ob ein laufender Task den gegebenen {@link SchemeAgencyIdTask} blockiert (VlRecalcTask ODER gleiche SchemeAgencyId). */
    private boolean hasConflictingSchemeAgencyTask(SchemeAgencyIdTask<?> schTask) {
        Predicate<SudTask<?>> conflictCondition = createConflictPredicate(schTask);
        return queue
            .getAllRunningTasks()
            .stream()
            .map(t -> (SudTask<?>) t)
            .anyMatch(conflictCondition);
    }

    /** Prüft, ob aktuell irgendein {@link SchemeAgencyIdTask} läuft (RS-unabhängig). */
    private boolean hasRunningSchemeAgencyTasks() {
        return queue.getAllRunningTasks().stream().anyMatch(SchemeAgencyIdTask.class::isInstance);
    }

    /**
     * Erstellt das Konflikt-Prädikat für einen wartenden {@link SchemeAgencyIdTask}.
     *
     * <p>Ein laufender Task gilt als blockierend wenn:
     * <ol>
     *   <li>Er ein {@link VlRecalcTask} ist – ein Index-Rebuild darf nicht mit laufenden
     *       Einzel-Schreiboperationen koexistieren, da er den Index komplett neu aufbaut.</li>
     *   <li>Er ein {@link SchemeAgencyIdTask} desselben RS ist – parallele Verarbeitung
     *       desselben Redaktionssystems führt zu nicht-deterministischen Ergebnissen.</li>
     * </ol>
     */
    private static Predicate<SudTask<?>> createConflictPredicate(SchemeAgencyIdTask<?> schTask) {
        String schemeAgencyId = schTask.getSchemeAgencyId();

        Predicate<SudTask<?>> isRecalculationTask = VlRecalcTask.class::isInstance; // jeder Recalc blockiert, unabhängig vom Bundesland
        Predicate<SudTask<?>> hasSameAgencyId = task ->
            task instanceof SchemeAgencyIdTask<?> schemeTask &&
            schemeAgencyId.equals(schemeTask.getSchemeAgencyId()); // nur Tasks desselben RS blockieren

        return isRecalculationTask.or(hasSameAgencyId);
    }

    /** Ermittelt den ausführbaren Code (Supplier) für einen Task über die Mediator-Liste. */
    private <T> Supplier<T> determineSupplier(SudTask<T> task) {
        List<Supplier<T>> suppliers = findMatchingSuppliers(task);

        return switch (suppliers.size()) {
            case 1 -> suppliers.getFirst();
            case 0 -> null;
            default -> throw new IllegalStateException(
                "Der Task %s kann nicht ausgeführt werden, da sein Mediator nicht eindeutig ist".formatted(
                    task
                )
            );
        };
    }

    /** Findet alle passenden Supplier für einen Task basierend auf den Mediatoren. */
    @SuppressWarnings("unchecked")
    private <T> List<Supplier<T>> findMatchingSuppliers(SudTask<T> task) {
        return mediators
            .stream()
            .filter(mediator -> mediator.getMediatorType().equals(task.getFutureType()))
            .map(mediator -> ((SudTaskMediator<T>) mediator).mediate(task))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Übergibt einen bereits aus der Warteschlange entfernten Task an den Executor. Bei Ablehnung
     * wird der Task aus den laufenden Tasks entfernt und sein Future exceptional abgeschlossen.
     */
    private <T> boolean submitToExecutor(SudTask<T> task, Supplier<T> taskSupplier) {
        log.debug("Übergebe {} zur Ausführung", task);
        queue.addRunningTask(task);
        try {
            task.getFuture().completeAsync(() -> executeTask(task, taskSupplier), executor);
            return true;
        } catch (RuntimeException exception) {
            queue.removeRunningTask(task);
            task.getFuture().completeExceptionally(exception);
            log.warn("Executor hat {} nicht angenommen", task, exception);
            return false;
        }
    }

    /** Führt einen Task aus und behandelt dabei alle auftretenden Exceptions. */
    private <T> T executeTask(SudTask<T> task, Supplier<T> taskSupplier) {
        try {
            log.debug("Starte Task {}", task);
            T result = taskSupplier.get();
            log.debug("Erfolgreich terminiert: {}, Resultat: {}", task, result);
            return result;
        } catch (Throwable throwable) {
            SudTaskException exception = createTaskException(task, throwable);
            log.warn(exception.getMessage(), exception);
            throw exception;
        } finally {
            queue.removeRunningTask(task);
            processWaitingTasks();
        }
    }

    /** Erstellt eine spezifische SudTaskException für fehlgeschlagene Tasks. */
    private SudTaskException createTaskException(SudTask<?> task, Throwable throwable) {
        Throwable rootCause = extractRootCause(throwable);
        ErrorCodeEnum errorCode = determineErrorCode(rootCause);
        String errorMessage = "Task gescheitert: %s: %s".formatted(task, rootCause.getMessage());
        return new SudTaskException(errorCode, errorMessage, throwable);
    }

    /** Extrahiert die Root-Cause aus CompletionExceptions. */
    private static Throwable extractRootCause(Throwable throwable) {
        return throwable instanceof CompletionException completionException
            ? completionException.getCause()
            : throwable;
    }

    /** Ermittelt den ErrorCode aus SuchdienstExceptions oder verwendet UNKNOWN_ERROR. */
    private static ErrorCodeEnum determineErrorCode(Throwable throwable) {
        return throwable instanceof SuchdienstException suchdienstException
            ? suchdienstException.getCode()
            : UNKNOWN_ERROR;
    }

    /** Blockiert bis alle Tasks, die das Prädikat erfüllen, beendet sind. */
    public void waitForTasks(Predicate<SudTask<?>> predicate) {
        CompletableFuture<?>[] taskFutures = allTasks()
            .stream()
            .map(t -> (SudTask<?>) t)
            .filter(predicate)
            .map(SudTask::getFuture)
            .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(taskFutures).join();
        } catch (Exception e) {
            log.warn("Fehler beim Warten auf Task", e);
        }
    }

    /** Fügt einen Task zur internen Warteschlange hinzu mit Duplikatsprüfung. */
    private <T> SudTaskSubmissionResult<T> submitToBuffer(SudTask<T> task) {
        boolean wasAdded = queue.addWaitingTask(task);

        if (wasAdded) {
            CompletableFuture<T> future = new CompletableFuture<>();
            task.setFuture(future);
            log.debug("Task submitted: {}", task);
            return new SudTaskSubmissionResult<>(ACCEPTED, future);
        } else {
            log.debug(
                "Ignoriere die Beauftragung von {}, da bereits ein gleichartiger Task beauftragt ist",
                task
            );
            return new SudTaskSubmissionResult<>(IGNORED_DUE_DUPLICATE, null);
        }
    }

    /** Prüft ob der ThreadPool-Executor aktuell voll ausgelastet ist. */
    private boolean executorIsBusy() {
        return queue.sizeRunningTasks() >= executor.getCorePoolSize();
    }

    /** Liefert die Gesamtmenge aller Tasks (wartend + laufend). */
    private Set<SudTaskHandle> allTasks() {
        return queue.getAllTasks();
    }

    /** Prüft ob der Task-Manager aktuell leer ist. */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /** Erstellt einen Snapshot des aktuellen Task-Status für Monitoring. */
    public SudTasksSnapshot getTasksSnapshot() {
        return queue.getTasksSnapshot();
    }
}
