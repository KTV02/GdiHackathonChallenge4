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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
public class RelationService {

    private final ElasticPublicSearchConnector publicSearchConnector;

    public RelationService(ElasticPublicSearchConnector publicSearchConnector) {
        this.publicSearchConnector = publicSearchConnector;
    }

    /**
     * Sucht alle Zustaendigkeiten, die zu den übergebenen LB-IDs und dem übergebenen ARS gehören.
     * @param lbIds Liste von LB-IDs
     * @param ars ARS
     * @param useArsHierarchy true, wenn die Hierarchie der ARS berücksichtigt werden soll
     * @param validDate Datum, für das die Gültigkeit geprüft werden soll
     * @param page Seitennummer
     * @param pageSize Seitengröße
     * @return Liste von ZustaendigkeitTransferObjekten
     */
    public Pair<
        Long,
        List<GenericZustaendigkeitTransferObjekt>
    > getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
        List<String> lbIds,
        String ars,
        boolean useArsHierarchy,
        LocalDate validDate,
        int page,
        int pageSize
    ) {
        var result = searchZustaendigkeitenByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            validDate,
            page,
            pageSize
        );

        var totalHits = result.total() != null ? result.total().value() : 0L;

        var relationEntities = result
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(GenericModelMapper::mapZustaendigkeitTransferObjektEntity)
            .toList();

        return Pair.of(totalHits, relationEntities);
    }

    /**
     * Hilfsmethode, die nach Relations sucht und dann bestimmte Felder darin als Entity zurückliefert.
     * fieldsGetter ist dafür zuständig aus einer Relation die Liste der gewünschten Objekte zu ziehen.
     * Eigentlich hätten wir hiermit ein Paging-Problem, nehmen hier jedoch an, dass es sich immer um einelementige Listen
     * handelt. Zumindest für die bisherigen Anwendungsfälle stimmt das. Wir wollen das ZustaendigkeitTransferObjekt bzw.
     * die (einzige!) in diesem enthaltene Zustaendigkeit haben. ZustaendigkeitTransferObjekt hat zwar formal eine Liste
     * von Zustaendigkeiten, dies wird aber nicht vom anliefernden Sammlerdienst ausgenutzt.
     */
    private HitsMetadata<ZustaendigkeitEntity> searchZustaendigkeitenByLbIdAndArs(
        List<String> lbIds,
        String ars,
        boolean useArsHierarchy,
        LocalDate validDate,
        int page,
        int pageSize
    ) {
        return publicSearchConnector.searchRelationByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            validDate,
            page,
            pageSize
        );
    }
}
