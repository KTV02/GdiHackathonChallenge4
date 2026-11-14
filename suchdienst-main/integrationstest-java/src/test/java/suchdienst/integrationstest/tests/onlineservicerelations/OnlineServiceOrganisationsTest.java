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
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.MV;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.NI;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.NW;
import static de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland.SH;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
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
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ImportQueueConnector;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.tests.onlineservicerelations.model.AddressItem;
import suchdienst.integrationstest.tests.onlineservicerelations.model.OrganisationItem;
import suchdienst.integrationstest.tests.onlineservicerelations.model.OrganisationsApiResponse;
import suchdienst.integrationstest.tests.onlineservicerelations.model.RoleItem;
import suchdienst.integrationstest.utils.WaitUtil;

/**
 * Testziel: Validiert das Verhalten des Endpunkts GET /v1beta1/onlineservices/{odId}/organisations.
 * <p>
 * Der Endpunkt liefert Organisationseinheiten, die für einen Onlinedienst zuständig sind.
 * Die Zuordnung erfolgt zur Indexierungszeit über einen 2-Wege-ARS-Abgleich zwischen
 * OD-Zuständigkeiten (ZS-OD) und OE-Zuständigkeiten (ZS-OE):
 * <ul>
 *   <li>Aufwärts: OE-ARS ist allgemeiner als OD-ARS (z.B. OE auf Land-Ebene, OD auf Gemeinde)</li>
 *   <li>Abwärts: OE-ARS ist spezifischer als OD-ARS (z.B. OE auf Gemeinde, OD auf Land-Ebene)</li>
 * </ul>
 * <p>
 * Szenarien:
 * <ol>
 *   <li>SH – Aufwärts-Match: OD Gemeinde, OE Land → OE gefunden</li>
 *   <li>HH – Stadtstaat: identische ARS → OE gefunden</li>
 *   <li>NI – Abwärts-Match: OD Land, OE Gemeinde → OE gefunden</li>
 *   <li>HE – Alle Hierarchie-Ebenen: 1 OD, 5 OEs auf je einer Ebene → alle gefunden</li>
 *   <li>NW – Cross-Branch: OD Kreis A, OE Kreis B → leere Liste</li>
 *   <li>BW – Bundesland-Isolation: OD BW, OE₁ BW + OE₂ BY → nur OE₁</li>
 *   <li>BUND – Bund-Ebene: OD/OE beide Bund → OE gefunden</li>
 *   <li>BB – Vollständige Response-Validierung aller Felder</li>
 *   <li>MV – Mehrere Rollen: 1 OD mit 3 LBs, 1 OE → 3 Rollen</li>
 *   <li>404 bei nicht existenter OD-ID</li>
 * </ol>
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineServiceOrganisationsTest {

    // --- SH: Aufwärts-Match (OE auf Land-Ebene, OD auf Gemeinde) ---
    private static final String SH_OD_ARS_GEMEINDE = "011111111111";
    private static final String SH_OE_ARS_LAND = "010000000000";
    private static XzufiFile shFile;

    // --- HH: Stadtstaat (identische ARS) ---
    private static final String HH_ARS = "020000000000";
    private static XzufiFile hhFile;

    // --- NI: Abwärts-Match (OE auf Gemeinde, OD auf Land) ---
    private static final String NI_OD_ARS_LAND = "030000000000";
    private static final String NI_OE_ARS_GEMEINDE = "031111111111";
    private static XzufiFile niFile;

    // --- HE: 5 OEs auf verschiedenen Hierarchie-Ebenen ---
    private static final String HE_OD_ARS_GEMEINDE = "061111111111";
    private static final String HE_OE_ARS_LAND = "060000000000";
    private static final String HE_OE_ARS_BEZIRK = "061000000000";
    private static final String HE_OE_ARS_KREIS = "061110000000";
    private static final String HE_OE_ARS_VERBAND = "061111111000";
    private static final String HE_OE_ARS_GEMEINDE = "061111111111";
    private static XzufiFile heFile;

    // --- NW: Cross-Branch (kein Match) ---
    private static final String NW_OD_ARS_KREIS_A = "051110000000";
    private static final String NW_OE_ARS_KREIS_B = "052220000000";
    private static XzufiFile nwFile;

    // --- BW: Bundesland-Isolation ---
    private static final String BW_OD_ARS_GEMEINDE = "081111111111";
    private static final String BW_OE_ARS_MATCH = "081111111111";
    private static final String BW_OE_ARS_NO_MATCH = "091111111111";
    private static XzufiFile bwFile;

    // --- BUND: Bund-Ebene ---
    private static final String BUND_ARS = "000000000000";
    private static XzufiFile bundFile;

    // --- BB: Vollständige Response-Validierung ---
    private static final String BB_ARS_GEMEINDE = "121111111111";
    private static XzufiFile bbFile;

    // --- MV: Mehrere Rollen pro OE ---
    private static final String MV_ARS_GEMEINDE = "131111111111";
    private static final String MV_ARS_KREIS = "131110000000";
    private static final String MV_ARS_LAND = "130000000000";
    private static XzufiFile mvFile;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        WaitUtil.waitFor(1, TimeUnit.SECONDS);

        String testcaseName = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        // --- SH: Aufwärts-Match – OD Gemeinde, OE Land ---
        shFile = createSimpleOeFile(new BundeslandConfig(SH), testcaseName, SH_OD_ARS_GEMEINDE, SH_OE_ARS_LAND);

        // --- HH: Stadtstaat – identische ARS ---
        hhFile = createSimpleOeFile(new BundeslandConfig(HH), testcaseName, HH_ARS, HH_ARS);

        // --- NI: Abwärts-Match – OD Land, OE Gemeinde ---
        niFile = createSimpleOeFile(new BundeslandConfig(NI), testcaseName, NI_OD_ARS_LAND, NI_OE_ARS_GEMEINDE);

        // --- HE: 5 OEs auf je einer Hierarchie-Ebene ---
        heFile = createMultiLevelOeFile(
            new BundeslandConfig(HE),
            testcaseName,
            HE_OD_ARS_GEMEINDE,
            HE_OE_ARS_LAND,
            HE_OE_ARS_BEZIRK,
            HE_OE_ARS_KREIS,
            HE_OE_ARS_VERBAND,
            HE_OE_ARS_GEMEINDE
        );

        // --- NW: Cross-Branch (verschiedene Kreise) – kein Match erwartet ---
        nwFile = createSimpleOeFile(new BundeslandConfig(NW), testcaseName, NW_OD_ARS_KREIS_A, NW_OE_ARS_KREIS_B);

        // --- BW: Isolation – OE₁ matcht, OE₂ (BY-ARS) matcht nicht ---
        bwFile = createIsolationFile(
            new BundeslandConfig(BW),
            testcaseName,
            BW_OD_ARS_GEMEINDE,
            BW_OE_ARS_MATCH,
            BW_OE_ARS_NO_MATCH
        );

        // --- BUND: OD und OE beide auf Bund-Ebene ---
        bundFile = createSimpleOeFile(new BundeslandConfig(BUND), testcaseName, BUND_ARS, BUND_ARS);

        // --- BB: Vollständige Validierung (getMax) ---
        BundeslandConfig bbConfig = new BundeslandConfig(BB);
        bbFile = XzufiFileCreator.newInstance(bbConfig).name(testcaseName).lb(1).od(1).oe(1).zsOd(1).zsOe(1).getMax();
        bbFile.getZustaendigkeitenOnlinedienst().getFirst().setArss(List.of(BB_ARS_GEMEINDE));
        bbFile.getZustaendigkeitenOrganisationseinheit().getFirst().setArss(List.of(BB_ARS_GEMEINDE));

        // --- MV: 1 OD, 3 LBs, 1 OE – OE erhält 3 Rollen ---
        mvFile = createMultiRoleFile(
            new BundeslandConfig(MV),
            testcaseName,
            MV_ARS_GEMEINDE,
            MV_ARS_KREIS,
            MV_ARS_LAND
        );

        // Alle Dateien importieren, nur beim letzten auf den Import warten
        String importId = null;
        for (XzufiFile file : List.of(shFile, hhFile, niFile, heFile, nwFile, bwFile, bundFile, bbFile, mvFile)) {
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
    // SH: Aufwärts-Match (OE auf Land-Ebene, OD auf Gemeinde)
    // OD-ARS "011111111111" (Gemeinde), OE-ARS "010000000000" (Land).
    // ARS.matchesHierarchy: trim("011111111111") = "011111111111",
    // trim("010000000000") = "01" → "011111111111".startsWith("01") → true
    // =====================================================

    @DisplayName("SH - Aufwärts-Match: OE auf Land-Ebene wird bei OD auf Gemeinde-Ebene gefunden")
    @Test
    void shAufwaertsMatch() {
        Onlinedienst od = shFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = shFile.getOrganisationseinheiten().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations darf nicht leer sein", response.organisations(), is(not(empty())));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);
    }

    @DisplayName("SH - Adressen sind gefüllt")
    @Test
    void shAdressenSindGefuellt() {
        Onlinedienst od = shFile.getOnlinedienste().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        List<AddressItem> adressen = response.organisations().getFirst().addresses();
        assertThat("es existiert eine Adresse", adressen, is(hasSize(3)));

        assertThat(
            "übereinstimmende Lieferadresse",
            adressen,
            hasItem(
                new AddressItem(
                    "DELIVERY_ADDRESS",
                    "Musterort - Delivery",
                    "00000",
                    "Musterstrasse - Delivery",
                    "123 D",
                    null
                )
            )
        );

        assertThat(
            "übereinstimmende Adresse",
            adressen,
            hasItem(new AddressItem("ADDRESS", "Musterort", "00000", "Musterstrasse", "123", null))
        );

        assertThat(
            "übereinstimmende adresse",
            adressen,
            hasItem(
                new AddressItem(
                    "POSTAL_ADDRESS",
                    "Musterort - Postal",
                    "00000",
                    "Musterstrasse - Postal",
                    "123 P",
                    null
                )
            )
        );
    }

    // =====================================================
    // HH: Stadtstaat (identische ARS auf allen Ebenen)
    // =====================================================

    @DisplayName("HH - Stadtstaat: OE wird bei identischer ARS gefunden")
    @Test
    void hhStadtstaatMatch() {
        Onlinedienst od = hhFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = hhFile.getOrganisationseinheiten().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations darf nicht leer sein", response.organisations(), is(not(empty())));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);
    }

    // =====================================================
    // NI: Abwärts-Match (OE auf Gemeinde, OD auf Land)
    // OD-ARS "030000000000" (Land), OE-ARS "031111111111" (Gemeinde).
    // ARS.matchesHierarchy: trim("030000000000") = "03",
    // trim("031111111111") = "031111111111" → "031111111111".startsWith("03") → true
    // =====================================================

    @DisplayName("NI - Abwärts-Match: OE auf Gemeinde-Ebene wird bei OD auf Land-Ebene gefunden")
    @Test
    void niAbwaertsMatch() {
        Onlinedienst od = niFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = niFile.getOrganisationseinheiten().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations darf nicht leer sein", response.organisations(), is(not(empty())));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);
    }

    // =====================================================
    // HE: 5 OEs auf verschiedenen Hierarchie-Ebenen
    // 1 OD auf Gemeinde-Ebene, 5 OEs auf Land/Bezirk/Kreis/Verband/Gemeinde.
    // ARS.matchesHierarchy matcht alle 5, da OD-ARS Prefix aller OE-ARS ist
    // oder OE-ARS Prefix der OD-ARS ist.
    // =====================================================

    @DisplayName("HE - Alle Hierarchie-Ebenen: OE auf jeder Ebene wird bei OD auf Gemeinde gefunden")
    @ParameterizedTest(name = "{displayName}: Ebene={1}")
    @MethodSource("heOeIndices")
    void heAlleEbenen(int oeIndex, String expectedOeArs) {
        Onlinedienst od = heFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = heFile.getOrganisationseinheiten().get(oeIndex);

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations muss mindestens 5 Einträge haben", response.organisations(), hasSize(5));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);
        validateRole(
            item,
            heFile.getLeistungsbeschreibungen().getFirst().getFullId(),
            expectedOeArs,
            heFile.getLeistungsbeschreibungen().getFirst().getReferenzLeiKa()
        );
    }

    static Stream<Arguments> heOeIndices() {
        return Stream.of(
            Arguments.of(0, HE_OE_ARS_LAND),
            Arguments.of(1, HE_OE_ARS_BEZIRK),
            Arguments.of(2, HE_OE_ARS_KREIS),
            Arguments.of(3, HE_OE_ARS_VERBAND),
            Arguments.of(4, HE_OE_ARS_GEMEINDE)
        );
    }

    // =====================================================
    // NW: Cross-Branch – OD und OE in verschiedenen Kreisen
    // OD-ARS "051110000000" (Kreis A), OE-ARS "052220000000" (Kreis B).
    // ARS.matchesHierarchy: trim("051110000000") = "05111",
    // trim("052220000000") = "05222" → weder Prefix des anderen → false
    // =====================================================

    @DisplayName("NW - Cross-Branch: OE in anderem Kreis wird nicht zugeordnet → leere Liste")
    @Test
    void nwCrossBranchLeereListe() {
        Onlinedienst od = nwFile.getOnlinedienste().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations muss leer sein bei Cross-Branch-ARS", response.organisations(), is(empty()));
    }

    // =====================================================
    // BW: Bundesland-Isolation
    // OD-ARS "081111111111" (BW), OE₁ "081111111111" (BW, Match),
    // OE₂ "091111111111" (BY, kein Match).
    // =====================================================

    @DisplayName("BW - Isolation: Nur OE mit passender ARS wird gefunden, BY-OE nicht")
    @Test
    void bwIsolationNurPassendeOe() {
        Onlinedienst od = bwFile.getOnlinedienste().getFirst();
        Organisationseinheit matchingOe = bwFile.getOrganisationseinheiten().get(0);
        Organisationseinheit nonMatchingOe = bwFile.getOrganisationseinheiten().get(1);

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("Es muss genau 1 OE gefunden werden", response.organisations(), hasSize(1));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), matchingOe.getFullId());
        validateOrganisationBasics(item, matchingOe);

        // OE₂ darf nicht enthalten sein
        List<String> oeIds = response.organisations().stream().map(OrganisationItem::oeId).toList();
        assertThat(
            "OE mit BY-ARS darf nicht in der Ergebnismenge sein",
            oeIds,
            not(hasItem(nonMatchingOe.getFullId()))
        );
    }

    // =====================================================
    // BUND: OD und OE beide auf Bund-Ebene "000000000000"
    // ARS.matchesHierarchy: trim("000000000000") = "00" für beide → Match
    // =====================================================

    @DisplayName("BUND - OD und OE auf Bund-Ebene: OE wird gefunden")
    @Test
    void bundExactMatch() {
        Onlinedienst od = bundFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = bundFile.getOrganisationseinheiten().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("organisations darf nicht leer sein", response.organisations(), is(not(empty())));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);
    }

    // =====================================================
    // BB: Vollständige Response-Validierung (getMax)
    // Validiert alle Felder: oeId, name, addresses, roles (lbId, leikakeys, ars),
    // externalLastUpdate
    // =====================================================

    @DisplayName("BB - Vollständige Response-Validierung aller Felder")
    @Test
    void bbVollstaendigeValidierung() {
        Onlinedienst od = bbFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = bbFile.getOrganisationseinheiten().getFirst();
        Leistungsbeschreibung lb = bbFile.getLeistungsbeschreibungen().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("externalLastUpdate darf nicht null sein", response.externalLastUpdate(), is(notNullValue()));
        assertThat("organisations darf nicht leer sein", response.organisations(), is(not(empty())));

        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());

        // OE-Stammdaten
        assertThat("oeId muss der Full-ID der OE entsprechen", item.oeId(), is(oe.getFullId()));
        assertThat("name muss dem Titel der OE entsprechen", item.name(), is(oe.getTitel()));

        // Adressen
        assertThat("addresses darf nicht null sein", item.addresses(), is(notNullValue()));

        // Rollen
        assertThat("roles darf nicht leer sein", item.roles(), is(not(empty())));
        validateRole(item, lb.getFullId(), BB_ARS_GEMEINDE, lb.getReferenzLeiKa());
    }

    // =====================================================
    // MV: Mehrere Rollen pro OE
    // 1 OD mit 3 LBs, 1 OE – je eine ZS-OD und ZS-OE pro LB auf
    // unterschiedlichen ARS-Ebenen. Die OE erhält dadurch 3 Rollen.
    // =====================================================

    @DisplayName("MV - Mehrere Rollen: 1 OE mit 3 Rollen (je 1 pro LB)")
    @Test
    void mvMehrereRollenProOe() {
        Onlinedienst od = mvFile.getOnlinedienste().getFirst();
        Organisationseinheit oe = mvFile.getOrganisationseinheiten().getFirst();

        OrganisationsApiResponse response = getOrganisationsResponse(od.getFullId());

        assertThat("Es muss genau 1 OE gefunden werden", response.organisations(), hasSize(1));
        OrganisationItem item = findExactlyOneOrganisation(response.organisations(), oe.getFullId());
        validateOrganisationBasics(item, oe);

        assertThat("Die OE muss 3 Rollen haben", item.roles(), hasSize(3));

        // Jede LB muss als Rolle vorhanden sein
        List<Leistungsbeschreibung> lbs = mvFile.getLeistungsbeschreibungen();
        List<String> expectedArsList = List.of(MV_ARS_GEMEINDE, MV_ARS_KREIS, MV_ARS_LAND);
        for (int i = 0; i < lbs.size(); i++) {
            Leistungsbeschreibung lb = lbs.get(i);
            validateRole(item, lb.getFullId(), expectedArsList.get(i), lb.getReferenzLeiKa());
        }
    }

    // =====================================================
    // 404 bei nicht existenter OD-ID
    // =====================================================

    @DisplayName("404 bei nicht existenter OD-ID")
    @Test
    void notFoundBeiUnbekannterOdId() {
        io.restassured.response.Response response = OnlineserviceConnector.getOrganisationsByOdId("non-existent-od-id");
        assertThat("HTTP-Status muss 404 sein", response.statusCode(), is(404));
    }

    // =====================================================
    // Hilfsmethoden: Testdaten-Erzeugung
    // =====================================================

    /**
     * Erzeugt eine XzufiFile mit 1 LB, 1 OD und 1 OE.
     * ZS-OD erhält {@code odArs}, ZS-OE erhält {@code oeArs}, beide verknüpft mit derselben LB.
     */
    private static XzufiFile createSimpleOeFile(
        BundeslandConfig config,
        String testcaseName,
        String odArs,
        String oeArs
    ) {
        XzufiFile file = XzufiFileCreator.newInstance(config)
            .name(testcaseName)
            .lb(1)
            .od(1)
            .oe(1)
            .zsOd(0) // todo: entfernen, da nachfolgend ohnehin neu gesetzt wird
            .zsOe(0) // todo: entfernen, da nachfolgend ohnehin neu gesetzt wird
            .getMin(); // todo: Eventuell auf `getMin(false)` ändern, da Zuständigkeiten nachfolgend neu gesetzt werden

        file.getZustaendigkeitenOnlinedienst().clear();
        file.getZustaendigkeitenOrganisationseinheit().clear();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();

        List<Anschrift> anschriften = oe.getAnschriften();

        Anschrift anschrift;

        anschrift = new Anschrift();
        anschrift.setTyp("001");
        anschrift.setOrt("Musterort");
        anschrift.setPostleitzahl("00000");
        anschrift.setStrasse("Musterstrasse");
        anschrift.setHausnummer("123");
        anschriften.add(anschrift);

        anschrift = new Anschrift();
        anschrift.setTyp("005");
        anschrift.setOrt("Musterort - Delivery");
        anschrift.setPostleitzahl("00000");
        anschrift.setStrasse("Musterstrasse - Delivery");
        anschrift.setHausnummer("123 D");
        anschriften.add(anschrift);

        anschrift = new Anschrift();
        anschrift.setTyp("006");
        anschrift.setOrt("Musterort - Postal");
        anschrift.setPostleitzahl("00000");
        anschrift.setStrasse("Musterstrasse - Postal");
        anschrift.setHausnummer("123 P");
        anschriften.add(anschrift);

        Zustaendigkeit zsOd = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
        zsOd.getArss().clear();
        zsOd.getArss().add(odArs);
        file.getZustaendigkeitenOnlinedienst().add(zsOd);

        Zustaendigkeit zsOe = ZsOrganisationseinheitCreator.getInstance(config).getMin(lb.getId(), oe.getId());
        zsOe.getArss().clear();
        zsOe.getArss().add(oeArs);
        file.getZustaendigkeitenOrganisationseinheit().add(zsOe);

        return file;
    }

    /**
     * Erzeugt eine XzufiFile mit 1 LB, 1 OD und N OEs.
     * ZS-OD verknüpft die LB mit dem OD auf {@code odArs}.
     * Je eine ZS-OE pro OE auf der jeweils angegebenen ARS-Ebene.
     */
    private static XzufiFile createMultiLevelOeFile(
        BundeslandConfig config,
        String testcaseName,
        String odArs,
        String... oeArsLevels
    ) {
        int oeCount = oeArsLevels.length;
        XzufiFile file = XzufiFileCreator.newInstance(config)
            .name(testcaseName)
            .lb(1)
            .od(1)
            .oe(oeCount)
            .zsOd(1)
            .zsOe(1)
            .getMin();

        file.getZustaendigkeitenOnlinedienst().clear();
        file.getZustaendigkeitenOrganisationseinheit().clear();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();

        Zustaendigkeit zsOd = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
        zsOd.getArss().clear();
        zsOd.getArss().add(odArs);
        file.getZustaendigkeitenOnlinedienst().add(zsOd);

        for (int i = 0; i < oeCount; i++) {
            Organisationseinheit oe = file.getOrganisationseinheiten().get(i);
            Zustaendigkeit zsOe = ZsOrganisationseinheitCreator.getInstance(config).getMin(lb.getId(), oe.getId());
            zsOe.getArss().clear();
            zsOe.getArss().add(oeArsLevels[i]);
            file.getZustaendigkeitenOrganisationseinheit().add(zsOe);
        }

        return file;
    }

    /**
     * Erzeugt eine XzufiFile mit 3 LBs, 1 OD und 1 OE.
     * Pro LB[i] wird je 1 ZS-OD und 1 ZS-OE mit {@code arsLevels[i]} erstellt.
     * Dadurch erhält die OE 3 Rollen.
     */
    private static XzufiFile createMultiRoleFile(BundeslandConfig config, String testcaseName, String... arsLevels) {
        int lbCount = arsLevels.length;
        XzufiFile file = XzufiFileCreator.newInstance(config)
            .name(testcaseName)
            .lb(lbCount)
            .od(1)
            .oe(1)
            .zsOd(1)
            .zsOe(1)
            .getMin();

        file.getZustaendigkeitenOnlinedienst().clear();
        file.getZustaendigkeitenOrganisationseinheit().clear();

        Onlinedienst od = file.getOnlinedienste().getFirst();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();

        for (int i = 0; i < lbCount; i++) {
            Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().get(i);

            Zustaendigkeit zsOd = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
            zsOd.getArss().clear();
            zsOd.getArss().add(arsLevels[i]);
            file.getZustaendigkeitenOnlinedienst().add(zsOd);

            Zustaendigkeit zsOe = ZsOrganisationseinheitCreator.getInstance(config).getMin(lb.getId(), oe.getId());
            zsOe.getArss().clear();
            zsOe.getArss().add(arsLevels[i]);
            file.getZustaendigkeitenOrganisationseinheit().add(zsOe);
        }

        return file;
    }

    /**
     * Erzeugt eine XzufiFile mit 1 LB, 1 OD und 2 OEs.
     * ZS-OD mit {@code odArs}, OE₁ mit {@code matchingOeArs} (erwartet: Match),
     * OE₂ mit {@code nonMatchingOeArs} (erwartet: kein Match).
     */
    private static XzufiFile createIsolationFile(
        BundeslandConfig config,
        String testcaseName,
        String odArs,
        String matchingOeArs,
        String nonMatchingOeArs
    ) {
        XzufiFile file = XzufiFileCreator.newInstance(config)
            .name(testcaseName)
            .lb(1)
            .od(1)
            .oe(2)
            .zsOd(1)
            .zsOe(1)
            .getMin();

        file.getZustaendigkeitenOnlinedienst().clear();
        file.getZustaendigkeitenOrganisationseinheit().clear();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Onlinedienst od = file.getOnlinedienste().getFirst();

        Zustaendigkeit zsOd = ZsOnlinedienstCreator.getInstance(config).getMin(lb.getId(), od.getId());
        zsOd.getArss().clear();
        zsOd.getArss().add(odArs);
        file.getZustaendigkeitenOnlinedienst().add(zsOd);

        // OE₁: matching ARS
        Organisationseinheit oe1 = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsOe1 = ZsOrganisationseinheitCreator.getInstance(config).getMin(lb.getId(), oe1.getId());
        zsOe1.getArss().clear();
        zsOe1.getArss().add(matchingOeArs);
        file.getZustaendigkeitenOrganisationseinheit().add(zsOe1);

        // OE₂: non-matching ARS
        Organisationseinheit oe2 = file.getOrganisationseinheiten().get(1);
        Zustaendigkeit zsOe2 = ZsOrganisationseinheitCreator.getInstance(config).getMin(lb.getId(), oe2.getId());
        zsOe2.getArss().clear();
        zsOe2.getArss().add(nonMatchingOeArs);
        file.getZustaendigkeitenOrganisationseinheit().add(zsOe2);

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

    private static OrganisationsApiResponse getOrganisationsResponse(String odFullId) {
        io.restassured.response.Response response = OnlineserviceConnector.getOrganisationsByOdId(odFullId);
        assertThat("HTTP-Status muss 200 sein", response.statusCode(), is(200));
        return response.as(OrganisationsApiResponse.class);
    }

    /**
     * Sucht in der Ergebnisliste nach genau einem Eintrag mit der gegebenen OE-ID.
     */
    private static OrganisationItem findExactlyOneOrganisation(List<OrganisationItem> organisations, String oeFullId) {
        List<OrganisationItem> filtered = organisations
            .stream()
            .filter(o -> oeFullId.equals(o.oeId()))
            .toList();

        assertThat(
            "Es muss genau ein Ergebnis für die OE '%s' vorhanden sein".formatted(oeFullId),
            filtered,
            hasSize(1)
        );
        return filtered.getFirst();
    }

    private static void validateOrganisationBasics(OrganisationItem item, Organisationseinheit expectedOe) {
        assertThat("oeId muss der Full-ID der OE entsprechen", item.oeId(), is(expectedOe.getFullId()));
        assertThat("name muss dem Titel der OE entsprechen", item.name(), is(expectedOe.getTitel()));
    }

    private static void validateRole(
        OrganisationItem item,
        String expectedLbId,
        String expectedArs,
        String expectedLeikaKey
    ) {
        List<RoleItem> roles = item.roles();
        assertThat("roles darf nicht null sein", roles, is(notNullValue()));

        RoleItem role = roles
            .stream()
            .filter(r -> expectedLbId.equals(r.lbId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Keine Rolle mit lbId '%s' gefunden".formatted(expectedLbId)));

        assertThat("role.ars muss die erwartete OE-ARS enthalten", role.ars(), hasItem(expectedArs));
        assertThat(
            "role.leikakeys muss den LeiKa-Schlüssel der Leistungsbeschreibung enthalten",
            role.leikakeys(),
            hasItem(expectedLeikaKey)
        );
    }
}
