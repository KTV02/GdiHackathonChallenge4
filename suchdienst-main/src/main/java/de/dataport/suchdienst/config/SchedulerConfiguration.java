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
package de.dataport.suchdienst.config;

import de.dataport.suchdienst.util.MdcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Anlegen des TaskSchedulers für den Suchdienst. Wichtig ist hier, dass der Scheduler niemals zwei Aktivitäten
 * parallel ausführt (z.b. Reindizierung und Import).
 * Darum setzen wir hier explizit die PoolSize auf 1.
 */
@Configuration
public class SchedulerConfiguration {

    @Bean
    public TaskScheduler singleTaskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setAwaitTerminationSeconds(15);
        scheduler.setPoolSize(1);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setTaskDecorator(MdcTemplate::wrap);
        return scheduler;
    }
}
