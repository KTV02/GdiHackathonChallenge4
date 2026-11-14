/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.backoffice.service.contributor;

import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_IMPORT;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_OD_REL_RECALC;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_RECALC;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_UPDATE;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.STABLE;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.WAITING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.taskmgr.model.ImportTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.taskmgr.model.out.SudTaskOutModel;
import de.dataport.suchdienst.taskmgr.model.out.SudTasksSnapshot;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.Bundeslaender;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-Tests für {@link TaskInfoContributor}, insbesondere die statische Methode
 * {@code determineProcessState} und das Zusammenspiel von collectData/deriveState.
 */
@ExtendWith(MockitoExtension.class)
class TaskInfoContributorTest {

    @Mock
    private SudTaskMgr sudTaskMgr;

    @InjectMocks
    private TaskInfoContributor contributor;

    // --- Tests für determineProcessState (statische Methode) ---

    @Test
    void determineProcessState_noRunningTasks_noWaitingWork_returnsStable() {
        assertEquals(STABLE, TaskInfoContributor.determineProcessState(null, false));
    }

    @Test
    void determineProcessState_noRunningTasks_withWaitingWork_returnsWaiting() {
        assertEquals(WAITING, TaskInfoContributor.determineProcessState(null, true));
    }

    @Test
    void determineProcessState_emptyRunningMap_noWaitingWork_returnsStable() {
        assertEquals(STABLE, TaskInfoContributor.determineProcessState(Map.of(), false));
    }

    @Test
    void determineProcessState_emptyRunningMap_withWaitingWork_returnsWaiting() {
        assertEquals(WAITING, TaskInfoContributor.determineProcessState(Map.of(), true));
    }

    @Test
    void determineProcessState_runningImportOnly_returnsRunningImport() {
        var running = Map.of(SudTaskType.IMPORT, 1L);
        assertEquals(RUNNING_IMPORT, TaskInfoContributor.determineProcessState(running, false));
    }

    @Test
    void determineProcessState_runningVlUpdate_returnsRunningVlUpdate() {
        var running = Map.of(SudTaskType.VL_UPDATE, 1L);
        assertEquals(RUNNING_VL_UPDATE, TaskInfoContributor.determineProcessState(running, false));
    }

    @Test
    void determineProcessState_runningVlRecalc_returnsRunningVlRecalc() {
        var running = Map.of(SudTaskType.VL_RECALC, 1L);
        assertEquals(RUNNING_VL_RECALC, TaskInfoContributor.determineProcessState(running, false));
    }

    @Test
    void determineProcessState_runningOdRelRecalc_returnsRunningOdRelRecalc() {
        var running = Map.of(SudTaskType.OD_RE_RECALC, 1L);
        assertEquals(
            RUNNING_OD_REL_RECALC,
            TaskInfoContributor.determineProcessState(running, false)
        );
    }

    @Test
    void determineProcessState_vlRecalcTakesPrecedenceOverVlUpdate() {
        var running = Map.of(SudTaskType.VL_RECALC, 1L, SudTaskType.VL_UPDATE, 1L);
        assertEquals(RUNNING_VL_RECALC, TaskInfoContributor.determineProcessState(running, true));
    }

    @Test
    void determineProcessState_vlUpdateTakesPrecedenceOverOdRelRecalc() {
        var running = Map.of(SudTaskType.VL_UPDATE, 1L, SudTaskType.OD_RE_RECALC, 1L);
        assertEquals(RUNNING_VL_UPDATE, TaskInfoContributor.determineProcessState(running, false));
    }

    @Test
    void determineProcessState_odRelRecalcTakesPrecedenceOverImport() {
        var running = Map.of(SudTaskType.OD_RE_RECALC, 1L, SudTaskType.IMPORT, 1L);
        assertEquals(
            RUNNING_OD_REL_RECALC,
            TaskInfoContributor.determineProcessState(running, false)
        );
    }

    // --- Integrative Tests für collectData + deriveState ---

    @Test
    void collectData_setsTaskCountsCorrectly() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        var waitingTasks = List.of(SudTaskOutModel.of(new ImportTask(shAgencyId, "SH", "req1")));
        var runningTasks = List.<SudTaskOutModel>of();
        Mockito.when(sudTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(runningTasks, waitingTasks)
        );

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        var b = builders.get(shAgencyId);
        assertEquals(1, b.importTaskCount());
        assertEquals(0, b.vlUpdateTaskCount());
        assertEquals(0, b.vlRecalcTaskCount());
        assertEquals(0, b.odRelRecalcTaskCount());
    }

    @Test
    void collectData_ignoresUnknownSchemeAgencyId() {
        String knownId = Bundeslaender.HAMBURG.getSchemeAgencyId();
        String unknownId = "UNKNOWN_AGENCY";
        var waitingTasks = List.of(
            SudTaskOutModel.of(new ImportTask(unknownId, "Unknown", "req1"))
        );
        Mockito.when(sudTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(List.of(), waitingTasks)
        );

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            knownId,
            DataOverviewOutModel.builder().fedState("02").name("Hamburg").schemeAgencyId(knownId)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);

        // Kein Crash, bekannter Builder bleibt unverändert
        assertEquals(0, builders.get(knownId).importTaskCount());
    }

    @Test
    void deriveState_stable_whenNoTasksAndNoQueue() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        Mockito.when(sudTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(List.of(), List.of())
        );

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            shAgencyId,
            DataOverviewOutModel.builder()
                .fedState("01")
                .name("Schleswig-Holstein")
                .schemeAgencyId(shAgencyId)
        );

        contributor.deriveState(builders);

        assertEquals(STABLE, builders.get(shAgencyId).build().processState());
    }

    @Test
    void deriveState_waiting_whenImportQueueCountPositive() {
        String shAgencyId = Bundeslaender.SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        Mockito.when(sudTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(List.of(), List.of())
        );

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        var builder = DataOverviewOutModel.builder()
            .fedState("01")
            .name("Schleswig-Holstein")
            .schemeAgencyId(shAgencyId);
        builder.importQueueCount(5);
        builders.put(shAgencyId, builder);

        contributor.deriveState(builders);

        assertEquals(WAITING, builders.get(shAgencyId).build().processState());
    }

    @Test
    void collectAndDerive_runningVlRecalc_setsCorrectState() {
        String beAgencyId = Bundeslaender.BERLIN.getSchemeAgencyId();
        var runningTasks = List.of(SudTaskOutModel.of(new VlRecalcTask(Bundeslaender.BERLIN)));
        var waitingTasks = List.of(
            SudTaskOutModel.of(
                new VlUpdateTask(
                    beAgencyId,
                    "Berlin",
                    Bundeslaender.BERLIN.getArsFederalstate(),
                    "lb1"
                )
            )
        );
        Mockito.when(sudTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(runningTasks, waitingTasks)
        );

        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        builders.put(
            beAgencyId,
            DataOverviewOutModel.builder().fedState("11").name("Berlin").schemeAgencyId(beAgencyId)
        );

        contributor.collectDataParallel(builders.keySet()).accept(builders);
        contributor.deriveState(builders);

        var result = builders.get(beAgencyId).build();
        assertEquals(1, result.vlRecalcTaskCount());
        assertEquals(1, result.vlUpdateTaskCount());
        assertEquals(RUNNING_VL_RECALC, result.processState());
    }
}
