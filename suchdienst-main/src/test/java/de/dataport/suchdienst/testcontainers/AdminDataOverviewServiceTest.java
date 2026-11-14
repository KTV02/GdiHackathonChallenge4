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

import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_IMPORT;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_RECALC;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.RUNNING_VL_UPDATE;
import static de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState.WAITING;
import static de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus.DONE;
import static de.dataport.suchdienst.util.Bundeslaender.BERLIN;
import static de.dataport.suchdienst.util.Bundeslaender.BREMEN;
import static de.dataport.suchdienst.util.Bundeslaender.HAMBURG;
import static de.dataport.suchdienst.util.Bundeslaender.NIEDERSACHSEN;
import static de.dataport.suchdienst.util.Bundeslaender.SCHLESWIG_HOLSTEIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModelProcessState;
import de.dataport.suchdienst.backoffice.service.AdminDataOverviewService;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.IndexRefreshMgr;
import de.dataport.suchdienst.geo.service.GeoService;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequestStatus;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.importer.service.ElasticQueuePersistenceService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import de.dataport.suchdienst.indexer.service.ArsService;
import de.dataport.suchdienst.indexer.service.LbImportService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.indexer.service.OdImportService;
import de.dataport.suchdienst.indexer.service.OeImportService;
import de.dataport.suchdienst.indexer.service.RelationImportService;
import de.dataport.suchdienst.indexer.service.SpImportService;
import de.dataport.suchdienst.taskmgr.model.ImportTask;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.taskmgr.model.out.SudTaskOutModel;
import de.dataport.suchdienst.taskmgr.model.out.SudTasksSnapshot;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.model.TransaktionsPosition;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testet den AdminDataOverviewService.
 * Dies ist als Container-Test angelegt, da es mit Integrationstests nicht möglich ist, die Komponenten des Servers zu mocken, sodass:
 * - Der TaskMgr verlässlich einen bestimmten Inhalt hat (der wiederum vom AdminDataOverviewService ausgelesen wird)
 * - Die ImportQueue mit bestimmten Elementen auf hinreichend komplexe, stabile Weise gefüllt ist, sodass es Sinn macht, die Aggregation
 *   mitzutesten (die auch vom AdminDataOverviewService ausgelesen wird).
 *   <p></p>
 *   Außerdem ist es hier explizit Testziel, die ElasticSearch-Queries zum Füllen der Datenübersicht mitzutesten.
 *  <p></p>
 *   Der Test arbeitet also folgendermassen:
 *    - fügt Domain-Objekte via ElasticSearchWriteConnector in die Indices ein. Befüllt dabei sowohl die Jzufi-Indices als auch die VL-Indizes
 *    - mockt den TaskMgr
 *    - fügt Elemente in den Index importqueue ein und sorgt dafür, dass diese nicht abgearbeitet werden, denn der ADOS soll die Queue ja
 *      gerade auslesen.
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Slf4j
@MockitoBeans(
    value = {
        // Kein ARS/PLZ Import
        @MockitoBean(types = ArsService.class),
        // Kein Leistungskatalog Import
        @MockitoBean(types = LeikaService.class),
        // Keine Abarbeitung der ImportQueue, Test verlangt einen stabilen Stand, damit die Zählerwerte validiert werden können
        @MockitoBean(types = QueueImportService.class),
        // Keine initialen Codelist-Importe über GeoService
        @MockitoBean(types = GeoService.class),
    }
)
class AdminDataOverviewServiceTest extends AbstractElasticsearchContainerTest {

    @Autowired
    private AdminDataOverviewService dataOverviewService;

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
    private ElasticQueuePersistenceService queuePersistenceService;

    @Autowired
    private ElasticWriteConnector elasticWriteConnector;

    @Autowired
    private IndexConfiguration indexConfiguration;

    @MockitoBean
    private SudTaskMgr mockTaskMgr;

    @BeforeEach
    void cleanIndexes() {
        List.of(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            IndexNames.ORGANISATIONSEINHEITEN,
            IndexNames.ONLINEDIENSTE,
            IndexNames.ZUSTAENDIGKEITEN,
            IndexNames.SPEZIALISIERUNGEN,
            IndexNames.IMPORTQUEUE
        ).forEach(indexName ->
            elasticWriteConnector.clearIndex(indexConfiguration.getCurrentIndexName(indexName))
        );
    }

    /**
     * Legt ein paar Domain Objekte in unterschiedlichen Bundesländern und prüft, ob die DataOverview die
     * erwarteten Zähler hat.
     */
    @Test
    void testJzufiCounters() {
        Mockito.when(mockTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(List.of(), List.of())
        );
        var shAgencyId = SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        var hhAgencyId = HAMBURG.getSchemeAgencyId();

        importLeistungEntity("lb-sh", SCHLESWIG_HOLSTEIN);
        importLeistungEntity("lb-hh-1", HAMBURG);
        importLeistungEntity("lb-hh-2", HAMBURG);
        importOeEntity("oe-sh", SCHLESWIG_HOLSTEIN);
        importOdEntity("od-hh", HAMBURG);
        importZsEntity("zs-hh", HAMBURG);
        importSpEntity("sp-hh-1", HAMBURG);
        importSpEntity("sp-hh-2", HAMBURG);

        var dataOverviewOutModels = dataOverviewService.getDataOverview();
        Assertions.assertEquals(Bundeslaender.values().length, dataOverviewOutModels.size());
        DataOverviewOutModel modelSh = getDataOverviewOutModel(dataOverviewOutModels, shAgencyId);
        assertEquals(1, modelSh.lbCount());
        assertEquals(1, modelSh.oeCount());
        assertEquals(0, modelSh.odCount());
        assertEquals(0, modelSh.zsCount());
        assertEquals(0, modelSh.spCount());
        DataOverviewOutModel modelHh = getDataOverviewOutModel(dataOverviewOutModels, hhAgencyId);
        assertEquals(2, modelHh.lbCount());
        assertEquals(0, modelHh.oeCount());
        assertEquals(1, modelHh.odCount());
        assertEquals(1, modelHh.zsCount());
        assertEquals(2, modelHh.spCount());

        // Alle anderen außer SH und HH haben ausschließlich 0 counter:
        dataOverviewOutModels
            .stream()
            .filter(
                m ->
                    !shAgencyId.equals(m.schemeAgencyId()) && !hhAgencyId.equals(m.schemeAgencyId())
            )
            .forEach(AdminDataOverviewServiceTest::isBlank);
    }

    private static void isBlank(DataOverviewOutModel m) {
        assertEquals(0, m.lbCount());
        assertEquals(0, m.oeCount());
        assertEquals(0, m.odCount());
        assertEquals(0, m.zsCount());
        assertEquals(0, m.spCount());
        assertEquals(0, m.vlCount());
        assertEquals(0, m.odRelCount());
        assertEquals(DataOverviewOutModelProcessState.STABLE, m.processState());
        assertEquals(0, m.importQueueCount());
        assertEquals(0, m.importTaskCount());
        assertEquals(0, m.vlUpdateTaskCount());
        assertEquals(0, m.vlRecalcTaskCount());
        assertEquals(0, m.odRelRecalcTaskCount());
    }

    /**
     * Legt Dokumente in der ImportQueue an und testet die Counter in der DataOverview.
     */
    @Test
    void testImportQueueCounters() {
        Mockito.when(mockTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(List.of(), List.of())
        );

        var shAgencyId = SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        var hhAgencyId = HAMBURG.getSchemeAgencyId();
        createOpenImportRequest(hhAgencyId);
        createOpenImportRequest(shAgencyId);
        var reOpenedImportRequest = createReOpenedImportRequest(shAgencyId);
        importLeistungEntity("lb-sh", SCHLESWIG_HOLSTEIN);
        var dataOverviewOutModels = dataOverviewService.getDataOverview();
        DataOverviewOutModel modelHh = getDataOverviewOutModel(dataOverviewOutModels, hhAgencyId);
        DataOverviewOutModel modelSh = getDataOverviewOutModel(dataOverviewOutModels, shAgencyId);
        assertEquals(1, modelHh.importQueueCount());
        assertEquals(2, modelSh.importQueueCount());
        // den Status eines ImportRequest ändern auf DONE, dann wird dieser in der DataOverview nicht mehr mitgezählt:
        queuePersistenceService.changeStatus(reOpenedImportRequest.getMetaData().id(), DONE, false);
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        var dataOverviewOutModels2 = dataOverviewService.getDataOverview();
        DataOverviewOutModel modelSh2 = getDataOverviewOutModel(dataOverviewOutModels2, shAgencyId);
        assertEquals(1, modelSh2.importQueueCount());
        assertEquals(1, modelSh2.lbCount()); // Xzufi-Counter wurden mit in die DataOverview eingemischt
        assertEquals(WAITING, modelSh2.processState());
        // Alle anderen außer SH und HH haben ausschließlich 0 counter:
        dataOverviewOutModels
            .stream()
            .filter(
                m ->
                    !shAgencyId.equals(m.schemeAgencyId()) && !hhAgencyId.equals(m.schemeAgencyId())
            )
            .forEach(AdminDataOverviewServiceTest::isBlank);
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testTaskCountersAndProcessState() {
        String shAgencyId = SCHLESWIG_HOLSTEIN.getSchemeAgencyId();
        String beAgencyId = BERLIN.getSchemeAgencyId();
        String niAgencyId = NIEDERSACHSEN.getSchemeAgencyId();
        String heAgencyId = BREMEN.getSchemeAgencyId();
        String hhAgencyId = HAMBURG.getSchemeAgencyId();

        var waitingTasks = List.of(
            SudTaskOutModel.of(new ImportTask(shAgencyId, "Schleswig-Holstein", "requestId")),
            SudTaskOutModel.of(new ImportTask(beAgencyId, "Berlin", "requestId")),
            SudTaskOutModel.of(
                new VlUpdateTask(beAgencyId, "Berlin", BERLIN.getArsFederalstate(), "lbId")
            )
        );
        var runningTasks = List.of(
            SudTaskOutModel.of(new ImportTask(niAgencyId, "Niedersachsen", "requestId")),
            SudTaskOutModel.of(
                new VlUpdateTask(heAgencyId, "Bremen", BREMEN.getArsFederalstate(), "lbId")
            ),
            SudTaskOutModel.of(new VlRecalcTask(BERLIN))
        );
        Mockito.when(mockTaskMgr.getTasksSnapshot()).thenReturn(
            new SudTasksSnapshot(runningTasks, waitingTasks)
        );

        createOpenImportRequest(hhAgencyId);
        createOpenImportRequest(niAgencyId);
        var dataOverviewOutModels = dataOverviewService.getDataOverview();

        var modelHh = getDataOverviewOutModel(dataOverviewOutModels, hhAgencyId);
        assertEquals(1, modelHh.importQueueCount());
        assertEquals(WAITING, modelHh.processState());
        assertEquals(0, modelHh.importTaskCount());
        assertEquals(0, modelHh.vlUpdateTaskCount());
        assertEquals(0, modelHh.vlRecalcTaskCount());

        var modelSh = getDataOverviewOutModel(dataOverviewOutModels, shAgencyId);
        assertEquals(0, modelSh.importQueueCount());
        assertEquals(WAITING, modelSh.processState());
        assertEquals(1, modelSh.importTaskCount());
        assertEquals(0, modelSh.vlUpdateTaskCount());
        assertEquals(0, modelSh.vlRecalcTaskCount());

        var modelNi = getDataOverviewOutModel(dataOverviewOutModels, niAgencyId);
        assertEquals(1, modelNi.importQueueCount());
        assertEquals(RUNNING_IMPORT, modelNi.processState());
        assertEquals(1, modelNi.importTaskCount());
        assertEquals(0, modelNi.vlUpdateTaskCount());
        assertEquals(0, modelNi.vlRecalcTaskCount());

        var modelHb = getDataOverviewOutModel(dataOverviewOutModels, heAgencyId);
        assertEquals(0, modelHb.importQueueCount());
        assertEquals(RUNNING_VL_UPDATE, modelHb.processState());
        assertEquals(0, modelHb.importTaskCount());
        assertEquals(1, modelHb.vlUpdateTaskCount());
        assertEquals(0, modelHb.vlRecalcTaskCount());

        var modelBe = getDataOverviewOutModel(dataOverviewOutModels, beAgencyId);
        assertEquals(0, modelBe.importQueueCount());
        assertEquals(RUNNING_VL_RECALC, modelBe.processState());
        assertEquals(1, modelBe.importTaskCount());
        assertEquals(1, modelBe.vlUpdateTaskCount());
        assertEquals(1, modelBe.vlRecalcTaskCount());
    }

    private ImportRequest createOpenImportRequest(String schemeAgancyId) {
        var importRequest = ImportRequest.createOpen(
            randomTransactionMetadata(schemeAgancyId),
            randomByteArray(),
            XZuFiVersion.XZUFI22
        );
        queuePersistenceService.write(importRequest);
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        return importRequest;
    }

    private ImportRequest createReOpenedImportRequest(String schemeAgancyId) {
        var importRequest = new ImportRequest(
            Instant.now(),
            Instant.now(),
            randomTransactionMetadata(schemeAgancyId),
            randomByteArray(),
            XZuFiVersion.XZUFI22,
            ImportRequestStatus.REOPENED
        );
        queuePersistenceService.write(importRequest);
        indexRefreshMgr.ensureFresh(IndexNames.IMPORTQUEUE);
        return importRequest;
    }

    private byte[] randomByteArray() {
        return UUID.randomUUID().toString().getBytes();
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

    @NotNull
    private static DataOverviewOutModel getDataOverviewOutModel(
        List<DataOverviewOutModel> dataOverviewOutModels,
        String schemeAgencyId
    ) {
        var optModel = dataOverviewOutModels
            .stream()
            .filter(m -> m.schemeAgencyId().equals(schemeAgencyId))
            .findFirst();
        assertTrue(optModel.isPresent());
        return optModel.get();
    }

    private void importLeistungEntity(String id, Bundeslaender bundesland) {
        var lb = new LeistungEntity22(Instant.now());
        lb.setId(id);
        lb.setArsFederalState(Set.of(bundesland.getArsFederalstate()));
        lb.setMetaData(randomTransactionMetadata(bundesland.getSchemeAgencyId()));
        lbImportService.importXzufiLbs(Set.of(lb));
        indexRefreshMgr.ensureFresh(IndexNames.LEISTUNGSBESCHREIBUNG);
    }

    private void importOeEntity(String id, Bundeslaender bundesland) {
        var oe = new OrganisationseinheitEntity22(Instant.now());
        oe.setId(id);
        oe.setArsFederalState(Set.of(bundesland.getArsFederalstate()));
        oe.setMetaData(randomTransactionMetadata(bundesland.getSchemeAgencyId()));
        oeImportService.importXzufiOes(Set.of(oe));
        indexRefreshMgr.ensureFresh(IndexNames.ORGANISATIONSEINHEITEN);
    }

    private void importOdEntity(String id, Bundeslaender bundesland) {
        var od = new OnlinedienstEntity22(Instant.now());
        od.setId(id);
        od.setArsFederalState(Set.of(bundesland.getArsFederalstate()));
        od.setMetaData(randomTransactionMetadata(bundesland.getSchemeAgencyId()));
        odImportService.importXzufiOds(Set.of(od));
        indexRefreshMgr.ensureFresh(IndexNames.ONLINEDIENSTE);
    }

    private void importZsEntity(String id, Bundeslaender bundesland) {
        var zs = new ZustaendigkeitEntity22(Instant.now());
        zs.setId(id);
        zs.setArsFederalState(bundesland.getArsFederalstate());
        zs.setMetaData(randomTransactionMetadata(bundesland.getSchemeAgencyId()));
        relationImportService.importXzufiRelations(Set.of(zs));
        indexRefreshMgr.ensureFresh(IndexNames.ZUSTAENDIGKEITEN);
    }

    private void importSpEntity(String id, Bundeslaender bundesland) {
        var sp = new SpezialisierungEntity22(Instant.now());
        sp.setId(id);
        sp.setArsFederalState(Set.of(bundesland.getArsFederalstate()));
        sp.setMetaData(randomTransactionMetadata(bundesland.getSchemeAgencyId()));
        spImportService.importXzufiSps(List.of(sp));
        indexRefreshMgr.ensureFresh(IndexNames.SPEZIALISIERUNGEN);
    }
}
