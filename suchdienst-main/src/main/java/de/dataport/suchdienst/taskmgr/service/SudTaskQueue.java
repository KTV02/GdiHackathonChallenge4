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

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Iterators;
import de.dataport.suchdienst.taskmgr.model.SchemeAgencyIdTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskHandle;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.out.SudTaskOutModel;
import de.dataport.suchdienst.taskmgr.model.out.SudTasksSnapshot;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interne Warteschlange für prioritätsbasierte Task-Verwaltung mit Thread-Safety.
 *
 * <p>Diese Klasse implementiert:
 * <ul>
 *   <li>Prioritätsbasierte Sortierung nach {@link SudTaskType}</li>
 *   <li>Faire Round-Robin-Verteilung von {@link SchemeAgencyIdTask}s über die verschiedenen
 *       SchemeAgencyIds hinweg (siehe {@link TaskBucket}), damit viele Tasks einer SchemeAgencyId
 *       nicht die wenigen Tasks einer anderen SchemeAgencyId blockieren</li>
 *   <li>Duplikaterkennung über {@link Set}-Semantik</li>
 *   <li>Thread-sichere Zugriffe durch Synchronisation</li>
 *   <li>Getrennte Verwaltung wartender und laufender Tasks</li>
 * </ul>
 *
 * <p><strong>Thread-Safety:</strong> Alle Methoden sind synchronized und können
 * sicher aus mehreren Threads aufgerufen werden.
 */
class SudTaskQueue {

    /**
     * Wartende Tasks organisiert nach Priorität der Task-Typen.
     *
     * <p>Die äußere Map ist nach {@link SudTaskType#COMPARE_BY_PRIORITY} sortiert. Jeder Eintrag
     * ist ein {@link TaskBucket}, der innerhalb seines Typs entweder Round-Robin (für
     * {@link SchemeAgencyIdTask}s) oder einfaches FIFO (für alle anderen Tasks) anwendet.
     */
    private final Map<SudTaskType, TaskBucket> waitingTasksByType;

    /**
     * Aktuell laufende Tasks, die dem Executor übergeben wurden.
     *
     * <p>Diese Tasks können nicht mehr aus der Ausführung entfernt werden
     * und werden nur noch für Konfliktprüfungen und Monitoring verwendet.
     */
    private final Set<SudTask<?>> runningTasks = new HashSet<>();

    /**
     * Initialisiert die Queue mit prioritätssortierter Task-Typ-Map.
     */
    SudTaskQueue() {
        this.waitingTasksByType = initializeWaitingTasksMap();
    }

    /**
     * Erstellt eine nach Priorität sortierte, unveränderbare Map für Task-Typen.
     *
     * @return prioritätssortierte Map mit initialisierten Buckets
     */
    private static Map<SudTaskType, TaskBucket> initializeWaitingTasksMap() {
        SudTaskType[] taskTypes = SudTaskType.values();
        Arrays.sort(taskTypes, SudTaskType.COMPARE_BY_PRIORITY);

        Map<SudTaskType, TaskBucket> insertionOrderedMap = new LinkedHashMap<>();
        for (SudTaskType type : taskTypes) {
            insertionOrderedMap.put(type, new TaskBucket());
        }
        return Collections.unmodifiableMap(insertionOrderedMap);
    }

    /** @return Anzahl der aktuell laufenden Tasks */
    synchronized int sizeRunningTasks() {
        return runningTasks.size();
    }

    /**
     * Fügt einen Task zur Warteschlange hinzu.
     *
     * @param task der hinzuzufügende Task
     * @return true wenn der Task hinzugefügt wurde (kein Duplikat)
     */
    synchronized boolean addWaitingTask(SudTask<?> task) {
        TaskBucket bucket = waitingTasksByType.get(task.getTaskType());
        return bucket.add(task);
    }

    /**
     * Markiert einen Task als laufend.
     *
     * @param task der zu markierende Task
     */
    synchronized void addRunningTask(SudTask<?> task) {
        runningTasks.add(task);
    }

    /** @return Gesamtanzahl wartender Tasks über alle Prioritäten */
    synchronized int sizeWaitingTasks() {
        return waitingTasksByType.values().stream().mapToInt(TaskBucket::size).sum();
    }

    /**
     * Entfernt einen Task aus den laufenden Tasks.
     *
     * @param task der zu entfernende Task
     */
    synchronized void removeRunningTask(SudTask<?> task) {
        runningTasks.remove(task);
    }

    /**
     * Prüft ob die Queue vollständig leer ist.
     *
     * @return true wenn keine wartenden oder laufenden Tasks existieren
     */
    synchronized boolean isEmpty() {
        boolean noRunningTasks = runningTasks.isEmpty();
        boolean noWaitingTasks = waitingTasksByType.values().stream().allMatch(TaskBucket::isEmpty);
        return noRunningTasks && noWaitingTasks;
    }

    /**
     * Liefert eine unveränderbare Sicht auf alle Tasks.
     * <p><strong>Wichtig:</strong> Nur für lesende Zugriffe verwenden!
     *
     * @return Menge aller Tasks (wartend + laufend)
     */
    synchronized Set<SudTaskHandle> getAllTasks() {
        Stream<SudTaskHandle> waitingTasksStream = waitingTasksByType
            .values()
            .stream()
            .flatMap(TaskBucket::allTasks);
        return Stream.concat(
            runningTasks.stream().map(SudTaskHandle.class::cast),
            waitingTasksStream
        ).collect(toSet());
    }

    /**
     * Liefert einen Iterator über die aktuell ausführbaren Kandidaten aus den wartenden Tasks in
     * Prioritätsreihenfolge.
     *
     * <p>Innerhalb eines Typs liefert ein {@link TaskBucket} pro Runde höchstens einen Task je
     * SchemeAgencyId (Round-Robin), gefolgt von den übrigen (nicht agency-bezogenen) Tasks in
     * FIFO-Reihenfolge. Da pro SchemeAgencyId ohnehin nur ein Task gleichzeitig laufen darf, genügt
     * ein einzelner Kandidat je SchemeAgencyId pro Aufruf.
     *
     * <p><strong>Thread-Safety:</strong> Beim Durchlaufen des Iterators muss
     * auf die {@link SudTaskQueue}-Instanz synchronisiert werden!
     *
     * @return Iterator über wartende Tasks nach Priorität sortiert
     */
    synchronized Iterator<SudTaskHandle> iteratorWaitingTasks() {
        return waitingTasksByType
            .values()
            .stream()
            .map(TaskBucket::iterator)
            .reduce(Collections.emptyIterator(), Iterators::concat);
    }

    /**
     * Erstellt einen Snapshot des aktuellen Task-Status.
     * <p><strong>Wichtig:</strong> Nur für lesende Zugriffe verwenden!
     *
     * @return unveränderlicher Snapshot für Monitoring
     */
    synchronized SudTasksSnapshot getTasksSnapshot() {
        List<SudTaskOutModel> currentlyRunningTasks = getAllRunningTasks()
            .stream()
            .map(SudTaskOutModel::of)
            .toList();
        List<SudTaskOutModel> currentlyWaitingTasks = getAllWaitingTasks()
            .stream()
            .map(SudTaskOutModel::of)
            .toList();
        return new SudTasksSnapshot(currentlyRunningTasks, currentlyWaitingTasks);
    }

    /**
     * Liefert eine unveränderbare Liste aller wartenden Tasks.
     * <p><strong>Wichtig:</strong> Nur für lesende Zugriffe verwenden!
     *
     * @return Liste wartender Tasks in Prioritätsreihenfolge
     */
    private synchronized List<SudTaskHandle> getAllWaitingTasks() {
        return waitingTasksByType.values().stream().flatMap(TaskBucket::allTasks).toList();
    }

    /**
     * Liefert eine unveränderbare Kopie der laufenden Tasks.
     * <p><strong>Wichtig:</strong> Nur für lesende Zugriffe verwenden!
     *
     * @return Menge der aktuell laufenden Tasks
     */
    synchronized Set<SudTaskHandle> getAllRunningTasks() {
        return Set.copyOf(runningTasks);
    }
}
