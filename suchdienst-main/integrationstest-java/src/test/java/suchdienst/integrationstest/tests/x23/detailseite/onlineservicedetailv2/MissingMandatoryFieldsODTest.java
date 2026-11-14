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
package suchdienst.integrationstest.tests.x23.detailseite.onlineservicedetailv2;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.validator.onlineservicedetailv2.OnlineserviceValidator;

/**
 * Testziel: Der Test prüft, dass bei fehlenden englischsprachigen Pflichtfeldern und Abfrage mit LanguageCode en die deutschsprachigen Inhalte übertragen werden
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MissingMandatoryFieldsODTest {

    @Test
    void detailsDataOdV2(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();

        String odBezeichnungDE = "MissingMandatoryODBezeichnungDE";
        String odBeschreibungDE = "MissingMandatoryODBeschreibungDE";
        String odHilfetextEN = "MissingMandatoryHelpTextEN";

        onlinedienst.setBezeichnung(odBezeichnungDE);
        onlinedienst.setBeschreibung(odBeschreibungDE);
        onlinedienst.setHilfeTextEnglisch(odHilfetextEN);
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri("https://missingmandatory.com/uri")
                    .titel("MissingMandatoryOdLinkTitel")
                    .beschreibung("MissingMandatoryOdLinkBeschreibung")
                    .positionDarstellung(1010)
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);

        Response response = OnlineserviceConnector.getOnlineserviceDetailV2(
            onlinedienst.getFullId(),
            Constants.LANGUAGE_EN
        );

        OnlineserviceValidator validator = new OnlineserviceValidator(response);
        // PV-10550: die englische Variante existiert jetzt, auch wenn sie keinen Namen hat.
        // Vorher wurde sie beim Parsen auf null gesetzt und hier wurde stattdessen die deutsche
        // Variante geliefert. Der Test prüft jetzt also, ob die englische Variante geliefert wird.
        validator
            .validateName(null)
            .validateDescription(null)
            .validateLanguageComplete("en", false)
            .validateHelpText(odHilfetextEN);
    }
}
