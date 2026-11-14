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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Asserter für den Dev- und Test-Betrieb. Meldungen werden als Fehler geloggt, und es wird optional eine Exception geworfen.
 * Dadurch sollen Fehlersituationen früher auffallen. Siehe z.b. PV-8896.
 */
@Component
@Profile({ Profiles.DEVELOPMENT, Profiles.INTEGRATION_TEST })
@Slf4j
public class DevAsserter implements Asserter {

    /**
     * Diese Property steuert, ob ein Assertion-Fehler zusätzlich als Exception geworfen wird.
     */
    @Value("${de.dataport.suchdienst.util.devasserter.throwonerror:false}")
    private boolean throwOnError;

    @PostConstruct
    private void logActive() {
        log.info("DevAsserter ist aktiv, throwOnError={}", throwOnError);
    }

    public void neverReached(String msg) {
        var stackTraceElements = new Throwable().getStackTrace();
        String logMsg;
        if (stackTraceElements.length >= 2) {
            var caller = stackTraceElements[1].toString();
            logMsg = "Assertion fehlgeschlagen in " + caller + ": " + msg;
        } else {
            logMsg = "Assertion fehlgeschlagen: " + msg;
        }
        log.error(logMsg);
        if (throwOnError) {
            throw new IllegalStateException(logMsg);
        }
    }
}
