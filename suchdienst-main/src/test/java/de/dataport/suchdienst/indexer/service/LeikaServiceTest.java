/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.dataport.pvog.pvexternaldownloader.service.PvExternalDownloaderLeistungskatalogService;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.LeikaEntry;
import de.dataport.suchdienst.indexer.model.mapping.LifeSituation;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.xoev.xzufi.v2_2_0.CodeKlassifizierungBenutzerdefiniert;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.Leistung;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = CodelistService.class)
class LeikaServiceTest {

    public static final CodelistEntry BUERGER = createCodelistEntry("1000000");
    public static final CodelistEntry GEBURT = createCodelistEntry("1010000");
    public static final CodelistEntry NACH_DER_GEBURT = createCodelistEntry("1010200");
    public static final CodelistEntry GESUNDHEIT_UND_VORSORGE = createCodelistEntry("1130000");
    public static final CodelistEntry PFLEGE = createCodelistEntry("1130400");
    private static final List<CodelistEntry> ALL_ENTRIES = List.of(
        BUERGER,
        GEBURT,
        NACH_DER_GEBURT,
        GESUNDHEIT_UND_VORSORGE,
        PFLEGE
    );
    public static final CodelistEntry NICHT_IN_DER_CODELIST = createCodelistEntry("9999999");

    @MockitoBean
    private CodelistService codelistService;

    @MockitoBean
    private PvExternalDownloaderLeistungskatalogService leistungskatalogService;

    @MockitoBean
    private ElasticWriteConnector writeConnectorMigration;

    @MockitoBean
    private ElasticBulkWriteConnector bulkWriteConnector;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @MockitoBean
    private IndexConfiguration indexConfig;

    @MockitoBean
    private MessageSource messageSource;

    public static final String PV_LAGEN_URI = "urn:xoev-de:fim:codeliste:pvlagen";
    private static final String PV_LAGEN_VERION = "20211001";

    @BeforeEach
    void prepareCodelistServiceMock() {
        when(codelistService.getCodelistEntries(anyString(), anyString())).thenReturn(ALL_ENTRIES);
        ALL_ENTRIES.forEach(codelistEntry ->
            when(
                codelistService.getCodelistEntry(any(), any(), Mockito.eq(codelistEntry.getCode()))
            ).thenReturn(codelistEntry)
        );
        when(
            codelistService.getCodelistEntry(
                any(),
                any(),
                Mockito.eq(NICHT_IN_DER_CODELIST.getCode())
            )
        ).thenReturn(
            CodelistEntry.empty(
                NICHT_IN_DER_CODELIST.getCode(),
                NICHT_IN_DER_CODELIST.getUri(),
                NICHT_IN_DER_CODELIST.getVersion(),
                ErrorTypeCodelist.CODE
            )
        );
    }

    @Test
    void testGetLifeSituationHierarchy() {
        assertAll(
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("2060990"),
                    equalTo(List.of("2060990", "2060000", "2000000"))
                ),
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("1234567"),
                    equalTo(List.of("1234567", "1230000", "1000000"))
                ),
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("1234500"),
                    equalTo(List.of("1234500", "1230000", "1000000"))
                ),
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("1230000"),
                    equalTo(List.of("1230000", "1000000"))
                ),
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("1000000"),
                    equalTo(List.of("1000000"))
                ),
            () ->
                assertThat(
                    LeikaService.getLifeSituationCodeHierarchy("1020400"),
                    equalTo(List.of("1020400", "1020000", "1000000"))
                ),
            () ->
                assertThrows(IllegalArgumentException.class, () ->
                    LeikaService.getLifeSituationCodeHierarchy("")
                ),
            () ->
                assertThrows(IllegalArgumentException.class, () ->
                    LeikaService.getLifeSituationCodeHierarchy("123456")
                ),
            () ->
                assertThrows(IllegalArgumentException.class, () ->
                    LeikaService.getLifeSituationCodeHierarchy("hello!!")
                )
        );
    }

    /**
     * Der einfachste Fall: Im Leistungskatalog ist eine Leistung, die der LL NACH_DER_GEBURT zugeordnet ist.
     * Dann soll ein LeikaEntry entstehen, der 3 Einträge in seinem Pfad hat, in der Reihenfolge NACH_DER_GEBURT, GEBURT, BUERGER.
     */
    @Test
    void testCreateLeikaEntriesEasiestCase() {
        var leikaService = new LeikaService(
            codelistService,
            leistungskatalogService,
            writeConnectorMigration,
            bulkWriteConnector,
            elasticReadConnector,
            indexConfig,
            null,
            messageSource
        );
        var leistungNachDerGeburt = createLeistung("99027002000000", NACH_DER_GEBURT);
        var leistungskatalog = Stream.of(leistungNachDerGeburt).map(GenericLeistung::new).toList();
        var leikaEntries = leikaService.createLeikaEntries(leistungskatalog);
        assertThat(leikaEntries, hasSize(1));
        var leikaEntry = leikaEntries.getFirst();
        assertThat(leikaEntry.getId(), is("99027002000000"));
        var codelistKeys = leikaEntry.getCodelistKeys();
        assertThat(codelistKeys.size(), is(1));
        var pathOfEntries = codelistKeys.iterator().next();
        assertThat(pathOfEntries.size(), is(3));
        assertPathOfKeys(pathOfEntries, NACH_DER_GEBURT, GEBURT, BUERGER);
    }

    /**
     * Hier legen wir im Leistungskatalog zwei Einträge an. Einen der mit NACH_DER_GEBURT verknüft ist und einen der
     * mit GEBURT und PFLEGE verknüpft ist.
     * Dann sollen zwei LeikaEntries mit den entsprechenden Pfaden entstehen, wobei für einen zwei Pfade vorhanden sind:
     * [GEBURT, BUERGER] und [PFLEGE, GESUNDHEIT_UND_VORSORGE, BUERGER].
     */
    @Test
    void testCreateLeikaEntriesTwoLeistungen() {
        var leikaService = new LeikaService(
            codelistService,
            leistungskatalogService,
            writeConnectorMigration,
            bulkWriteConnector,
            elasticReadConnector,
            indexConfig,
            null,
            messageSource
        );
        Leistung leistungNachDerGeburt = createLeistung("99027002000000", NACH_DER_GEBURT);
        Leistung leistungMulti = createLeistung("99027002111111", GEBURT, PFLEGE);
        List<GenericLeistung> leistungskatalog = Stream.of(leistungNachDerGeburt, leistungMulti)
            .map(GenericLeistung::new)
            .toList();
        var leikaEntries = leikaService.createLeikaEntries(leistungskatalog);
        assertThat(leikaEntries, hasSize(2));

        var leikaEntryNachDerGeburt = leikaEntries
            .stream()
            .filter(le -> le.getId().equals("99027002000000"))
            .findFirst()
            .orElse(null);
        assertThat(leikaEntryNachDerGeburt, notNullValue());
        var codelistEntriesNachDerGeburt = leikaEntryNachDerGeburt.getCodelistKeys();
        assertThat(codelistEntriesNachDerGeburt.size(), is(1));
        var pathOfEntries1 = codelistEntriesNachDerGeburt.iterator().next();
        assertPathOfKeys(pathOfEntries1, NACH_DER_GEBURT, GEBURT, BUERGER);

        var leikaEntryMulti = leikaEntries
            .stream()
            .filter(le -> le.getId().equals("99027002111111"))
            .findFirst()
            .orElse(null);
        assertThat(leikaEntryMulti, notNullValue());
        var codelistEntriesMulti = leikaEntryMulti.getCodelistKeys();
        assertThat(codelistEntriesMulti.size(), is(2));
        var pathOfEntries2 = codelistEntriesMulti
            .stream()
            .filter(cle -> cle.size() == 2)
            .findFirst()
            .orElse(null);
        assertPathOfKeys(pathOfEntries2, GEBURT, BUERGER);
        var pathOfEntries3 = codelistEntriesMulti
            .stream()
            .filter(cle -> cle.size() == 3)
            .findFirst()
            .orElse(null);
        assertPathOfKeys(pathOfEntries3, PFLEGE, GESUNDHEIT_UND_VORSORGE, BUERGER);
    }

    /**
     * Hier legen wir im Leistungskatalog einen Eintrag mit einem Code an der nicht in der Codeliste ist.
     * Das sollte einen leeren LeikaEntry ergeben.
     */
    @Test
    void testCreateLeikaEntryWithInvalidCodelistAssignment() {
        when(codelistService.getCodelistEntries(anyString(), anyString())).thenReturn(ALL_ENTRIES);
        var leikaService = new LeikaService(
            codelistService,
            leistungskatalogService,
            writeConnectorMigration,
            bulkWriteConnector,
            elasticReadConnector,
            indexConfig,
            null,
            messageSource
        );
        var leistung = createLeistung("99027002000077", NICHT_IN_DER_CODELIST);
        var leistungskatalog = Stream.of(leistung).map(GenericLeistung::new).toList();
        var leikaEntries = leikaService.createLeikaEntries(leistungskatalog);
        assertThat(leikaEntries, hasSize(1));
        var leikaEntry = leikaEntries.getFirst();
        var codelistEntries = leikaEntry.getCodelistKeys();
        assertThat(codelistEntries.size(), is(0));
    }

    private void assertPathOfKeys(List<CodelistKey> path, CodelistEntry... expectedEntries) {
        var expectedCodePath = Arrays.stream(expectedEntries).map(CodelistEntry::getCode).toList();
        assertThat(path.stream().map(CodelistKey::code).toList(), equalTo(expectedCodePath));
    }

    private static CodelistKey createCodelistKey(String code) {
        return new CodelistKey(PV_LAGEN_URI, PV_LAGEN_VERION, code);
    }

    private de.xoev.xzufi.v2_2_0.Leistung createLeistung(String leikaId, CodelistEntry... entries) {
        var leistung = new de.xoev.xzufi.v2_2_0.Leistung();
        var id = new Identifikator();
        id.setValue("1");
        id.setSchemeAgencyID("L10001");
        var idSekundaer = new Identifikator();
        idSekundaer.setValue(leikaId);
        leistung.setId(id);
        leistung.getIdSekundaer().add(idSekundaer);
        Arrays.stream(entries).forEach(entry -> {
            var klassifizierung = new CodeKlassifizierungBenutzerdefiniert();
            klassifizierung.setListURI(PV_LAGEN_URI);
            klassifizierung.setListVersionID(PV_LAGEN_VERION);
            klassifizierung.setCode(entry.getCode());
            leistung.getKlassifizierung().add(klassifizierung);
        });
        return leistung;
    }

    /**
     * Eine Leistung mit drei Leika-Keys anlegen. Den Katalog so vorbereiten dass die Leika-Keys auf folgende Lebenslagen-Pfade abgebildet
     * werden:
     * Lk1             ->  {(L1,L11,L111)}
     * Lk2             ->  {(L1,L12,L121)}
     * Lk3             ->  {(L2,L21,L211)}
     * <p>
     * Die Leistung muss danach diesen Lebenslagen-Baum haben:
     * <p>
     *         L1                   L2
     *        /    \                |
     *     L11     L12             L21
     *      |       |               |
     *     L111    L121            L211
     *
     */
    @Test
    void testRecomputeLifeSituationsWithThreeLeikaKeys() {
        var cl1 = createCodelistKey("1000000");
        var cl11 = createCodelistKey("1010000");
        var cl12 = createCodelistKey("1020000");
        var cl111 = createCodelistKey("1010100");
        var cl121 = createCodelistKey("1020100");
        var cl2 = createCodelistKey("2000000");
        var cl21 = createCodelistKey("2010000");
        var cl211 = createCodelistKey("2010100");

        var lk1 = new LeikaEntry("lk1", Set.of(List.of(cl111, cl11, cl1)));
        var lk2 = new LeikaEntry("lk2", Set.of(List.of(cl121, cl12, cl1)));
        var lk3 = new LeikaEntry("lk3", Set.of(List.of(cl211, cl21, cl2)));

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId("lab1");
        lb.setLeikakeys(Set.of(lk1.getId(), lk2.getId(), lk3.getId()));

        var leistungskatalog = new Leistungskatalog(List.of(lk1, lk2, lk3));

        // Hier benutzen wir einen Spy, um das Verhalten der Methode getLeistungskatalog so zu verändern, dass der gerade
        // aufgebaute Katalog geliefert wird. Wir wollen hier ja nicht die Entstehung des Katalogs aus den Downloads testen, sondern dessen
        // Weiterverarbeitung zu dem Lebenslagen-Baum.
        var leikaService = Mockito.spy(
            new LeikaService(
                codelistService,
                leistungskatalogService,
                writeConnectorMigration,
                bulkWriteConnector,
                elasticReadConnector,
                indexConfig,
                null,
                messageSource
            )
        );
        Mockito.doReturn(leistungskatalog).when(leikaService).getLeistungskatalog();

        Leistung emptyLb = new Leistung();
        Identifikator id = new Identifikator();
        id.setValue("1");
        id.setSchemeAgencyID("L10001");
        emptyLb.setId(id);

        leikaService.computeLifeSituations(lb, new GenericLeistung(emptyLb));

        var level1LifeSits = lb.getLifeSituations();
        assertThat(level1LifeSits.size(), is(2));
        assertLifeSituationCodes(level1LifeSits, "1000000", "2000000");
        var lifeSit1 = assertLifeSituationPresent(level1LifeSits, "1000000");
        var lifeSit1Children = lifeSit1.getChildren();
        assertLifeSituationCodes(lifeSit1Children, "1010000", "1020000");
        var lifeSit11 = assertLifeSituationPresent(lifeSit1Children, "1010000");
        var lifeSit11Children = lifeSit11.getChildren();
        assertLifeSituationCodes(lifeSit11Children, "1010100");

        var lifeSit2 = assertLifeSituationPresent(level1LifeSits, "2000000");
        var lifeSit2Children = lifeSit2.getChildren();
        assertLifeSituationCodes(lifeSit2Children, "2010000");
        var lifeSit21 = assertLifeSituationPresent(lifeSit2Children, "2010000");
        var lifeSit21Children = lifeSit21.getChildren();
        assertLifeSituationCodes(lifeSit21Children, "2010100");
    }

    /**
     * Eine Leistung mit einem Leika-Schlüssel anlegen, der nicht im Katalog ist. Dann sollen die Klassifikationen der Leistung
     * zur Bildung der Lebenslagen herangezogen werden.
     */
    @Test
    void testRecomputeLifeSituationsWithKlassifikationAsFallback() {
        var emptyLeistungskatalog = new Leistungskatalog(emptyList());

        // Hier benutzen wir einen Spy, um das Verhalten der Methode getLeistungskatalog so zu verändern, dass der gerade
        // aufgebaute Katalog geliefert wird. Wir wollen hier ja nicht die Entstehung des Katalogs aus den Downloads testen, sondern dessen
        // Weiterverarbeitung zu dem Lebenslagen-Baum.
        var leikaService = Mockito.spy(
            new LeikaService(
                codelistService,
                leistungskatalogService,
                writeConnectorMigration,
                bulkWriteConnector,
                elasticReadConnector,
                indexConfig,
                null,
                messageSource
            )
        );
        Mockito.doReturn(emptyLeistungskatalog).when(leikaService).getLeistungskatalog();

        // Eine LB anlegen, die eine Klassifizierung mit Code NACH_DER_GEBURT hat:
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId("lab1");
        lb.setLeikakeys(emptySet());
        CodeKlassifizierungBenutzerdefiniert codeKlassNachDerGeburt =
            new CodeKlassifizierungBenutzerdefiniert();
        codeKlassNachDerGeburt.setCode(NACH_DER_GEBURT.getCode());
        codeKlassNachDerGeburt.setListURI(NACH_DER_GEBURT.getUri());
        codeKlassNachDerGeburt.setListVersionID(NACH_DER_GEBURT.getVersion());
        CodeKlassifizierungBenutzerdefiniert codeKlassUnbekannt =
            new CodeKlassifizierungBenutzerdefiniert();
        codeKlassUnbekannt.setCode(NICHT_IN_DER_CODELIST.getCode());
        codeKlassUnbekannt.setListURI(NICHT_IN_DER_CODELIST.getUri());
        codeKlassUnbekannt.setListVersionID(NICHT_IN_DER_CODELIST.getVersion());
        Leistung leistung = new Leistung();
        leistung.getKlassifizierung().add(codeKlassNachDerGeburt);
        leistung.getKlassifizierung().add(codeKlassUnbekannt);
        Identifikator id = new Identifikator();
        id.setValue("1");
        id.setSchemeAgencyID("L10001");
        leistung.setId(id);

        when(codelistService.getCodelistEntries(anyString(), anyString())).thenReturn(ALL_ENTRIES);

        leikaService.computeLifeSituations(lb, new GenericLeistung(leistung));

        var level1LifeSits = lb.getLifeSituations();
        assertThat(level1LifeSits.size(), is(1));
        assertLifeSituationCodes(level1LifeSits, BUERGER.getCode());
        var lifeSit1 = assertLifeSituationPresent(level1LifeSits, BUERGER.getCode());
        var lifeSit1Children = lifeSit1.getChildren();
        assertLifeSituationCodes(lifeSit1Children, GEBURT.getCode());
        var lifeSit11 = assertLifeSituationPresent(lifeSit1Children, GEBURT.getCode());
        var lifeSit11Children = lifeSit11.getChildren();
        assertLifeSituationCodes(lifeSit11Children, NACH_DER_GEBURT.getCode());
    }

    /**
     * Eine Leistung mit zwei Leika-Keys anlegen. Den Katalog so vorbereiten dass die Leika-Keys auf folgende Lebenslagen-Pfade abgebildet
     * werden:
     * AusbildungsfoerdungBewilligung             ->  {(Bürger,Ausbildung,Studium)}
     * AusbildungsfoerdungBewilligungFuerSchueler ->  {(Bürger,Ausbildung,Studium), (Bürger,Ausbildung,Schule)}
     * <p>
     * Die Leistung muss danach diesen Lebenslagen-Baum haben:
     * <p>
     * Bürger
     *   |
     * Ausbildung
     *   |      |
     * Schule   Studium
     */
    @Test
    void testRecomputeLifeSituationsWithTwoLeikaKeys() {
        var clBuerger = createCodelistKey("1000000");
        var clAusbildung = createCodelistKey("1030000");
        var clSchule = createCodelistKey("1030100");
        var clStudium = createCodelistKey("1030300");

        var leAusbildungsfoerdungBewilligung = new LeikaEntry(
            "99022001017000",
            Set.of(List.of(clStudium, clAusbildung, clBuerger))
        );
        var leAusbildungsfoerdungBewilligungFuerSchueler = new LeikaEntry(
            "99022001017002",
            Set.of(
                List.of(clStudium, clAusbildung, clBuerger),
                List.of(clSchule, clAusbildung, clBuerger)
            )
        );

        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setId("lab1");
        lb.setLeikakeys(
            Set.of(
                leAusbildungsfoerdungBewilligung.getId(),
                leAusbildungsfoerdungBewilligungFuerSchueler.getId()
            )
        );

        var leistungskatalog = new Leistungskatalog(
            List.of(leAusbildungsfoerdungBewilligung, leAusbildungsfoerdungBewilligungFuerSchueler)
        );

        // Hier benutzen wir einen Spy, um das Verhalten der Methode getLeistungskatalog so zu verändern, dass der gerade
        // aufgebaute Katalog geliefert wird. Wir wollen hier ja nicht die Entstehung des Katalogs aus den Downloads testen, sondern dessen
        // Weiterverarbeitung zu dem Lebenslagen-Baum.
        var leikaService = Mockito.spy(
            new LeikaService(
                codelistService,
                leistungskatalogService,
                writeConnectorMigration,
                bulkWriteConnector,
                elasticReadConnector,
                indexConfig,
                null,
                messageSource
            )
        );
        Mockito.doReturn(leistungskatalog).when(leikaService).getLeistungskatalog();

        Leistung emptyLb = new Leistung();
        Identifikator id = new Identifikator();
        id.setValue("1");
        id.setSchemeAgencyID("L10001");
        emptyLb.setId(id);

        leikaService.computeLifeSituations(lb, new GenericLeistung(emptyLb));

        var level1LifeSits = lb.getLifeSituations();
        assertThat(level1LifeSits.size(), is(1));
        var level1LifeSit = level1LifeSits.iterator().next();
        assertThat(level1LifeSit.getCode(), is(clBuerger.code()));
        var level2LifeSits = level1LifeSit.getChildren();
        assertThat(level2LifeSits.size(), is(1));
        var level2LifeSit = level2LifeSits.iterator().next();
        assertThat(level2LifeSit.getCode(), equalTo(clAusbildung.code()));
        var level3LifeSits = level2LifeSit.getChildren();
        assertThat(level3LifeSits.size(), equalTo(2));
        assertLifeSituationCodes(level3LifeSits, clSchule.code(), clStudium.code());
        var lbLifeSituationCodes = lb.getLifeSituationCodes();
        // lbLifeSituationCodes darf nur die direkt der lb zugeordneten Codes enthalten, nicht die Ober-Ebenen, siehe PV-9464
        assertThat(lbLifeSituationCodes, equalTo(Set.of(clSchule.code(), clStudium.code())));
    }

    private void assertLifeSituationCodes(
        Collection<LifeSituation> level1LifeSits,
        String... codes
    ) {
        assertThat(
            level1LifeSits.stream().map(LifeSituation::getCode).collect(toSet()),
            equalTo(new HashSet<>(Arrays.asList(codes)))
        );
    }

    private LifeSituation assertLifeSituationPresent(
        Collection<LifeSituation> lifeSits,
        String code
    ) {
        var lifeSit = lifeSits
            .stream()
            .filter(l -> l.getCode().equals(code))
            .findFirst()
            .orElse(null);
        assertThat("Erwartete Lebenslage %s nicht in Menge %s enthalten", lifeSit, notNullValue());
        return lifeSit;
    }

    private static CodelistEntry createCodelistEntry(String code) {
        var entry = new CodelistEntry();
        entry.setCode(code);
        entry.setUri(PV_LAGEN_URI);
        entry.setVersion(PV_LAGEN_VERION);
        return entry;
    }
}
