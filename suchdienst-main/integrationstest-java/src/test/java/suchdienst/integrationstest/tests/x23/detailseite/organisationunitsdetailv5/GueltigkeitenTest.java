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
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import io.restassured.response.Response;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class GueltigkeitenTest {

    @Test
    void oeUngueltig(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();

        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getGueltigkeiten().clear();
        organisationseinheit.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2010, 8, 1))
                .ende(LocalDate.of(2020, 8, 1))
                .build()
        );

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);

        Response responseOrganisation = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            LocalDate.now()
        );

        // Validierung
        ErrorValidator errorValidator = new ErrorValidator(responseOrganisation);
        errorValidator.validateNotFound();
    }
}
