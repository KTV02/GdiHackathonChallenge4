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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexRequest;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.elasticsearch.tasks.ListResponse;
import co.elastic.clients.json.JsonData;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.util.MdcTemplate;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
public class ReindexHandler {

    private final SuchdienstProperties props;
    private final ElasticsearchClient client;

    private final String destinationIndex;

    public ReindexHandler(
        SuchdienstProperties props,
        ElasticsearchClient client,
        String destinationIndex
    ) {
        this.props = props;
        this.client = client;
        this.destinationIndex = destinationIndex;
    }

    protected String submitReindexTask(String sourceIndex) {
        // Den Reindex-request aufbauen, um ihn als Json auszuführen, damit wait_for_completion=false gesetzt werden kann
        ReindexRequest.Builder requestBuilder = new ReindexRequest.Builder();
        requestBuilder.requestsPerSecond(props.getElastic().getReindexRequestsPerSecond());
        requestBuilder.source(s ->
            s.index(sourceIndex).size(props.getElastic().getReindexBatchSize())
        );
        requestBuilder.refresh(true);
        requestBuilder.waitForCompletion(false);
        requestBuilder.dest(d -> d.index(destinationIndex));
        ReindexRequest request = requestBuilder.build();
        try {
            // Das Auslösen eines submitReindexTask ist analog zu einem Reindex-Request mit wait_for_completion=false
            ReindexResponse response = client.reindex(request);
            return response.task();
        } catch (IOException e) {
            // PV-8946 catch-log-rethrow
            log.error(
                "Fehler beim Erstellen der Reindex Task. Destination-Index: {}, Source-Index: {}",
                destinationIndex,
                sourceIndex,
                e
            );
            throw new UncheckedIOException(e);
        }
    }

    protected void checkCurrentRunningTasks(String taskId) {
        try (
            ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor()
        ) {
            scheduledExecutorService.scheduleWithFixedDelay(
                MdcTemplate.wrap(() -> {
                    boolean finished = requestTask(taskId);
                    if (finished) {
                        scheduledExecutorService.shutdown();
                    }
                }),
                500,
                5000,
                TimeUnit.MILLISECONDS
            );
            while (!scheduledExecutorService.isTerminated()) {
                try {
                    if (scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        break;
                    }
                    log.info("Warten auf Reindex mit der ID {}", taskId);
                } catch (InterruptedException e) {
                    log.error(
                        "Fehler im Reindex-Task {} mit der Fehlermeldung: ",
                        taskId,
                        e.getCause()
                    );
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e); // NOSONAR - PV-8946 catch-log-rethrow
                }
            }
        }
    }

    private boolean requestTask(String taskId) {
        try {
            // Holt die aktuelle Liste an Tasks aus der Elasticsearch
            ListResponse response = client.tasks().list(l -> l.actions("*reindex").detailed(true));

            // Filtere die potenziellen Fehler
            Optional<String> oFailure = response
                .nodes()
                .values()
                .stream()
                .filter(n -> n.tasks().containsKey(taskId))
                .map(n -> n.tasks().get(taskId))
                .flatMap(s -> extractFailures(s.status()).stream())
                .findFirst();

            if (oFailure.isPresent()) {
                throw new IllegalStateException(oFailure.get());
            }

            if (
                response
                    .nodes()
                    .values()
                    .stream()
                    .noneMatch(node -> node.tasks().containsKey(taskId))
            ) {
                // Task nicht mehr gefunden
                return true;
            }
        } catch (Exception e) {
            log.error("Fehler im Reindex-Task {} mit der Fehlermeldung:", taskId, e.getCause());
            throw new IllegalStateException(e); // NOSONAR - PV-8946 catch-log-rethrow
        }
        return false;
    }

    protected void checkFinishedTasks(String taskId) {
        // Prüfen, ob der Task fehlerfrei durchgelaufen ist
        try {
            GetTasksResponse taskResponse = client.tasks().get(r -> r.taskId(taskId));

            var resp = taskResponse.response();
            List<String> failures = resp == null ? null : extractFailures(resp);
            if (ObjectUtils.isNotEmpty(failures)) {
                throw new RuntimeException(
                    // NOSONAR
                    String.format(
                        "Fehler beim Reindizieren. Task-Id: %s | Failures: %s",
                        taskId,
                        failures
                    )
                );
            } else if (taskResponse.error() != null) {
                throw new RuntimeException(
                    // NOSONAR
                    String.format(
                        "Fehler beim Reindizieren. Task-Id: %s | Error: %s",
                        taskId,
                        taskResponse.error().reason()
                    )
                );
            } else {
                log.info("Task erfolgreich. {}", taskResponse);
            }
        } catch (Exception e) {
            throw new RuntimeException(e); // NOSONAR - PV-8946 catch-rethrow
        }
    }

    protected void updateDestIndexSettings(IndexSettings settings) {
        try {
            PutIndicesSettingsResponse settingsResponse = client
                .indices()
                .putSettings(r -> r.index(destinationIndex).settings(settings));
            boolean acknowledged = settingsResponse.acknowledged();
            if (!acknowledged) {
                throw new RuntimeException(settingsResponse.toString()); // NOSONAR
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e); // PV-8946 catch-rethrow
        }
    }

    protected IndexSettings getSettings() {
        try {
            GetIndicesSettingsResponse settingsResponse = client
                .indices()
                .getSettings(r -> r.index(destinationIndex));
            IndexSettings settings = settingsResponse.get(destinationIndex).settings();
            return Objects.nonNull(settings.index()) ? settings.index() : settings;
        } catch (IOException e) {
            // PV-8946 catch-rethrow
            throw new UncheckedIOException(e);
        }
    }

    private List<String> extractFailures(JsonData status) {
        if (status == null) {
            log.info(
                "JSON Antwort von Elasticsearch konnte nicht verarbeitet werden, kein Status empfangen!"
            );
            return List.of();
        }
        JsonObject statusObject = status.toJson().asJsonObject();
        JsonValue failureObject = statusObject.get("failures");
        if (failureObject == null) {
            return List.of();
        }
        JsonArray failures = failureObject.asJsonArray();
        return failures
            .stream()
            .filter(s -> s.getValueType().equals(JsonValue.ValueType.STRING))
            .map(JsonString.class::cast)
            .map(JsonString::getString)
            .toList();
    }
}
