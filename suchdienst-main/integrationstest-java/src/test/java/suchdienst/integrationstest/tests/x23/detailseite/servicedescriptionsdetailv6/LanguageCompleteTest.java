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

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.DynamischerParameter;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LanguageCompleteTest {

    private final BundeslandConfig bundeslandConfig = new BundeslandConfig(
        Bundesland.BY,
        "L100042",
        "090000000000",
        List.of("091620000000")
    );

    private XzufiFile fileENComplete;
    private String lbId;

    @BeforeEach
    void setupTestClass(TestInfo testInfo) {
        String testClassName = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        fileENComplete = XzufiFileCreator.newInstance(bundeslandConfig).name(testClassName).lb(1).oe(1).od(1).getMin();

        Leistungsbeschreibung lbENComplete = fileENComplete.getLeistungsbeschreibungen().getFirst();
        lbENComplete.getTextmodule().clear();
        lbENComplete.getUrsprungsportale().clear();

        lbENComplete.setTextmodule(
            List.of(
                Textmodul.builder()
                    .leikaCode("02")
                    .inhalt("DE_02")
                    .inhaltEnglisch("EN_02")
                    .positionDarstellung(2)
                    .build(),
                Textmodul.builder()
                    .leikaCode("03")
                    .inhalt("DE_03")
                    .inhaltEnglisch("EN_03")
                    .positionDarstellung(3)
                    .build(),
                Textmodul.builder()
                    .leikaCode("06")
                    .inhalt("DE_06")
                    .inhaltEnglisch("EN_06")
                    .positionDarstellung(6)
                    .build()
            )
        );

        Onlinedienst od = fileENComplete.getOnlinedienste().getFirst();
        od.setBezeichnung("OD_DE");
        od.setBezeichnungEnglisch("OD_EN");
        od.setBeschreibung("OD_Beschreibung_DE");
        od.setBeschreibungEnglisch("OD_Description_EN");
        od.setLinks(
            List.of(
                Link.builder()
                    .uri("http://od.de")
                    .uriEnglisch("http://od.en")
                    .titel("OD_Titel_DE")
                    .titelEnglisch("OD_Titel_EN")
                    .beschreibung("OD_Link_DE")
                    .beschreibungEnglisch("OD_Link_EN")
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName("paramDE")
                                .parameterNameEnglisch("paramEN")
                                .build()
                        )
                    )
                    .build()
            )
        );

        lbId = lbENComplete.getFullId();
    }

    @Test
    @DisplayName("english_language_complete_true")
    void englishLanguageCompleteTrue() {
        ImportUtil.pushAndWait23(fileENComplete);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("091620000000", lbId, "en");
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateLanguageCompleteForEN(true);
    }

    @Test
    @DisplayName("english_language_incomplete_false")
    void englishLanguageIncompleteFalse() {
        Leistungsbeschreibung lbENIncomplete = fileENComplete.getLeistungsbeschreibungen().getFirst();
        lbENIncomplete.setTextmodule(
            List.of(
                Textmodul.builder()
                    .leikaCode("02")
                    .inhalt("DE_02")
                    .inhaltEnglisch("") // Englisch fehlt
                    .positionDarstellung(2)
                    .build(),
                Textmodul.builder()
                    .leikaCode("03")
                    .inhalt("DE_03")
                    .inhaltEnglisch("EN_03")
                    .positionDarstellung(3)
                    .build()
            )
        );
        ImportUtil.pushAndWait23(fileENComplete);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("091620000000", lbId, "en");
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateLanguageCompleteForEN(false);
    }

    @Test
    @DisplayName("online_service_english_incomplete_false")
    void onlineServiceEnglishIncompleteFalse() {
        Onlinedienst od = fileENComplete.getOnlinedienste().getFirst();
        od.setBezeichnung("OD_DE");
        od.setBezeichnungEnglisch(""); // Englisch fehlt
        od.setBeschreibung("OD_Beschreibung_DE");
        od.setBeschreibungEnglisch(""); // Englisch fehlt
        ImportUtil.pushAndWait23(fileENComplete);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("091620000000", lbId, "en");
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateLanguageCompleteForEN(false);
    }

    @Test
    @DisplayName("german_language_request")
    void germanLanguageRequest() {
        ImportUtil.pushAndWait23(fileENComplete);
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6("091620000000", lbId, "de");
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateLanguageCompleteForEN(true);
    }
}
