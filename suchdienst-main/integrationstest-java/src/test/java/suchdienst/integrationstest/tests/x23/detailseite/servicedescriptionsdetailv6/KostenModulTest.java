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
import de.dataport.pvog.testdatengenerator.creator.modul.KostenmodulCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.Modul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Amount;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenVariabel;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.math.BigDecimal;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.KostenmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class KostenModulTest {

    @Test
    void modulInLB(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Kostenmodul kostenmodul = Kostenmodul.builder()
            .beschreibung("Beschreibung des Kostenmodules")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .beschreibung("Beschreibung des Links")
                        .titel("Titel des Links")
                        .uri("Uri des Links")
                        .positionDarstellung(111)
                        .build()
                )
            )
            .kosten(
                List.of(
                    KostenVariabel.builder()
                        .codeTyp("001")
                        .beschreibungen("Beschreibung der Kosten")
                        .linkKostenbildung(Link.builder().uri("Link zur Kostenbildung").build())
                        .kassenzeichen("Kassenzeichen")
                        .vorkasse(true)
                        .positionDarstellung(10)
                        .betragUntergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(40)).build())
                        .betragObergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(80)).build())
                        .build()
                )
            )
            .build();
        leistung.setKostenmodule(List.of(kostenmodul));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateType("_moduleDetail")
            .validateTitle("Kosten & Gebühren")
            .validateText("Beschreibung des Kostenmodules")
            .validateCode("98")
            // Validierung Link
            .validateLinkCount(1)
            .validateLinkUri(0, "Uri des Links")
            .validateLinkTitle(0, "Titel des Links")
            .validateLinkDescription(0, "Beschreibung des Links")
            // Validierung Details
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Abgabe: 40,00 Euro - 80,00 Euro")
            .validateDetailsText(0, "Beschreibung der Kosten")
            // known issue PV-13064 .validateDetailsCode(0, "001")
            .validateDetailsLink(0, "Link zur Kostenbildung")
            .validateDetailsAdditionalInformationCount(0, 1)
            .validateDetailsVorkasse(0, "Ja")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());
    }

    @Test
    void modulInLBEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).zsOe(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Kostenmodul kostenmodul = Kostenmodul.builder()
            .beschreibung("Beschreibung des Kostenmodules")
            .beschreibungEnglisch("COSTS_BESCHREIBUNG_EN")
            .positionDarstellung(5)
            .weiterfuehrendeLink(
                List.of(
                    Link.builder()
                        .beschreibung("Beschreibung des Links")
                        .beschreibungEnglisch("Costmodul Link Description EN")
                        .titel("Titel des Links")
                        .titelEnglisch("Costmodul Link Titel EN")
                        .uri("Uri des Links")
                        .uriEnglisch("http://www.costmodul.en/")
                        .positionDarstellung(111)
                        .build()
                )
            )
            .kosten(
                List.of(
                    KostenVariabel.builder()
                        .codeTyp("001")
                        .beschreibungen("Beschreibung der Kosten")
                        .beschreibungenEnglisch("Description_of_costs_EN")
                        .kassenzeichen("Kassenzeichen")
                        .vorkasse(true)
                        .positionDarstellung(10)
                        .betragUntergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(40)).build())
                        .betragObergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(80)).build())
                        .build()
                )
            )
            .build();
        leistung.setKostenmodule(List.of(kostenmodul));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateType("_moduleDetail")
            .validateTitle("Expenses & Charges")
            .validateText("COSTS_BESCHREIBUNG_EN")
            .validateCode("98")
            // Validierung Link
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.costmodul.en/")
            .validateLinkTitle(0, "Costmodul Link Titel EN")
            .validateLinkDescription(0, "Costmodul Link Description EN")
            // Validierung Details
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "emission: 40.00 Euro - 80.00 Euro")
            .validateDetailsText(0, "Description_of_costs_EN")
            // known issue PV-13064 .validateDetailsCode(0, "001")
            .validateDetailsAdditionalInformationCount(0, 1)
            .validateDetailsAdditionalInformationByKey(0, "Prepayment", "Yes")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());
    }

    @Test
    void modulInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.setKostenmodule(List.of(KostenmodulCreator.getInstance().getMax()));
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        ModulSpezialisierung modul = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Kostenmodul.builder()
                    .beschreibung("Beschreibung des Kostenmodules")
                    .positionDarstellung(5)
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .beschreibung("Beschreibung des Links")
                                .titel("Titel des Links")
                                .uri("Uri des Links")
                                .positionDarstellung(111)
                                .build()
                        )
                    )
                    .kosten(
                        List.of(
                            KostenVariabel.builder()
                                .codeTyp("001")
                                .beschreibungen("Beschreibung der Kosten")
                                .linkKostenbildung(Link.builder().uri("Link zur Kostenbildung").build())
                                .kassenzeichen("Kassenzeichen")
                                .vorkasse(true)
                                .positionDarstellung(10)
                                .betragUntergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(40)).build()
                                )
                                .betragObergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(80)).build()
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung.getModulSpezialisierungen().add(modul);

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateType("_moduleDetail")
            .validateTitle("Kosten & Gebühren")
            .validateText("Beschreibung des Kostenmodules")
            .validateCode("98")
            // Validierung Link
            .validateLinkCount(1)
            .validateLinkUri(0, "Uri des Links")
            .validateLinkTitle(0, "Titel des Links")
            .validateLinkDescription(0, "Beschreibung des Links")
            // Validierung Details
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "Abgabe: 40,00 Euro - 80,00 Euro")
            .validateDetailsText(0, "Beschreibung der Kosten")
            // known issue PV-13064 .validateDetailsCode(0, "001")
            .validateDetailsLink(0, "Link zur Kostenbildung")
            .validateDetailsAdditionalInformationCount(0, 1)
            .validateDetailsVorkasse(0, "Ja")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());
    }

    @Test
    void modulInSPEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        ModulSpezialisierung modul = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Kostenmodul.builder()
                    .beschreibung("Beschreibung des Kostenmodules")
                    .beschreibungEnglisch("COSTS_BESCHREIBUNG_EN")
                    .positionDarstellung(5)
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .beschreibung("Beschreibung des Links")
                                .beschreibungEnglisch("Costmodul Link Description EN")
                                .titel("Titel des Links")
                                .titelEnglisch("Costmodul Link Titel EN")
                                .uri("Uri des Links")
                                .uriEnglisch("http://www.costmodul.en/")
                                .positionDarstellung(111)
                                .build()
                        )
                    )
                    .kosten(
                        List.of(
                            KostenVariabel.builder()
                                .codeTyp("001")
                                .beschreibungen("Beschreibung der Kosten")
                                .beschreibungenEnglisch("Description_of_costs_EN")
                                .kassenzeichen("Kassenzeichen")
                                .vorkasse(true)
                                .positionDarstellung(10)
                                .betragUntergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(40)).build()
                                )
                                .betragObergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(80)).build()
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung.getModulSpezialisierungen().add(modul);

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateType("_moduleDetail")
            .validateTitle("Expenses & Charges")
            .validateText("COSTS_BESCHREIBUNG_EN")
            .validateCode("98")
            // Validierung Link
            .validateLinkCount(1)
            .validateLinkUri(0, "http://www.costmodul.en/")
            .validateLinkTitle(0, "Costmodul Link Titel EN")
            .validateLinkDescription(0, "Costmodul Link Description EN")
            // Validierung Details
            .validateDetailsCount(1)
            .validateDetailsTitle(0, "emission: 40.00 Euro - 80.00 Euro")
            .validateDetailsText(0, "Description_of_costs_EN")
            // known issue PV-13064 .validateDetailsCode(0, "001")
            .validateDetailsAdditionalInformationCount(0, 1)
            .validateDetailsAdditionalInformationByKey(0, "Prepayment", "Yes")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());
    }

    @Test
    void reihenfolgeKosten(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen und einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Kostenmodul kostenmodul = Kostenmodul.builder()
            .positionDarstellung(5)
            .kosten(
                List.of(
                    KostenVariabel.builder()
                        .codeTyp("001")
                        .positionDarstellung(20)
                        .beschreibungen("Beschreibung")
                        .betragUntergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(20)).build())
                        .betragObergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(200)).build())
                        .build(),
                    KostenVariabel.builder()
                        .codeTyp("001")
                        .positionDarstellung(10)
                        .betragUntergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(10)).build())
                        .betragObergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(100)).build())
                        .build(),
                    KostenVariabel.builder()
                        .codeTyp("001")
                        .positionDarstellung(30)
                        .betragUntergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(30)).build())
                        .betragObergrenze(Amount.builder().currencyCode("EUR").value(new BigDecimal(300)).build())
                        .build()
                )
            )
            .build();
        leistung.setKostenmodule(List.of(kostenmodul));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, false);
        validator
            .validateCount(1)
            .validateType("_moduleDetail")
            .validateTitle("Kosten & Gebühren")
            // Validierung Details
            .validateDetailsCount(3)
            .validateDetailsTitle(0, "Abgabe: 10,00 Euro - 100,00 Euro")
            .validateDetailsTitle(1, "Abgabe: 20,00 Euro - 200,00 Euro")
            .validateDetailsText(1, "Beschreibung")
            .validateDetailsTitle(2, "Abgabe: 30,00 Euro - 300,00 Euro")
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE_OFFLINE.name());
    }
}
