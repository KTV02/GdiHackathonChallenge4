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
package suchdienst.integrationstest.tests.x23.detailseite.organisationunitsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.AddressValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class AddressTest {

    @Test
    void address(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getAnschriften().clear();
        organisationseinheit
            .getAnschriften()
            .add(
                Anschrift.builder()
                    .ort("Nürnberg")
                    .hausnummer("822")
                    .typ("001")
                    .postleitzahl("90403")
                    .strasse("Billstraße")
                    .infoBarrierefreiheit("Barrierearm")
                    .infoOEPNV("Gut erreichbar mit den Oeffis")
                    .infoParkplatz("Wenig Parkplaetze")
                    .kennzeichenRollstuhlgerecht(true)
                    .kennzeichenAufzug(true)
                    .zusatz("Zusatzinfos nur ueber Hintereingang zu erreichen")
                    .build()
            );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());

        //Validierung
        AddressValidator validator = new AddressValidator(response);
        validator
            .validateCode(0, "001")
            .validateCity(0, "Nürnberg")
            .validateHouseNumber(0, "822")
            .validateType(0, "Hausanschrift")
            .validateZip(0, "90403")
            .validateStreet(0, "Billstraße")
            .validateAccessible(0, "Barrierearm")
            .validateOepnv(0, "Gut erreichbar mit den Oeffis")
            .validateParking(0, "Wenig Parkplaetze")
            .validateWheelchairAccessible(0, true)
            .validateLift(0, true)
            .validateAdditional(0, "Zusatzinfos nur ueber Hintereingang zu erreichen");
    }
}
