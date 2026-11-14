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
package suchdienst.integrationstest.tests.lebenslagen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.mocks.LeistungskatalogMock;
import suchdienst.integrationstest.utils.ArtifactWriter;
import suchdienst.integrationstest.utils.WaitUtil;

public class KatalogImportUtil {

    public static void resetLeistungskatalog(String fileName) {
        String content = getLebenslagenContent(fileName);

        // Testfall verändert mehrfach den Leistungskatalog, wir wollen einen Validen zustand hinterlassen
        ElasticsearchConnector.removeAllData();
        ElasticsearchConnector.removeAllDataByIndex(List.of("suchdienst_leistungskatalog*"));
        // warte auf Löschen des Datenbestands
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        ArtifactWriter artifactWriter = new ArtifactWriter("IMPORT_LK");

        LeistungskatalogMock lkmock = new LeistungskatalogMock();
        lkmock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(artifactWriter),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
        lkmock.stop();
    }

    private static String getLebenslagenContent(String fileName) {
        try {
            return Files.readString(Paths.get(ClassLoader.getSystemResource(fileName).toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e); // NOSONAR - java:S112 - runtime exception ist hier gewollt
        }
    }
}
