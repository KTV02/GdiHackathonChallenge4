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
package de.dataport.suchdienst.backoffice.service.contributor;

import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_IMPORT;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_OD_REL_RECALC;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_RECALC;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_UPDATE;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.STABLE;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.WAITING;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.out.SudTaskOutModel;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Ermittelt Task-Zähler und den daraus abgeleiteten {@link DataOverviewOutModelProcessState}.
 * <p>
 * <b>Phase 1 ({@link #collectData})</b>: Schreibt Import-, VL- und OD-Task-Zähler
 * (waiting + running) in die Builder.
 * <p>
 * <b>Phase 2 ({@link #deriveState})</b>: Liest die in Phase 1 gesammelten Zähler
 * (inkl. {@code importQueueCount} aus {@link ImportQueueCountContributor}) und leitet
 * den {@link DataOverviewOutModelProcessState} ab.
 */
@Component
public class TaskInfoContributor implements DataOverviewContributor {

    private final SudTaskMgr sudTaskMgr;

    public TaskInfoContributor(SudTaskMgr sudTaskMgr) {
        this.sudTaskMgr = sudTaskMgr;
    }

    @Override
    public Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        var tasksSnapshot = sudTaskMgr.getTasksSnapshot();

        // Task-Zähler (waiting + running) pro SchemeAgencyId und TaskTyp
        Map<String, Map<SudTaskType, Long>> allTasksHistogram = Stream.concat(
            tasksSnapshot.getWaitingTasks().stream(),
            tasksSnapshot.getRunningTasks().stream()
        ).collect(
            groupingBy(
                SudTaskOutModel::getSchemeAgencyId,
                groupingBy(SudTaskOutModel::getTaskType, counting())
            )
        );

        return builders ->
            allTasksHistogram.forEach((schemeAgencyId, taskCounts) -> {
                var builder = builders.get(schemeAgencyId);
                if (builder != null) {
                    builder
                        .importTaskCount(taskCounts.getOrDefault(SudTaskType.IMPORT, 0L))
                        .vlUpdateTaskCount(taskCounts.getOrDefault(SudTaskType.VL_UPDATE, 0L))
                        .vlRecalcTaskCount(taskCounts.getOrDefault(SudTaskType.VL_RECALC, 0L))
                        .odRelRecalcTaskCount(
                            taskCounts.getOrDefault(SudTaskType.OD_RE_RECALC, 0L)
                        );
                }
            });
    }

    @Override
    public void deriveState(Map<String, DataOverviewOutModel.Builder> builders) {
        var tasksSnapshot = sudTaskMgr.getTasksSnapshot();

        // ProcessState aus den runningTasks ableiten
        Map<String, Map<SudTaskType, Long>> runningTasksHistogram = tasksSnapshot
            .getRunningTasks()
            .stream()
            .collect(
                groupingBy(
                    SudTaskOutModel::getSchemeAgencyId,
                    groupingBy(SudTaskOutModel::getTaskType, counting())
                )
            );

        builders.values().forEach(builder -> {
            var runningTasks = runningTasksHistogram.get(builder.schemeAgencyId());
            boolean hasWaitingWork =
                builder.importQueueCount() > 0 ||
                builder.importTaskCount() > 0 ||
                builder.vlUpdateTaskCount() > 0 ||
                builder.vlRecalcTaskCount() > 0 ||
                builder.odRelRecalcTaskCount() > 0;
            builder.processState(determineProcessState(runningTasks, hasWaitingWork));
        });
    }

    /**
     * Ermittelt den aggregierten Verarbeitungszustand anhand der laufenden Tasks und ob Arbeit wartet.
     */
    static DataOverviewOutModelProcessState determineProcessState(
        Map<SudTaskType, Long> runningTaskCounts,
        boolean hasWaitingWork
    ) {
        if (runningTaskCounts != null && !runningTaskCounts.isEmpty()) {
            if (runningTaskCounts.getOrDefault(SudTaskType.VL_RECALC, 0L) > 0) {
                return RUNNING_VL_RECALC;
            }
            if (runningTaskCounts.getOrDefault(SudTaskType.VL_UPDATE, 0L) > 0) {
                return RUNNING_VL_UPDATE;
            }
            if (runningTaskCounts.getOrDefault(SudTaskType.OD_RE_RECALC, 0L) > 0) {
                return RUNNING_OD_REL_RECALC;
            }
            return RUNNING_IMPORT;
        }
        return hasWaitingWork ? WAITING : STABLE;
    }
}
