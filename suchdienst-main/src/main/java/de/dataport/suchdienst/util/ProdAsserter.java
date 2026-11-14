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
package de.dataport.suchdienst.util;

import de.dataport.suchdienst.config.Profiles;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Asserter für den Produktiv-Betrieb. Hier werden Meldungen nur als Warnungen geloggt.
 */
@Component
@Profile(Profiles.PRODUCTION)
@Slf4j
public class ProdAsserter implements Asserter {

    @PostConstruct
    private void logActive() {
        log.info("ProdAsserter ist aktiv.");
    }

    @Override
    public void neverReached(String msg) {
        var stackTraceElements = new Throwable().getStackTrace();
        if (stackTraceElements.length >= 2) {
            var caller = stackTraceElements[1].toString();
            log.warn("Assertion fehlgeschlagen in {}: {}", caller, msg);
        } else {
            log.warn("Assertion fehlgeschlagen in: {}", msg);
        }
    }
}
