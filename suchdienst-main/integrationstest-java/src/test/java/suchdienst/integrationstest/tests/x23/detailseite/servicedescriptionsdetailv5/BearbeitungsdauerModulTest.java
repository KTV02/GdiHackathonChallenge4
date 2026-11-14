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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.modul.BearbeitungsdauermodulCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristDauer;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristStichtag;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulbearbeitungsdauer.Bearbeitungsdauermodul;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.BearbeitungsdauermodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class BearbeitungsdauerModulTest {

    @Test
    void modulInLBFristStichtag(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                        .titel("Titel des links")
                        .beschreibung("Beschreibung des Links")
                        .positionDarstellung(6)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp")
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2001)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2001)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            // known issue PV-13064 .validateReplace(false)
            .validateType("_moduleDetail")
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkTitle(0, "Titel des links")
            .validateLinkDescription(0, "Beschreibung des Links")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001")
            .validateDetailsType(0, "_module")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInLBFristDauer(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                        .titel("Titel des links")
                        .beschreibung("Beschreibung des Links")
                        .positionDarstellung(6)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristDauer.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp")
                        .positionDarstellung(4)
                        .dauer(15848)
                        .einheit("WERKTAG")
                        .dauerBis(848498484)
                        .einheitBis("SEKUNDE")
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            // known issue PV-13064 .validateReplace(false)
            .validateType("_moduleDetail")
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkTitle(0, "Titel des links")
            .validateLinkDescription(0, "Beschreibung des Links")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "15848 Werktage bis 848498484 Sekunden")
            .validateDetailsType(0, "_module")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInLBEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .beschreibungEnglisch("Duration_Description_EN")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                        .uriEnglisch("http://www.duration.en/")
                        .titel("Titel des links")
                        .titelEnglisch("Duration Link Titel EN")
                        .beschreibung("Beschreibung des Links")
                        .beschreibungEnglisch("Duration Link Description EN")
                        .positionDarstellung(6)
                        .positionDarstellungEnglisch(1)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp")
                        .beschreibungenEnglisch("Frist Duration Description EN")
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2001)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2001)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Processing time")
            .validateText("Duration_Description_EN")
            // known issue PV-13064 .validateReplace(false)
            .validateType("_moduleDetail")
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.duration.en/")
            .validateLinkTitle(0, "Duration Link Titel EN")
            .validateLinkDescription(0, "Duration Link Description EN")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Frist Duration Description EN")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001")
            .validateDetailsType(0, "_module")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Testdaten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.setBearbeitungsdauermodule(List.of(BearbeitungsdauermodulCreator.getInstance().getMax()));
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        ModulSpezialisierung modul = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Bearbeitungsdauermodul.builder()
                    .beschreibung("Beschreibung des Modules")
                    .positionDarstellung(5)
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                                .titel("Titel des links")
                                .beschreibung("Beschreibung des Links")
                                .positionDarstellung(6)
                                .build()
                        )
                    )
                    .bearbeitungsdauer(
                        List.of(
                            FristStichtag.builder()
                                .beschreibungen("Beschreibung von FristOhneTyp")
                                .positionDarstellung(4)
                                .vonTag(26)
                                .vonMonat(10)
                                .vonJahr(2001)
                                .bisTag(26)
                                .bisMonat(10)
                                .bisJahr(2001)
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung.getModulSpezialisierungen().add(modul);

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        //Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            //.validateReplace(true) // known issue PV-13064
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkTitle(0, "Titel des links")
            .validateLinkDescription(0, "Beschreibung des Links")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false)
            .validateDetailsAdditionalInformationCount(0, 0);
    }

    @Test
    void modulInSPEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        ModulSpezialisierung modul = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Bearbeitungsdauermodul.builder()
                    .beschreibung("Beschreibung des Modules")
                    .beschreibungEnglisch("Duration_Description_EN")
                    .positionDarstellung(5)
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                                .uriEnglisch("http://www.duration.en/")
                                .titel("Titel des links")
                                .titelEnglisch("Duration Link Titel EN")
                                .beschreibung("Beschreibung des Links")
                                .beschreibungEnglisch("Duration Link Description EN")
                                .positionDarstellung(6)
                                .positionDarstellungEnglisch(1)
                                .build()
                        )
                    )
                    .bearbeitungsdauer(
                        List.of(
                            FristStichtag.builder()
                                .beschreibungen("Beschreibung von FristOhneTyp")
                                .beschreibungenEnglisch("Frist Duration Description EN")
                                .positionDarstellung(4)
                                .vonTag(26)
                                .vonMonat(10)
                                .vonJahr(2001)
                                .bisTag(26)
                                .bisMonat(10)
                                .bisJahr(2001)
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung.getModulSpezialisierungen().add(modul);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Processing time")
            .validateText("Duration_Description_EN")
            //.validateReplace(true) // known issue PV-13064
            .validateType("_moduleDetail")
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.duration.en/")
            .validateLinkTitle(0, "Duration Link Titel EN")
            .validateLinkDescription(0, "Duration Link Description EN")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Frist Duration Description EN")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001")
            .validateDetailsType(0, "_module")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void modulInLBFristDauerOhneEinheitBis(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                        .titel("Titel des links")
                        .beschreibung("Beschreibung des Links")
                        .positionDarstellung(6)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristDauer.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp")
                        .positionDarstellung(4)
                        .dauer(3)
                        .einheit("TAG")
                        .dauerBis(6)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            // known issue PV-13064 .validateReplace(false)
            .validateType("_moduleDetail")
            .validateCode("95")
            .validateFilled(true)
            .validatePlaceholder(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkTitle(0, "Titel des links")
            .validateLinkDescription(0, "Beschreibung des Links")
            .validateLinkReplace(0, false)
            .validateLinkDynamicParameterCount(0, 0)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "3 - 6 Tage")
            .validateDetailsType(0, "_module")
            .validateDetailsLinkCount(0, 0)
            .validateDetailsAdditionalInformationCount(0, 0)
            .validateDetailsFilled(0, true)
            .validateDetailsPlaceholder(0, false);
    }

    @Test
    void manyListItems(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul_Link1")
                        .titel("Titel des Links1")
                        .beschreibung("Beschreibung des Links1")
                        .positionDarstellung(6)
                        .build(),
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul_Link2")
                        .titel("Titel des Links2")
                        .beschreibung("Beschreibung des Links2")
                        .positionDarstellung(7)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp1")
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2001)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2001)
                        .build(),
                    FristStichtag.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp2")
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2002)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2002)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            // known issue PV-13064 .validateReplace(false)
            .validateLinkCount(2)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul_Link1")
            .validateLinkTitle(0, "Titel des Links1")
            .validateLinkDescription(0, "Beschreibung des Links1")
            .validateLinkUri(1, "https://lmgtfy.com/?q=Bearbeitungsdauermodul_Link2")
            .validateLinkTitle(1, "Titel des Links2")
            .validateLinkDescription(1, "Beschreibung des Links2")
            .validateLinkReplace(0, false)
            .validateDetailsCount(2)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp1")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001")
            .validateDetailsTitle(1, "Beschreibung von FristOhneTyp2")
            // known issue PV-13064 .validateDetailsReplace(1, false)
            .validateDetailsCode(1, "95")
            .validateDetailsText(1, "26.10.2002 - 26.10.2002");
    }

    @Test
    void notCompleteDate(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("Beschreibung des Modules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul")
                        .titel("Titel des links")
                        .beschreibung("Beschreibung des Links")
                        .positionDarstellung(6)
                        .build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .beschreibungen("Beschreibung von FristOhneTyp")
                        .positionDarstellung(4)
                        .vonTag(1)
                        .vonMonat(5)
                        .bisTag(1)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung des Modules")
            // known issue PV-13064 .validateReplace(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkTitle(0, "Titel des links")
            .validateLinkDescription(0, "Beschreibung des Links")
            .validateLinkReplace(0, false)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Beschreibung von FristOhneTyp")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "01.05. - 01.");
    }

    @Test
    void emptyValues(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Testdaten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .beschreibung("")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(Link.builder().uri("").titel("").beschreibung("").positionDarstellung(6).build())
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .beschreibungen("")
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2001)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2001)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("")
            // known issue PV-13064 .validateReplace(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "")
            .validateLinkTitle(0, "")
            .validateLinkDescription(0, "")
            .validateLinkReplace(0, false)
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "")
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001");
    }

    @Test
    void minModul(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Testdaten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.setBearbeitungsdauermodule(
            List.of(Bearbeitungsdauermodul.builder().beschreibung("Beschreibung").positionDarstellung(4).build())
        );

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            .validateText("Beschreibung")
            // known issue PV-13064 .validateReplace(false)
            .validateLinkCount(0)
            .validateDetailsCount(0);
    }

    @Test
    void minChildren(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Testdaten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Bearbeitungsdauermodul bearbeitungsdauermodul = Bearbeitungsdauermodul.builder()
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder().uri("https://lmgtfy.com/?q=Bearbeitungsdauermodul").positionDarstellung(6).build()
                )
            )
            .bearbeitungsdauer(
                List.of(
                    FristStichtag.builder()
                        .positionDarstellung(4)
                        .vonTag(26)
                        .vonMonat(10)
                        .vonJahr(2001)
                        .bisTag(26)
                        .bisMonat(10)
                        .bisJahr(2001)
                        .build()
                )
            )
            .build();
        leistung.setBearbeitungsdauermodule(List.of(bearbeitungsdauermodul));

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        // Validierung
        BearbeitungsdauermodulValidator validator = new BearbeitungsdauermodulValidator(response);
        validator
            .validateCount(1)
            .validateTitle("Bearbeitungsdauer")
            // known issue PV-13064 .validateReplace(false)
            .validateLinkCount(1)
            .validateLinkUri(0, "https://lmgtfy.com/?q=Bearbeitungsdauermodul")
            .validateLinkReplace(0, false)
            .validateDetailsCount(1)
            // known issue PV-13064 .validateDetailsReplace(0, false)
            .validateDetailsCode(0, "95")
            .validateDetailsText(0, "26.10.2001 - 26.10.2001");
    }
}
