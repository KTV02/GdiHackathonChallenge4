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
package suchdienst.integrationstest.tests.x23.sonstiges;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel: Es wird das korrekte Verhalten im Zusammenspiel von Bund Leistungen und exakter ARS Suche geprüft
 * Bei useArsHierarchy=true soll die Bundesleistung von allen Ars Bundesländer Schlüsseln und Hierarchie Ebenen gefunden werden
 * Bei useArsHierarchy=false soll die Bundesleistung von keinem Ars Bundesland Schlüssel und Hierarchie Ebene gefunden werden
 * Bei useArsHierarchy=false soll die Bundesleistung bei Abfrage über den Bundes Ars (000000000000) gefunden werden
 */
class ExakteArsSucheBundTest {

    public record ArsBundesland(String bundesland, String arsLand, String arsKreis, String arsGemeinde) {}

    private static final ArsBundesland arsBundeslandSH = new ArsBundesland(
        Bundesland.SH.name(),
        "010000000000",
        "010600000000",
        "010605024012"
    );
    private static final ArsBundesland arsBundeslandHH = new ArsBundesland(
        Bundesland.HH.name(),
        "020000000000",
        "020600000000",
        "020605024012"
    );
    private static final ArsBundesland arsBundeslandNI = new ArsBundesland(
        Bundesland.NI.name(),
        "030000000000",
        "033510000000",
        "033510004004"
    );
    private static final ArsBundesland arsBundeslandHB = new ArsBundesland(
        Bundesland.HB.name(),
        "040000000000",
        "040110000000",
        "040110004004"
    );
    private static final ArsBundesland arsBundeslandNW = new ArsBundesland(
        Bundesland.NW.name(),
        "050000000000",
        "051660000000",
        "051660004004"
    );
    private static final ArsBundesland arsBundeslandHE = new ArsBundesland(
        Bundesland.HE.name(),
        "060000000000",
        "064310000000",
        "064310017017"
    );
    private static final ArsBundesland arsBundeslandRP = new ArsBundesland(
        Bundesland.RP.name(),
        "070000000000",
        "073310000000",
        "073315001001"
    );
    private static final ArsBundesland arsBundeslandBW = new ArsBundesland(
        Bundesland.BW.name(),
        "080000000000",
        "081170000000",
        "081170010010"
    );
    private static final ArsBundesland arsBundeslandBY = new ArsBundesland(
        Bundesland.BY.name(),
        "090000000000",
        "091740000000",
        "091740111111"
    );
    private static final ArsBundesland arsBundeslandSL = new ArsBundesland(
        Bundesland.SL.name(),
        "100000000000",
        "100460000000",
        "100460111111"
    );
    private static final ArsBundesland arsBundeslandBE = new ArsBundesland(
        Bundesland.BE.name(),
        "110000000000",
        "110460000000",
        "110460111111"
    );
    private static final ArsBundesland arsBundeslandBB = new ArsBundesland(
        Bundesland.BB.name(),
        "120000000000",
        "120710000000",
        "120715101028"
    );
    private static final ArsBundesland arsBundeslandMV = new ArsBundesland(
        Bundesland.MV.name(),
        "130000000000",
        "130760000000",
        "130765652009"
    );
    private static final ArsBundesland arsBundeslandSN = new ArsBundesland(
        Bundesland.SN.name(),
        "140000000000",
        "146260000000",
        "146265220150"
    );
    private static final ArsBundesland arsBundeslandST = new ArsBundesland(
        Bundesland.ST.name(),
        "150000000000",
        "150890000000",
        "150895051041"
    );
    private static final ArsBundesland arsBundeslandTH = new ArsBundesland(
        Bundesland.TH.name(),
        "160000000000",
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

    private static String lbFullId;
    private static String lbId;
    private static String beschreibung;
    private static String leika;
    private static String oeId;
    private static String odId;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        //warte auf Löschen des Datenbestands
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        // Bundedsleistung wird initial importiert
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bcbund = new BundeslandConfig(Bundesland.BUND);
        XzufiFile file = XzufiFileCreator.newInstance(bcbund).name(testclassname).lb(1).oe(1).od(1).getMin();
        lbFullId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        lbId = file.getLeistungsbeschreibungen().getFirst().getId();
        beschreibung = file.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
        leika = file.getLeistungsbeschreibungen().getFirst().getReferenzLeiKa();
        oeId = file.getOrganisationseinheiten().getFirst().getId();
        odId = file.getOnlinedienste().getFirst().getId();
        ImportUtil.pushAndWait23(file);
    }

    @DisplayName("Exakte Suche nach Bundleistung - Trefferliste")
    @ParameterizedTest(name = "{displayName} Land:{0}")
    @MethodSource("testDataProvider")
    void trefferliste(ArsBundesland arsBundesland) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt
        // eine Bund Leistung wird von Länder ARS Schlüssel gefunden
        List<String> arsList = List.of(arsBundesland.arsLand, arsBundesland.arsKreis, arsBundesland.arsGemeinde);
        arsList.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, beschreibung, true);
            ServiceDescriptionResultListValidator validatorServiceDescriptions =
                new ServiceDescriptionResultListValidator(response);
            validatorServiceDescriptions.validateContains(lbFullId).validateCount(1);
        });
        // useHierarchy Flag ist auf false gesetzt
        // die Bund Leistung wird bei keiner Abfrage über Länderschlüssel gefunden
        arsList.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, beschreibung, false);
            ServiceDescriptionResultListValidator validatorServiceDescriptions =
                new ServiceDescriptionResultListValidator(response);
            validatorServiceDescriptions.validateNotContains(lbFullId);
        });
    }

    @Test
    void trefferlisteBund() {
        // Bundesleistung wird bei exakter Ars Suche und Ars Schlüssel des Bundes gefunden
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8("000000000000", beschreibung, false);
        ServiceDescriptionResultListValidator validatorServiceDescriptions = new ServiceDescriptionResultListValidator(
            response
        );
        validatorServiceDescriptions.validateContains(lbFullId);
    }

    @DisplayName("Exakte Suche nach Bundleistung - Detailseite")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void details(ArsBundesland arsBundesland) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt
        // eine Bund Leistung wird von Länder ARS Schlüssel gefunden
        List<String> arsList = List.of(arsBundesland.arsLand, arsBundesland.arsKreis, arsBundesland.arsGemeinde);
        arsList.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbFullId, true);
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(response);
            validatorDetail.validateId(lbFullId);
        });

        // useHierarchy Flag ist auf false gesetzt
        // die Bund Leistung wird bei keiner Abfrage über Länderschlüssel gefunden
        arsList.forEach(ars -> {
            Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbFullId, false);
            ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(response);
            validatorDetail.validateNotFound();
        });
    }

    @Test
    void detailsBund() {
        // Bundesleistung wird bei exakter Ars Suche und Ars Schlüssel des Bundes gefunden
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5("000000000000", lbFullId, false);
        ServiceDescriptionValidator validatorDetail = new ServiceDescriptionValidator(response);
        validatorDetail.validateId(lbFullId);
    }

    @DisplayName("Exakte Suche nach Bundleistung mit LeiKa ID ")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void leikaV3ExakteSuche(ArsBundesland arsBundesland) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt
        // eine Bund Leistung wird von Länder ARS Schlüssel gefunden
        List<String> arsList = List.of(arsBundesland.arsLand, arsBundesland.arsKreis, arsBundesland.arsGemeinde);
        arsList.forEach(ars -> {
            Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leika, true);
            LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
            validatorLeika.validateContains(lbFullId).validateIdsCount(lbFullId, 1);
        });

        // useHierarchy Flag ist auf false gesetzt
        // die Bund Leistung wird bei keiner Abfrage über Länderschlüssel gefunden
        arsList.forEach(ars -> {
            Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leika, false);
            LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
            validatorLeika.validateNotContains(lbFullId);
        });
    }

    @Test
    void leikaSucheBund() {
        // Bundesleistung wird bei exakter Ars Suche und Ars Schlüssel des Bundes gefunden
        Response responseLeika = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(
            "000000000000",
            leika,
            false
        );
        LeikaIdResultListValidator validatorLeika = new LeikaIdResultListValidator(responseLeika);
        validatorLeika.validateContains(lbFullId);
    }

    @DisplayName("Exakte Suche nach Bundesleistung - Relations")
    @ParameterizedTest(name = "{displayName}: {0}")
    @MethodSource("testDataProvider")
    void relationV2ExakteSuche(ArsBundesland arsBundesland) {
        //Testfälle
        // Default Verhalten: UseHierarchy Flag ist auf true gesetzt
        // eine Bund Leistung wird von Länder ARS Schlüssel gefunden
        List<String> arsList = List.of(arsBundesland.arsLand, arsBundesland.arsKreis, arsBundesland.arsGemeinde);
        arsList.forEach(ars -> {
            Response response = RelationsConnector.getRelationsLbIdsArsV3(
                List.of(lbFullId),
                ars,
                true,
                Constants.XzufiVersion.V2_3_1
            );
            //in den relations wird der ars des Bundes erwartet
            String oeIdValue = lbId + "." + oeId + ".000000000000";
            String odIdValue = lbId + "." + odId + ".000000000000";

            RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
            validator
                .validateCount(2)
                .validateParentIdValue(0, odId)
                .validateIdValue(0, odIdValue)
                .validateParentIdValue(1, oeId)
                .validateIdValue(1, oeIdValue);
        });

        // useHierarchy Flag ist auf false gesetzt
        // die Bund Leistung wird bei keiner Abfrage über Länderschlüssel gefunden
        arsList.forEach(ars -> {
            Response response = RelationsConnector.getRelationsLbIdsArsV3(
                List.of(lbFullId),
                ars,
                false,
                Constants.XzufiVersion.V2_3_1
            );
            RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
            validator.validateCount(0);
        });
    }

    @Test
    void relationsSucheBund() {
        // Bundesleistung wird bei exakter Ars Suche und Ars Schlüssel des Bundes gefunden
        Response response = RelationsConnector.getRelationsLbIdsArsV3(
            List.of(lbFullId),
            "000000000000",
            false,
            Constants.XzufiVersion.V2_3_1
        );
        String oeIdValue = lbId + "." + oeId + ".000000000000";
        String odIdValue = lbId + "." + odId + ".000000000000";

        RelationV3ResultListValidator validator = new RelationV3ResultListValidator(response);
        validator
            .validateCount(2)
            .validateParentIdValue(0, odId)
            .validateIdValue(0, odIdValue)
            .validateParentIdValue(1, oeId)
            .validateIdValue(1, oeIdValue);
    }

    static Stream<Arguments> testDataProvider() {
        return arsBundeslaender
            .stream()
            .map(t ->
                arguments(
                    named(
                        String.format(
                            "Bundesland: '%s', Land: '%s', Kreis: '%s', Gemeinde: '%s'",
                            t.bundesland(),
                            t.arsLand(),
                            t.arsKreis(),
                            t.arsGemeinde()
                        ),
                        t
                    )
                )
            );
    }
}
