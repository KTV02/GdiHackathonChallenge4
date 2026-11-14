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
package suchdienst.integrationstest.tests.jzufi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.TestUtil;

/** Gemeinsame Namens- und Datumsformatierung fuer die JZuFi-v2-Fachtests. */
final class JzufiV2Fixtures {

    private JzufiV2Fixtures() {}

    static String name(TestInfo info) {
        return TestUtil.addTimestamp(
            info.getTestClass().orElseThrow().getSimpleName() +
                "-" +
                info.getTestMethod().orElseThrow().getName() +
                "-" +
                TestUtil.generateRandomWord()
        );
    }

    static String xmlDate(LocalDate date) {
        // Die Generator-DSL schreibt xsd:date mit der lokalen Zeitzone.
        return date.atStartOfDay(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu-MM-ddXXX"));
    }
}
