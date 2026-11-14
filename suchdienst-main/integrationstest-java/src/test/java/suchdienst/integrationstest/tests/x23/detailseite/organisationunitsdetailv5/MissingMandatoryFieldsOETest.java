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
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MissingMandatoryFieldsOETest {

    /*
    Der Test prüft, dass bei fehlenden englischsprachigen Pflichtfeldern und Abfrage mit LanguageCode en die
    deutschsprachigen Inhalte übertragen werden
*/

    @Test
    void detailsDataOE(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).od(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        String oeTitelDE = "TitelOrganisationseinheitDE";
        organisationseinheit.setTitel(oeTitelDE);
        organisationseinheit.setBeschreibungEnglisch("DescriptionOrgaEN");
        ImportUtil.pushAndWait23(file);

        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String lbId = lb.getFullId();

        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            lbId,
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator.validateTitle(oeTitelDE).validateLanguageCompleteIsEmpty();
    }
}
