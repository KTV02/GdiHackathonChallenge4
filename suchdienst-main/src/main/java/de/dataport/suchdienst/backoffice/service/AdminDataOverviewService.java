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
package de.dataport.suchdienst.backoffice.service;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.backoffice.service.contributor.DataOverviewContributor;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.MdcTemplate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrator für die Seite "Datenübersicht" im Backoffice.
 * <p>
 * Erstellt pro Bundesland (und ggf. zusätzlich entdeckter SchemeAgencyIds) einen
 * {@link DataOverviewOutModel.Builder}, lässt alle registrierten
 * {@link DataOverviewContributor Contributors} ihre Daten eintragen und baut
 * daraus die finalen, immutablen {@link DataOverviewOutModel Records}.
 */
@Slf4j
@Service
public class AdminDataOverviewService {

    private final List<DataOverviewContributor> contributors;
    private final ElasticInternalReadConnector elasticReadConnector;

    public AdminDataOverviewService(
        List<DataOverviewContributor> contributors,
        ElasticInternalReadConnector elasticReadConnector
    ) {
        this.contributors = contributors;
        this.elasticReadConnector = elasticReadConnector;
    }

    public List<DataOverviewOutModel> getDataOverview() {
        // 1. Für jedes Bundesland einen Builder anlegen:
        Map<String, DataOverviewOutModel.Builder> builders = new LinkedHashMap<>();
        Arrays.stream(Bundeslaender.values()).forEach(bl ->
            builders.put(
                bl.getSchemeAgencyId(),
                DataOverviewOutModel.builder()
                    .fedState(bl.getArsFederalstate())
                    .name(bl.getFederalstateName())
                    .schemeAgencyId(bl.getSchemeAgencyId())
            )
        );

        // 2. Producer aus den XZuFi-Indices entdecken — ggf. neue SchemeAgencyIds hinzufügen:
        discoverProducers(builders);

        // 3. Phase 1 – rohe Zähler parallel sammeln und anschließend sequentiell mergen:
        collectDataInParallel(builders);

        // 4. Phase 2 – abgeleitete Zustände berechnen (darf Phase-1-Ergebnisse lesen):
        contributors.forEach(contributor -> contributor.deriveState(builders));

        // 5. Builder → Records, sortiert zurückgeben:
        return builders
            .values()
            .stream()
            .map(DataOverviewOutModel.Builder::build)
            .sorted(
                Comparator.comparing(DataOverviewOutModel::hasFedState)
                    .reversed()
                    .thenComparing(DataOverviewOutModel::fedState)
                    .thenComparing(DataOverviewOutModel::name)
            )
            .toList();
    }

    private void collectDataInParallel(Map<String, DataOverviewOutModel.Builder> builders) {
        Set<String> schemeAgencyIds = Set.copyOf(builders.keySet());
        var mdcContext = MdcTemplate.currentContext();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Consumer<Map<String, DataOverviewOutModel.Builder>>>> futures = contributors
                .stream()
                .map(contributor ->
                    executor.submit(() ->
                        MdcTemplate.callWithContext(mdcContext, () ->
                            contributor.collectDataParallel(schemeAgencyIds)
                        )
                    )
                )
                .toList();

            List<Consumer<Map<String, DataOverviewOutModel.Builder>>> updates = futures
                .stream()
                .map(this::getOrCancelAll)
                .toList();

            updates.forEach(update -> update.accept(builders));
        }
    }

    private Consumer<Map<String, DataOverviewOutModel.Builder>> getOrCancelAll(
        Future<Consumer<Map<String, DataOverviewOutModel.Builder>>> future
    ) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Thread interrupted while collecting data overview in parallel",
                e
            );
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to collect data overview in parallel", e);
        }
    }

    /**
     * Entdeckt Producer (Redaktionssysteme) aus den XZuFi-Indices und legt für bisher unbekannte
     * SchemeAgencyIds neue Builder an.
     */
    private void discoverProducers(Map<String, DataOverviewOutModel.Builder> builders) {
        Map<String, String> agencyIdToProducer = // NOSONAR - S1854 - erhöht so die Lesbarkeit
            elasticReadConnector.getDataOverviewAgencyIdAndProducer(IndexNames.XZUFI_INDICES);

        agencyIdToProducer.forEach((schemeAgencyId, producerName) ->
            builders.compute(schemeAgencyId, (_, existing) -> {
                var builder = Objects.requireNonNullElseGet(existing, () ->
                    DataOverviewOutModel.builder().schemeAgencyId(schemeAgencyId).name(producerName)
                );
                builder.name(producerName);
                return builder;
            })
        );
    }
}
