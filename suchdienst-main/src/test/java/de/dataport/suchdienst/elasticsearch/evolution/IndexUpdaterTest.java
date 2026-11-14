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
package de.dataport.suchdienst.elasticsearch.evolution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexState;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.tasks.ElasticsearchTasksClient;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.elasticsearch.tasks.ListResponse;
import com.google.common.collect.Lists;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.evolution.description.TestIndexMigrationDescription;
import de.dataport.suchdienst.elasticsearch.evolution.description.TestIndexMigrationDescription2;
import de.dataport.suchdienst.elasticsearch.evolution.description.TestIndexMigrationNoSourceIndex;
import de.dataport.suchdienst.elasticsearch.evolution.description.TestIndexUpdateDescription;
import de.dataport.suchdienst.elasticsearch.model.UpdateDescription;
import de.dataport.suchdienst.elasticsearch.model.UpdateStatus;
import de.dataport.suchdienst.indexer.exception.IndexUpdateException;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {
        IndexUpdater.class,
        IndexConfiguration.class,
        SuchdienstProperties.class,
        ResourceBundleConfig.class,
    }
)
@MockitoBean(types = { ImportSchedulingService.class })
class IndexUpdaterTest {

    @Autowired
    private IndexUpdater updater;

    @Autowired
    private IndexConfiguration indexConfiguration;

    @MockitoBean
    private ElasticsearchClient client;

    @MockitoBean
    private ElasticWriteConnector writeConnectorMigration;

    @MockitoBean
    private ElasticReadConnector elasticReadConnector;

    @MockitoBean
    private UpdateConfiguration configuration;

    @Test
    void testUpdate() throws IOException {
        TestIndexUpdateDescription indexDescription = Mockito.spy(new TestIndexUpdateDescription());
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(indexDescription)
        );
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(
            elasticReadConnector.getLastEntriesFromUpdateHistory(Mockito.any(), Mockito.anyInt())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(
            writeConnectorMigration.index(
                Mockito.any(),
                Mockito.eq(IndexNames.UPDATEHISTORY),
                Mockito.any()
            )
        ).then(a -> {
            UpdateDescription description = a.getArgument(0);
            assertAll(
                () -> assertThat(description.getDescription(), equalTo("Für Testzwecke")),
                () ->
                    assertThat(
                        description.getStatus(),
                        anyOf(equalTo(UpdateStatus.STARTED), equalTo(UpdateStatus.CREATED))
                    ),
                () -> assertThat(description.getVersion(), equalTo(1))
            );
            return null;
        });

        Mockito.when(
            elasticReadConnector.indexExists(
                indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY)
            )
        ).thenReturn(true);
        updater.updateElasticSearch();

        // Diese haben den Status created und started
        Mockito.verify(writeConnectorMigration, Mockito.times(1)).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );

        // Neuer Index muss erstellt werden
        Mockito.verify(writeConnectorMigration, Mockito.times(1)).createIndexFromJsonString(
            Mockito.anyString(),
            Mockito.isNull(),
            Mockito.anyString()
        );
    }

    @Test
    void testDoubleVersion() throws IOException {
        TestIndexUpdateDescription indexDescription = Mockito.spy(new TestIndexUpdateDescription());
        TestIndexUpdateDescription indexDescription2 = Mockito.spy(
            new TestIndexUpdateDescription()
        );
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(indexDescription, indexDescription2)
        );
        try {
            updater.updateElasticSearch();
            fail();
        } catch (IndexUpdateException e) {
            assertEquals(
                "Indexupdate fehlgeschlagen, da doppelte neue Versionen vorhanden sind.",
                e.getText()
            );
        }

        // Diese haben den Status finished
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
        // Diese haben den Status started
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );

        // Neuer Index darf nichterstellt werden
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).createIndexFromJsonString(
            Mockito.anyString(),
            Mockito.isNull(),
            Mockito.anyString()
        );

        // Neuer Index darf nichterstellt werden
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).createIndexFromJsonString(
            Mockito.anyString(),
            Mockito.isNull(),
            Mockito.anyString()
        );
    }

    @Test
    void testUpdateAlreadyDone() throws IOException {
        TestIndexUpdateDescription indexDescription = Mockito.spy(new TestIndexUpdateDescription());
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(indexDescription)
        );

        Mockito.when(
            elasticReadConnector.searchUpdateHistory(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(1L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(
            elasticReadConnector.getLastEntriesFromUpdateHistory(Mockito.any(), Mockito.anyInt())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );

        Mockito.when(
            writeConnectorMigration.index(
                Mockito.any(),
                Mockito.eq(IndexNames.UPDATEHISTORY),
                Mockito.any()
            )
        ).then(a -> {
            fail("Es darf kein Update mehr durchgeführt werden.");
            return null;
        });

        Mockito.when(
            elasticReadConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName())
        ).thenReturn(true);

        updater.updateElasticSearch();

        // Diese haben den Status finished
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
        // Diese haben den Status started
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );

        // Neuer Index darf nichterstellt werden
        Mockito.verify(writeConnectorMigration, Mockito.times(0)).createIndexFromJsonString(
            Mockito.anyString(),
            Mockito.isNull(),
            Mockito.anyString()
        );
    }

    @Test
    void testUpdateError() throws IOException {
        Mockito.when(
            elasticReadConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName())
        ).thenReturn(true);
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(new TestIndexUpdateDescription())
        );
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(HitsMetadata.of(b -> b.hits(List.of()).maxScore(0.0)));

        Mockito.when(
            elasticReadConnector.getLastEntriesFromUpdateHistory(Mockito.any(), Mockito.anyInt())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );

        Mockito.when(
            writeConnectorMigration.createIndexFromJsonString(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
            )
        ).thenThrow(new IOException("Fehler beim Erstellen des Indexes"));

        Mockito.when(
            writeConnectorMigration.index(
                Mockito.any(),
                Mockito.eq(IndexNames.UPDATEHISTORY),
                Mockito.any()
            )
        ).then(a -> {
            UpdateDescription description = a.getArgument(0);
            assertAll(
                () -> assertThat(description.getDescription(), equalTo("Für Testzwecke")),
                () ->
                    assertThat(
                        description.getStatus(),
                        anyOf(equalTo(UpdateStatus.STARTED), equalTo(UpdateStatus.ERROR))
                    ),
                () -> assertThat(description.getVersion(), equalTo(1))
            );
            return null;
        });
        assertThrows(
            IndexUpdateException.class,
            () -> updater.updateElasticSearch(),
            "Es hätte ein Fehler auftreten sollen"
        );
    }

    @Test
    void testUpdateStopsWhenTooManyFlawedUpdateTriesExist() throws IOException {
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(new TestIndexUpdateDescription())
        );
        Mockito.when(
            elasticReadConnector.indexExists(IndexNames.UPDATEHISTORY.getIndexName())
        ).thenReturn(true);
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(
            elasticReadConnector.getLastEntriesFromUpdateHistory(Mockito.any(), Mockito.anyInt())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(6L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );

        IndexUpdateException exception = assertThrows(IndexUpdateException.class, () ->
            updater.updateElasticSearch()
        );

        assertThat(exception.getText(), equalTo("Zu viele fehlerhafte Indexupdateversuche."));
        Mockito.verify(elasticReadConnector).getLastEntriesFromUpdateHistory(
            Mockito.any(),
            Mockito.eq(10)
        );
        Mockito.verify(writeConnectorMigration, Mockito.never()).createIndexFromJsonString(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString()
        );
        Mockito.verify(writeConnectorMigration, Mockito.never()).index(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
    }

    /**
     * Führt eine Migration von Version 1 auf Version 3 durch. Die Version 2 soll übersprungen werden
     */
    @Test
    void testMigration1() throws IOException {
        // Index erstellen
        TestIndexUpdateDescription des1 = new TestIndexUpdateDescription();
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(des1)
        );

        // Index migrieren, es soll mig1 ausgeführt werden, also von Version 1 auf 3
        TestIndexMigrationDescription mig1 = new TestIndexMigrationDescription();
        TestIndexMigrationDescription2 mig2 = new TestIndexMigrationDescription2();
        Mockito.when(configuration.getMigrationDescriptions()).thenReturn(
            Lists.newArrayList(mig1, mig2)
        );

        mockForMigrations(mig1);

        // Index wurde bereits in der Version 1 und 2 angelegt. Das heißt es muss von 2 nach 3 migriert werden
        UpdateDescription description1 = new UpdateDescription();
        description1.setVersion(1);
        description1.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription1 = Hit.of(h ->
            h.source(description1).index("").id("")
        );
        UpdateDescription description2 = new UpdateDescription();
        description2.setVersion(3);
        description2.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription2 = Hit.of(h ->
            h.source(description2).index("").id("")
        );
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(
                Mockito.eq(Optional.empty()),
                Mockito.any(),
                Mockito.eq(UpdateStatus.CREATED)
            )
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of(hitUpdateDescription1, hitUpdateDescription2))
                    .total(v -> v.value(2L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(elasticReadConnector.indexExists(mig1.getSourceIndex())).thenReturn(true);

        updater.updateElasticSearch();

        ArgumentCaptor<UpdateDescription> captorUpdateDescription = ArgumentCaptor.forClass(
            UpdateDescription.class
        );

        // Wird 3 mal aufgerufen, einmal Index erstellen, einmal Start Migration und einmal Ende Migration
        Mockito.verify(writeConnectorMigration, Mockito.times(3)).index(
            captorUpdateDescription.capture(),
            Mockito.eq(IndexNames.UPDATEHISTORY),
            Mockito.isNull()
        );
        UpdateDescription lastFinishedUpdate = captorUpdateDescription.getValue();
        assertThat(lastFinishedUpdate.getVersion(), equalTo(3));
        assertThat(lastFinishedUpdate.getDescription(), equalTo(mig1.getDescription()));
    }

    /**
     * Führt eine Migration von Version 2 auf Version 3 durch. Das heißt die Migration von 1 auf 2 wurde vorher schon durchgeführt
     */
    @Test
    void testMigration2() throws IOException {
        // Index erstellen
        TestIndexUpdateDescription des1 = new TestIndexUpdateDescription();
        Mockito.when(configuration.getDefaultUpdateDescriptions()).thenReturn(
            Lists.newArrayList(des1)
        );

        // Index migrieren, es soll mig1 ausgeführt werden, also von Version 1 auf 3
        TestIndexMigrationDescription mig1 = new TestIndexMigrationDescription();
        TestIndexMigrationDescription2 mig2 = new TestIndexMigrationDescription2();
        Mockito.when(configuration.getMigrationDescriptions()).thenReturn(
            Lists.newArrayList(mig1, mig2)
        );

        mockForMigrations(mig2);

        // Index wurde bereits in der Version 1 und 2 angelegt. Das heißt es muss von 2 nach 3 migriert werden
        UpdateDescription description1 = new UpdateDescription();
        description1.setVersion(1);
        description1.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription1 = Hit.of(h ->
            h.source(description1).index("").id("")
        );
        UpdateDescription description2 = new UpdateDescription();
        description2.setVersion(2);
        description2.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription2 = Hit.of(h ->
            h.source(description2).index("").id("")
        );
        UpdateDescription description3 = new UpdateDescription();
        description3.setVersion(3);
        description3.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription3 = Hit.of(h ->
            h.source(description3).index("").id("")
        );
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(
                Mockito.eq(Optional.empty()),
                Mockito.any(),
                Mockito.eq(UpdateStatus.CREATED)
            )
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(
                        List.of(hitUpdateDescription1, hitUpdateDescription2, hitUpdateDescription3)
                    )
                    .total(v -> v.value(3L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(elasticReadConnector.indexExists(mig2.getSourceIndex())).thenReturn(true);

        updater.updateElasticSearch();

        ArgumentCaptor<UpdateDescription> captorUpdateDescription = ArgumentCaptor.forClass(
            UpdateDescription.class
        );

        // Wird 3 mal aufgerufen, einmal Index erstellen, einmal Start Migration und einmal Ende Migration
        Mockito.verify(writeConnectorMigration, Mockito.times(3)).index(
            captorUpdateDescription.capture(),
            Mockito.eq(IndexNames.UPDATEHISTORY),
            Mockito.isNull()
        );
        UpdateDescription lastFinishedUpdate = captorUpdateDescription.getValue();
        assertThat(lastFinishedUpdate.getVersion(), equalTo(3));
        assertThat(lastFinishedUpdate.getDescription(), equalTo(mig2.getDescription()));
    }

    // Mockt alles damit keine Nullpointer-Exceptions kommen
    private void mockForMigrations(IndexMigrationDescription migrationDescription)
        throws IOException {
        ElasticsearchIndicesClient indicesClient = Mockito.mock(ElasticsearchIndicesClient.class);
        GetIndicesSettingsResponse settingsResponse = Mockito.mock(
            GetIndicesSettingsResponse.class
        );
        PutIndicesSettingsResponse putSettingsResponse = Mockito.mock(
            PutIndicesSettingsResponse.class
        );
        Mockito.when(putSettingsResponse.acknowledged()).thenReturn(true);
        IndexState indexState = Mockito.mock(IndexState.class);
        IndexSettings settings = Mockito.mock(IndexSettings.class);
        Mockito.when(client.indices()).thenReturn(indicesClient);
        Mockito.when(indicesClient.getSettings(Mockito.any(Function.class))).thenReturn(
            settingsResponse
        );
        Mockito.when(indicesClient.putSettings(Mockito.any(Function.class))).thenReturn(
            putSettingsResponse
        );
        Mockito.when(settingsResponse.get(migrationDescription.getDestIndex())).thenReturn(
            indexState
        );
        Mockito.when(indexState.settings()).thenReturn(settings);

        ReindexResponse reindexResponse = Mockito.mock(ReindexResponse.class);
        Mockito.when(client.reindex(Mockito.any(ReindexRequest.class))).thenReturn(reindexResponse);
        Mockito.when(reindexResponse.task()).thenReturn("junittask");

        ElasticsearchTasksClient taskClient = Mockito.mock(ElasticsearchTasksClient.class);
        Mockito.when(client.tasks()).thenReturn(taskClient);
        Mockito.when(taskClient.list(Mockito.any(Function.class))).thenReturn(
            new ListResponse.Builder().build()
        );

        GetTasksResponse getTasksResponse = Mockito.mock(GetTasksResponse.class);
        Mockito.when(taskClient.get(Mockito.any(Function.class))).thenReturn(getTasksResponse);

        Mockito.when(
            elasticReadConnector.searchUpdateHistory(Mockito.any(), Mockito.any(), Mockito.any())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );
        Mockito.when(
            elasticReadConnector.getLastEntriesFromUpdateHistory(Mockito.any(), Mockito.anyInt())
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of())
                    .total(v -> v.value(0L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );

        Mockito.when(
            elasticReadConnector.indexExists(
                indexConfiguration.getCurrentIndexName(IndexNames.UPDATEHISTORY)
            )
        ).thenReturn(true);
    }

    @Test
    void testNonExistingSourceIndex() throws IOException {
        TestIndexMigrationNoSourceIndex des1 = new TestIndexMigrationNoSourceIndex();
        Mockito.when(configuration.getMigrationDescriptions()).thenReturn(Lists.newArrayList(des1));

        mockForMigrations(des1);

        // Index wurde bereits in der Version 1 und 2 angelegt. Das heißt es muss von 1 nach 2 migriert werden
        UpdateDescription description1 = new UpdateDescription();
        description1.setVersion(1);
        description1.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription1 = Hit.of(h ->
            h.source(description1).index("").id("")
        );
        UpdateDescription description2 = new UpdateDescription();
        description2.setVersion(2);
        description2.setStatus(UpdateStatus.CREATED);
        Hit<UpdateDescription> hitUpdateDescription2 = Hit.of(h ->
            h.source(description2).index("").id("")
        );
        Mockito.when(
            elasticReadConnector.searchUpdateHistory(
                Mockito.eq(Optional.empty()),
                Mockito.any(),
                Mockito.eq(UpdateStatus.CREATED)
            )
        ).thenReturn(
            HitsMetadata.of(b ->
                b
                    .hits(List.of(hitUpdateDescription1, hitUpdateDescription2))
                    .total(v -> v.value(2L).relation(TotalHitsRelation.Eq))
                    .maxScore(0.0)
            )
        );

        try {
            updater.updateElasticSearch();
            fail();
        } catch (IndexUpdateException e) {
            assertThat(
                e.getMessage(),
                equalTo(
                    "Im Migrationsschritt mit der Zielversion " +
                        des1.getVersion() +
                        ", der erforderlichen Version " +
                        des1.getRequiredPreVersion() +
                        " und der Beschreibung " +
                        des1.getDescription() +
                        " wurde kein Quellindex angegeben."
                )
            );
        }

        ArgumentCaptor<UpdateDescription> captorUpdateDescription = ArgumentCaptor.forClass(
            UpdateDescription.class
        );

        // Wird 1 mal beim Starten der Migration aufgerufen
        Mockito.verify(writeConnectorMigration, Mockito.times(1)).index(
            captorUpdateDescription.capture(),
            Mockito.eq(IndexNames.UPDATEHISTORY),
            Mockito.isNull()
        );
        UpdateDescription lastFinishedUpdate = captorUpdateDescription.getValue();
        assertThat(lastFinishedUpdate.getVersion(), equalTo(2));
        assertThat(lastFinishedUpdate.getStatus(), equalTo(UpdateStatus.STARTED));
    }
}
