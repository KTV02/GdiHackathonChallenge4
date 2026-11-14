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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv5;

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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;

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

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "http://www.ursprungsportal.de/")
            .validateSourcePortalTitle(0, "LINK_Ursprungsportal")
            .validateSourcePortalReplace(0, false);
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

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "http://www.ursprungsportal.com/")
            .validateSourcePortalTitle(0, "Ursprungsportal Englisch")
            .validateSourcePortalReplace(0, false);
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

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "http://www.ursprungsportalSpez.de/")
            .validateSourcePortalTitle(0, "LINK_Ursprungsportal_spez");
        // known issue PV-13064 - validateSourcePortalReplace(0, true)
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

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateId(leistung.getFullId())
            .validateSourcePortalsCount(1)
            .validateSourcePortalUri(0, "http://www.ursprungsportalSpez.com/")
            .validateSourcePortalTitle(0, "LINK_Ursprungsportal_spez_Englisch");
        // known issue PV-13064 - validateSourcePortalReplace(0, true)
    }
}
