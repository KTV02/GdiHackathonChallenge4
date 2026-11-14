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
package de.dataport.suchdienst.util;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

/**
 * Leert den MDC vor und nach jedem Test. Der MDC ist thread-lokal und wird von JUnit über Testklassen
 * hinweg im selben Thread weiterverwendet, Reste würden sonst in andere Tests lecken.
 */
public class MdcCleanupExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        MDC.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        MDC.clear();
    }
}
