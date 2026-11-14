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
import de.dataport.pvog.testdatengenerator.model.Rolle;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
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
class RollenTest {

    private static Leistungsbeschreibung lb;
    private static Organisationseinheit oe1;
    private static Organisationseinheit oe2;
    private static Organisationseinheit oe3;
    private static String ars;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(4).getMax();

        lb = file.getLeistungsbeschreibungen().getFirst();

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        oe1 = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zs1 = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        // 01 (x22) == 02 + 03 (x23) -> Zuständige Stelle und Ansprechpunkt / Competent office and contact point
        zs1.setRolle(List.of(Rolle.builder().code("02").build(), Rolle.builder().code("03").build()));

        // 02 -> Zuständige Stelle / Competent office
        oe2 = file.getOrganisationseinheiten().get(1);
        Zustaendigkeit zs2 = file.getZustaendigkeitenOrganisationseinheit().get(1);
        zs2.setRolle(List.of(Rolle.builder().code("02").build()));

        // 03 -> Ansprechpunkt / Contact point
        oe3 = file.getOrganisationseinheiten().get(2);
        Zustaendigkeit zs3 = file.getZustaendigkeitenOrganisationseinheit().get(2);
        zs3.setRolle(List.of(Rolle.builder().code("03").build()));

        ImportUtil.pushAndWait23(file);
    }

    @Test
    void germanRoleCode01() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(oe1.getFullId(), lb.getFullId());

        OrganisationValidator validator = new OrganisationValidator(response);

        validator
            .validateTitle(oe1.getTitel())
            .validateRoleCode("01")
            .validateRoleName("Zuständige Stelle und Ansprechpunkt");
    }

    @Test
    void germanRoleCode02() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(oe2.getFullId(), lb.getFullId());

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe2.getTitel()).validateRoleCode("02").validateRoleName("Zuständige Stelle");
    }

    @Test
    void germanRoleCode03() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(oe3.getFullId(), lb.getFullId());

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe3.getTitel()).validateRoleCode("03").validateRoleName("Ansprechpunkt");
    }

    @Test
    void germanRoleArsCode01() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe1.getFullId(),
            ars,
            lb.getFullId()
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator
            .validateTitle(oe1.getTitel())
            .validateRoleCode("01")
            .validateRoleName("Zuständige Stelle und Ansprechpunkt");
    }

    @Test
    void germanRoleArsCode02() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe2.getFullId(),
            ars,
            lb.getFullId()
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe2.getTitel()).validateRoleCode("02").validateRoleName("Zuständige Stelle");
    }

    @Test
    void germanRoleArsCode03() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe3.getFullId(),
            ars,
            lb.getFullId()
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe3.getTitel()).validateRoleCode("03").validateRoleName("Ansprechpunkt");
    }

    @Test
    void englishRoleCode01() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe1.getFullId(),
            null,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator
            .validateTitle(oe1.getTitelEnglisch())
            .validateRoleCode("01")
            .validateRoleName("Competent office and contact point");
    }

    @Test
    void englishRoleCode02() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe2.getFullId(),
            null,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe2.getTitelEnglisch()).validateRoleCode("02").validateRoleName("Competent office");
    }

    @Test
    void englishRoleCode03() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe3.getFullId(),
            null,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe3.getTitelEnglisch()).validateRoleCode("03").validateRoleName("Contact point");
    }

    @Test
    void englishRoleArsCode01() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe1.getFullId(),
            ars,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator
            .validateTitle(oe1.getTitelEnglisch())
            .validateRoleCode("01")
            .validateRoleName("Competent office and contact point");
    }

    @Test
    void englishRoleArsCode02() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe2.getFullId(),
            ars,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe2.getTitelEnglisch()).validateRoleCode("02").validateRoleName("Competent office");
    }

    @Test
    void englishRoleArsCode03() {
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe3.getFullId(),
            ars,
            lb.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );

        OrganisationValidator validator = new OrganisationValidator(response);

        validator.validateTitle(oe3.getTitelEnglisch()).validateRoleCode("03").validateRoleName("Contact point");
    }
}
