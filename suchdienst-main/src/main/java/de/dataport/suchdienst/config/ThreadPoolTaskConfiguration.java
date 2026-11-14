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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolTaskConfiguration {

    /**
     * Thread Pool für den TaskMgr
     */
    @Bean(name = "taskmgrExecutor")
    public ThreadPoolTaskExecutor taskmgrThreadPool(SuchdienstProperties properties) {
        int threadCount = properties.getImporter().getTaskMgrThreadCount();
        return threadPool("taskmgr_executor-", threadCount, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Unlimitierter Thread Pool für I/O Aufgaben.
     * Konfiguration ahmt {@link java.util.concurrent.Executors#newCachedThreadPool()} nach
     */
    @Bean(name = "ioExecutor")
    public ThreadPoolTaskExecutor ioThreadPool() {
        return threadPool("io_executor-", 1, 0, Integer.MAX_VALUE);
    }

    /**
     * Thread Pool für den XZuFi Parser, angepasst für CPU-gebundene Aufgaben mit hohem I/O Anteil.
     * Es stehen 24 Threads zur Verfügung, alle weiteren Tasks werden in eine unendliche Warteschlange gestellt.
     */
    @Bean(name = "xzufiParserExecutor")
    public ThreadPoolTaskExecutor xzufiParserThreadPool() {
        return threadPool("xzufi_parser-", 24, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static ThreadPoolTaskExecutor threadPool(
        String namePrefix,
        int corePoolSize,
        int queueCapacity,
        int maxPoolSize
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(namePrefix);

        // Anzahl an Threads die zu Beginn zur Verfügung stehen
        executor.setCorePoolSize(corePoolSize);
        // Anzahl an Plätzen in der Warteschlange
        executor.setQueueCapacity(queueCapacity);
        // Wenn Warteschlange voll, dann werden neue Threads erzeugt.
        executor.setMaxPoolSize(maxPoolSize);

        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        // Der MDC ist thread-lokal und wird sonst nicht an die Pool-Threads weitergegeben.
        executor.setTaskDecorator(MdcTemplate::wrap);

        executor.initialize();
        return executor;
    }
}
