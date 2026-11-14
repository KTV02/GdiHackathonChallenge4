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
package suchdienst.integrationstest.tests.plzimport;

import io.restassured.response.Response;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.validator.locationsv2.LocationsValidator;

/**
 * Testziel: Es wird geprüft, dass beim Hinzufügen einer vorher nicht vohandenen PLZ diese danach gefunden wird und dem richtigen Ort zugewiesen ist.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class PlzImportTest {

    @BeforeEach
    void setup() throws URISyntaxException {
        Path plzBasis = Paths.get(ClassLoader.getSystemResource("./tests/plzimport/PLZ.csv.gz").toURI());
        AdminConnector.uploadPlz(plzBasis.toFile());
        ImportUtil.waitForLocationSuggestionCount("20095", 1);
    }

    @AfterAll
    static void resetArsData() {
        ImportUtil.resetArsData();
    }

    @Test
    void testPlzUpload() throws URISyntaxException {
        // Prüfe, ob eine PLZ gefunden wird, die gefunden werden soll.
        Response response1 = LocationsConnector.getLocationsSuggestionsV2("20095", false);
        new LocationsValidator(response1).validateName(0, "Hamburg, Freie und Hansestadt");

        // Prüfe, ob eine PLZ nicht gefunden wird, die nicht gefunden werden soll
        Response response2 = LocationsConnector.getLocationsSuggestionsV2("22042", false);
        new LocationsValidator(response2).validateCount(0);

        //Lade PLZ Liste hoch, mit der fehlenden PLZ
        Path plzModified = Paths.get(ClassLoader.getSystemResource("./tests/plzimport/PLZ_modified.csv.gz").toURI());
        AdminConnector.uploadPlz(plzModified.toFile());
        ImportUtil.waitForLocationSuggestionCount("22042", 1);

        //Prüfe, ob zuvor fehlende PLZ nun gefunden wird
        Response response3 = LocationsConnector.getLocationsSuggestionsV2("22042", false);
        new LocationsValidator(response3).validateName(0, "Hamburg, Freie und Hansestadt");
    }
}
