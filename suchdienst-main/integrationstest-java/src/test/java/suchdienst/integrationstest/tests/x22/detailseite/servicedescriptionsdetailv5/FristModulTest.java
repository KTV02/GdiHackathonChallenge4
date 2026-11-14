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
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristDauer;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulfrist.Fristmodul;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FristmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class FristModulTest {

    @Test
    void modulInLB(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Fristmodul modul = Fristmodul.builder()
            .beschreibung("Frist_Beschreibung_02")
            .fristen(
                List.of(
                    FristDauer.builder()
                        .typ("002")
                        .beschreibungen("FRIST_Beschreibung")
                        .positionDarstellung(0)
                        .dauer(1)
                        .einheit("TAG")
                        .dauerBis(5)
                        .einheitBis("TAG")
                        .build()
                )
            )
            .links(
                List.of(
                    Link.builder()
                        .uri("http://www.frist.de/")
                        .titel("Titel Frist Uri")
                        .beschreibung("Beschreibung_Frist_uri")
                        .positionDarstellung(0)
                        .build()
                )
            )
            .positionDarstellung(12)
            .build();
        leistung.setFristmodule(List.of(modul));

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        FristmodulValidator validator = new FristmodulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Fristen")
            .validateText("Frist_Beschreibung_02")
            // known issue PV-13064 .validateReplace(false)
            .validateCode("97")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.frist.de/")
            .validateLinkTitle(0, "Titel Frist Uri")
            .validateLinkDescription(0, "Beschreibung_Frist_uri")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            //.validateDetailsCode(0, "002") // known issue PV-13064
            .validateDetailsTitle(0, "Antragsfrist")
            .validateDetailsText(0, "FRIST_Beschreibung: 1 - 5 Tage")
            .validateDetailsLinkCount(0, 0)
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInLBEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Fristmodul modul = Fristmodul.builder()
            .beschreibung("Frist_Beschreibung_02")
            .beschreibungEnglisch("Frist_Beschreibung_02_EN")
            .fristen(
                List.of(
                    FristDauer.builder()
                        .typ("002")
                        .beschreibungen("FRIST_Beschreibung")
                        .beschreibungenEnglisch("FRIST_Description")
                        .positionDarstellung(0)
                        .dauer(1)
                        .einheit("TAG")
                        .dauerBis(5)
                        .einheitBis("TAG")
                        .build()
                )
            )
            .links(
                List.of(
                    Link.builder()
                        .uri("http://www.frist.de/")
                        .uriEnglisch("http://www.frist_en.en/")
                        .titel("Titel Frist Uri")
                        .titelEnglisch("Title Frist Uri EN")
                        .beschreibung("Beschreibung_Frist_uri")
                        .beschreibungEnglisch("Description_Frist_uri_EN")
                        .positionDarstellung(0)
                        .positionDarstellungEnglisch(1)
                        .build()
                )
            )
            .positionDarstellung(12)
            .build();
        leistung.setFristmodule(List.of(modul));

        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        FristmodulValidator validator = new FristmodulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Deadlines")
            .validateText("Frist_Beschreibung_02_EN")
            // known issue PV-13064 .validateReplace(false)
            .validateCode("97")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.frist_en.en/")
            .validateLinkTitle(0, "Title Frist Uri EN")
            .validateLinkDescription(0, "Description_Frist_uri_EN")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            //.validateDetailsCode(0, "002") // known issue PV-13064
            .validateDetailsTitle(0, "Application deadline")
            .validateDetailsText(0, "FRIST_Description: 1 - 5 days")
            .validateDetailsLinkCount(0, 0)
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInSP(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Fristmodul.builder()
                            .beschreibung("Frist_Beschreibung_02_spez")
                            .fristen(
                                List.of(
                                    FristDauer.builder()
                                        .typ("002")
                                        .beschreibungen("FRIST_Beschreibung_spez")
                                        .positionDarstellung(0)
                                        .dauer(1)
                                        .einheit("TAG")
                                        .dauerBis(5)
                                        .einheitBis("TAG")
                                        .build()
                                )
                            )
                            .links(
                                List.of(
                                    Link.builder()
                                        .uri("http://www.fristSpez.de/")
                                        .titel("Titel Frist Uri spez")
                                        .beschreibung("Beschreibung_Frist_uri_spez")
                                        .positionDarstellung(0)
                                        .build()
                                )
                            )
                            .positionDarstellung(12)
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait(file);
        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        FristmodulValidator validator = new FristmodulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Fristen")
            .validateText("Frist_Beschreibung_02_spez")
            //.validateReplace(true) // known issue PV-13064
            .validateCode("97")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.fristSpez.de/")
            .validateLinkTitle(0, "Titel Frist Uri spez")
            .validateLinkDescription(0, "Beschreibung_Frist_uri_spez")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            //.validateDetailsCode(0, "002") // known issue PV-13064
            .validateDetailsTitle(0, "Antragsfrist")
            .validateDetailsText(0, "FRIST_Beschreibung_spez: 1 - 5 Tage")
            .validateDetailsLinkCount(0, 0)
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInSPEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Fristmodul.builder()
                            .beschreibung("Frist_Beschreibung_02")
                            .beschreibungEnglisch("Frist_Beschreibung_02_EN")
                            .fristen(
                                List.of(
                                    FristDauer.builder()
                                        .typ("002")
                                        .beschreibungen("FRIST_Beschreibung")
                                        .beschreibungenEnglisch("FRIST_Description")
                                        .positionDarstellung(0)
                                        .dauer(1)
                                        .einheit("TAG")
                                        .dauerBis(5)
                                        .einheitBis("TAG")
                                        .build()
                                )
                            )
                            .links(
                                List.of(
                                    Link.builder()
                                        .uri("http://www.frist.de/")
                                        .uriEnglisch("http://www.frist_en.en/")
                                        .titel("Titel Frist Uri")
                                        .titelEnglisch("Title Frist Uri EN")
                                        .beschreibung("Beschreibung_Frist_uri")
                                        .beschreibungEnglisch("Description_Frist_uri_EN")
                                        .positionDarstellung(0)
                                        .positionDarstellungEnglisch(1)
                                        .build()
                                )
                            )
                            .positionDarstellung(12)
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

        FristmodulValidator validator = new FristmodulValidator(response);
        validator
            .validateType("_moduleDetail")
            .validateTitle("Deadlines")
            .validateText("Frist_Beschreibung_02_EN")
            //.validateReplace(true) // known issue PV-13064
            .validateCode("97")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.frist_en.en/")
            .validateLinkTitle(0, "Title Frist Uri EN")
            .validateLinkDescription(0, "Description_Frist_uri_EN")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            //.validateDetailsCode(0, "002") // known issue PV-13064
            .validateDetailsTitle(0, "Application deadline")
            .validateDetailsText(0, "FRIST_Description: 1 - 5 days")
            .validateDetailsLinkCount(0, 0)
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }
}
