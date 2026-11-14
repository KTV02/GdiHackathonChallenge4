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

import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.TestInfo;

public class TestUtil {

    public static String addTimestamp(String name) {
        return generateTimestamp() + "-" + name;
    }

    public static String generateTimestamp() {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(timestamp);
    }

    public static boolean arsIsSubArsOfOneListItem(String arsToCheck, List<String> arsList) {
        List<String> arsPatternList = arsList
            .stream()
            .map(arsItem -> arsItem.replaceAll("0*$", ""))
            .map(arsRegex -> "^" + arsRegex + "\\w*")
            .toList();
        for (String arsPattern : arsPatternList) {
            if (arsToCheck.matches(arsPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Erzeugt ein zufälliges Wort aus Großbuchstaben, das als eindeutiger Suchbegriff-Bestandteil in
     * Testdaten verwendet wird. Die Länge von 8 Zeichen (statt zuvor 4) erhöht den Wertebereich deutlich.
     * Das ist wichtig, weil die Such-API (V8) immer mit Fuzzy-Suche arbeitet (Toleranz bis zu 2
     * Zeichen ab 8-Buchstaben-Wörtern) und solche Wörter sonst als Tippfehler toleriert und fälschlich
     * als Treffer gewertet werden.
     */
    public static String generateRandomWord() {
        ThreadLocalRandom wuerfel = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            builder.append((char) (wuerfel.nextInt(26) + 65));
        }
        return builder.toString();
    }

    public static String testcasename(TestInfo testInfo, XzufiFileDistributor distributor) {
        String testMethodName = testInfo.getTestMethod().orElseThrow().getName();
        String distributorClassName = distributor.getClass().getSimpleName();
        return TestUtil.addTimestamp(testMethodName + "-" + distributorClassName);
    }
}
