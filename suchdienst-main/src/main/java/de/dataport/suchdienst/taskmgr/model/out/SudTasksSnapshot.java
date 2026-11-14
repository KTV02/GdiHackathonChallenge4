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
package de.dataport.suchdienst.taskmgr.model.out;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ein Abbild der im Mgr befindlichen Tasks, kann z.b. über eine API veröffentlicht werden.
 */
@Getter
@AllArgsConstructor
public class SudTasksSnapshot {

    // Diese Tasks laufen gerade (sind im ThreadPoolExecutor)
    private List<SudTaskOutModel> runningTasks;

    // Diese Tasks sind in der Warteliste, hier ist Priorität etc. berücksichtigt, also die zukünftige Ausführungsreihenfolge reflektiert.
    private List<SudTaskOutModel> waitingTasks;

    /**
     * Liefert eine Zusammenfassung über die Gesamtzahl der laufenden und wartenden Tasks und eine Aufschlüsselung der wartenden
     * nach TaskTyp.
     */
    public String toString() {
        var runningTaskCnt = runningTasks.size();
        var waitingTaskCnt = waitingTasks.size();
        var logItems = new ArrayList<String>();
        logItems.add(
            String.format(
                "Tasks:%d (laufend:%d, wartend:%d)",
                runningTaskCnt + waitingTaskCnt,
                runningTaskCnt,
                waitingTaskCnt
            )
        );
        if (waitingTaskCnt > 0) {
            var stringBuilder = new StringBuilder("Wartende Tasks pro Typ: ");
            Map<SudTaskType, Long> taskTypeHistogram = waitingTasks
                .stream()
                .collect(groupingBy(SudTaskOutModel::getTaskType, counting()));
            taskTypeHistogram
                .keySet()
                .stream()
                .sorted(SudTaskType.COMPARE_BY_PRIORITY)
                .forEach(taskType ->
                    stringBuilder
                        .append(taskType)
                        .append(":")
                        .append(taskTypeHistogram.get(taskType))
                        .append(" ")
                );
            logItems.add(stringBuilder.toString());
        }
        return String.join("; ", logItems);
    }
}
