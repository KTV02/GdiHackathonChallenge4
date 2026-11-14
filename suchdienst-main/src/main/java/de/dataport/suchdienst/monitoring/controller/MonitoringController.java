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
package de.dataport.suchdienst.monitoring.controller;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.elasticsearch.api.Elastic.QueryHelper;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.ApiWarnException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.monitoring.model.MonitoringTransactionMetaData;
import de.dataport.suchdienst.monitoring.model.out.MonitoringResponse;
import de.dataport.suchdienst.monitoring.model.out.SearchAfterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "monitoring", produces = "application/json")
@Tag(name = "Monitoring-API zum validieren der Datenkonsistenz")
@Validated
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class MonitoringController {

    public enum DataType {
        LB,
        OE,
        OD,
        SP,
        FO,
        ZSOE,
        ZSOD,
        ZSFO,
    }

    private static final SourceConfig CTX_TX_ONLY_OWN_METADATA = SourceConfig.of(c ->
        c.filter(f -> f.includes("id", "metaData"))
    );

    private static final Query MATCH_ALL_QUERY = QueryBuilders.matchAll().build()._toQuery();
    private static final Function<String, Query> AGENCYID_QUERY_FUNCTION = schemeAgencyId ->
        QueryHelper.term("metaData.schemeAgencyId", schemeAgencyId);

    private final ElasticInternalReadConnector internalReadConnector;
    private final ElasticReadConnector readConnector;

    public MonitoringController(
        ElasticInternalReadConnector internalReadConnector,
        ElasticReadConnector readConnector
    ) {
        this.internalReadConnector = internalReadConnector;
        this.readConnector = readConnector;
    }

    @GetMapping(value = "import-active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Prüft, ob für eine SchemeAgencyId aktuell ein Import läuft.")
    @PreAuthorize(AuthorityRoles.Testing.IMPORT_ACTIVE)
    public ResponseEntity<Boolean> isImportActiveForSchemeAgencyId(
        @RequestParam(value = "scheme_agency_id") String schemeAgencyId
    ) {
        if (internalReadConnector.countPendingImportQueue(schemeAgencyId) > 0) {
            return ResponseEntity.ok(Boolean.TRUE);
        } else {
            return ResponseEntity.ok(Boolean.FALSE);
        }
    }

    @GetMapping(value = "countVO", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Zählt alle Geschäftsobjekte einer Art für eine SchemeAgencyId")
    @PreAuthorize(AuthorityRoles.Testing.SD_IDS)
    public ResponseEntity<Long> countVO(
        @RequestParam("objektTyp") DataType objectType,
        @RequestParam(value = "schemeAgencyId") String schemeAgencyId
    ) {
        long count = switch (objectType) {
            case LB -> internalReadConnector.countDocs(
                IndexNames.LEISTUNGSBESCHREIBUNG,
                AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
            );
            case OE -> internalReadConnector.countDocs(
                IndexNames.ORGANISATIONSEINHEITEN,
                AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
            );
            case OD -> internalReadConnector.countDocs(
                IndexNames.ONLINEDIENSTE,
                AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
            );
            case FO -> internalReadConnector.countDocs(
                IndexNames.FORMULARE,
                AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
            );
            case SP -> internalReadConnector.countDocs(
                IndexNames.SPEZIALISIERUNGEN,
                AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
            );
            case ZSOE -> internalReadConnector.countDocs(
                IndexNames.ZUSTAENDIGKEITEN,
                QueryBuilders.bool()
                    .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                    .filter(QueryHelper.term("typ", RelationTyp.ORGANISATION.name()))
                    .build()
                    ._toQuery()
            );
            case ZSOD -> internalReadConnector.countDocs(
                IndexNames.ZUSTAENDIGKEITEN,
                QueryBuilders.bool()
                    .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                    .filter(QueryHelper.term("typ", RelationTyp.ONLINESERVICE.name()))
                    .build()
                    ._toQuery()
            );
            case ZSFO -> internalReadConnector.countDocs(
                IndexNames.ZUSTAENDIGKEITEN,
                QueryBuilders.bool()
                    .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                    .filter(QueryHelper.term("typ", RelationTyp.FORM.name()))
                    .build()
                    ._toQuery()
            );
        };
        return ResponseEntity.ok(count);
    }

    @GetMapping(value = "getVO", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gibt alle Ids der LBs zurueck")
    @PreAuthorize(AuthorityRoles.Testing.SD_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getVO(
        @RequestParam("objektTyp") DataType objectType,
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return switch (objectType) {
            case LB -> getLbIds(size, schemeAgencyId, lastId);
            case OE -> getOeIds(size, schemeAgencyId, lastId);
            case OD -> getOdIds(size, schemeAgencyId, lastId);
            case FO -> getFoIds(size, schemeAgencyId, lastId);
            case SP -> getSpIds(size, schemeAgencyId, lastId);
            case ZSOE -> getZsOeIds(size, schemeAgencyId, lastId);
            case ZSOD -> getZsOdIds(size, schemeAgencyId, lastId);
            case ZSFO -> getZsFoIds(size, schemeAgencyId, lastId);
        };
    }

    @GetMapping(value = "lb", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der LBs zurueck",
        description = "Gibt alle Ids der LBs zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.SD_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getLbIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return executeTestRequest(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            size,
            lastId,
            StringUtils.isNotEmpty(schemeAgencyId)
                ? AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
                : MATCH_ALL_QUERY
        );
    }

    @GetMapping(value = "oe", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der OEs zurueck",
        description = "Gibt alle Ids der OEs zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.OU_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getOeIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return executeTestRequest(
            IndexNames.ORGANISATIONSEINHEITEN,
            size,
            lastId,
            StringUtils.isNotEmpty(schemeAgencyId)
                ? AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
                : MATCH_ALL_QUERY
        );
    }

    @GetMapping(value = "fo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der FOs zurueck",
        description = "Gibt alle Ids der FOs zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.FO_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getFoIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return executeTestRequest(
            IndexNames.FORMULARE,
            size,
            lastId,
            StringUtils.isNotEmpty(schemeAgencyId)
                ? AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
                : MATCH_ALL_QUERY
        );
    }

    @GetMapping(value = "sp", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der SPs zurueck",
        description = "Gibt alle Ids der SPs zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.SP_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getSpIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return executeTestRequest(
            IndexNames.SPEZIALISIERUNGEN,
            size,
            lastId,
            StringUtils.isNotEmpty(schemeAgencyId)
                ? AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
                : MATCH_ALL_QUERY
        );
    }

    @GetMapping(value = "od", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der Ods zurueck",
        description = "Gibt alle Ids der Ods zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.OD_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getOdIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        return executeTestRequest(
            IndexNames.ONLINEDIENSTE,
            size,
            lastId,
            StringUtils.isNotEmpty(schemeAgencyId)
                ? AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId)
                : MATCH_ALL_QUERY
        );
    }

    @GetMapping(value = "zsoe", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der Zs-Oes zurueck",
        description = "Gibt alle Ids der Zs-Oes zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.OU_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getZsOeIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        Query query;
        if (StringUtils.isNotEmpty(schemeAgencyId)) {
            query = QueryBuilders.bool()
                .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                .filter(QueryHelper.term("typ", RelationTyp.ORGANISATION.name()))
                .build()
                ._toQuery();
        } else {
            query = QueryBuilders.term(t -> t.field("typ").value(RelationTyp.ORGANISATION.name()));
        }
        return executeTestRequest(IndexNames.ZUSTAENDIGKEITEN, size, lastId, query);
    }

    @GetMapping(value = "zsod", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der Zs-Ods zurueck",
        description = "Gibt alle Ids der Zs-Ods zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.OD_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getZsOdIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        Query query;
        if (StringUtils.isNotEmpty(schemeAgencyId)) {
            query = QueryBuilders.bool()
                .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                .filter(QueryHelper.term("typ", RelationTyp.ONLINESERVICE.name()))
                .build()
                ._toQuery();
        } else {
            query = QueryBuilders.term(t -> t.field("typ").value(RelationTyp.ONLINESERVICE.name()));
        }
        return executeTestRequest(IndexNames.ZUSTAENDIGKEITEN, size, lastId, query);
    }

    @GetMapping(value = "zsfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt alle Ids der Zs-FOs zurueck",
        description = "Gibt alle Ids der Zs-FOs zurueck"
    )
    @PreAuthorize(AuthorityRoles.Testing.FO_IDS)
    public ResponseEntity<MonitoringResponse<Identifiable, String>> getZsFoIds(
        @RequestParam("size") @Min(1) @Max(Elastic.WINDOW_SIZE - 1) int size,
        @RequestParam(value = "schemeAgencyId", required = false) String schemeAgencyId,
        @RequestParam(value = "lastId", required = false) String lastId
    ) {
        Query query;
        if (StringUtils.isNotEmpty(schemeAgencyId)) {
            query = QueryBuilders.bool()
                .filter(AGENCYID_QUERY_FUNCTION.apply(schemeAgencyId))
                .filter(QueryHelper.term("typ", RelationTyp.FORM.name()))
                .build()
                ._toQuery();
        } else {
            query = QueryBuilders.term(t -> t.field("typ").value(RelationTyp.FORM.name()));
        }
        return executeTestRequest(IndexNames.ZUSTAENDIGKEITEN, size, lastId, query);
    }

    private ResponseEntity<MonitoringResponse<Identifiable, String>> executeTestRequest(
        IndexNames index,
        int size,
        @Nullable String lastId,
        Query query
    ) {
        return searchAfter(index, size, lastId, query)
            .map(response -> ResponseEntity.ok().body(response))
            .orElseThrow(() ->
                new ApiWarnException(
                    ErrorCodeEnum.UNKNOWN_ERROR,
                    "Fehler in der Monitoring-API",
                    ExceptionType.TECHNISCH
                )
            );
    }

    private Optional<MonitoringResponse<Identifiable, String>> searchAfter(
        IndexNames index,
        int searchSize,
        @Nullable String lastId,
        Query query
    ) {
        try {
            HitsMetadata<MonitoringTransactionMetaData> searchHits = readConnector.searchAfter(
                index,
                lastId,
                searchSize,
                query,
                MonitoringController.CTX_TX_ONLY_OWN_METADATA,
                true,
                MonitoringTransactionMetaData.class
            );

            int hitCount = searchHits.hits().size();
            Long totalCount = searchHits.total().value();
            final boolean isLast = hitCount != searchSize;

            List<Identifiable> list = searchHits
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(Identifiable.class::cast)
                .toList();
            var response = SearchAfterResponse.from(list, Identifiable::getId);
            return Optional.of(new MonitoringResponse<>(response.withLast(isLast), totalCount));
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Suchen in Index: {}.", index, e);
            return Optional.empty();
        }
    }
}
