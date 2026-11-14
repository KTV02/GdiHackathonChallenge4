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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
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
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class SourcePortalTest {

    @Test
    void dataInLb(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.setUrsprungsportale(
            List.of(
                UrsprungsportalModul.builder()
                    .title("LINK_Ursprungsportal")
                    .uri("http://www.ursprungsportal.de/")
                    .positionDarstellung(0)
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalFirstUri(0, "http://www.ursprungsportal.de/")
            .validateSourcePortalFirstTitle(0, "LINK_Ursprungsportal")
            .validateSourcePortalTitle(0, "Ursprungsportal");
    }

    @Test
    void dataInLbEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.setUrsprungsportale(
            List.of(
                UrsprungsportalModul.builder()
                    .title("LINK_Ursprungsportal")
                    .titleEnglisch("Ursprungsportal Englisch")
                    .uri("http://www.ursprungsportal.de/")
                    .uriEnglisch("http://www.ursprungsportal.com/")
                    .positionDarstellung(0)
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalFirstUri(0, "http://www.ursprungsportal.com/")
            .validateSourcePortalFirstTitle(0, "Ursprungsportal Englisch")
            .validateSourcePortalTitle(0, "Source Portal");
    }

    @Test
    void dataInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        UrsprungsportalModul.builder()
                            .title("LINK_Ursprungsportal_spez")
                            .uri("http://www.ursprungsportalSpez.de/")
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalFirstUri(0, "http://www.ursprungsportalSpez.de/")
            .validateSourcePortalFirstTitle(0, "LINK_Ursprungsportal_spez")
            .validateSourcePortalTitle(0, "Ursprungsportal");
    }

    @Test
    void dataInSpEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        UrsprungsportalModul.builder()
                            .title("LINK_Ursprungsportal_spez")
                            .titleEnglisch("LINK_Ursprungsportal_spez_Englisch")
                            .uri("http://www.ursprungsportalSpez.de/")
                            .uriEnglisch("http://www.ursprungsportalSpez.com/")
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalFirstUri(0, "http://www.ursprungsportalSpez.com/")
            .validateSourcePortalFirstTitle(0, "LINK_Ursprungsportal_spez_Englisch")
            .validateSourcePortalTitle(0, "Source Portal");
    }
}
