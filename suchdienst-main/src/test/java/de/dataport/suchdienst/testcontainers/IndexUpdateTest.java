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
package de.dataport.suchdienst.testcontainers;

import static de.dataport.suchdienst.elasticsearch.api.IndexNames.ARS;
import static de.dataport.suchdienst.util.Bundeslaender.BUND;
import static de.dataport.suchdienst.util.Bundeslaender.SCHLESWIG_HOLSTEIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOptionsBuilders;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import com.google.common.collect.Lists;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22FormularGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22LeistungsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22OnlinedienstGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22OrganisationseinheitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22SpezialisierungsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import de.dataport.suchdienst.SuchdienstInitializer;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.IndexRefreshMgr;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.elasticsearch.evolution.DefaultDescription;
import de.dataport.suchdienst.elasticsearch.evolution.IndexMigrationDescription;
import de.dataport.suchdienst.elasticsearch.evolution.IndexUpdater;
import de.dataport.suchdienst.elasticsearch.evolution.UpdateConfiguration;
import de.dataport.suchdienst.elasticsearch.model.UpdateDescription;
import de.dataport.suchdienst.elasticsearch.model.UpdateStatus;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.service.ArsService;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.LbImportService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.indexer.service.OdImportService;
import de.dataport.suchdienst.indexer.service.OeImportService;
import de.dataport.suchdienst.indexer.service.RelationImportService;
import de.dataport.suchdienst.indexer.service.SpImportService;
import de.dataport.suchdienst.indexer.service.VerwaltungsleistungImportService;
import de.dataport.suchdienst.indexer.service.XzufiFetchService;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionArs1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionCodeList1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionLeistungsbeschreibungen1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionLeistungsbeschreibungen2;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionLeistungsbeschreibungen3;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionOnlinedienste1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionOrganisationseinheiten1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionSpezialisierungen1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionVerwaltungsleistung1;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionVerwaltungsleistung3;
import de.dataport.suchdienst.testcontainers.description.TestIndexDescriptionZustaendigkeiten1;
import de.dataport.suchdienst.testcontainers.description.migration.TestIndexDeleteLeistungen;
import de.dataport.suchdienst.testcontainers.description.migration.TestIndexMigrationLeistungen;
import de.dataport.suchdienst.testcontainers.description.migration.TestIndexMigrationLeistungen1_3;
import de.dataport.suchdienst.testcontainers.description.migration.TestIndexMigrationLeistungen2_3;
import de.dataport.suchdienst.util.model.TransaktionsPosition;
import de.xoev.xzufi.v2_2_0.Leistung;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_2_0.SchreibeObjekt;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Slf4j
@MockitoBeans(
    value = {
        // Keine Standard Initialisierung, wir kümmern uns im Test um alles!
        @MockitoBean(types = SuchdienstInitializer.class),
        // Kein Leistungskatalog Import
        @MockitoBean(types = LeikaService.class),
    }
)
class IndexUpdateTest extends AbstractElasticsearchContainerTest {

    @Autowired
    private LbImportService lbImportService;

    @Autowired
    private OeImportService oeImportService;

    @Autowired
    private OdImportService odImportService;

    @Autowired
    private RelationImportService relationImportService;

    @Autowired
    private SpImportService spImportService;

    @Autowired
    private IndexRefreshMgr indexRefreshMgr;

    @Autowired
    private VerwaltungsleistungImportService verwaltungsleistungImportService;

    @Autowired
    private SudTaskMgr sudTaskMgr;

    @Autowired
    private XzufiFetchService xzufiFetchService;

    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private IndexUpdater indexUpdater;

    @Autowired
    private CodelistService codelistService;

    @Autowired
    private ArsService arsService;

    @MockitoBean
    UpdateConfiguration updateConfiguration;

    @Autowired
    private ElasticReadConnector readConnector;

    @Autowired
    private ElasticPublicSearchConnector publicSearchConnector;

    TestIndexDescriptionArs1 indexDescriptionArs1 = Mockito.spy(new TestIndexDescriptionArs1());
    TestIndexDescriptionLeistungsbeschreibungen1 indexDescriptionLeistungsbeschreibungen1 =
        Mockito.spy(new TestIndexDescriptionLeistungsbeschreibungen1());

    TestIndexDescriptionLeistungsbeschreibungen2 indexDescriptionLeistungsbeschreibungen2 =
        Mockito.spy(new TestIndexDescriptionLeistungsbeschreibungen2());

    TestIndexDescriptionLeistungsbeschreibungen3 indexDescriptionLeistungsbeschreibungen3 =
        Mockito.spy(new TestIndexDescriptionLeistungsbeschreibungen3());
    TestIndexDescriptionOrganisationseinheiten1 indexDescriptionOrganisationseinheiten1 =
        Mockito.spy(new TestIndexDescriptionOrganisationseinheiten1());
    TestIndexDescriptionZustaendigkeiten1 indexDescriptionZustaendigkeiten1 = Mockito.spy(
        new TestIndexDescriptionZustaendigkeiten1()
    );
    TestIndexDescriptionVerwaltungsleistung1 indexDescriptionVerwaltungsleistungSH1 = Mockito.spy(
        new TestIndexDescriptionVerwaltungsleistung1("01")
    );

    TestIndexDescriptionVerwaltungsleistung3 indexDescriptionVerwaltungsleistungSH3 = Mockito.spy(
        new TestIndexDescriptionVerwaltungsleistung3("01")
    );

    TestIndexDescriptionOnlinedienste1 indexDescriptionOnlinedienste1 = Mockito.spy(
        new TestIndexDescriptionOnlinedienste1()
    );
    TestIndexDescriptionSpezialisierungen1 indexDescriptionSpezialisierungen1 = Mockito.spy(
        new TestIndexDescriptionSpezialisierungen1()
    );
    TestIndexDescriptionCodeList1 indexDescriptionCodeList1 = Mockito.spy(
        new TestIndexDescriptionCodeList1()
    );

    // Migration
    TestIndexMigrationLeistungen indexMigrationLeistungen = Mockito.spy(
        new TestIndexMigrationLeistungen()
    );
    TestIndexMigrationLeistungen1_3 indexMigrationLeistungen13 = Mockito.spy(
        new TestIndexMigrationLeistungen1_3()
    );
    TestIndexMigrationLeistungen2_3 indexMigrationLeistungen23 = Mockito.spy(
        new TestIndexMigrationLeistungen2_3()
    );

    // Delete Leistungen1
    TestIndexDeleteLeistungen indexDeleteLeistungen = Mockito.spy(new TestIndexDeleteLeistungen());

    private static List<DefaultDescription> defaultDescriptionList = new ArrayList<>();

    /**
     * <b>Testziel</b>:
     * <li>Es soll geprüft werden, dass nach einer Indexmigration die erwarteten Indizes angelegt, UpdateHistory Einträge erzeugt bzw. gelöscht und Daten migriert wurden</li>
     */

    @BeforeEach
    void setup() throws IOException {
        deleteAllIndices();
        disableDynamicMappingThoughIndexTemplate();
    }

    /**
     * <b>Testszenario</b>:
     * <li>leere Datenbank</li>
     * <li>keine Migration oder Löschklasse wird verwendet</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt.</li>
     */
    @Test
    void emptyDBnoMigrationStepsTest() {
        // ******** Setup ***********
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen1,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            Collections.emptyList()
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Migration ***********
        indexUpdater.updateElasticSearch();

        //******** Validation ***********
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );
        validateExpectedIndices(defaultDescriptionList);
        HitsMetadata<UpdateDescription> hits = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundUpdateHistoryEntries = hits
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundComparableEntries = createFoundHistoryEntries(
            foundUpdateHistoryEntries
        );
        List<ComparableUpdateHistoryEntry> expectedComparableEntries = createExpectedHistoryEntries(
            defaultDescriptionList
        );
        assertThat(foundComparableEntries, hasSize(expectedComparableEntries.size()));
        assertThat(foundComparableEntries, containsInAnyOrder(expectedComparableEntries.toArray()));

        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        assertThat(hitsStarted.hits().size(), is(0));
        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));
        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        assertThat(hitsDeleted.hits().size(), is(0));
        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        assertThat(hitsFinished.hits().size(), is(0));
    }

    /**
     * <b>Testszenario</b>:
     * <li>leere Datenbank</li>
     * <li>Klassen für Indexmigration - Leistungsindex von Version 1 zu 3 bzw. 2 zu 3 sind angelegt</li>
     * <li>Klassen für Löschung von Leistungsindex Version 1 ist angelegt</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList - für Leistungsindex Version 3</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt. Created Einträge für defaultDescriptionList Elemente</li>
     * <li>Started UpdateHistory Eintrag für Löschklasse</li>
     * <li>Deleted UpdateHistory Eintrag für Löschklasse</li>
     * <li>Keine UpdateHistory Einträge für Migrationsklassen</li>
     */
    @Test
    void emptyDBMigrationStepsTest() {
        // ******** Setup ***********
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen3,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            List.of(
                indexMigrationLeistungen,
                indexMigrationLeistungen13,
                indexMigrationLeistungen23
            )
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            List.of(indexDeleteLeistungen)
        );

        // ******** Trigger Migration ***********
        indexUpdater.updateElasticSearch();

        //******** Validation ***********
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );
        validateExpectedIndices(defaultDescriptionList);
        HitsMetadata<UpdateDescription> hits = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundUpdateHistoryEntries = hits
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundComparableEntries = createFoundHistoryEntries(
            foundUpdateHistoryEntries
        );
        List<ComparableUpdateHistoryEntry> expectedComparableEntries = createExpectedHistoryEntries(
            defaultDescriptionList
        );
        assertThat(foundComparableEntries, hasSize(expectedComparableEntries.size()));
        assertThat(foundComparableEntries, containsInAnyOrder(expectedComparableEntries.toArray()));

        // obwohl mit einer leeren DB gestartet wird, wird für Index Löschschritt ein STARTED Eintrag erzeugt
        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        List<UpdateDescription> foundStarteddUpdateHistoryEntries = hitsStarted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundStartedComparableEntries =
            createFoundHistoryEntries(foundStarteddUpdateHistoryEntries);
        ComparableUpdateHistoryEntry deleteEntry = new ComparableUpdateHistoryEntry();
        deleteEntry.setIndexName(indexDeleteLeistungen.getAbstractIndexName().getIndexName());
        deleteEntry.setVersion(indexDeleteLeistungen.getVersion());
        deleteEntry.setDescription(indexDeleteLeistungen.getDescription());
        deleteEntry.setStatus(UpdateStatus.STARTED.name());
        // erwarte STARTED für genau einen Delete
        assertThat(hitsStarted.hits().size(), is(1));
        assertThat(
            foundStartedComparableEntries,
            containsInAnyOrder(List.of(deleteEntry).toArray())
        );

        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));
        // obwohl mit einer leeren DB gestartet wird, wird für Index Löschschritt ein DELETED Eintrag erzeugt
        deleteEntry.setStatus(UpdateStatus.DELETED.name());
        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        List<UpdateDescription> foundDeletedUpdateHistoryEntries = hitsDeleted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundDeletedComparableEntries =
            createFoundHistoryEntries(foundDeletedUpdateHistoryEntries);
        assertThat(hitsDeleted.hits().size(), is(1));
        assertThat(
            foundDeletedComparableEntries,
            containsInAnyOrder(List.of(deleteEntry).toArray())
        );

        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        assertThat(hitsFinished.hits().size(), is(0));
    }

    /**
     * <b>Testszenario</b>:
     * <li>Basis ist eine Datenbank mit Indizes in Version 1</li>
     * <li>BOs LB, OE, OD, SP, ZS wurden importiert und das Erzeugen einer VL angestoßen</li>
     * <li>Für die Indexmigration dient als Basis eine Liste von Indizes - Unterschied zum initialen Bestand <b>Leistungsindex ist jetzt Version 3</b></li>
     * <li>Klassen für Indexmigration - Leistungsindex von Version 1 zu 3 bzw. 2 zu 3 sind angelegt</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList - für Leistungsindex Version 3</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt. </li>
     * <ul>
     * <li>Created Einträge für defaultDescriptionList Elemente</li>
     * <li>Started UpdateHistory Eintrag für  Migration 1 - 3</li>
     * <li>Finished UpdateHistory Eintrag für Migration 1 - 3</li>
     * </ul>
     * <li>Leistung ist im neuen Index suchdienst_leistungsbeschreibungen3 abrufbar bzw. wurde migriert</li>
     */

    @Test
    void filledDBMigrationStep1To3Test() {
        // ******** Setup ***********
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen1,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            Collections.emptyList()
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Initial Index Creation ***********
        indexUpdater.updateElasticSearch();

        // ******** Create and Import Testdata ***********
        prepareImport();
        XzufiFile createdFile = createAndImportBOs("filledDBMigrationStep1To3Test");
        String lbFullId = createdFile.getLeistungsbeschreibungen().getFirst().getFullId();
        generateVerwaltungsleistungen(lbFullId);

        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen3,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            List.of(indexMigrationLeistungen13, indexMigrationLeistungen23)
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Index Migration ***********
        indexUpdater.updateElasticSearch();

        //******** Validation ***********
        validateExpectedIndices(defaultDescriptionList);
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );

        HitsMetadata<UpdateDescription> hitsCreated = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundCreatedUpdateHistoryEntries = hitsCreated
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundCreatedComparableEntries =
            createFoundHistoryEntries(foundCreatedUpdateHistoryEntries);
        // erwartete CREATED Einträge ⇒ aktuelle DescriptionList + initialer Leistungsindex in Version 1
        List<DefaultDescription> expectedDescriptions = new ArrayList<>(defaultDescriptionList);
        expectedDescriptions.add(indexDescriptionLeistungsbeschreibungen1);
        List<ComparableUpdateHistoryEntry> expectedCreatedComparableEntries =
            createExpectedHistoryEntries(expectedDescriptions);
        // Validiere alle Created History Entries
        assertThat(foundCreatedComparableEntries, hasSize(expectedCreatedComparableEntries.size()));
        assertThat(
            foundCreatedComparableEntries,
            containsInAnyOrder(expectedCreatedComparableEntries.toArray())
        );

        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        List<UpdateDescription> foundStarteddUpdateHistoryEntries = hitsStarted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundStartedComparableEntries =
            createFoundHistoryEntries(foundStarteddUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedMigrationDescriptions = new ArrayList<>();
        expectedMigrationDescriptions.add(indexMigrationLeistungen13);
        List<ComparableUpdateHistoryEntry> expectedStartedDescriptions =
            createExpectedHistoryEntriesMigration(
                expectedMigrationDescriptions,
                UpdateStatus.STARTED.name()
            );
        assertThat(hitsStarted.hits().size(), is(expectedStartedDescriptions.size()));
        assertThat(
            foundStartedComparableEntries,
            containsInAnyOrder(expectedStartedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));
        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        assertThat(hitsDeleted.hits().size(), is(0));

        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        List<UpdateDescription> foundFinishedUpdateHistoryEntries = hitsFinished
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundFinishedComparableEntries =
            createFoundHistoryEntries(foundFinishedUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedFinishedDescriptions = new ArrayList<>();
        expectedFinishedDescriptions.add(indexMigrationLeistungen13);
        List<ComparableUpdateHistoryEntry> expectedFinishedComparableEntries =
            createExpectedHistoryEntriesMigration(
                expectedFinishedDescriptions,
                UpdateStatus.FINISHED.name()
            );
        assertThat(hitsFinished.hits().size(), is(expectedFinishedComparableEntries.size()));
        assertThat(
            foundFinishedComparableEntries,
            containsInAnyOrder(expectedFinishedComparableEntries.toArray())
        );
        // validiere migrierten Index
        Optional<LeistungEntity> importedLB = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            createdFile.getLeistungsbeschreibungen().getFirst().getFullId(),
            LeistungEntity.class
        );
        assertTrue(
            importedLB.isPresent(),
            "Importierte Leistung konnte im migrierten Leistungsindex nicht gefunden werden"
        );
    }

    /**
     * <b>Testszenario</b>:
     * <li>Basis ist eine Datenbank mit Indizes in Version 1 - bis auf Leistungsindex in Version 2</li>
     * <li>BOs LB, OE, OD, SP, ZS wurden importiert und das Erzeugen einer VL angestoßen</li>
     * <li>Für die Indexmigration dient als Basis eine Liste von Indizes - Unterschied zum initialen Bestand <b>Leistungsindex ist jetzt Version 3</b></li>
     * <li>Klassen für Indexmigration - Leistungsindex von Version 1 zu 3 bzw. 2 zu 3 sind angelegt</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList - für Leistungsindex Version 3</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt. </li>
     * <ul>
     * <li>Created Einträge für defaultDescriptionList Elemente</li>
     * <li>Started UpdateHistory Eintrag für Migration 2 - 3</li>
     * <li>Finished UpdateHistory Eintrag für Migration 2 - 3</li>
     * </ul>
     * <li>Leistung ist im neuen Index suchdienst_leistungsbeschreibungen3 abrufbar bzw. wurde migriert</li>
     */

    @Test
    void filledDBMigrationStep2To3Test() {
        // ******** Setup ***********
        // Initial liegt Leistungsbeschreibung2 vor
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen2,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            Collections.emptyList()
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Initial Index Creation ***********
        indexUpdater.updateElasticSearch();

        // ******** Create and Import Testdata ***********
        prepareImport();
        XzufiFile createdFile = createAndImportBOs("filledDBMigrationStep2To3Test");
        String lbFullId = createdFile.getLeistungsbeschreibungen().getFirst().getFullId();
        generateVerwaltungsleistungen(lbFullId);

        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen3,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            List.of(indexMigrationLeistungen13, indexMigrationLeistungen23)
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Index Migration ***********
        indexUpdater.updateElasticSearch();

        //******** Validation ***********
        validateExpectedIndices(defaultDescriptionList);
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );
        // leistungsindex2 ist noch vorhanden
        assertTrue(
            readConnector.indexExists(indexDescriptionLeistungsbeschreibungen2.getDestIndex()),
            "Index:" +
                indexDescriptionLeistungsbeschreibungen2.getDestIndex() +
                " ist nicht vorhanden, obwohl kein delete definiert wurde"
        );

        HitsMetadata<UpdateDescription> hitsCreated = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundCreatedUpdateHistoryEntries = hitsCreated
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundCreatedComparableEntries =
            createFoundHistoryEntries(foundCreatedUpdateHistoryEntries);
        // erwartete CREATED Einträge ⇒ aktuelle DescriptionList + initialer Leistungsindex in Version 2
        List<DefaultDescription> expectedDescriptions = new ArrayList<>(defaultDescriptionList);
        expectedDescriptions.add(indexDescriptionLeistungsbeschreibungen2);
        List<ComparableUpdateHistoryEntry> expectedCreatedComparableEntries =
            createExpectedHistoryEntries(expectedDescriptions);
        // Validiere alle Created History Entries
        assertThat(foundCreatedComparableEntries, hasSize(expectedCreatedComparableEntries.size()));
        assertThat(
            foundCreatedComparableEntries,
            containsInAnyOrder(expectedCreatedComparableEntries.toArray())
        );

        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        List<UpdateDescription> foundStarteddUpdateHistoryEntries = hitsStarted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundStartedComparableEntries =
            createFoundHistoryEntries(foundStarteddUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedMigrationDescriptions = new ArrayList<>();
        expectedMigrationDescriptions.add(indexMigrationLeistungen23);
        List<ComparableUpdateHistoryEntry> expectedStartedDescriptions =
            createExpectedHistoryEntriesMigration(
                expectedMigrationDescriptions,
                UpdateStatus.STARTED.name()
            );
        assertThat(hitsStarted.hits().size(), is(expectedStartedDescriptions.size()));
        assertThat(
            foundStartedComparableEntries,
            containsInAnyOrder(expectedStartedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));
        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        assertThat(hitsDeleted.hits().size(), is(0));

        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        List<UpdateDescription> foundFinishedUpdateHistoryEntries = hitsFinished
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundFinishedComparableEntries =
            createFoundHistoryEntries(foundFinishedUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedFinishedDescriptions = new ArrayList<>();
        expectedFinishedDescriptions.add(indexMigrationLeistungen23);
        List<ComparableUpdateHistoryEntry> expectedFinishedComparableEntries =
            createExpectedHistoryEntriesMigration(
                expectedFinishedDescriptions,
                UpdateStatus.FINISHED.name()
            );
        assertThat(hitsFinished.hits().size(), is(expectedFinishedComparableEntries.size()));
        assertThat(
            foundFinishedComparableEntries,
            containsInAnyOrder(expectedFinishedComparableEntries.toArray())
        );
        // validiere migrierten Index
        Optional<LeistungEntity> importedLB = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            createdFile.getLeistungsbeschreibungen().getFirst().getFullId(),
            LeistungEntity.class
        );
        assertTrue(
            importedLB.isPresent(),
            "Importierte Leistung konnte im migrierten Leistungsindex nicht gefunden werden"
        );
    }

    /**
     * <b>Testszenario</b>:
     * <li>Basis ist eine Datenbank mit Indizes in Version 1</li>
     * <li>BOs LB, OE, OD, SP, ZS wurden importiert und das Erzeugen einer VL angestoßen</li>
     * <li>Für die Indexmigration dient als Basis eine Liste von Indizes - Unterschied zum initialen Bestand <b>Leistungsindex ist jetzt Version 3</b></li>
     * <li>Klassen für Indexmigration - Leistungsindex von Version 1 zu 3 bzw. 2 zu 3 sind angelegt</li>
     * <li>Klassen für das Löschen von Leistungsindex in Version ist angelegt</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList - für Leistungsindex Version 3</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt. </li>
     * <ul>
     * <li>Created Einträge für defaultDescriptionList Elemente</li>
     * <li>Started UpdateHistory Eintrag für Löschklasse und Migration 1 - 3</li>
     * <li>Deleted UpdateHistory Eintrag für Löschklasse</li>
     * <li>Finished UpdateHistory Eintrag für Migration 1 - 3</li>
     * </ul>
     * <li>Leistung ist im neuen Index suchdienst_leistungsbeschreibungen3 abrufbar bzw. wurde migriert</li>
     * <li>Leistungsindex in Version 1 wurde gelöscht</li>
     */
    @Test
    void filledDBMigrationStep1To3Delete1Test() {
        // ******** Setup ***********
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen1,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            Collections.emptyList()
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Initial Index Creation ***********
        indexUpdater.updateElasticSearch();

        // ******** Create and Import Testdata ***********
        prepareImport();
        XzufiFile createdFile = createAndImportBOs("filledDBMigrationStep1To3Test");
        String lbFullId = createdFile.getLeistungsbeschreibungen().getFirst().getFullId();
        generateVerwaltungsleistungen(lbFullId);

        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen3,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            List.of(indexMigrationLeistungen13, indexMigrationLeistungen23)
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            List.of(indexDeleteLeistungen)
        );

        // ******** Trigger Index Migration ***********
        indexUpdater.updateElasticSearch();

        //******** Validation ***********
        validateExpectedIndices(defaultDescriptionList);
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );
        // leistungsindex1 ist noch vorhanden
        assertFalse(
            readConnector.indexExists(indexDescriptionLeistungsbeschreibungen1.getDestIndex()),
            "Index:" +
                indexDescriptionLeistungsbeschreibungen1.getDestIndex() +
                " ist vorhanden, obwohl ein delete definiert wurde"
        );

        HitsMetadata<UpdateDescription> hitsCreated = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundCreatedUpdateHistoryEntries = hitsCreated
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundCreatedComparableEntries =
            createFoundHistoryEntries(foundCreatedUpdateHistoryEntries);
        // erwartete CREATED Einträge ⇒ aktuelle DescriptionList + initialer Leistungsindex in Version 1
        List<DefaultDescription> expectedDescriptions = new ArrayList<>(defaultDescriptionList);
        expectedDescriptions.add(indexDescriptionLeistungsbeschreibungen1);
        List<ComparableUpdateHistoryEntry> expectedCreatedComparableEntries =
            createExpectedHistoryEntries(expectedDescriptions);
        // Validiere alle Created History Entries
        assertThat(foundCreatedComparableEntries, hasSize(expectedCreatedComparableEntries.size()));
        assertThat(
            foundCreatedComparableEntries,
            containsInAnyOrder(expectedCreatedComparableEntries.toArray())
        );

        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        List<UpdateDescription> foundStarteddUpdateHistoryEntries = hitsStarted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundStartedComparableEntries =
            createFoundHistoryEntries(foundStarteddUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedMigrationDescriptions = new ArrayList<>();
        expectedMigrationDescriptions.add(indexMigrationLeistungen13);
        List<ComparableUpdateHistoryEntry> expectedStartedDescriptions =
            createExpectedHistoryEntriesMigration(
                expectedMigrationDescriptions,
                UpdateStatus.STARTED.name()
            );
        // für Deletion wird auch ein Started Eintrag erzeugt
        ComparableUpdateHistoryEntry deleteEntry = new ComparableUpdateHistoryEntry();
        deleteEntry.setIndexName(indexDeleteLeistungen.getAbstractIndexName().getIndexName());
        deleteEntry.setVersion(indexDeleteLeistungen.getVersion());
        deleteEntry.setDescription(indexDeleteLeistungen.getDescription());
        deleteEntry.setStatus(UpdateStatus.STARTED.name());
        expectedStartedDescriptions.add(deleteEntry);

        assertThat(hitsStarted.hits().size(), is(expectedStartedDescriptions.size()));
        assertThat(
            foundStartedComparableEntries,
            containsInAnyOrder(expectedStartedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));

        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        List<UpdateDescription> foundDeletedUpdateHistoryEntries = hitsDeleted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundDeletedComparableEntries =
            createFoundHistoryEntries(foundDeletedUpdateHistoryEntries);
        deleteEntry.setStatus(UpdateStatus.DELETED.name());
        List<ComparableUpdateHistoryEntry> expectedDeletedDescriptions = List.of(deleteEntry);
        assertThat(hitsDeleted.hits().size(), is(expectedDeletedDescriptions.size()));
        assertThat(
            foundDeletedComparableEntries,
            containsInAnyOrder(expectedDeletedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        List<UpdateDescription> foundFinishedUpdateHistoryEntries = hitsFinished
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundFinishedComparableEntries =
            createFoundHistoryEntries(foundFinishedUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedFinishedDescriptions = new ArrayList<>();
        expectedFinishedDescriptions.add(indexMigrationLeistungen13);
        List<ComparableUpdateHistoryEntry> expectedFinishedComparableEntries =
            createExpectedHistoryEntriesMigration(
                expectedFinishedDescriptions,
                UpdateStatus.FINISHED.name()
            );
        assertThat(hitsFinished.hits().size(), is(expectedFinishedComparableEntries.size()));
        assertThat(
            foundFinishedComparableEntries,
            containsInAnyOrder(expectedFinishedComparableEntries.toArray())
        );
        // validiere migrierten Index
        Optional<LeistungEntity> importedLB = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            createdFile.getLeistungsbeschreibungen().getFirst().getFullId(),
            LeistungEntity.class
        );
        assertTrue(
            importedLB.isPresent(),
            "Importierte Leistung konnte im migrierten Leistungsindex nicht gefunden werden"
        );
    }

    /**
     * <b>Testszenario</b>:
     * <li>Basis ist eine Datenbank mit Indizes in Version 1 - bis auf Leistungsindex in Version 2</li>
     * <li>BOs LB, OE, OD, SP, ZS wurden importiert und das Erzeugen einer VL angestoßen</li>
     * <li>weitere BOs LB, OE, OD, SP, ZS werden importiert <b>aber keine Erzeugung der VL angestoßen - liegt also nicht als VL vor</b></li>
     * <li>Für die Indexmigration dient als Basis eine Liste von Indizes - Unterschied zum initialen Bestand <b>Leistungsindex ist jetzt Version 3 und Verwaltungsindex in Version 2</b></li>
     * <li>Klassen für Indexmigration - Leistungsindex von Version 1 zu 3 bzw. 2 zu 3 sind angelegt</li>
     * <li>Klasse für das Löschen von Leistungsindex in Version 1 ist angelegt</li>
     * <li>Indexmigration wird angestoßen - updateElasticsearch</li>
     * <li>es wird eine definierte Anzahl Indizes angelegt - Basis ist defaultDescriptionList - für Leistungsindex Version 3 - für Verwaltungsindex 2</li>
     * <b>Prüfung</b>
     * <li>alle Indizes aus der Vorgabeliste werden angelegt</li>
     * <li>alle erwarteten UpdateHistory Einträge wurden angelegt. </li>
     * <ul>
     * <li>Created Einträge für defaultDescriptionList Elemente</li>
     * <li>Started UpdateHistory Eintrag für Löschklasse und Migration 2 - 3</li>
     * <li>Deleted UpdateHistory Eintrag für Löschklasse</li>
     * <li>Finished UpdateHistory Eintrag für Migration 2 - 3</li>
     * </ul>
     * <li>Beide Leistungen sind im neuen Index suchdienst_leistungsbeschreibungen3 abrufbar bzw. wurde migriert</li>
     * <li>Die Verwaltungsleistungen wurden migriert - beide Leistungen sind als Verwaltungsleistung abrufbar</li>
     */
    @Test
    void filledDBMigrationStep2o3Delete1VLUpdateTest() {
        // ******** Setup ***********
        sudTaskMgr.setMediators(List.of(verwaltungsleistungImportService));
        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen2,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH1,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            Collections.emptyList()
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            Collections.emptyList()
        );

        // ******** Trigger Initial Index Creation ***********
        indexUpdater.updateElasticSearch();

        // ******** Create and Import Testdata ***********
        prepareImport();
        XzufiFile createdFile = createAndImportBOs("filledDBMigrationStep2To3Delete1VLUpdateTest");
        String lbFullId = createdFile.getLeistungsbeschreibungen().getFirst().getFullId();
        generateVerwaltungsleistungen(lbFullId);
        //erzeuge Einträge, die nicht als Verwaltungsleistung importiert wurden und erst nach der IndexMigration verfügbar sein sollten
        XzufiFile createdFileNotImported = createAndImportBOs(
            "filledDBMigrationStep2To3Delete1VLUpdateTestVL"
        );

        defaultDescriptionList = List.of(
            indexDescriptionLeistungsbeschreibungen3,
            indexDescriptionOrganisationseinheiten1,
            indexDescriptionZustaendigkeiten1,
            indexDescriptionVerwaltungsleistungSH3,
            indexDescriptionOnlinedienste1,
            indexDescriptionSpezialisierungen1,
            indexDescriptionCodeList1,
            indexDescriptionArs1
        );
        Mockito.when(updateConfiguration.getDefaultUpdateDescriptions()).thenReturn(
            defaultDescriptionList
        );
        Mockito.when(updateConfiguration.getMigrationDescriptions()).thenReturn(
            List.of(indexMigrationLeistungen13, indexMigrationLeistungen23)
        );
        Mockito.when(updateConfiguration.getDeleteDescriptions()).thenReturn(
            List.of(indexDeleteLeistungen)
        );

        // ******** Trigger Index Migration ***********
        indexUpdater.updateElasticSearch();
        indexRefreshMgr.markAsDirty("suchdienst_verwaltungsleistungen_sh3*");
        indexRefreshMgr.ensureFresh("suchdienst_verwaltungsleistungen_sh3*");
        indexRefreshMgr.markAsDirty(IndexNames.LEISTUNGSBESCHREIBUNG);
        indexRefreshMgr.ensureFresh(IndexNames.LEISTUNGSBESCHREIBUNG);

        //******** Validation ***********
        validateExpectedIndices(defaultDescriptionList);
        assertTrue(
            readConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName()),
            "UPDATEHISTORY Index wurde nicht angelegt"
        );
        // leistungsindex1 ist noch vorhanden
        assertFalse(
            readConnector.indexExists(indexDescriptionLeistungsbeschreibungen1.getDestIndex()),
            "Index:" +
                indexDescriptionLeistungsbeschreibungen1.getDestIndex() +
                " ist vorhanden, obwohl er nicht definiert wurde"
        );

        HitsMetadata<UpdateDescription> hitsCreated = searchUpdateHistory(UpdateStatus.CREATED);
        List<UpdateDescription> foundCreatedUpdateHistoryEntries = hitsCreated
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundCreatedComparableEntries =
            createFoundHistoryEntries(foundCreatedUpdateHistoryEntries);
        // erwartete CREATED Einträge ⇒ aktuelle DescriptionList + initialer Leistungsindex in Version 2 + initialer Verwaltungsleistungsindex SH in Version 1
        List<DefaultDescription> expectedDescriptions = new ArrayList<>(defaultDescriptionList);
        expectedDescriptions.add(indexDescriptionLeistungsbeschreibungen2);
        expectedDescriptions.add(indexDescriptionVerwaltungsleistungSH1);
        List<ComparableUpdateHistoryEntry> expectedCreatedComparableEntries =
            createExpectedHistoryEntries(expectedDescriptions);
        // Validiere alle Created History Entries
        assertThat(foundCreatedComparableEntries, hasSize(expectedCreatedComparableEntries.size()));
        assertThat(
            foundCreatedComparableEntries,
            containsInAnyOrder(expectedCreatedComparableEntries.toArray())
        );

        HitsMetadata<UpdateDescription> hitsStarted = searchUpdateHistory(UpdateStatus.STARTED);
        List<UpdateDescription> foundStarteddUpdateHistoryEntries = hitsStarted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundStartedComparableEntries =
            createFoundHistoryEntries(foundStarteddUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedMigrationDescriptions = new ArrayList<>();
        expectedMigrationDescriptions.add(indexMigrationLeistungen23);
        List<ComparableUpdateHistoryEntry> expectedStartedDescriptions =
            createExpectedHistoryEntriesMigration(
                expectedMigrationDescriptions,
                UpdateStatus.STARTED.name()
            );

        // für Deletion wird auch ein Started Eintrag erzeugt
        ComparableUpdateHistoryEntry deleteEntry = new ComparableUpdateHistoryEntry();
        deleteEntry.setIndexName(indexDeleteLeistungen.getAbstractIndexName().getIndexName());
        deleteEntry.setVersion(indexDeleteLeistungen.getVersion());
        deleteEntry.setDescription(indexDeleteLeistungen.getDescription());
        deleteEntry.setStatus(UpdateStatus.STARTED.name());
        expectedStartedDescriptions.add(deleteEntry);

        assertThat(hitsStarted.hits().size(), is(expectedStartedDescriptions.size()));
        assertThat(
            foundStartedComparableEntries,
            containsInAnyOrder(expectedStartedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsWarn = searchUpdateHistory(UpdateStatus.WARN);
        assertThat(hitsWarn.hits().size(), is(0));

        HitsMetadata<UpdateDescription> hitsDeleted = searchUpdateHistory(UpdateStatus.DELETED);
        List<UpdateDescription> foundDeletedUpdateHistoryEntries = hitsDeleted
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundDeletedComparableEntries =
            createFoundHistoryEntries(foundDeletedUpdateHistoryEntries);
        deleteEntry.setStatus(UpdateStatus.DELETED.name());
        List<ComparableUpdateHistoryEntry> expectedDeletedDescriptions = List.of(deleteEntry);
        assertThat(hitsDeleted.hits().size(), is(expectedDeletedDescriptions.size()));
        assertThat(
            foundDeletedComparableEntries,
            containsInAnyOrder(expectedDeletedDescriptions.toArray())
        );

        HitsMetadata<UpdateDescription> hitsFinished = searchUpdateHistory(UpdateStatus.FINISHED);
        List<UpdateDescription> foundFinishedUpdateHistoryEntries = hitsFinished
            .hits()
            .stream()
            .map(Hit::source)
            .toList();
        List<ComparableUpdateHistoryEntry> foundFinishedComparableEntries =
            createFoundHistoryEntries(foundFinishedUpdateHistoryEntries);
        List<IndexMigrationDescription> expectedFinishedDescriptions = new ArrayList<>();
        expectedFinishedDescriptions.add(indexMigrationLeistungen23);
        List<ComparableUpdateHistoryEntry> expectedFinishedComparableEntries =
            createExpectedHistoryEntriesMigration(
                expectedFinishedDescriptions,
                UpdateStatus.FINISHED.name()
            );
        assertThat(hitsFinished.hits().size(), is(expectedFinishedComparableEntries.size()));
        assertThat(
            foundFinishedComparableEntries,
            containsInAnyOrder(expectedFinishedComparableEntries.toArray())
        );
        // validiere migrierten Index
        Optional<LeistungEntity> importedLB = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            createdFile.getLeistungsbeschreibungen().getFirst().getFullId(),
            LeistungEntity.class
        );
        assertTrue(
            importedLB.isPresent(),
            "Importierte Leistung konnte im migrierten Leistungsindex nicht gefunden werden"
        );
        // zweite Leistung sollte auch migriert werden sein
        Optional<LeistungEntity> importedLBVL = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            createdFileNotImported.getLeistungsbeschreibungen().getFirst().getFullId(),
            LeistungEntity.class
        );
        assertTrue(
            importedLBVL.isPresent(),
            "Importierte Leistung konnte im migrierten Leistungsindex nicht gefunden werden"
        );
        String lbFullIdVL = createdFileNotImported
            .getLeistungsbeschreibungen()
            .getFirst()
            .getFullId();
        String arsVL = createdFileNotImported
            .getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .getArss()
            .getFirst();
        Optional<Verwaltungsleistung> importedVL = publicSearchConnector.searchLbByIdAndArs(
            lbFullIdVL,
            arsVL,
            true,
            true,
            LocalDate.now()
        );
        assertTrue(
            importedVL.isPresent(),
            "nach Indexmigration wurde für die zweite importierte Leistung keine Verwaltungsleistung erzeugt"
        );
        // alte Verwaltungsleistung sollte nach Indexmigration auch noch abrufbar sein
        Optional<Verwaltungsleistung> importedOldVL = publicSearchConnector.searchLbByIdAndArs(
            lbFullId,
            arsVL,
            true,
            true,
            LocalDate.now()
        );
        assertTrue(
            importedOldVL.isPresent(),
            "nach Indexmigration ist die zuvor erzeugte erste Verwaltungsleistung nicht mehr abrufbar"
        );
    }

    private void prepareImport() {
        // Voraussetzung für den Import von Transferobjekten sind gefüllte ARS und Codelisten Indizes
        try {
            importArsList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        importStandardCodelists();
    }

    private XzufiFile createAndImportBOs(String name) {
        var shFedState = SCHLESWIG_HOLSTEIN.getArsFederalstate();
        BundeslandConfig bcSH = new BundeslandConfig(BundeslandConfig.Bundesland.SH);
        String schemeAgencySH = bcSH.getSchemeAgencyId();

        CodelistConfig22 codelistConfig = new CodelistConfig22();
        Xzufi22LeistungsGenerator lbGenerator = new Xzufi22LeistungsGenerator(codelistConfig);
        Xzufi22OnlinedienstGenerator odGenerator = new Xzufi22OnlinedienstGenerator(codelistConfig);
        Xzufi22SpezialisierungsGenerator spGenerator = new Xzufi22SpezialisierungsGenerator(
            codelistConfig
        );
        Xzufi22ZustaendigkeitsGenerator zsGenerator = new Xzufi22ZustaendigkeitsGenerator(
            codelistConfig
        );
        Xzufi22FormularGenerator foGenerator = new Xzufi22FormularGenerator(
            codelistConfig,
            zsGenerator
        );
        Xzufi22OrganisationseinheitsGenerator oeGenerator =
            new Xzufi22OrganisationseinheitsGenerator(codelistConfig, foGenerator);

        XzufiFile fileSH = XzufiFileCreator.newInstance(bcSH)
            .name(name + "SH")
            .lb(1)
            .oe(1)
            .od(1)
            .sp(1)
            .getMin();
        Leistungsbeschreibung lb = fileSH.getLeistungsbeschreibungen().getFirst();
        Leistung xzufiLeistung = lbGenerator.generateLeistung(lb);

        Organisationseinheit oe = fileSH.getOrganisationseinheiten().getFirst();
        OrganisationseinheitErweitert xzufiOE = oeGenerator.generateOrganisationseinheit(oe);

        Zustaendigkeit zsOE = fileSH.getZustaendigkeitenOrganisationseinheit().getFirst();
        String relationId =
            zsOE.getSchemeAgencyId() +
            ".ZSOE." +
            lb.getId() +
            "." +
            zsOE.getParentId() +
            "." +
            zsOE.getArss().getFirst();

        List<SchreibeObjekt> schreibeObjektList = zsGenerator.generateZustaendigkeitOETranferObjekt(
            zsOE
        );
        ZustaendigkeitTransferObjekt xzufiZSOE = schreibeObjektList
            .getFirst()
            .getZustaendigkeitTransferObjekt();

        Onlinedienst od = fileSH.getOnlinedienste().getFirst();
        OnlinedienstErweitert xzufiOD = odGenerator.generateOnlinedienst(od);
        Zustaendigkeit zsOD = fileSH.getZustaendigkeitenOnlinedienst().getFirst();
        String relationIdOd =
            zsOD.getSchemeAgencyId() +
            ".ZSOD." +
            lb.getId() +
            "." +
            zsOD.getParentId() +
            "." +
            zsOD.getArss().getFirst();
        List<SchreibeObjekt> schreibeObjektListOD =
            zsGenerator.generateZustaendigkeitODTranferObjekt(zsOD);
        ZustaendigkeitTransferObjekt xzufiZSOD = schreibeObjektListOD
            .getFirst()
            .getZustaendigkeitTransferObjekt();

        Spezialisierung sp = fileSH.getSpezialisierungen().getFirst();
        LeistungSpezialisierung xzufiSP = spGenerator.generateSpezialisierung(sp);

        //Leistungs, Organisationseinheit, Onlinedienst, Relations Index befüllen
        try {
            importLeistungsEntity(lb.getFullId(), shFedState, xzufiLeistung, schemeAgencySH);
            importOeEntity(oe.getFullId(), shFedState, xzufiOE, schemeAgencySH);
            importOdEntity(od.getFullId(), shFedState, xzufiOD, schemeAgencySH);
            importSpezialisierungsEntity(sp.getFullId(), shFedState, xzufiSP, schemeAgencySH);
            importZsEntity(
                relationId,
                shFedState,
                xzufiZSOE,
                schemeAgencySH,
                lb.getFullId(),
                oe.getFullId(),
                RelationTyp.ORGANISATION
            );
            importZsEntity(
                relationIdOd,
                shFedState,
                xzufiZSOD,
                schemeAgencySH,
                lb.getFullId(),
                od.getFullId(),
                RelationTyp.ONLINESERVICE
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        indexRefreshMgr.markAsDirty(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            IndexNames.ORGANISATIONSEINHEITEN,
            IndexNames.ONLINEDIENSTE,
            IndexNames.SPEZIALISIERUNGEN,
            IndexNames.ZUSTAENDIGKEITEN
        );
        indexRefreshMgr.ensureFresh(IndexNames.LEISTUNGSBESCHREIBUNG);
        indexRefreshMgr.ensureFresh(IndexNames.ORGANISATIONSEINHEITEN);
        indexRefreshMgr.ensureFresh(IndexNames.ONLINEDIENSTE);
        indexRefreshMgr.ensureFresh(IndexNames.SPEZIALISIERUNGEN);
        indexRefreshMgr.ensureFresh(IndexNames.ZUSTAENDIGKEITEN);
        return fileSH;
    }

    private void generateVerwaltungsleistungen(String fullIdLeistung) {
        //Verwaltungsleistung generieren lassen
        var shFedState = SCHLESWIG_HOLSTEIN.getArsFederalstate();
        var bdFedState = BUND.getArsFederalstate();

        Optional<GenericTransferOperation> transferOperation040502 =
            xzufiFetchService.fetchTransferOperation(fullIdLeistung, shFedState);
        transferOperation040502.ifPresent(to ->
            verwaltungsleistungImportService.importVerwaltungsleistungen(to, shFedState)
        );
        indexRefreshMgr.markAsDirty(IndexNames.VERWALTUNGSLEISTUNGEN_SH);
        indexRefreshMgr.ensureFresh(IndexNames.VERWALTUNGSLEISTUNGEN_SH);

        transferOperation040502 = xzufiFetchService.fetchTransferOperation(
            fullIdLeistung,
            bdFedState
        );
        transferOperation040502.ifPresent(to ->
            verwaltungsleistungImportService.importVerwaltungsleistungen(to, bdFedState)
        );

        indexRefreshMgr.markAsDirty(IndexNames.VERWALTUNGSLEISTUNGEN_SH);
        indexRefreshMgr.ensureFresh(IndexNames.VERWALTUNGSLEISTUNGEN_SH);
    }

    private void deleteAllIndices() {
        IndicesResponse indicesResponse;
        try {
            indicesResponse = client.cat().indices();
            indicesResponse.indices().forEach(info -> {
                try {
                    client.indices().delete(r -> r.index(info.index()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableDynamicMappingThoughIndexTemplate() throws IOException {
        // Legt ein Index Template an
        IndexTemplateMapping indexTemplateMapping = IndexTemplateMapping.of(itm ->
            itm.mappings(m ->
                m
                    // Verhindert, dass aus dem Model automatisch ein Mapping angelegt wird.
                    .dynamic(DynamicMapping.False)
            )
        );

        PutIndexTemplateRequest putIndexTemplateRequest = PutIndexTemplateRequest.of(
            indexTemplateRequest ->
                indexTemplateRequest
                    .name("suchdiensttemplate")
                    .indexPatterns("suchdienst_*")
                    .template(indexTemplateMapping)
        );
        client.indices().putIndexTemplate(putIndexTemplateRequest);
    }

    @Data
    private static class ComparableUpdateHistoryEntry {

        private int version;
        private String description;
        private String indexName;
        private String status;
    }

    private TransactionMetadata randomTransactionMetadata(String schemeAgencyId) {
        return new TransactionMetadata(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            TransaktionsPosition.FORTSCHRITT.getPosition(),
            schemeAgencyId,
            "Sammlerdienst"
        );
    }

    private void importLeistungsEntity(
        String id,
        String arsFedState,
        Leistung leistung,
        String schemeAgency
    ) {
        var lb = new LeistungEntity22(Instant.now());
        lb.setId(id);
        lb.setArsFederalState(Set.of(arsFedState));
        lb.setJzufi(leistung);
        lb.setMetaData(randomTransactionMetadata(schemeAgency));
        lbImportService.importXzufiLbs(Set.of(lb));
    }

    private void importOeEntity(
        String id,
        String arsFedState,
        OrganisationseinheitErweitert xzufiOe,
        String schemeAgency
    ) {
        var oe = new OrganisationseinheitEntity22(Instant.now());
        oe.setId(id);
        oe.setArsFederalState(Set.of(arsFedState));
        oe.setJzufi(xzufiOe);
        oe.setMetaData(randomTransactionMetadata(schemeAgency));
        oeImportService.importXzufiOes(Set.of(oe));
    }

    private void importOdEntity(
        String id,
        String arsFedState,
        OnlinedienstErweitert xzufiOd,
        String schemeAgency
    ) {
        var od = new OnlinedienstEntity22(Instant.now());
        od.setId(id);
        od.setArsFederalState(Set.of(arsFedState));
        od.setJzufi(xzufiOd);
        od.setMetaData(randomTransactionMetadata(schemeAgency));
        odImportService.importXzufiOds(Set.of(od));
    }

    private void importZsEntity(
        String id,
        String arsFedState,
        ZustaendigkeitTransferObjekt xzufiZs,
        String schemeAgency,
        String lbId,
        String parentId,
        RelationTyp relationTyp
    ) {
        var zs = new ZustaendigkeitEntity22(Instant.now());
        zs.setId(id);
        zs.setArsFederalState(arsFedState);
        zs.setLbId(lbId);
        zs.setParentId(parentId);
        zs.setTyp(relationTyp);
        zs.setJzufi(xzufiZs);
        zs.setMetaData(randomTransactionMetadata(schemeAgency));
        relationImportService.importXzufiRelations(Set.of(zs));
    }

    private void importSpezialisierungsEntity(
        String id,
        String arsFedState,
        LeistungSpezialisierung spezialisierung,
        String schemeAgency
    ) {
        var sp = new SpezialisierungEntity22(Instant.now());
        sp.setId(id);
        sp.setArsFederalState(Set.of(arsFedState));
        sp.setJzufi(spezialisierung);
        sp.setMetaData(randomTransactionMetadata(schemeAgency));
        spImportService.importXzufiSps(List.of(sp));
    }

    private void importStandardCodelists() {
        try {
            Resource[] codeLists = new PathMatchingResourcePatternResolver().getResources(
                "classpath:/codelisten/*.xml"
            );
            List<CodelistEntry> allEntries = Arrays.stream(codeLists)
                .map(codelistService::parseCodelistDocument)
                .filter(ObjectUtils::isNotEmpty)
                // Filtert Codelisten heraus, welche bereits gespeichert wurden,
                // so bleiben manuelle Updates über das Backoffice erhalten und werden nicht überschrieben
                .filter(
                    list ->
                        !readConnector.codeListExists(
                            list.getFirst().getUri(),
                            list.getFirst().getVersion()
                        )
                )
                .flatMap(Collection::stream)
                .toList();
            codelistService.saveCodelistInElasticSearch(allEntries);
            log.info("{} Standard-Codelisteinträge importiert.", allEntries.size());
        } catch (IOException | UncheckedIOException e) {
            // PV-8946 catch-log-skip
            log.warn("Kann Standard-Codelisten nicht laden: {}", e.getMessage(), e);
        }
    }

    private void importArsList() throws IOException {
        if (readConnector.isIndexEmpty(ARS)) {
            log.info("ARS & PLZ Liste importieren");
            try (InputStream stream = getClass().getResourceAsStream("/ars/GV100AD_31072026.txt")) {
                arsService.importArsList(stream, StandardCharsets.UTF_8);
            }
            log.info("ARS Liste fertig importiert");
            try (
                var gzis = new GZIPInputStream(
                    getClass().getResourceAsStream("/ars/plz-nach-hk-de-2026.csv.gz")
                );
                var isr = new InputStreamReader(gzis, StandardCharsets.UTF_8);
                var br = new BufferedReader(isr);
            ) {
                Stream<String> lines = br.lines();
                var zipDef = ZipcodeCsvDefinition.builder()
                    .delimiter(";")
                    .headerLine(false)
                    .rsColumn(0)
                    .zipColumn(1)
                    .rsKind(RegionalKeyKind.AGS)
                    .build();
                arsService.importPostcodes(lines, zipDef);
            }
            log.info("PLZ Liste fertig importiert");
        }
    }

    private void validateExpectedIndices(List<DefaultDescription> descriptionList) {
        descriptionList.forEach(description -> {
            if (description.getDestIndex().contains("verwaltungsleistungen")) {
                // Abfrage über wildcard, da eine Uhrzeit an den Index geschrieben wird
                assertTrue(
                    readConnector.indexExists(description.getDestIndex() + "*"),
                    "Index wurde nicht angelegt:" + AliasNames.VERWALTUNGSLEISTUNGEN_SH.name()
                );
            } else {
                assertTrue(
                    readConnector.indexExists(description.getDestIndex()),
                    "Index wurde nicht angelegt:" + description.getDestIndex()
                );
            }
        });
    }

    public HitsMetadata<UpdateDescription> searchUpdateHistory(UpdateStatus updateStatus) {
        List<Query> termQueries = Lists.newArrayList(
            Elastic.QueryHelper.term("status", updateStatus.name())
        );
        Query boolQuery = QueryBuilders.bool(b -> b.filter(termQueries));
        SortOptions sortOptions = SortOptionsBuilders.field(builder ->
            builder.field("version").order(SortOrder.Asc)
        );
        try {
            indexRefreshMgr.ensureFresh(IndexNames.UPDATEHISTORY);
            SearchResponse<UpdateDescription> response = client.search(
                r ->
                    r
                        .index(IndexNames.UPDATEHISTORY.getIndexName())
                        .query(boolQuery)
                        .sort(sortOptions),
                UpdateDescription.class
            );
            return response.hits();
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            var errMsg = "Fehler bei der Suche in Upatehistory";
            throw new ElasticSearchException(ErrorCodeEnum.ELSE_IO_ERROR, errMsg, e);
        }
    }

    private List<ComparableUpdateHistoryEntry> createExpectedHistoryEntries(
        List<DefaultDescription> descriptionList
    ) {
        List<ComparableUpdateHistoryEntry> createdUpdateHistoryEntryList = new ArrayList<>();
        descriptionList.forEach(description -> {
            ComparableUpdateHistoryEntry historyEntry = new ComparableUpdateHistoryEntry();
            historyEntry.setStatus(UpdateStatus.CREATED.name());
            historyEntry.setDescription(description.getDescription());
            historyEntry.setIndexName(description.getAbstractIndexName().getIndexName());
            historyEntry.setVersion(description.getVersion());
            createdUpdateHistoryEntryList.add(historyEntry);
        });
        return createdUpdateHistoryEntryList;
    }

    private List<ComparableUpdateHistoryEntry> createExpectedHistoryEntriesMigration(
        List<IndexMigrationDescription> descriptionList,
        String expectedState
    ) {
        List<ComparableUpdateHistoryEntry> createdUpdateHistoryEntryList = new ArrayList<>();
        descriptionList.forEach(description -> {
            ComparableUpdateHistoryEntry historyEntry = new ComparableUpdateHistoryEntry();
            historyEntry.setStatus(expectedState);
            historyEntry.setDescription(description.getDescription());
            historyEntry.setIndexName(description.getAbstractIndexName().getIndexName());
            historyEntry.setVersion(description.getVersion());
            createdUpdateHistoryEntryList.add(historyEntry);
        });
        return createdUpdateHistoryEntryList;
    }

    private List<ComparableUpdateHistoryEntry> createFoundHistoryEntries(
        List<UpdateDescription> foundEntries
    ) {
        List<ComparableUpdateHistoryEntry> createdUpdateHistoryEntryList = new ArrayList<>();
        foundEntries.forEach(description -> {
            ComparableUpdateHistoryEntry historyEntry = new ComparableUpdateHistoryEntry();
            historyEntry.setStatus(description.getStatus().name());
            historyEntry.setDescription(description.getDescription());
            historyEntry.setIndexName(description.getIndexName());
            historyEntry.setVersion(description.getVersion());
            createdUpdateHistoryEntryList.add(historyEntry);
        });
        return createdUpdateHistoryEntryList;
    }
}
