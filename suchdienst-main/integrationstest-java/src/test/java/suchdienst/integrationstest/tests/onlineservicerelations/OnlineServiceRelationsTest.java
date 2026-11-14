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
package suchdienst.integrationstest.tests.onlineservicerelations;

import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.BB;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.BUND;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.BW;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.HE;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.HH;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.NI;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.NW;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.SH;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ImportQueueConnector;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.tests.onlineservicerelations.model.HyperLink;
import suchdienst.integrationstest.tests.onlineservicerelations.model.OnlineServiceResult;
import suchdienst.integrationstest.tests.onlineservicerelations.model.ServiceDescriptionInfo;
import suchdienst.integrationstest.utils.WaitUtil;

/**
 * Testziel: Validiert das Verhalten des Endpunkts GET /v1beta2/onlineservices.
 * <p>
 * Der Endpunkt nutzt eine 2-Wege-ARS-Suche:
 * <ul>
 *   <li>Aufwärts: terms-Query auf die ARS-Hierarchie (Gemeinde → Land → Bund)</li>
 *   <li>Abwärts: prefix-Query auf den gesuchten ARS-Prefix (z.B. "01" findet "011111111111")</li>
 * </ul>
 * <p>
 * Szenarien:
 * <ol>
 *   <li>SH – Abwärts-Suche: OD auf Gemeinde-Ebene, Suche mit höheren Ebenen findet per prefix</li>
 *   <li>HH – Stadtstaat: alle Hierarchie-Ebenen kollabiert auf "020000000000"</li>
 *   <li>NI – Aufwärts-Suche: OD auf Land-Ebene, Suche mit Gemeinde-ARS findet per terms</li>
 *   <li>NW – Mehrere LBs pro OD: Ein OD mit 3 Leistungen auf verschiedenen ARS-Ebenen</li>
 *   <li>HE – Mehrere ODs auf verschiedenen Hierarchie-Ebenen: Suche mit Gemeinde findet alle</li>
 *   <li>BW – Bundesland-Isolation: OD eines BL darf nicht in einem anderen BL gefunden werden</li>
 *   <li>BUND – Bund-Ebene: OD auf Bund-Ebene wird in allen Bundesländern gefunden</li>
 *   <li>BB – Vollständige Response-Validierung: alle Felder des Response-Modells prüfen</li>
 *   <li>Leika-Filter: Ergebnisse werden auf passende Onlinedienste eingeschränkt, die Relationen bleiben vollständig</li>
 * </ol>
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineServiceRelationsTest {

    // --- SH: Abwärts-Suche (OD auf Gemeinde-Ebene) ---
    private static final String SH_ARS_GEMEINDE = "011111111111";
    private static XzufiFile shFile;

    // --- HH: Stadtstaat (alle Ebenen kollabiert) ---
    private static final String HH_ARS = "020000000000";
    private static XzufiFile hhFile;

    // --- NI: OD auf Land-Ebene ---
    private static final String NI_ARS_LAND = "030000000000";
    private static XzufiFile niFile;

    // --- NW: Mehrere LBs pro OD ---
    private static final String NW_ARS_GEMEINDE = "051111111111";
    private static final String NW_ARS_KREIS = "051110000000";
    private static final String NW_ARS_LAND = "050000000000";
    private static XzufiFile nwFile;

    // --- HE: Mehrere ODs auf verschiedenen Hierarchie-Ebenen ---
    private static final String HE_ARS_LAND = "060000000000";
    private static final String HE_ARS_BEZIRK = "061000000000";
    private static final String HE_ARS_KREIS = "061110000000";
    private static final String HE_ARS_VERBAND = "061111111000";
    private static final String HE_ARS_GEMEINDE = "061111111111";
    private static XzufiFile heFile;

    // --- BW: Bundesland-Isolation ---
    private static final String BW_ARS_GEMEINDE = "081111111111";
    private static XzufiFile bwFile;

    // --- BUND: Bund-Ebene ---
    private static final String BUND_ARS = "000000000000";
    private static XzufiFile bundFile;

    // --- BB: Vollständige Response-Validierung ---
    private static final String BB_ARS_GEMEINDE = "121111111111";
    private static XzufiFile bbFile;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        String testcaseName = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        // --- SH: Ein OD auf Gemeinde-Ebene ---
        shFile = createSimpleFile(new BundeslandConfig(SH), testcaseName, SH_ARS_GEMEINDE);

        // --- HH: Ein OD auf Stadtstaat-Ebene (alle Hierarchie-Stufen = "020000000000") ---
        hhFile = createSimpleFile(new BundeslandConfig(HH), testcaseName, HH_ARS);

        // --- NI: Ein OD auf Land-Ebene ---
        niFile = createSimpleFile(new BundeslandConfig(NI), testcaseName, NI_ARS_LAND);

        // --- NW: Ein OD mit 3 LBs auf verschiedenen ARS-Ebenen ---
        nwFile = createMultiLbFile(new BundeslandConfig(NW), testcaseName);

        // --- HE: 5 ODs auf je einer Hierarchie-Ebene ---
        heFile = createMultiOdFile(new BundeslandConfig(HE), testcaseName);

        // --- BW: Ein OD auf Gemeinde-Ebene (für Isolationstest) ---
        bwFile = createSimpleFile(new BundeslandConfig(BW), testcaseName, BW_ARS_GEMEINDE);

        // --- BUND: Ein OD auf Bund-Ebene ---
        bundFile = createSimpleFile(new BundeslandConfig(BUND), testcaseName, BUND_ARS);

        // --- BB: Ein OD mit maximalem Detailgrad (für vollständige Validierung) ---
        BundeslandConfig bbConfig = new BundeslandConfig(BB);
        bbFile = XzufiFileCreator.newInstance(bbConfig).name(testcaseName).lb(1).od(1).getMax();
        bbFile.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(BB_ARS_GEMEINDE));

        // Alle Dateien importieren, nur beim letzten auf den Import warten.
        String importId = null;
        for (XzufiFile file : List.of(shFile, hhFile, niFile, nwFile, heFile, bwFile, bundFile, bbFile)) {
            importId = ImportUtil.push23(file);
        }
        ImportUtil.waitForImport(importId);

        // Onlinedienst-Relationsindex neu berechnen
        AdminConnector.recalculateOnlineServiceRelations();
        waitForTasksCompleted();
    }

    @AfterAll
    static void cleanUp() {
        ElasticsearchConnector.removeAllData();
    }

    // =====================================================
    // SH: Abwärts-Suche (Filterung nach unten / prefix-Query)
    // Testdaten-ARS: Gemeinde "011111111111" (spezifischste Ebene).
    // Such-ARS: höhere Ebenen (Verband, Kreis, Bezirk, Land).
    // Mechanismus: hierarchyTrim() kürzt die Such-ARS, der prefix-Query
    // matched die OD-ARS, weil sie mit dem getrimmten Prefix beginnt.
    // Beispiel: Suche mit Land "010000000000" → trim → "01" → prefix
    //           matched "011111111111", weil diese mit "01" beginnt.
    // =====================================================

    @DisplayName("SH - Abwärts-Suche: OD auf Gemeinde-Ebene wird über höhere Hierarchie-Stufen gefunden")
    @ParameterizedTest(name = "{displayName}: searchArs={0}")
    @ValueSource(
        strings = {
            "011111111000", // Verband (prefix "011111111" findet "011111111111")
            "011110000000", // Kreis (prefix "01111" findet "011111111111")
            "011000000000", // Bezirk (prefix "011" findet "011111111111")
            "010000000000", // Land (prefix "01" findet "011111111111")
        }
    )
    void shAbwaertsSuche(String searchArs) {
        Onlinedienst od = shFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = shFile.getLeistungsbeschreibungen().getFirst();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(searchArs);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), SH_ARS_GEMEINDE, lb.getReferenzLeiKa());
    }

    // =====================================================
    // HH: Stadtstaat (alle Ebenen kollabiert)
    // Bei Stadtstaaten sind alle Hierarchie-Ebenen identisch.
    // =====================================================

    @DisplayName("HH - Stadtstaat: OD wird sowohl über Land-ARS als auch Gemeinde-ARS gefunden")
    @ParameterizedTest(name = "{displayName}: searchArs={0}")
    @ValueSource(strings = { "020000000000", "02" })
    void hhStadtstaat(String searchArs) {
        Onlinedienst od = hhFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = hhFile.getLeistungsbeschreibungen().getFirst();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(searchArs);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), HH_ARS, lb.getReferenzLeiKa());
    }

    // =====================================================
    // NI: Aufwärts-Suche (Filterung nach oben / terms-Query)
    // Testdaten-ARS: Land "030000000000" (allgemeine Ebene).
    // Such-ARS: spezifischere Ebenen (Gemeinde, Kreis, Bezirk).
    // Mechanismus: getArsHierarchy() expandiert die Such-ARS nach oben,
    // der terms-Query matched, weil die OD-ARS in der Hierarchie enthalten ist.
    // Beispiel: Suche mit Gemeinde "031111111111" → Hierarchie enthält
    //           "030000000000" → terms-Query matched die OD-ARS.
    // =====================================================

    @DisplayName("NI - Aufwärts-Suche: OD auf Land-ARS wird bei Suche mit spezifischerer ARS gefunden")
    @ParameterizedTest(name = "{displayName}: searchArs={0}")
    @ValueSource(strings = { "031111111111", "031110000000", "031000000000" })
    void niAufwaertsSuche(String searchArs) {
        Onlinedienst od = niFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = niFile.getLeistungsbeschreibungen().getFirst();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(searchArs);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), NI_ARS_LAND, lb.getReferenzLeiKa());
    }

    // =====================================================
    // NW: Mehrere LBs pro OD
    // Ein einziger OD hat 3 Zuständigkeiten zu 3 verschiedenen LBs,
    // jede auf einer anderen ARS-Ebene.
    // =====================================================

    @DisplayName("NW - Mehrere LBs pro OD: Ein OD mit 3 Relationen auf unterschiedlichen ARS-Ebenen")
    @Test
    void nwMehrereLbsProOd() {
        Onlinedienst od = nwFile.getOnlinedienste().getFirst();

        // Suche mit Gemeinde-ARS: Alle 3 Relationen müssen per terms/prefix gefunden werden.
        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(NW_ARS_GEMEINDE);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);

        // Es müssen genau 3 Relationen vorhanden sein
        assertThat("Es müssen 3 Relationen vorhanden sein", result.serviceDescriptions(), hasSize(3));

        // Jede LB muss als Relation vorhanden sein
        List<Leistungsbeschreibung> lbs = nwFile.getLeistungsbeschreibungen();
        List<String> expectedArsList = List.of(NW_ARS_GEMEINDE, NW_ARS_KREIS, NW_ARS_LAND);
        for (int i = 0; i < lbs.size(); i++) {
            Leistungsbeschreibung lb = lbs.get(i);
            validateRelation(result, lb.getFullId(), expectedArsList.get(i), lb.getReferenzLeiKa());
        }
    }

    // =====================================================
    // HE: Mehrere ODs auf verschiedenen Hierarchie-Ebenen
    // 5 ODs mit je einer LB-Zuständigkeit auf einer anderen Ebene.
    // Suche mit Gemeinde-ARS muss alle 5 ODs finden.
    // =====================================================

    @DisplayName("HE - Mehrere ODs auf verschiedenen Ebenen werden alle bei Gemeinde-Suche gefunden")
    @ParameterizedTest(name = "{displayName}: Ebene={1}")
    @MethodSource("heOdIndices")
    void heMultipleOdsAlleGefunden(int odIndex, String expectedArs) {
        Onlinedienst od = heFile.getOnlinedienste().get(odIndex);
        Leistungsbeschreibung lb = heFile.getLeistungsbeschreibungen().get(odIndex);

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(HE_ARS_GEMEINDE);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), expectedArs, lb.getReferenzLeiKa());
    }

    static Stream<Arguments> heOdIndices() {
        return Stream.of(
            Arguments.of(0, HE_ARS_LAND),
            Arguments.of(1, HE_ARS_BEZIRK),
            Arguments.of(2, HE_ARS_KREIS),
            Arguments.of(3, HE_ARS_VERBAND),
            Arguments.of(4, HE_ARS_GEMEINDE)
        );
    }

    // =====================================================
    // BW: Bundesland-Isolation
    // Ein OD aus BW darf nicht bei Suche mit BY-ARS gefunden werden.
    // =====================================================

    @DisplayName("BW - Positiv: OD wird im eigenen Bundesland gefunden")
    @Test
    void bwIsolationPositiv() {
        Onlinedienst od = bwFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = bwFile.getLeistungsbeschreibungen().getFirst();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(BW_ARS_GEMEINDE);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), BW_ARS_GEMEINDE, lb.getReferenzLeiKa());
    }

    @DisplayName("BW - Negativ: OD aus BW wird bei Suche mit BY-ARS nicht gefunden")
    @Test
    void bwIsolationNegativ() {
        Onlinedienst bwOd = bwFile.getOnlinedienste().getFirst();
        String byArs = "091111111111";

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(byArs);
        List<OnlineServiceResult> resultsForBwOd = results
            .stream()
            .filter(r -> bwOd.getFullId().equals(r.id()))
            .toList();

        assertThat("OD aus BW darf nicht bei Suche mit BY-ARS gefunden werden", resultsForBwOd, is(empty()));
    }

    // =====================================================
    // BUND: OD auf Bund-Ebene wird in allen Bundesländern gefunden
    // Der Service fügt ARS.BUND ("000000000000") immer zur terms-Query hinzu.
    // =====================================================

    @DisplayName("BUND - OD auf Bund-Ebene wird von verschiedenen Bundesländern aus gefunden")
    @ParameterizedTest(name = "{displayName}: searchArs={0}")
    @ValueSource(strings = { "011111111111", "051111111111", "091111111111", "121111111111" })
    void bundEbeneImmerGefunden(String searchArs) {
        Onlinedienst od = bundFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = bundFile.getLeistungsbeschreibungen().getFirst();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(searchArs);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateRelation(result, lb.getFullId(), BUND_ARS, lb.getReferenzLeiKa());
    }

    @DisplayName("NW - Leika-Filter: Es werden nur passende Onlinedienste geliefert, Relationen bleiben vollständig")
    @Test
    void leikaFilterSchraenktOnlinediensteEinNichtRelationen() {
        Onlinedienst od = nwFile.getOnlinedienste().getFirst();
        List<Leistungsbeschreibung> lbs = nwFile.getLeistungsbeschreibungen();
        Leistungsbeschreibung matchingLb = lbs.getFirst();
        Leistungsbeschreibung nonMatchingLb = lbs.get(1);
        Leistungsbeschreibung anotherNonMatchingLb = lbs.get(2);

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(
            NW_ARS_GEMEINDE,
            List.of(matchingLb.getReferenzLeiKa())
        );
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        assertThat(
            "Der gematchte Onlinedienst muss weiterhin alle seine Relationen enthalten",
            result.serviceDescriptions(),
            hasSize(3)
        );
        validateRelation(result, matchingLb.getFullId(), NW_ARS_GEMEINDE, matchingLb.getReferenzLeiKa());
        validateRelation(result, nonMatchingLb.getFullId(), NW_ARS_KREIS, nonMatchingLb.getReferenzLeiKa());
        validateRelation(
            result,
            anotherNonMatchingLb.getFullId(),
            NW_ARS_LAND,
            anotherNonMatchingLb.getReferenzLeiKa()
        );

        assertThat(
            "Alle Leistungsbeschreibungen des gelieferten Onlinedienstes müssen enthalten sein",
            result.serviceDescriptions().stream().map(ServiceDescriptionInfo::lbId).toList(),
            is(lbs.stream().map(Leistungsbeschreibung::getFullId).toList())
        );
    }

    // =====================================================
    // BB: Vollständige Response-Validierung (getMax())
    // Validiert alle Felder: id, name, links (uri/title/description), relations
    // =====================================================

    @DisplayName("BB - Vollständige Response-Validierung aller Felder")
    @Test
    void bbVollstaendigeValidierung() {
        Onlinedienst od = bbFile.getOnlinedienste().getFirst();
        Leistungsbeschreibung lb = bbFile.getLeistungsbeschreibungen().getFirst();
        List<Link> expectedLinks = od.getLinks();

        List<OnlineServiceResult> results = OnlineserviceConnector.getAllOnlineServicesByArs(BB_ARS_GEMEINDE);
        OnlineServiceResult result = findExactlyOne(results, od.getFullId());

        validateOnlineservice(result, od);
        validateLinks(result, expectedLinks);
        validateRelation(result, lb.getFullId(), BB_ARS_GEMEINDE, lb.getReferenzLeiKa());
    }

    // =====================================================
    // Hilfsmethoden: Testdaten-Erzeugung
    // =====================================================

    /**
     * Erzeugt eine einfache XzufiFile mit 1 LB, 1 OD und einer Zuständigkeit auf der gegebenen ARS.
     */
    private static XzufiFile createSimpleFile(BundeslandConfig config, String testcaseName, String ars) {
        XzufiFile file = XzufiFileCreator.newInstance(config).name(testcaseName).lb(1).od(1).getMin();
        file.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(ars));
        return file;
    }

    /**
     * Erzeugt eine XzufiFile mit 3 LBs und 1 OD.
     * Die auto-generierten Zuständigkeiten werden gelöscht und manuell 3 ZS-ODs angelegt,
     * je eine pro LB auf einer anderen ARS-Ebene (Gemeinde, Kreis, Land).
     */
    private static XzufiFile createMultiLbFile(BundeslandConfig config, String testcaseName) {
        XzufiFile file = XzufiFileCreator.newInstance(config).name(testcaseName).lb(3).od(1).zsOd(1).getMin();

        // Auto-generierte Zuständigkeiten entfernen und manuell neu anlegen
        file.getZustaendigkeitenOnlinedienst().clear();

        List<String> arsList = List.of(NW_ARS_GEMEINDE, NW_ARS_KREIS, NW_ARS_LAND);
        Onlinedienst od = file.getOnlinedienste().getFirst();
        for (int i = 0; i < file.getLeistungsbeschreibungen().size(); i++) {
            Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().get(i);
            Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
            zs.getArss().clear();
            zs.getArss().add(arsList.get(i));
            file.getZustaendigkeitenOnlinedienst().add(zs);
        }

        return file;
    }

    /**
     * Erzeugt eine XzufiFile mit 5 LBs und 5 ODs.
     * Die auto-generierten Zuständigkeiten werden gelöscht und manuell 5 ZS-ODs angelegt,
     * wobei jeweils LB[i] ↔ OD[i] auf der i-ten ARS-Hierarchie-Ebene verknüpft wird.
     */
    private static XzufiFile createMultiOdFile(BundeslandConfig config, String testcaseName) {
        XzufiFile file = XzufiFileCreator.newInstance(config).name(testcaseName).lb(5).od(5).zsOd(1).getMin();

        // Auto-generierte Zuständigkeiten entfernen und manuell neu anlegen
        file.getZustaendigkeitenOnlinedienst().clear();

        List<String> arsList = List.of(HE_ARS_LAND, HE_ARS_BEZIRK, HE_ARS_KREIS, HE_ARS_VERBAND, HE_ARS_GEMEINDE);
        for (int i = 0; i < 5; i++) {
            Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().get(i);
            Onlinedienst od = file.getOnlinedienste().get(i);
            Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
            zs.getArss().clear();
            zs.getArss().add(arsList.get(i));
            file.getZustaendigkeitenOnlinedienst().add(zs);
        }

        return file;
    }

    // =====================================================
    // Hilfsmethoden: Warten & Validierung
    // =====================================================

    private static void waitForTasksCompleted() {
        WaitUtil.waitFor(500, TimeUnit.MILLISECONDS);
        await("Warten auf Neuberechnung")
            .atMost(Duration.ofSeconds(60))
            .with()
            .pollInterval(Duration.ofMillis(1000))
            .until(ImportQueueConnector::getUpToDate, is(true));
        WaitUtil.waitFor(500, TimeUnit.MILLISECONDS);
    }

    /**
     * Sucht in der Ergebnisliste nach genau einem Treffer mit der gegebenen OD-ID.
     */
    private static OnlineServiceResult findExactlyOne(List<OnlineServiceResult> results, String odFullId) {
        List<OnlineServiceResult> filtered = results
            .stream()
            .filter(r -> odFullId.equals(r.id()))
            .toList();

        assertThat(
            "Es muss genau ein Ergebnis für den Onlinedienst '%s' vorhanden sein".formatted(odFullId),
            filtered,
            hasSize(1)
        );
        return filtered.getFirst();
    }

    private static void validateOnlineservice(OnlineServiceResult result, Onlinedienst expectedOd) {
        assertThat(
            "onlineservice.id muss der ID des Onlinedienstes entsprechen",
            result.id(),
            is(expectedOd.getFullId())
        );
        assertThat(
            "onlineservice.name muss der Bezeichnung des Onlinedienstes entsprechen",
            result.name(),
            is(expectedOd.getBezeichnung())
        );
    }

    private static void validateLinks(OnlineServiceResult result, List<Link> expectedLinks) {
        List<HyperLink> actualLinks = result.links();
        assertThat("links darf nicht null sein", actualLinks, notNullValue());
        assertThat("Anzahl der Links muss übereinstimmen", actualLinks, hasSize(expectedLinks.size()));
        for (int i = 0; i < expectedLinks.size(); i++) {
            Link expected = expectedLinks.get(i);
            HyperLink actual = actualLinks.get(i);
            assertThat("link[%d].uri muss übereinstimmen".formatted(i), actual.uri(), is(expected.getUri()));
            assertThat(
                "link[%d].title muss dem Titel entsprechen".formatted(i),
                actual.title(),
                is(expected.getTitel())
            );
            assertThat(
                "link[%d].description muss der Beschreibung entsprechen".formatted(i),
                actual.description(),
                is(expected.getBeschreibung())
            );
        }
    }

    private static void validateRelation(
        OnlineServiceResult result,
        String expectedLbId,
        String expectedArs,
        String expectedLeikaKey
    ) {
        List<ServiceDescriptionInfo> relations = result.serviceDescriptions();
        assertThat("relations darf nicht null sein", relations, notNullValue());

        ServiceDescriptionInfo relation = relations
            .stream()
            .filter(r -> expectedLbId.equals(r.lbId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Keine Relation mit lbId '%s' gefunden".formatted(expectedLbId)));

        assertThat("relation.ars muss das erwartete ARS enthalten", relation.ars(), hasItem(expectedArs));
        assertThat(
            "relation.leikakeys muss den LeiKa-Schlüssel der Leistungsbeschreibung enthalten",
            relation.leikakeys(),
            hasItem(expectedLeikaKey)
        );
    }
}
