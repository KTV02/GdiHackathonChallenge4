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
package de.dataport.suchdienst.config;

import static org.assertj.core.api.Assertions.assertThat;

import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@ExtendWith(MdcCleanupExtension.class)
class SchedulerConfigurationTest {

    @Test
    void scheduler_uebertraegt_den_mdc_und_hinterlaesst_den_thread_sauber() throws Exception {
        var scheduler = (ThreadPoolTaskScheduler) new SchedulerConfiguration().singleTaskExecutor();
        scheduler.initialize();
        try {
            AtomicReference<Map<String, String>> contextInTask = new AtomicReference<>();
            MDC.put(MdcKeys.REQUEST_ID, "request-1");

            scheduler
                .schedule(() -> contextInTask.set(MdcTemplate.currentContext()), Instant.now())
                .get();

            assertThat(contextInTask.get()).containsEntry(MdcKeys.REQUEST_ID, "request-1");

            MDC.clear();
            scheduler
                .schedule(() -> contextInTask.set(MdcTemplate.currentContext()), Instant.now())
                .get();

            assertThat(contextInTask.get()).isEmpty();
        } finally {
            scheduler.destroy();
        }
    }
}
