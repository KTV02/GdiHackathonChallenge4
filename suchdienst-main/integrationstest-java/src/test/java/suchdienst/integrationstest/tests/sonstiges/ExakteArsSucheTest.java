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
package suchdienst.integrationstest.tests.sonstiges;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.RelationsConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.utils.WaitUtil;
import suchdienst.integrationstest.validator.relations.RelationV3ResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel: Es wird validiert, dass bei einer exakten ARS Suche nur die Informationen gefunden werden, die genau zu dieser ARS gehören.
 */
class ExakteArsSucheTest {

    public record ArsBundesland(Bundesland bundesland, String arsKreis, String arsGemeinde) {}

    private static final ArsBundesland arsBundeslandSH = new ArsBundesland(
        Bundesland.SH,
        "010600000000",
        "010605024012"
    );
    private static final ArsBundesland arsBundeslandHH = new ArsBundesland(
        Bundesland.HH,
        "020600000000",
        "020605024012"
    );
    private static final ArsBundesland arsBundeslandNI = new ArsBundesland(
        Bundesland.NI,
        "033510000000",
        "033510004004"
    );
    private static final ArsBundesland arsBundeslandHB = new ArsBundesland(
        Bundesland.HB,
        "040110000000",
        "040110004004"
    );
    private static final ArsBundesland arsBundeslandNW = new ArsBundesland(
        Bundesland.NW,
        "051660000000",
        "051660004004"
    );
    private static final ArsBundesland arsBundeslandHE = new ArsBundesland(
        Bundesland.HE,
        "064310000000",
        "064310017017"
    );
    private static final ArsBundesland arsBundeslandRP = new ArsBundesland(
        Bundesland.RP,
        "073310000000",
        "073315001001"
    );
    private static final ArsBundesland arsBundeslandBW = new ArsBundesland(
        Bundesland.BW,
        "081170000000",
        "081170010010"
    );
    private static final ArsBundesland arsBundeslandBY = new ArsBundesland(
        Bundesland.BY,
        "091740000000",
        "091740111111"
    );
    private static final ArsBundesland arsBundeslandSL = new ArsBundesland(
        Bundesland.SL,
        "100460000000",
        "100460111111"
    );
    private static final ArsBundesland arsBundeslandBE = new ArsBundesland(
        Bundesland.BE,
        "110460000000",
        "110460111111"
    );
    private static final ArsBundesland arsBundeslandBB = new ArsBundesland(
        Bundesland.BB,
        "120710000000",
        "120715101028"
    );
    private static final ArsBundesland arsBundeslandMV = new ArsBundesland(
        Bundesland.MV,
        "130760000000",
        "130765652009"
    );
    private static final ArsBundesland arsBundeslandSN = new ArsBundesland(
        Bundesland.SN,
        "146260000000",
        "146265220150"
    );
    private static final ArsBundesland arsBundeslandST = new ArsBundesland(
        Bundesland.ST,
        "150890000000",
        "150895051041"
    );
    private static final ArsBundesland arsBundeslandTH = new ArsBundesland(
        Bundesland.TH,
        "160740000000",
        "160745005012"
    );
    private static final List<ArsBundesland> arsBundeslaender = List.of(
        arsBundeslandSH,
        arsBundeslandHH,
        arsBundeslandNI,
        arsBundeslandHB,
        arsBundeslandNW,
        arsBundeslandHE,
        arsBundeslandRP,
        arsBundeslandBW,
        arsBundeslandBY,
        arsBundeslandSL,
        arsBundeslandBE,
        arsBundeslandBB,
        arsBundeslandMV,
        arsBundeslandSN,
        arsBundeslandST,
        arsBundeslandTH
    );

    public record ArsValidationValues(
        String bundeslandKuerzel,
        String arsGemeinde,
        String arsKreis,
        String lbId,
        String lbFullId,
        String lbBeschreibung,
        String lbName,
        String leika,
        String oeGemeindeId,
        String oeKreisId,
        String oeGemeindeFullId,
        String oeKreisFullId,
        String odGemeindeId,
        String odKreisId,
        String odGemeindeFullId,
        String odKreisFullId,
        String odGemeindeUrl,
        String odKreisUrl,
        String spezModultext
    ) {}

    private static List<ArsValidationValues> testData;

    private static final List<String> stadtStaaten = List.of(
        Bundesland.HH.name(),
        Bundesland.BE.name(),
        Bundesland.HB.name()
    );

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        //warte auf Löschen des Datenbestands
        WaitUtil.waitFor(2, TimeUnit.SECONDS);

        testData = createTestData(testInfo);
    }

    @DisplayName("Exakte Suche - Trefferliste ")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void trefferlisteExakteSuche(ArsValidationValues arsValidationValues) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true - Gemeinde wird abgefragt
        // alle Onlinedienste gefunden und die Spezialisierung greift.
        // useArsHierarchy=tru liefert nur für gespeicherte Ars Schluessel einen Treffer
        // Testdaten von Stadtstaaten sind nicht in der ArsListe enthalten und werden daher für useArsHierarchy=true nicht beruecksichtigt
        if (!stadtStaaten.contains(arsValidationValues.bundeslandKuerzel)) {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(
                arsValidationValues.arsGemeinde,
                arsValidationValues.lbBeschreibung,
                true
            );
            ServiceDescriptionResultListValidator validatorServiceDescriptions =
                new ServiceDescriptionResultListValidator(response);
            validatorServiceDescriptions
                .validateContains(arsValidationValues.lbFullId)
                .validateOnlineServiceLinkHasUri(arsValidationValues.lbFullId, arsValidationValues.odGemeindeUrl)
                .validateOnlineServiceLinkHasUri(arsValidationValues.lbFullId, arsValidationValues.odKreisUrl)
                .validateOnlineServiceLinksCount(arsValidationValues.lbFullId, 2)
                .validateName(0, arsValidationValues.spezModultext);
        }
        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars der Gemeinde gesucht
        // nur der Onlinedienst der Gemeinde wird gefunden
        // Spezialisierung ist nur für Kreis gültig und greift hier nicht

        Response responseArsGemeinde = ServiceDescriptionConnector.getServiceDescriptionsV8(
            arsValidationValues.arsGemeinde,
            arsValidationValues.lbBeschreibung,
            false
        );
        ServiceDescriptionResultListValidator validatorServiceDescriptionsGemeinde =
            new ServiceDescriptionResultListValidator(responseArsGemeinde);
        validatorServiceDescriptionsGemeinde
            .validateContains(arsValidationValues.lbFullId)
            .validateOnlineServiceLinkHasUri(arsValidationValues.lbFullId, arsValidationValues.odGemeindeUrl)
            .validateOnlineServiceLinksCount(arsValidationValues.lbFullId, 1)
            .validateName(0, arsValidationValues.lbName);

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars des Kreises gesucht
        // nur der Onlinedienst des Kreises wird gefunden
        // Spezialisierung war für Kreis definiert greift
        Response responseArsKreis = ServiceDescriptionConnector.getServiceDescriptionsV8(
            arsValidationValues.arsKreis,
            arsValidationValues.lbBeschreibung,
            false
        );
        ServiceDescriptionResultListValidator validatorServiceDescriptionsKreis =
            new ServiceDescriptionResultListValidator(responseArsKreis);
        validatorServiceDescriptionsKreis
            .validateContains(arsValidationValues.lbFullId)
            .validateOnlineServiceLinkHasUri(arsValidationValues.lbFullId, arsValidationValues.odKreisUrl)
            .validateOnlineServiceLinksCount(arsValidationValues.lbFullId, 1)
            .validateName(0, arsValidationValues.spezModultext);
    }

    @DisplayName("Exakte Suche - Detailseite")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void detailsV5ExakteSuche(ArsValidationValues arsValidationValues) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt
        // es werden alle Onlinedienste, Organisationseinheiten gefunden und die Spezialisierung greift.
        // useArsHierarchy=tru liefert nur für gespeicherte Ars Schluessel einen Treffer
        // Testdaten von Stadtstaaten sind nicht in der ArsListe enthalten und werden daher für useArsHierarchy=true nicht beruecksichtigt
        if (!stadtStaaten.contains(arsValidationValues.bundeslandKuerzel)) {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
                arsValidationValues.arsGemeinde,
                arsValidationValues.lbFullId,
                true
            );
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(response);
            validatorDetail
                .validateId(arsValidationValues.lbFullId)
                .validateName(arsValidationValues.spezModultext)
                .validateOrganisationIdsContains(arsValidationValues.oeGemeindeFullId)
                .validateOrganisationIdsContains(arsValidationValues.oeKreisFullId)
                .validateOrganisationIdsCount(2);
            OnlineServiceLinkValidator onlineServiceLinkValidator = new OnlineServiceLinkValidator(response);
            onlineServiceLinkValidator
                .validateId(0, arsValidationValues.odGemeindeFullId)
                .validateId(1, arsValidationValues.odKreisFullId)
                .validateCount(2);
        }
        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars der Gemeinde gesucht
        // nur die zugehörigen (Gemeinde) Informationen werden erwartet.

        Response responseArsGemeinde = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsValidationValues.arsGemeinde,
            arsValidationValues.lbFullId,
            false
        );
        ServiceDescriptionValidator validatorDetailArsGemeinde = new ServiceDescriptionValidator(responseArsGemeinde);
        validatorDetailArsGemeinde
            .validateId(arsValidationValues.lbFullId)
            .validateName(arsValidationValues.lbName)
            .validateOrganisationIdsContains(arsValidationValues.oeGemeindeFullId)
            .validateOrganisationIdsCount(1);
        OnlineServiceLinkValidator onlineServiceLinkGemeindeValidator = new OnlineServiceLinkValidator(
            responseArsGemeinde
        );
        onlineServiceLinkGemeindeValidator.validateCount(1).validateId(0, arsValidationValues.odGemeindeFullId);

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars des Kreises gesucht
        // nur die zugehörigen (Kreis) Informationen werden erwartet - Spezialisierung greift.
        Response responseArsKreis = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            arsValidationValues.arsKreis,
            arsValidationValues.lbFullId,
            false
        );
        ServiceDescriptionValidator validatorDetailArsKreis = new ServiceDescriptionValidator(responseArsKreis);
        validatorDetailArsKreis
            .validateId(arsValidationValues.lbFullId)
            .validateName(arsValidationValues.spezModultext)
            .validateOrganisationIdsContains(arsValidationValues.oeKreisFullId)
            .validateOrganisationIdsCount(1);
        OnlineServiceLinkValidator onlineServiceLinkKreisValidator = new OnlineServiceLinkValidator(responseArsKreis);
        onlineServiceLinkKreisValidator.validateCount(1).validateId(0, arsValidationValues.odKreisFullId);
    }

    @DisplayName("Exakte Suche - LeiKa ID Suche")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void leikaV3ExakteSuche(ArsValidationValues arsValidationValues) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt,
        // und es werden alle Onlinedienste, Organisationseinheiten gefunden und die Spezialisierung greift.
        // useArsHierarchy=tru liefert nur für gespeicherte Ars Schluessel einen Treffer
        // Testdaten von Stadtstaaten sind nicht in der ArsListe enthalten und werden daher für useArsHierarchy=true nicht beruecksichtigt
        if (!stadtStaaten.contains(arsValidationValues.bundeslandKuerzel)) {
            Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
                arsValidationValues.arsGemeinde,
                arsValidationValues.leika,
                true
            );
            LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
            validatorLeika
                .validateContains(arsValidationValues.lbFullId)
                .validateOnlineServiceLinksUri(0, 0, arsValidationValues.odGemeindeUrl)
                .validateOnlineServiceLinksUri(0, 1, arsValidationValues.odKreisUrl)
                .validateOnlineServiceLinksCount(0, 2)
                .validateName(0, arsValidationValues.spezModultext);
        }

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars der Gemeinde gesucht,
        // nur die zugehörigen (Gemeinde) Informationen werden erwartet.
        Response responseArsGemeinde = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            arsValidationValues.arsGemeinde,
            arsValidationValues.leika,
            false
        );
        LeikaIdResultListValidator validatorLeikaGemeinde = new LeikaIdResultListValidator(responseArsGemeinde);
        validatorLeikaGemeinde
            .validateContains(arsValidationValues.lbFullId)
            .validateOnlineServiceLinksUri(0, 0, arsValidationValues.odGemeindeUrl)
            .validateOnlineServiceLinksCount(0, 1)
            .validateName(0, arsValidationValues.lbName);

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars des Kreises gesucht
        // nur die zugehörigen (Kreis) Informationen werden erwartet - Spezialisierung greift.
        Response responseArsKreis = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            arsValidationValues.arsKreis,
            arsValidationValues.leika,
            false
        );
        LeikaIdResultListValidator validatorLeikaKreis = new LeikaIdResultListValidator(responseArsKreis);
        validatorLeikaKreis
            .validateContains(arsValidationValues.lbFullId)
            .validateOnlineServiceLinksUri(0, 0, arsValidationValues.odKreisUrl)
            .validateOnlineServiceLinksCount(0, 1)
            .validateName(0, arsValidationValues.spezModultext);
    }

    @DisplayName("Exakte Suche - Relations")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void relationV3ExakteSuche(ArsValidationValues arsValidationValues) {
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt,
        // und es werden alle Onlinedienste und Organisationseinheiten gefunden.
        Response response = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(arsValidationValues.lbFullId),
            arsValidationValues.arsGemeinde,
            true,
            Constants.XzufiVersion.V2_2_0
        );
        String oeGemeindeArsIdValue =
            arsValidationValues.lbId + "." + arsValidationValues.oeGemeindeId + "." + arsValidationValues.arsGemeinde;
        String oeKreisArsIdValue =
            arsValidationValues.lbId + "." + arsValidationValues.oeKreisId + "." + arsValidationValues.arsKreis;
        String odGemeindeArsIdValue =
            arsValidationValues.lbId + "." + arsValidationValues.odGemeindeId + "." + arsValidationValues.arsGemeinde;
        String odKreisArsIdValue =
            arsValidationValues.lbId + "." + arsValidationValues.odKreisId + "." + arsValidationValues.arsKreis;

        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(4)
            .validateParentIdValue(0, arsValidationValues.odGemeindeId)
            .validateIdValue(0, odGemeindeArsIdValue)
            .validateParentIdValue(1, arsValidationValues.odKreisId)
            .validateIdValue(1, odKreisArsIdValue)
            .validateParentIdValue(2, arsValidationValues.oeGemeindeId)
            .validateIdValue(2, oeGemeindeArsIdValue)
            .validateParentIdValue(3, arsValidationValues.oeKreisId)
            .validateIdValue(3, oeKreisArsIdValue);

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars der Gemeinde gesucht
        // nur die zugehörigen (Gemeinde) Informationen werden erwartet.
        Response responseGemeindeArs = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(arsValidationValues.lbFullId),
            arsValidationValues.arsGemeinde,
            false,
            Constants.XzufiVersion.V2_2_0
        );
        RelationV3ResultListValidator validatorGemeindeArs = new RelationV3ResultListValidator(responseGemeindeArs);
        validatorGemeindeArs
            .validateCount(2)
            .validateParentIdValue(0, arsValidationValues.odGemeindeId)
            .validateIdValue(0, odGemeindeArsIdValue)
            .validateParentIdValue(1, arsValidationValues.oeGemeindeId)
            .validateIdValue(1, oeGemeindeArsIdValue);

        // UseHierarchy Flag ist auf false gesetzt,
        // es wird mit der Ars des Kreises gesucht
        // nur die zugehörigen (Kreis) Informationen werden erwartet
        Response responseKreisArs = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(arsValidationValues.lbFullId),
            arsValidationValues.arsKreis,
            false,
            Constants.XzufiVersion.V2_2_0
        );
        RelationV3ResultListValidator validatorKreisArs = new RelationV3ResultListValidator(responseKreisArs);
        validatorKreisArs
            .validateCount(2)
            .validateParentIdValue(0, arsValidationValues.odKreisId)
            .validateIdValue(0, odKreisArsIdValue)
            .validateParentIdValue(1, arsValidationValues.oeKreisId)
            .validateIdValue(1, oeKreisArsIdValue);
    }

    private static List<ArsValidationValues> createTestData(TestInfo testInfo) {
        List<ArsValidationValues> arsValidationValuesList = new ArrayList<>();
        int leikaIdCounter = 10;
        String testcasename = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        String importId = null;
        for (ArsBundesland arsBundesland : arsBundeslaender) {
            // Testdaten

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmm");
            String leikaId = "9900" + leikaIdCounter + "04" + dateFormat.format(timestamp);

            BundeslandConfig bundeslandConfig = new BundeslandConfig(arsBundesland.bundesland());
            XzufiFile xzufiFile = XzufiFileCreator.newInstance(bundeslandConfig)
                .name(testcasename)
                .lb(1)
                .oe(2)
                .od(2)
                .sp(1)
                .getMin();

            // Für die Tests werden die ARS des Gemeindeverbandes und des Kreises verwendet.
            String arsGemeinde = arsBundesland.arsGemeinde();
            String arsKreis = arsBundesland.arsKreis();

            Leistungsbeschreibung lb = xzufiFile.getLeistungsbeschreibungen().getFirst();
            String lbId = lb.getId();
            String lbFullId = lb.getFullId();
            String lbBeschreibung = lb.getTextmodule().get(0).getInhalt();
            String lbName = lb.getTextmodule().get(1).getInhalt();
            lb.setReferenzLeiKa(leikaId);
            String lbLeika = lb.getReferenzLeiKa();

            Organisationseinheit oeGemeinde = xzufiFile.getOrganisationseinheiten().get(0);
            Organisationseinheit oeKreis = xzufiFile.getOrganisationseinheiten().get(1);

            String oeGemeindeFullId = oeGemeinde.getFullId();
            String oeKreisFullId = oeKreis.getFullId();

            String oeGemeindeId = oeGemeinde.getId();
            String oeKreisId = oeKreis.getId();

            Onlinedienst odGemeinde = xzufiFile.getOnlinedienste().get(0);
            Onlinedienst odKreis = xzufiFile.getOnlinedienste().get(1);

            String odGemeindeFullId = odGemeinde.getFullId();
            String odKreisFullId = odKreis.getFullId();

            String odGemeindeId = odGemeinde.getId();
            String odKreisId = odKreis.getId();

            odGemeinde
                .getLinks()
                .getFirst()
                .setUri("https://lmgtfy.com/?q=" + arsGemeinde);
            odKreis
                .getLinks()
                .getFirst()
                .setUri("https://lmgtfy.com/?q=" + arsKreis);
            String odGemeindeUrl = odGemeinde.getLinks().getFirst().getUri();
            String odKreisUrl = odKreis.getLinks().getFirst().getUri();

            Zustaendigkeit zsoeGemeinde = xzufiFile.getZustaendigkeitenOrganisationseinheit().get(0);
            Zustaendigkeit zsoeKreis = xzufiFile.getZustaendigkeitenOrganisationseinheit().get(1);

            Zustaendigkeit zsodGemeinde = xzufiFile.getZustaendigkeitenOnlinedienst().get(0);
            Zustaendigkeit zsodKreis = xzufiFile.getZustaendigkeitenOnlinedienst().get(1);

            Spezialisierung spezialisierung = xzufiFile.getSpezialisierungen().getFirst();

            zsoeGemeinde.getArss().clear();
            zsoeGemeinde.getArss().add(arsGemeinde);

            zsoeKreis.getArss().clear();
            zsoeKreis.getArss().add(arsKreis);

            zsodGemeinde.getArss().clear();
            zsodGemeinde.getArss().add(arsGemeinde);

            zsodKreis.getArss().clear();
            zsodKreis.getArss().add(arsKreis);

            spezialisierung.getArs().clear();
            spezialisierung.getArs().add(arsKreis);

            String spezTextmodulInhalt = testcasename + "SpezTextModul03" + arsKreis;

            spezialisierung
                .getModulSpezialisierungen()
                .add(
                    ModulSpezialisierung.builder()
                        .ersetzung(true)
                        .modul(Textmodul.builder().leikaCode("03").inhalt(spezTextmodulInhalt).build())
                        .build()
                );

            // Import
            importId = ImportUtil.push(xzufiFile);
            ArsValidationValues arsValidationValues = new ArsValidationValues(
                arsBundesland.bundesland().name(),
                arsGemeinde,
                arsKreis,
                lbId,
                lbFullId,
                lbBeschreibung,
                lbName,
                lbLeika,
                oeGemeindeId,
                oeKreisId,
                oeGemeindeFullId,
                oeKreisFullId,
                odGemeindeId,
                odKreisId,
                odGemeindeFullId,
                odKreisFullId,
                odGemeindeUrl,
                odKreisUrl,
                spezTextmodulInhalt
            );
            arsValidationValuesList.add(arsValidationValues);
            leikaIdCounter++;
        }
        ImportUtil.waitForImport(importId);
        return arsValidationValuesList;
    }

    static Stream<Arguments> testDataProvider() {
        return testData
            .stream()
            .map(t ->
                arguments(
                    named(
                        String.format(
                            "Bundesland: '%s', Kreis: '%s', Gemeinde: '%s'",
                            t.bundeslandKuerzel(),
                            t.arsKreis(),
                            t.arsGemeinde()
                        ),
                        t
                    )
                )
            );
    }
}
