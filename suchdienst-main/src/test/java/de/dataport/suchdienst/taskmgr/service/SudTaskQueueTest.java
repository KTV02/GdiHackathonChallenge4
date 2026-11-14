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
package de.dataport.suchdienst.taskmgr.service;

import static de.dataport.suchdienst.taskmgr.model.SudTaskType.IMPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.taskmgr.model.SchemeAgencyIdTask;
import de.dataport.suchdienst.taskmgr.model.SudTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskHandle;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class SudTaskQueueTest {

    @Test
    void rotatesAgenciesAndKeepsDuplicateDetectionAndQueueSizeConsistent() {
        SudTaskQueue queue = new SudTaskQueue();
        SudTask<Void> firstAgencyTask = new TestSchemeAgencyIdTask("agency-a", "producer-a");
        SudTask<Void> secondAgencyTask = new TestSchemeAgencyIdTask("agency-a", "producer-b");
        SudTask<Void> otherAgencyTask = new TestSchemeAgencyIdTask("agency-b", "producer-a");

        assertThat(queue.addWaitingTask(firstAgencyTask), is(true));
        assertThat(queue.addWaitingTask(secondAgencyTask), is(true));
        assertThat(queue.addWaitingTask(otherAgencyTask), is(true));
        assertThat(
            queue.addWaitingTask(new TestSchemeAgencyIdTask("agency-a", "producer-a")),
            is(false)
        );
        assertThat(queue.sizeWaitingTasks(), is(3));

        Iterator<SudTaskHandle> iterator = queue.iteratorWaitingTasks();
        assertThat(iterator.next(), is(firstAgencyTask));
        iterator.remove();

        assertThat(iterator.next(), is(otherAgencyTask));
        iterator.remove();
        assertThat(queue.sizeWaitingTasks(), is(1));

        Iterator<SudTaskHandle> nextRound = queue.iteratorWaitingTasks();
        assertThat(nextRound.next(), is(secondAgencyTask));
        nextRound.remove();
        assertThat(queue.sizeWaitingTasks(), is(0));
        assertThat(queue.isEmpty(), is(true));
    }

    private static final class TestSchemeAgencyIdTask extends SchemeAgencyIdTask<Void> {

        private TestSchemeAgencyIdTask(String schemeAgencyId, String producer) {
            super(IMPORT, schemeAgencyId, producer);
        }

        @Override
        public Class<Void> getFutureType() {
            return Void.class;
        }
    }
}
