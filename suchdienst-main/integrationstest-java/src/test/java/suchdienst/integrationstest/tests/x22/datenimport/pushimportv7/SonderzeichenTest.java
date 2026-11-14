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
package suchdienst.integrationstest.tests.x22.datenimport.pushimportv7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.PushDistributor;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristStichtag;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulbearbeitungsdauer.Bearbeitungsdauermodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Amount;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenVariabel;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.OrganisationValidator;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.location.AddressValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.BearbeitungsdauermodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.KostenmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class SonderzeichenTest {

    private static final PushDistributor distributor = new PushDistributor();

    private static final String SPECIAL_CHARACTERS =
        "A C̨̆ F K N T a̋ n̄ p̀ Æ Ä Ü Ö ß í ĩ ń Ǎ̂  ̐  ̋  , ' - ~ ‡ ! # $ % & \" ( ) * + / : ; < = > ? @ [ \\ ] ^ _ { | } ¡ ¢ £ ¥ § © ª « ¬ ® ¯ ° ± ² ³ μ ¶ ¹ º » ¿ × ÷ €¤ ½ ¼";
    private static final String SPECIAL_CHARACTERS_URI = "http://www.gidf.de/?:/#[]@!$'()*+,;=&";

    @Test
    void pushTextmodule(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Daten erstellen und einspielen *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(
            Textmodul.builder()
                .leikaCode("02")
                .inhalt(SPECIAL_CHARACTERS + " Textmodul02")
                .positionDarstellung(2)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("03")
                .inhalt(SPECIAL_CHARACTERS + " Textmodul03")
                .positionDarstellung(3)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("06")
                .inhalt(SPECIAL_CHARACTERS + " Textmodul06")
                .positionDarstellung(6)
                .build()
        );
        leistung.setTextmodule(textmodule);

        //***** Daten einspielen und abrufen *****
        ImportUtil.pushAndWait(file, distributor);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        textmodulValidator
            .validateText(0, SPECIAL_CHARACTERS + " Textmodul06")
            .validateText(1, SPECIAL_CHARACTERS + " Textmodul02")
            .validateText(2, SPECIAL_CHARACTERS + " Textmodul03");
    }

    @Test
    void pushKostenmodul(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Daten erstellen und einspielen *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.setKostenmodule(
            List.of(
                Kostenmodul.builder()
                    .beschreibung(SPECIAL_CHARACTERS + " Beschreibung des Kostenmodules")
                    .positionDarstellung(5)
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .beschreibung(SPECIAL_CHARACTERS + " Beschreibung des Links")
                                .titel(SPECIAL_CHARACTERS + " Titel des Links")
                                .uri(SPECIAL_CHARACTERS_URI)
                                .positionDarstellung(111)
                                .build()
                        )
                    )
                    .kosten(
                        List.of(
                            KostenVariabel.builder()
                                .codeTyp("001")
                                .beschreibungen(SPECIAL_CHARACTERS + " Beschreibung der Kosten")
                                .linkKostenbildung(Link.builder().uri("Link zur Kostenbildung").build())
                                .vorkasse(true)
                                .positionDarstellung(10)
                                .beschreibungenVariabilitaet(SPECIAL_CHARACTERS + " BeschreibungenVariabilitaet")
                                .betragUntergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(42)).build()
                                )
                                .betragObergrenze(
                                    Amount.builder().currencyCode("EUR").value(new BigDecimal(420)).build()
                                )
                                .build()
                        )
                    )
                    .build()
            )
        );

        //***** Daten einspielen und abrufen *****
        ImportUtil.pushAndWait(file, distributor);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        //***** Validierung *****
        KostenmodulValidator validator = new KostenmodulValidator(response);
        validator
            .validateText(SPECIAL_CHARACTERS + " Beschreibung des Kostenmodules")
            .validateLinkTitle(0, SPECIAL_CHARACTERS + " Titel des Links")
            .validateLinkDescription(0, SPECIAL_CHARACTERS + " Beschreibung des Links")
            .validateLinkUri(0, SPECIAL_CHARACTERS_URI)
            .validateDetailsText(0, SPECIAL_CHARACTERS + " Beschreibung der Kosten")
            .validateDetailsAdditionalInformation(0, 0, SPECIAL_CHARACTERS + " BeschreibungenVariabilitaet");
    }

    @Test
    void pushBearbeitungsdauermodul(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Daten erstellen und einspielen *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        leistung.setBearbeitungsdauermodule(
            List.of(
                Bearbeitungsdauermodul.builder()
                    .positionDarstellung(4)
                    .beschreibung(SPECIAL_CHARACTERS + " Beschreibung Bearbeitungsdauer")
                    .weiterfuehrendeLink(
                        List.of(
                            Link.builder()
                                .uri(SPECIAL_CHARACTERS_URI)
                                .titel(SPECIAL_CHARACTERS + " Title Link")
                                .beschreibung(SPECIAL_CHARACTERS + " Beschreibung Link")
                                .positionDarstellung(6)
                                .build()
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
                                .beschreibungen(SPECIAL_CHARACTERS + " Beschreibung FristStichtag")
                                .build()
                        )
                    )
                    .build()
            )
        );

        //***** Daten einspielen und abrufen *****
        ImportUtil.pushAndWait(file, distributor);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        //***** Validierung *****
        BearbeitungsdauermodulValidator bearbeitungsdauermodulValidator = new BearbeitungsdauermodulValidator(response);
        bearbeitungsdauermodulValidator
            .validateText(SPECIAL_CHARACTERS + " Beschreibung Bearbeitungsdauer")
            .validateLinkTitle(0, SPECIAL_CHARACTERS + " Title Link")
            .validateLinkDescription(0, SPECIAL_CHARACTERS + " Beschreibung Link")
            .validateLinkUri(0, SPECIAL_CHARACTERS_URI)
            .validateDetailsTitle(0, SPECIAL_CHARACTERS + " Beschreibung FristStichtag");
    }

    @Test
    void pushOrganisationseinheit(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Daten erstellen und einspielen *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();

        organisationseinheit.setTitel(SPECIAL_CHARACTERS + " Title");
        organisationseinheit.setBeschreibung(SPECIAL_CHARACTERS + " Beschreibung");
        organisationseinheit.setAnschriften(
            List.of(
                Anschrift.builder()
                    .typ("001")
                    .strasse("Straße")
                    .hausnummer("82")
                    .postleitzahl("20539")
                    .ort("Ort")
                    .infoParkplatz(SPECIAL_CHARACTERS + " Parkplatz")
                    .infoOEPNV(SPECIAL_CHARACTERS + " OEPNV")
                    .infoBarrierefreiheit(SPECIAL_CHARACTERS + " Barrierefreiheit")
                    .kennzeichenAufzug(false)
                    .kennzeichenRollstuhlgerecht(false)
                    .build()
            )
        );

        //***** Daten einspielen und abrufen *****
        ImportUtil.pushAndWait(file, distributor);
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(organisationseinheit.getFullId());

        OrganisationValidator organisationValidator = new OrganisationValidator(response);
        organisationValidator
            .validateTitle(SPECIAL_CHARACTERS + " Title")
            .validateDescription(SPECIAL_CHARACTERS + " Beschreibung");

        AddressValidator addressValidator = new AddressValidator(response);
        addressValidator
            .validateParking(0, SPECIAL_CHARACTERS + " Parkplatz")
            .validateOepnv(0, SPECIAL_CHARACTERS + " OEPNV")
            .validateAccessible(0, SPECIAL_CHARACTERS + " Barrierefreiheit");
    }

    @Test
    void pushOnlinedienst(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Daten erstellen und einspielen *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).od(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();

        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri(SPECIAL_CHARACTERS_URI)
                    .titel(SPECIAL_CHARACTERS + " Title")
                    .beschreibung(SPECIAL_CHARACTERS + " Beschreibung")
                    .positionDarstellung(1)
                    .build()
            )
        );

        //***** Daten einspielen und abrufen *****
        ImportUtil.pushAndWait(file, distributor);
        String ars = file.getZustaendigkeitenOnlinedienst().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, leistung.getFullId());

        OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
        onlineServiceLinkValidator
            .validateTitle(0, SPECIAL_CHARACTERS + " Title")
            .validateDescription(0, SPECIAL_CHARACTERS + " Beschreibung")
            .validateUri(0, SPECIAL_CHARACTERS_URI);
    }
}
