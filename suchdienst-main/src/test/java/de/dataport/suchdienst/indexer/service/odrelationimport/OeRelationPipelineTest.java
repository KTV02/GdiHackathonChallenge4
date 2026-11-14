/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.AddressType;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Address;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role.Source;
import de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.CodeAnschrifttyp;
import de.xoev.xzufi.v2_3_1.CodeOrganisationseinheitsrolleFuerOnlinedienst;
import de.xoev.xzufi.v2_3_1.CodeZustaendigkeitsrolle;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.NameOrganisation;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitOnlinedienstMitRolle;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-Tests für die OE-Relation-Pipeline ({@link ArsMatchingService}, {@link RoleExtractor}, {@link OeStammdatenEnricher}).
 *
 * <p>Schwerpunkte:
 * <ul>
 *   <li>ARS-Hierarchie-Matching (aufwärts, abwärts, exakt, kein Treffer)</li>
 *   <li>Rollen-Auswertung aus ZS-OE-Zuständigkeiten (Source=ZS_OE)</li>
 *   <li>Rollen-Auswertung aus direkten XZuFi-2.3-Referenzen (Source=OD)</li>
 *   <li>Kombination beider Quellen für dieselbe OE</li>
 *   <li>Stammdaten-Anreicherung und Fallback bei fehlenden Stammdaten</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OeRelationPipelineTest {

    private static final String SCHEME_AGENCY_ID = "de.bund";
    private static final String OE_ID = SCHEME_AGENCY_ID + ".OE.oe-1";
    private static final String OE_ID_2 = SCHEME_AGENCY_ID + ".OE.oe-2";
    private static final String LB_XZUFI_ID = "lb-1";
    private static final String LB_ID = SCHEME_AGENCY_ID + ".LB." + LB_XZUFI_ID;
    private static final String ROLLE_CODE = "01";
    private static final String ROLLE_NAME = "Fachlicher Ansprechpartner";
    private static final String ARS_GEMEINDE = "091620000000";
    private static final String ARS_ANDERES_LAND = "020000000000";
    private static final String ZS_OE_ROLLE_URI =
        "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle";
    private final ArsMatchingService arsMatchingService = new ArsMatchingService();
    private final RoleExtractor roleExtractor = new RoleExtractor(arsMatchingService);
    private final OeStammdatenEnricher oeStammdatenEnricher = new OeStammdatenEnricher();

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /** Erzeugt ein ZS-ID-Identifikator mit SchemeID für den Zuständigkeits-Konstruktor. */
    private static Identifikator zsId(String schemeId) {
        Identifikator id = new Identifikator();
        id.setValue("zs-" + System.nanoTime());
        id.setSchemeID(schemeId);
        id.setSchemeAgencyID(SCHEME_AGENCY_ID);
        return id;
    }

    /**
     * Zerlegt eine volle Suchdienst-ID (z.B. {@code de.bund.LB.lb-1}) in den dazugehörigen
     * XZuFi-Identifikator mit Roh-Wert ({@code lb-1}) und {@code schemeAgencyID}.
     */
    private static Identifikator decomposeFullId(String composedId) {
        String prefix = SCHEME_AGENCY_ID + ".";
        String withoutAgency = composedId.substring(prefix.length());
        String rawValue = withoutAgency.substring(withoutAgency.indexOf('.') + 1);
        return identifikatorWith(rawValue, SCHEME_AGENCY_ID);
    }

    /** Erzeugt eine OD-Zuständigkeit als {@link ConvertedZustaendigkeit} mit lbId und ARS. */
    private static ConvertedZustaendigkeit odZs(String lbId, String ars) {
        ZustaendigkeitOnlinedienst zs = new ZustaendigkeitOnlinedienst();
        zs.setId(zsId("ZustaendigkeitOnlinedienst"));
        if (lbId != null) {
            zs.getLeistungID().add(decomposeFullId(lbId));
        }
        if (ars != null) {
            Identifikator gebiet = new Identifikator();
            gebiet.setValue(ars);
            zs.getGebietID().add(gebiet);
        }
        ZustaendigkeitTransferObjekt jzufi = new ZustaendigkeitTransferObjekt();
        jzufi.getZustaendigkeit().add(zs);
        jzufi.setUebergeordnetesObjektID(identifikatorWith("dummy-od", SCHEME_AGENCY_ID));
        return ConvertedZustaendigkeit.of(new GenericZustaendigkeitTransferObjekt(jzufi));
    }

    /** Erzeugt eine OE-Zuständigkeit als {@link ConvertedZustaendigkeit} mit parentId (OE-ID), lbId, ARS und optionalen Rollen. */
    private static ConvertedZustaendigkeit oeZs(
        String oeId,
        String lbId,
        String ars,
        CodeZustaendigkeitsrolle... rollen
    ) {
        ZustaendigkeitOrganisationseinheit zs = new ZustaendigkeitOrganisationseinheit();
        zs.setId(zsId("ZustaendigkeitOrganisationseinheit"));
        if (lbId != null) {
            zs.getLeistungID().add(decomposeFullId(lbId));
        }
        if (ars != null) {
            Identifikator gebiet = new Identifikator();
            gebiet.setValue(ars);
            zs.getGebietID().add(gebiet);
        }
        if (rollen != null) {
            for (CodeZustaendigkeitsrolle rolle : rollen) {
                zs.getRolle().add(rolle);
            }
        }
        ZustaendigkeitTransferObjekt jzufi = new ZustaendigkeitTransferObjekt();
        jzufi.getZustaendigkeit().add(zs);
        jzufi.setUebergeordnetesObjektID(decomposeFullId(oeId));
        GenericZustaendigkeitTransferObjekt generic = new GenericZustaendigkeitTransferObjekt(
            jzufi
        );
        return ConvertedZustaendigkeit.of(generic);
    }

    /** Erzeugt eine {@link CodeZustaendigkeitsrolle} mit Code und Standard-URI für ZS-OE. */
    private static CodeZustaendigkeitsrolle role(String code, String name) {
        CodeZustaendigkeitsrolle r = new CodeZustaendigkeitsrolle();
        r.setCode(code);
        r.setName(name);
        r.setListURI(ZS_OE_ROLLE_URI);
        return r;
    }

    /**
     * Erzeugt eine OD-Zuständigkeit ({@link ConvertedZustaendigkeit}) mit direkter
     * OE-Referenz. Die enthaltene {@link ZustaendigkeitOnlinedienst} verweist auf die
     * übergebene OE mit der angegebenen Rolle.
     */
    private static ConvertedZustaendigkeit odZsWithDirectOe(
        String oeId,
        String lbId,
        String rolleCode,
        String gebietArs
    ) {
        return odZsWithDirectOe(oeId, lbId, rolleCode, gebietArs, null);
    }

    private static ConvertedZustaendigkeit odZsWithDirectOe(
        String oeId,
        String lbId,
        String rolleCode,
        String gebietArs,
        String rolleUri
    ) {
        ZustaendigkeitOnlinedienst zs = new ZustaendigkeitOnlinedienst();
        zs.setId(zsId("ZustaendigkeitOnlinedienst"));

        zs.getLeistungID().add(decomposeFullId(lbId));
        if (gebietArs != null) {
            Identifikator gebiet = new Identifikator();
            gebiet.setValue(gebietArs);
            gebiet.setSchemeAgencyID(SCHEME_AGENCY_ID);
            zs.getGebietID().add(gebiet);
        }

        OrganisationseinheitOnlinedienstMitRolle oeMitRolle =
            new OrganisationseinheitOnlinedienstMitRolle();
        oeMitRolle.setOrganisationseinheitID(identifikatorWith(oeId, SCHEME_AGENCY_ID));

        CodeOrganisationseinheitsrolleFuerOnlinedienst rolle =
            new CodeOrganisationseinheitsrolleFuerOnlinedienst();
        rolle.setCode(rolleCode);
        if (rolleUri != null) {
            rolle.setListURI(rolleUri);
        }
        oeMitRolle.getRolle().add(rolle);

        zs.getOrganisationseinheit().add(oeMitRolle);

        ZustaendigkeitTransferObjekt jzufi = new ZustaendigkeitTransferObjekt();
        jzufi.getZustaendigkeit().add(zs);
        jzufi.setUebergeordnetesObjektID(identifikatorWith("dummy-od", SCHEME_AGENCY_ID));

        return ConvertedZustaendigkeit.of(new GenericZustaendigkeitTransferObjekt(jzufi));
    }

    private static Identifikator identifikatorWith(String value, String schemeAgencyId) {
        Identifikator id = new Identifikator();
        id.setValue(value);
        id.setSchemeAgencyID(schemeAgencyId);
        return id;
    }

    /** Erzeugt eine gemockte {@link GenericOrganisationseinheit} mit Name und optionalen Anschriften. */
    private static GenericOrganisationseinheit stubOeStammdaten(
        String name,
        List<AnschriftOrganisationseinheit> anschriften
    ) {
        OrganisationseinheitErweitert xoe = new OrganisationseinheitErweitert();
        if (name != null) {
            NameOrganisation nameOrg = new NameOrganisation();
            StringLocalized sl = new StringLocalized();
            sl.setLanguageCode("de");
            sl.setValue(name);
            nameOrg.getName().add(sl);
            xoe.getName().add(nameOrg);
        }
        if (anschriften != null) {
            xoe.getAnschrift().addAll(anschriften);
        }
        GenericOrganisationseinheit oe = mock(GenericOrganisationseinheit.class);
        when(oe.getOrConvertXZuFi23()).thenReturn(xoe);
        return oe;
    }

    /**
     * Pipeline-Helfer: Kombiniert {@link RoleExtractor#extractRoles} und
     * {@link OeStammdatenEnricher#enrich} analog zum ehemaligen
     * {@code OeRelationFactory#buildOrganisations}.
     */
    private List<OrganisationseinheitRollenZuordnung> buildOrganisations(
        Map<String, List<ConvertedZustaendigkeit>> odByLbId,
        OeZustaendigkeitenLookup oeZustaendigkeiten,
        OeStammdatenLookup oeStammdaten,
        LeikaLookup leikas
    ) {
        Map<String, List<Role>> rollenByOeId = roleExtractor.extractRoles(
            odByLbId,
            oeZustaendigkeiten,
            leikas
        );
        return oeStammdatenEnricher.enrich(rollenByOeId, oeStammdaten);
    }

    // -------------------------------------------------------------------------
    // ARS-Matching: buildOrganisations
    // -------------------------------------------------------------------------

    /**
     * Exakter ARS-Treffer: OD-ARS und OE-ARS sind identisch → OE wird in die Ergebnismenge aufgenommen.
     */
    @Test
    void exactArsMatch_returnsOeWithZsOeRole() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).oeId()).isEqualTo(OE_ID);
        assertThat(result.get(0).rollen()).hasSize(1);
        assertThat(result.get(0).rollen().get(0).source()).isEqualTo(Source.ZS_OE);
    }

    /**
     * Aufwärts-Match: OE-ARS ist ein übergeordneter Knoten des OD-ARS (OE auf Kreisebene,
     * OD auf Gemeindeebene) → OE wird einbezogen.
     */
    @Test
    void arsMatchUpward_oeArsIsAncestorOfOdArs() {
        // given
        // OD-ARS: spezifische Gemeinde im Landkreis 09162
        // OE-ARS: übergeordneter Landkreis → Aufwärts-Match erwartet
        String arsSpecificGemeinde = "091620152000"; // Gemeinde in Landkreis 09162

        var odZs = odZs(LB_ID, arsSpecificGemeinde);
        var oeZs = oeZs(OE_ID, LB_ID, "091620000000", role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then – OE-ARS ist Landkreis, OD-ARS ist Gemeinde im gleichen Landkreis → Treffer
        assertThat(result).hasSize(1);
        assertThat(result.get(0).oeId()).isEqualTo(OE_ID);
    }

    /**
     * Abwärts-Match: OD-ARS ist ein übergeordneter Knoten des OE-ARS (OD auf Landesebene,
     * OE auf Gemeindeebene) → OE wird einbezogen.
     */
    @Test
    void arsMatchDownward_odArsIsAncestorOfOeArs() {
        // given
        // OD-ARS auf Landesebene Bayern (09), OE-ARS auf Gemeindeebene innerhalb Bayerns
        String odArsLand = "090000000000";
        String oeArsGemeinde = "091620000000"; // Gemeinde in Bayern → hierarchisch unterhalb

        var odZs = odZs(LB_ID, odArsLand);
        var oeZs = oeZs(OE_ID, LB_ID, oeArsGemeinde, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).oeId()).isEqualTo(OE_ID);
    }

    /**
     * Kein ARS-Treffer: OD- und OE-ARS liegen in verschiedenen Bundesländern → leeres Ergebnis.
     */
    @Test
    void arsMismatch_differentFederalStates_returnsEmpty() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE); // Bayern
        var oeZs = oeZs(OE_ID, LB_ID, ARS_ANDERES_LAND, role(ROLLE_CODE, ROLLE_NAME)); // Berlin

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).isEmpty();
    }

    /**
     * OD-ARS ist null → kein Matching-Versuch, leeres Ergebnis.
     */
    @Test
    void odArsNull_skipsArsMatchingEntirely() {
        // given
        var odZs = odZs(LB_ID, null); // kein ARS
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).isEmpty();
    }

    /**
     * OE-ARS ist null → hasArsMatch gibt false zurück, OE wird nicht aufgenommen.
     */
    @Test
    void oeArsNull_skipsEntry() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, null, role(ROLLE_CODE, ROLLE_NAME)); // kein ARS

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).isEmpty();
    }

    /**
     * OE-parentId ist null → hasArsMatch gibt false zurück, OE wird nicht aufgenommen.
     */
    @Test
    void oeParentIdNull_skipsEntry() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);

        // GenericZustaendigkeitTransferObjekt erzwingt non-null parentId im Konstruktor,
        // daher simulieren wir den Fall über einen Mock.
        var mockGeneric = mock(GenericZustaendigkeitTransferObjekt.class);
        when(mockGeneric.getId()).thenReturn("zs-oe-null-parent");
        when(mockGeneric.getParentId()).thenReturn(null);
        when(mockGeneric.getGebietIds()).thenReturn(List.of());
        when(mockGeneric.getLbIds()).thenReturn(List.of(LB_ID));
        var oeZs = ConvertedZustaendigkeit.of(mockGeneric);

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).isEmpty();
    }

    /**
     * Zwei OD-Zuständigkeiten mit unterschiedlichen ARS-Werten matchen dieselbe OE+Rolle
     * → die ARS-Werte werden in einem einzigen Role-Objekt zusammengeführt.
     */
    @Test
    void multipleOdArsMatchSameOeRole_arsValuesAreMerged() {
        // given
        String ars1 = "091620000000";
        String ars2 = "091630000000";
        // OE-ARS auf Landesebene Bayern → matcht beide Gemeinden
        String oeArs = "090000000000";

        var odZs1 = odZs(LB_ID, ars1);
        var odZs2 = odZs(LB_ID, ars2);
        var oeZs = oeZs(OE_ID, LB_ID, oeArs, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs1, odZs2));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then – eine OE mit einer Rolle, aber OE-ARS (nicht OD-ARS) im Set
        assertThat(result).hasSize(1);
        Role rolle = result.get(0).rollen().get(0);
        // Der OE-ARS ist "090000000000", nicht die OD-ARS-Werte
        assertThat(rolle.ars()).containsOnly(oeArs);
    }

    /**
     * Mehrere OE-Zuständigkeiten matchen dieselbe OD-Zuständigkeit → alle OEs werden zurückgegeben.
     */
    @Test
    void multipleOeMatchSameOdArs_allOesReturned() {
        // given
        var odZs = odZs(LB_ID, "090000000000"); // Bayern
        var oeZs1 = oeZs(OE_ID, LB_ID, "091620000000", role("01", "Stelle A"));
        var oeZs2 = oeZs(OE_ID_2, LB_ID, "091630000000", role("02", "Stelle B"));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs1, oeZs2)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(OrganisationseinheitRollenZuordnung::oeId)
            .containsExactly(OE_ID, OE_ID_2); // alphabetisch sortiert
    }

    /**
     * Ergebnisliste ist alphabetisch nach OE-ID sortiert.
     */
    @Test
    void resultIsSortedAlphabeticallyByOeId() {
        // given
        String oeIdA = SCHEME_AGENCY_ID + ".OE.aaa";
        String oeIdM = SCHEME_AGENCY_ID + ".OE.mmm";
        String oeIdZ = SCHEME_AGENCY_ID + ".OE.zzz";

        var odZs = odZs(LB_ID, "090000000000");
        var oeZsA = oeZs(oeIdA, LB_ID, "091620000000", role("01", "A"));
        var oeZsM = oeZs(oeIdM, LB_ID, "091630000000", role("01", "M"));
        var oeZsZ = oeZs(oeIdZ, LB_ID, "091640000000", role("01", "Z"));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(
            Map.of(LB_ID, List.of(oeZsZ, oeZsA, oeZsM))
        );
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result)
            .extracting(OrganisationseinheitRollenZuordnung::oeId)
            .containsExactly(oeIdA, oeIdM, oeIdZ);
    }

    // -------------------------------------------------------------------------
    // Rollen-Auswertung ZS-OE (Source=ZS_OE)
    // -------------------------------------------------------------------------

    /**
     * Der OE-ARS aus der ZS-OE (nicht der OD-ARS) wird in der Rolle ausgegeben.
     */
    @Test
    void oeRole_arsFromOeNotFromOd() {
        // given
        String odArs = "090000000000"; // Landesebene
        String oeArs = "091620000000"; // Gemeindeebene

        var odZs = odZs(LB_ID, odArs);
        var oeZs = oeZs(OE_ID, LB_ID, oeArs, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then – ARS in der Rolle muss OE-ARS sein, nicht OD-ARS
        assertThat(result).hasSize(1);
        Role rolle = result.get(0).rollen().get(0);
        assertThat(rolle.ars()).containsOnly(oeArs);
        assertThat(rolle.ars()).doesNotContain(odArs);
    }

    /**
     * Leika-IDs aus dem LeikaLookup werden in der Rolle mitgeliefert.
     */
    @Test
    void oeRole_leikaIdsAttachedFromLookup() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of(LB_ID, Set.of("99108017000000", "99108017000001")));

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rollen().get(0).leikakeys()).containsExactlyInAnyOrder(
            "99108017000000",
            "99108017000001"
        );
    }

    /**
     * Die lbId aus der ZS-OE wird in der Rolle abgelegt.
     */
    @Test
    void oeRole_lbIdFromZustaendigkeit() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result.get(0).rollen().get(0).lbId()).isEqualTo(LB_ID);
    }

    /**
     * Rollenname aus ZS-OE-Zuständigkeit: bekannter Code wird auf ENUM gemappt.
     */
    @Test
    void oeRole_codeFromZustaendigkeitsrolle() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        // Code "02" → COMPETENT_OFFICE (urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle.02)
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role("02", "Zuständige Stelle"));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result.get(0).rollen().get(0).role()).isEqualTo(RoleType.COMPETENT_OFFICE);
    }

    /**
     * Responsibility ist null → Rolle wird auf UNBESTIMMT gemappt.
     */
    @Test
    void oeRole_nullResponsibility_roleIsUnbestimmt() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, null); // kein Responsibility-Objekt

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rollen().get(0).role()).isEqualTo(RoleType.UNKNOWN);
    }

    // -------------------------------------------------------------------------
    // Rollen-Auswertung XZuFi 2.3 Direktreferenzen (Source=OD)
    // -------------------------------------------------------------------------

    /**
     * Direkte OE-Referenz aus XZuFi 2.3 → Rolle mit Source=OD.
     */
    @Test
    void directOeRef_roleWithSourceOd() {
        // given
        // Roher Wert "oe-1" → getIdentificatorString erzeugt OE_ID="de.bund.OE.oe-1"
        var directZs = odZsWithDirectOe("oe-1", LB_ID, ROLLE_CODE, null);
        var odByLbId = Map.of(LB_ID, List.of(directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).oeId()).isEqualTo(OE_ID);
        assertThat(result.get(0).rollen()).hasSize(1);
        assertThat(result.get(0).rollen().get(0).source()).isEqualTo(Source.OD);
    }

    /**
     * Rollencode mit passender URI wird auf das korrekte ENUM gemappt.
     * (Ehemals: Rollenname wird aus der Codeliste aufgelöst.)
     */
    @Test
    void directOeRef_roleCodeWithUriMapsToEnum() {
        // given – ROLLE_CODE="01" + OD-URI → PROFESSIONAL_POINT_OF_CONTACT
        var directZs = odZsWithDirectOe(
            "oe-direct",
            LB_ID,
            ROLLE_CODE,
            null,
            "urn:xoev-de:fim:codeliste:xzufi.organisationseinheitsrollefueronlinedienst"
        );
        var odByLbId = Map.of(LB_ID, List.of(directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rollen().get(0).role()).isEqualTo(
            RoleType.PROFESSIONAL_POINT_OF_CONTACT
        );
    }

    /**
     * Fehlt die URI, kann kein ENUM gemappt werden → UNBESTIMMT.
     * (Ehemals: Ohne Codelist-Text Fallback auf Code.)
     */
    @Test
    void directOeRef_missingUri_yieldsUnbestimmt() {
        // given – keine listURI gesetzt → fromCode(null, code) → UNBESTIMMT
        var directZs = odZsWithDirectOe("oe-direct", LB_ID, ROLLE_CODE, null);
        var odByLbId = Map.of(LB_ID, List.of(directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result.get(0).rollen().get(0).role()).isEqualTo(RoleType.UNKNOWN);
    }

    /**
     * Direkte OE-Referenz ohne OD-Zuständigkeiten im Lookup → OE trotzdem im Ergebnis.
     * Keine ARS-Hierarchiesuche erforderlich.
     */
    @Test
    void directOeRef_noArsHierarchyNeeded_oeStillReturned() {
        // given
        var directZs = odZsWithDirectOe("oe-direct", LB_ID, ROLLE_CODE, null);
        var odByLbId = Map.of(LB_ID, List.of(directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(new HashMap<>());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rollen().get(0).source()).isEqualTo(Source.OD);
    }

    /**
     * Direkte OE-Referenz mit Gebiets-ARS → ARS wird in der Rolle gesetzt.
     */
    @Test
    void directOeRef_gebietArsStoredInRole() {
        // given
        String gebietArs = ARS_GEMEINDE;
        var directZs = odZsWithDirectOe("oe-direct", LB_ID, ROLLE_CODE, gebietArs);
        var odByLbId = Map.of(LB_ID, List.of(directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(new HashMap<>());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result.get(0).rollen().get(0).ars()).containsOnly(gebietArs);
    }

    /**
     * Dieselbe OE ist sowohl in ZS-OD (direkt) als auch in ZS-OE (hierarchisch) enthalten
     * → zwei separate Role-Objekte mit unterschiedlichem Source.
     */
    @Test
    void oeInBothZsOdAndZsOe_producesTwoRoles() {
        // given
        // ZS-OE-Pfad
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        // ZS-OD-Pfad (direkte Referenz derselben OE) mit realistischem LB-ID-Format.
        // Der XZuFi-Identifikator trägt den Rohwert "lb-1"; die Suchdienst-Voll-ID lautet "de.bund.LB.lb-1".
        var directZs = odZsWithDirectOe("oe-1", LB_ID, ROLLE_CODE, null);

        var odByLbId = Map.of(LB_ID, List.of(odZs, directZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(Map.of(LB_ID, Set.of()));

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then – eine OE mit zwei Rollen (ZS_OE und OD)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rollen()).hasSize(2);
        assertThat(result.get(0).rollen())
            .extracting(Role::source)
            .containsExactlyInAnyOrder(Source.ZS_OE, Source.OD);
    }

    /**
     * Zwei OD-Zuständigkeits-Entitäten mit je einer direkten OE → beide OEs im Ergebnis.
     */
    @Test
    void xzufi23MultipleZustaendigkeiten_allDirectOesCollected() {
        // given
        // Zwei Entitäten, jede mit einer direkten OE-Referenz
        var directZsA = odZsWithDirectOe("oe-a", LB_ID, ROLLE_CODE, null);
        var directZsB = odZsWithDirectOe("oe-b", LB_ID, ROLLE_CODE, null);

        var odByLbId = Map.of(LB_ID, List.of(directZsA, directZsB));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());
        var oeStammdaten = new OeStammdatenLookup(Map.of());
        var leikas = new LeikaLookup(new HashMap<>());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(OrganisationseinheitRollenZuordnung::oeId)
            .containsExactlyInAnyOrder(
                SCHEME_AGENCY_ID + ".OE.oe-a",
                SCHEME_AGENCY_ID + ".OE.oe-b"
            );
    }

    // -------------------------------------------------------------------------
    // Stammdaten-Anreicherung
    // -------------------------------------------------------------------------

    /**
     * OE-Stammdaten vorhanden → Name und Adressen werden in die Zuordnung übernommen.
     */
    @Test
    void oeStammdaten_nameAndAddressPopulated() {
        // given
        // Anschrift aufbauen
        AnschriftOrganisationseinheit anschrift = new AnschriftOrganisationseinheit();
        CodeAnschrifttyp typ = new CodeAnschrifttyp();
        // Code "001" → AddressType.ADDRESS (urn:xoev-de:fim:codeliste:xzufi.anschrifttyp.001)
        typ.setCode("001");
        anschrift.setTyp(typ);
        anschrift.setOrt("München");
        anschrift.setPostleitzahl("80335");
        anschrift.setStrasse("Prielmayerstr.");
        anschrift.setHausnummer("7a");

        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));

        GenericOrganisationseinheit oeStub = stubOeStammdaten("Testbehörde", List.of(anschrift));
        var oeStammdaten = new OeStammdatenLookup(Map.of(OE_ID, oeStub));
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        OrganisationseinheitRollenZuordnung zuordnung = result.get(0);
        assertThat(zuordnung.name()).isEqualTo("Testbehörde");
        assertThat(zuordnung.addresses()).hasSize(1);
        Address address = zuordnung.addresses().get(0);
        assertThat(address.type()).isEqualTo(AddressType.ADDRESS);
        assertThat(address.city()).isEqualTo("München");
        assertThat(address.zip()).isEqualTo("80335");
        assertThat(address.street()).isEqualTo("Prielmayerstr.");
        assertThat(address.houseNumber()).isEqualTo("7a"); // String, auch "7a" möglich
    }

    /**
     * OE-Stammdaten fehlen → minimale Zuordnung mit null-Name und leerer Adressliste,
     * Rollen bleiben erhalten.
     */
    @Test
    void oeStammdatenMissing_minimalZuordnungReturned_rollesPreserved() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        var oeStammdaten = new OeStammdatenLookup(Map.of()); // leer → kein Eintrag für OE_ID
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result).hasSize(1);
        OrganisationseinheitRollenZuordnung zuordnung = result.get(0);
        assertThat(zuordnung.oeId()).isEqualTo(OE_ID);
        assertThat(zuordnung.name()).isNull();
        assertThat(zuordnung.addresses()).isEmpty();
        assertThat(zuordnung.rollen()).hasSize(1); // Rolle bleibt erhalten
    }

    /**
     * Anschrift ohne Typ (typ=null) wird gefiltert und nicht in die Adressliste aufgenommen.
     */
    @Test
    void addressWithNullTyp_isFilteredOut() {
        // given
        AnschriftOrganisationseinheit anschriftOhneTyp = new AnschriftOrganisationseinheit();
        anschriftOhneTyp.setOrt("München");
        // typ wird nicht gesetzt → null

        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odByLbId = Map.of(LB_ID, List.of(odZs));
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));
        GenericOrganisationseinheit oeStub = stubOeStammdaten(null, List.of(anschriftOhneTyp));
        var oeStammdaten = new OeStammdatenLookup(Map.of(OE_ID, oeStub));
        var leikas = new LeikaLookup(Map.of());

        // when
        List<OrganisationseinheitRollenZuordnung> result = buildOrganisations(
            odByLbId,
            oeZustaendigkeiten,
            oeStammdaten,
            leikas
        );

        // then
        assertThat(result.get(0).addresses()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // collectNeededOeIds
    // -------------------------------------------------------------------------

    /**
     * Leerer Lookup → leere OE-ID-Menge.
     */
    @Test
    void collectNeededOeIds_emptyOdList_returnsEmpty() {
        // given
        var odZustaendigkeiten = new OdZustaendigkeitenLookup(Map.of());
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());

        // when
        Set<String> result = arsMatchingService.collectNeededOeIds(
            odZustaendigkeiten,
            oeZustaendigkeiten
        );

        // then
        assertThat(result).isEmpty();
    }

    /**
     * ARS-Abgleich liefert einen Treffer → OE-ID über ZS-OE-Pfad ins Ergebnis.
     */
    @Test
    void collectNeededOeIds_arsMatch_oeIdFromZsOe() {
        // given
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odZustaendigkeiten = new OdZustaendigkeitenLookup(
            Map.of("od-1", Map.of(LB_ID, List.of(odZs)))
        );
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));

        // when
        Set<String> result = arsMatchingService.collectNeededOeIds(
            odZustaendigkeiten,
            oeZustaendigkeiten
        );

        // then
        assertThat(result).contains(OE_ID);
    }

    /**
     * Direkte OE-Referenz in XZuFi-2.3-OD-Zuständigkeit → OE-ID über Direkt-Pfad ins Ergebnis.
     */
    @Test
    void collectNeededOeIds_directRef_oeIdFromXZuFi23() {
        // given
        var directZs = odZsWithDirectOe("oe-direct", LB_ID, ROLLE_CODE, null);

        var odZustaendigkeiten = new OdZustaendigkeitenLookup(
            Map.of("od-1", Map.of(LB_ID, List.of(directZs)))
        );
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of());

        // when
        Set<String> result = arsMatchingService.collectNeededOeIds(
            odZustaendigkeiten,
            oeZustaendigkeiten
        );

        // then
        assertThat(result).contains(SCHEME_AGENCY_ID + ".OE.oe-direct");
    }

    /**
     * Beide Pfade (ARS und Direkt) liefern verschiedene OEs → Union beider IDs.
     */
    @Test
    void collectNeededOeIds_bothPaths_returnsUnion() {
        // given – Direkt-Referenz (ZS-OD-Pfad)
        var directZs = odZsWithDirectOe("oe-direct", LB_ID, ROLLE_CODE, null);

        // ARS-Treffer (ZS-OE-Pfad) in einem anderen OD
        var odZs = odZs(LB_ID, ARS_GEMEINDE);
        var oeZs = oeZs(OE_ID, LB_ID, ARS_GEMEINDE, role(ROLLE_CODE, ROLLE_NAME));

        var odZustaendigkeiten = new OdZustaendigkeitenLookup(
            Map.of(
                "od-direct",
                Map.of(LB_ID, List.of(directZs)),
                "od-ars",
                Map.of(LB_ID, List.of(odZs))
            )
        );
        var oeZustaendigkeiten = new OeZustaendigkeitenLookup(Map.of(LB_ID, List.of(oeZs)));

        // when
        Set<String> result = arsMatchingService.collectNeededOeIds(
            odZustaendigkeiten,
            oeZustaendigkeiten
        );

        // then
        assertThat(result)
            .contains(SCHEME_AGENCY_ID + ".OE.oe-direct")
            .contains(OE_ID);
    }
}
