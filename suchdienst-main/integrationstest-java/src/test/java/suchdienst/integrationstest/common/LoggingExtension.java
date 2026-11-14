/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class LoggingExtension
    implements BeforeEachCallback, AfterEachCallback, ExtensionContext.Store.CloseableResource
{

    @Override
    public void beforeEach(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Logger contextLogger = LoggerFactory.getLogger(testClass);

        StopWatch sw = new StopWatch();
        ExtensionContext.Store store = context.getStore(
            ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod())
        );
        store.put("stopwatch", sw);
        sw.start();

        contextLogger.info("Start: {}", context.getDisplayName());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Logger contextLogger = LoggerFactory.getLogger(testClass);
        ExtensionContext.Store store = context.getStore(
            ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod())
        );
        StopWatch stopwatch = store.get("stopwatch", StopWatch.class);
        contextLogger.info("Ende: {}, Dauer: {}", context.getDisplayName(), stopwatch.formatTime());
    }

    @Override
    public void close() {
        synchronized (EnvironmentManagerExtension.class) {
            log.info("######### Testdurchführung abgeschlossen #########");
        }
    }
}
