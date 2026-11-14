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

import de.dataport.suchdienst.taskmgr.model.SchemeAgencyIdTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskHandle;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Verwaltet die wartenden Tasks eines einzelnen {@link SudTaskType}.
 *
 * <p>{@link SchemeAgencyIdTask}s werden nach ihrer SchemeAgencyId gruppiert und per
 * Round-Robin angeboten: pro Runde wird höchstens ein (der jeweils älteste) Task je
 * SchemeAgencyId als Kandidat geliefert. Nach einer erfolgreichen Entnahme (per
 * {@link Iterator#remove()}) rückt die betroffene SchemeAgencyId ans Ende der Rotation, damit
 * andere SchemeAgencyIds in der nächsten Runde bevorzugt drankommen. Da laut Konfliktregel
 * ohnehin nur ein Task je SchemeAgencyId gleichzeitig laufen darf, ist ein Kandidat je
 * SchemeAgencyId pro Aufruf ausreichend – ein zweiter Task derselben SchemeAgencyId wäre in
 * derselben Runde immer blockiert.
 *
 * <p>Alle anderen Tasks (z. B. {@link de.dataport.suchdienst.taskmgr.model.BundeslandTask})
 * werden unverändert in einfacher Einfüge-Reihenfolge (FIFO) verwaltet.
 */
public class TaskBucket {

    /**
     * Wartende Tasks je SchemeAgencyId. Die Einfügereihenfolge der LinkedHashMap ist zugleich
     * die Round-Robin-Reihenfolge der aktiven SchemeAgencyIds.
     */
    private final LinkedHashMap<String, AgencyQueue> agencyQueues = new LinkedHashMap<>();

    /** Wartende Tasks ohne SchemeAgencyId-Bezug, in Einfüge-Reihenfolge (FIFO, unverändert). */
    private final Set<SudTask<?>> plainTasks = new LinkedHashSet<>();

    /** Fügt einen Task hinzu. @return true, wenn kein Duplikat vorlag. */
    boolean add(SudTask<?> task) {
        if (task instanceof SchemeAgencyIdTask<?> schemeAgencyIdTask) {
            return addSchemeAgencyIdTask(schemeAgencyIdTask);
        }
        return plainTasks.add(task);
    }

    private boolean addSchemeAgencyIdTask(SchemeAgencyIdTask<?> task) {
        String schemeAgencyId = task.getSchemeAgencyId();
        AgencyQueue agencyQueue = agencyQueues.computeIfAbsent(schemeAgencyId, unusedId ->
            new AgencyQueue()
        );
        return agencyQueue.add(task);
    }

    /** @return Gesamtanzahl wartender Tasks in diesem Bucket. */
    int size() {
        return agencyQueues.values().stream().mapToInt(AgencyQueue::size).sum() + plainTasks.size();
    }

    /** @return true, wenn keine Tasks in diesem Bucket warten. */
    boolean isEmpty() {
        return agencyQueues.isEmpty() && plainTasks.isEmpty();
    }

    /** @return alle wartenden Tasks dieses Buckets (nicht nur die Round-Robin-Kandidaten). */
    Stream<SudTaskHandle> allTasks() {
        Stream<SudTaskHandle> agencyTasks = agencyQueues
            .values()
            .stream()
            .flatMap(AgencyQueue::stream);
        return Stream.concat(agencyTasks, plainTasks.stream().map(SudTaskHandle.class::cast));
    }

    /**
     * Liefert einen Iterator, der pro SchemeAgencyId genau einen Kandidaten (Round-Robin, in
     * Rotationsreihenfolge) und danach die übrigen Tasks in FIFO-Reihenfolge liefert.
     */
    Iterator<SudTaskHandle> iterator() {
        return new RoundRobinIterator();
    }

    /**
     * Iteriert die Round-Robin-Kandidaten dieses Buckets: zunächst je Runde ein Kandidat pro
     * aktiver SchemeAgencyId (der jeweils älteste Task ihrer {@link AgencyQueue}, in der
     * Rotationsreihenfolge zu Beginn dieses Durchlaufs), danach die übrigen (nicht
     * agency-bezogenen) Tasks in FIFO-Reihenfolge.
     *
     * <p>Die Rotationsreihenfolge wird als Schnappschuss beim Erzeugen des Iterators fixiert
     * (Kopie der {@code agencyQueues}-Keys), damit während eines Durchlaufs Änderungen an der
     * "echten" Rotation (ausgelöst durch {@link #remove()}) das laufende Durchlaufen nicht
     * verfälschen. Erst der nächste {@code iterator()}-Aufruf sieht die aktualisierte Rotation.
     *
     * <p>{@link #remove()} entfernt den zuletzt per {@link #next()} gelieferten Task
     * tatsächlich aus dem Bucket und stößt bei einem agency-bezogenen Task
     * {@link #removeAgencyHead(String)} an, wodurch die SchemeAgencyId ans Ende der Rotation
     * rückt.
     *
     * <p><strong>Wichtig:</strong> Dieser Iterator wirkt nur INNERHALB eines Buckets, also
     * innerhalb eines einzelnen {@link SudTaskType}. Die Reihenfolge zwischen verschiedenen
     * {@link SudTaskType}s (Prioritätsreihenfolge) wird bereits vorher in
     * {@link SudTaskQueue#iteratorWaitingTasks()} über die Reihenfolge der prioritätssortierten
     * {@code waitingTasksByType}-Map hergestellt – der Iterator eines höher priorisierten Typs
     * wird dort vollständig durchlaufen, bevor der erste Kandidat des nächsten Typs angeboten
     * wird. Da in der Praxis pro {@link SudTaskType} entweder ausschließlich
     * {@link SchemeAgencyIdTask}s oder ausschließlich Nicht-Agency-Tasks vorkommen, ist die
     * "Agency vor Plain"-Reihenfolge hier rein defensiv und ohne fachliche Auswirkung.
     */
    private final class RoundRobinIterator implements Iterator<SudTaskHandle> {

        // Schnappschuss der aktuellen Rotationsreihenfolge zu Beginn dieses Durchlaufs:
        private final Iterator<String> agencyOrder = new ArrayList<>(
            agencyQueues.keySet()
        ).iterator();
        private final Iterator<SudTask<?>> plainIterator = plainTasks.iterator();

        // Zustand des zuletzt per next() gelieferten, aber noch nicht entfernten Kandidaten:
        private String pendingAgencyId;
        private boolean pendingIsPlainTask;
        private boolean canRemove;

        @Override
        public boolean hasNext() {
            return agencyOrder.hasNext() || plainIterator.hasNext();
        }

        /**
         * Liefert den nächsten Kandidaten: zuerst der Reihe nach je einen (ältesten) Task pro
         * noch nicht besuchter SchemeAgencyId der Rotation, danach die verbleibenden
         * Nicht-Agency-Tasks in FIFO-Reihenfolge.
         */
        @Override
        public SudTaskHandle next() {
            if (agencyOrder.hasNext()) {
                pendingAgencyId = agencyOrder.next();
                pendingIsPlainTask = false;
                canRemove = true;
                return agencyQueues.get(pendingAgencyId).peekFirst();
            }
            SudTask<?> task = plainIterator.next();
            pendingIsPlainTask = true;
            canRemove = true;
            return task;
        }

        /**
         * Entfernt den zuletzt von {@link #next()} gelieferten Kandidaten endgültig aus dem
         * Bucket. Bei einem agency-bezogenen Kandidaten wird zusätzlich die Rotation
         * aktualisiert (siehe {@link #removeAgencyHead(String)}).
         */
        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("next() muss vor remove() aufgerufen werden");
            }
            if (pendingIsPlainTask) {
                plainIterator.remove();
            } else {
                removeAgencyHead(pendingAgencyId);
            }
            canRemove = false;
        }

        /** Entfernt den ältesten Task einer SchemeAgencyId und aktualisiert die Rotation. */
        private void removeAgencyHead(String schemeAgencyId) {
            AgencyQueue agencyQueue = agencyQueues.remove(schemeAgencyId);
            agencyQueue.removeFirst();
            if (!agencyQueue.isEmpty()) {
                agencyQueues.put(schemeAgencyId, agencyQueue);
            }
        }
    }

    /**
     * FIFO-Warteschlange der Tasks einer einzelnen SchemeAgencyId.
     *
     * <p>Kombiniert eine {@link Deque} (liefert die Einfüge-/Abarbeitungsreihenfolge) mit einem
     * begleitenden {@link HashSet} (liefert die Duplikaterkennung in O(1) statt einer linearen
     * Suche über die Deque). Beide Strukturen werden bei jeder Änderung synchron gehalten und
     * enthalten stets dieselben Elemente.
     */
    private static final class AgencyQueue {

        private final Deque<SudTask<?>> tasks = new ArrayDeque<>();
        private final Set<SudTask<?>> taskSet = new HashSet<>();

        /** Fügt einen Task hinzu, sofern er (per {@code equals}) noch nicht enthalten ist. */
        boolean add(SudTask<?> task) {
            if (!taskSet.add(task)) {
                return false;
            }
            tasks.addLast(task);
            return true;
        }

        /** @return der älteste (als nächstes auszuführende) Task, ohne ihn zu entfernen. */
        SudTaskHandle peekFirst() {
            return tasks.peekFirst();
        }

        /** Entfernt den ältesten Task und hält {@code taskSet} synchron. */
        void removeFirst() {
            taskSet.remove(tasks.removeFirst());
        }

        /** @return true, wenn keine Tasks mehr für diese SchemeAgencyId warten. */
        boolean isEmpty() {
            return tasks.isEmpty();
        }

        /** @return Anzahl der wartenden Tasks dieser SchemeAgencyId. */
        int size() {
            return tasks.size();
        }

        /** @return alle wartenden Tasks dieser SchemeAgencyId in Einfüge-Reihenfolge. */
        Stream<SudTaskHandle> stream() {
            return tasks.stream().map(SudTaskHandle.class::cast);
        }
    }
}
