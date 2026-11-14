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
package de.dataport.suchdienst.elasticsearch.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.esql.objects.ObjectsEsqlAdapter;
import co.elastic.clients.elasticsearch._types.FieldValue;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
public class ElasticQueryConnector {

    private final ElasticsearchClient client;
    private final IndexConfiguration indexConfiguration;

    record ArsResult(String ars) {}

    public ElasticQueryConnector(
        ElasticsearchClient client,
        IndexConfiguration indexConfiguration
    ) {
        this.client = client;
        this.indexConfiguration = indexConfiguration;
    }

    public List<String> findArsByOeIdAndLeikaId(String leikaId, String oeId) {
        String lbIndex = indexConfiguration.getCurrentIndexName(IndexNames.LEISTUNGSBESCHREIBUNG);
        String zsIndex = indexConfiguration.getCurrentIndexName(IndexNames.ZUSTAENDIGKEITEN);
        var query = """
        FROM %s
        | KEEP id, leikaId
        | MV_EXPAND leikaId
        | WHERE leikaId == ?
        | RENAME id AS lbId
        | LOOKUP JOIN %s ON lbId
        | WHERE parentId == ?
        | KEEP ars
        | SORT ars
        | LIMIT 10000
        """.formatted(lbIndex, zsIndex);
        log.debug("Executing ES|QL query:\n{}", query);

        StopWatch sw = new StopWatch();
        sw.start();
        List<ArsResult> arsList = executeQuery(query, ArsResult.class, leikaId, oeId);
        sw.stop();

        log.debug(
            "{} ms ES|QL query finished with {} results",
            sw.getTotalTimeMillis(),
            arsList.size()
        );
        return arsList.stream().map(ArsResult::ars).distinct().toList();
    }

    private <T> List<T> executeQuery(String query, Class<T> clazz, Object... parameters) {
        var parameterValues = Arrays.stream(parameters).map(FieldValue::of).toList();
        try {
            var esqlAdapter = ObjectsEsqlAdapter.of(clazz);
            return (List<T>) client
                .esql()
                .query(esqlAdapter, q -> q.query(query).params(parameterValues));
        } catch (Exception e) {
            throw new ElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                String.format("Fehler bei ES:QL Query:%n%s", query),
                e
            );
        }
    }
}
