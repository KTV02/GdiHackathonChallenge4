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
package de.dataport.suchdienst.backoffice.controller;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.exception.AdminElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import de.dataport.suchdienst.util.PageRequestCheck;
import de.dataport.suchdienst.util.ResultPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpunkte fuer die Administrationsanwendung
 */
@Slf4j
@RestController
@RequestMapping(value = "admin/backoffice", produces = "application/json")
@Tag(
    name = "Backoffice",
    description = "Alle Endpunkte fuer das Steuern der Suchdienste stehen hier " +
        "z.B einer Backoffice-Anwendung bereit. Diese Endpunkte sind mit oauth2 gesichert."
)
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminController implements PageRequestCheck {

    private final ElasticInternalReadConnector readConnector;
    private final BkgGeoLocationResolver geo;
    private final MessageSource messageSource;

    public AdminController(
        ElasticInternalReadConnector readConnector,
        BkgGeoLocationResolver geo,
        MessageSource messageSource
    ) {
        this.readConnector = readConnector;
        this.geo = geo;
        this.messageSource = messageSource;
    }

    @GetMapping(value = "elastic-status/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "ElasticSearch health",
        description = "Zeigt den health Status von der Elastic Search an."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Health status",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(implementation = ElasticHealthResponse.class)
                    ),
                    examples = @ExampleObject(
                        value = """
                        [
                            {
                                "epoch": "1658315331",
                                "timestamp": "11:08:51",
                                "cluster": "elasticsearch",
                                "status": "yellow",
                                "node.total": "1",
                                "node.data": "1",
                                "shards": "53",
                                "pri": "53",
                                "relo": "0",
                                "init": "0",
                                "unassign": "44",
                                "pending_tasks": "0",
                                "max_task_wait_time": "-",
                                "active_shards_percent": "54.6%"
                            }
                        ]"""
                    ) // @ExampleObject
                ) // @Content
            ),
            @ApiResponse(
                responseCode = "401",
                description = "unauthorized",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                        value = """
                        {
                            "timestamp": "2022-12-06T07:00:00.000000000Z",
                            "code": "UNAUTHORIZED",
                            "message": "Sie haben keine Autorisation fuer diese Ressource.",
                            "requestId": "16110a27-70b2-422d-82d3-0937c41a6ead",
                            "codeNumber": 1916
                        }"""
                    ) // @ExampleObject
                ) //  @Content
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim Abruf der Daten aus Elasticsearch",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                        value = """
                        {
                            "timestamp": "2022-11-02T07:42:51.522915300Z",
                            "code": "ELSE_IO_ERROR",
                            "message": "Fehler beim Abrufen der angeforderten ELSE-Daten",
                            "codeNumber": 1301
                        }"""
                    ) // @ExampleObject
                ) //  @Content
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Backoffice.ES_HEALTH)
    public String getElasticSearchHealth() {
        log.info("ADMIN: Lade Status von Elasticsearch.");
        try {
            return readConnector.clusterHealth();
        } catch (IOException e) {
            throw new AdminElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                e instanceof ConnectException
                    ? "Fehler beim Herstellen der Verbindung zu Elasticsearch"
                    : "Fehler beim Abrufen der angeforderten ELSE-Daten",
                e
            );
        }
    }

    @GetMapping(value = "elastic-status/indices", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Ruft die ElasticSearch-Indizes ab",
        description = "Ruft die ElasticSearch-Indizes ab"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Indizes-Status",
                content = @Content(
                    examples = @ExampleObject(
                        value = """
                        [
                            {
                                "health": "yellow",
                                "status": "open",
                                "index": "suchdienst_zustaendigkeiten",
                                "uuid": "LEF5fgcKSnGBi12mZ7Spew",
                                "pri": "1",
                                "rep": "1",
                                "docs.count": "0",
                                "docs.deleted": "0",
                                "store.size": "226b",
                                "pri.store.size": "226b"
                            },
                            {
                                "health": "green",
                                "status": "open",
                                "index": ".geoip_databases",
                                "uuid": "oOypKaW0TcCIujm2zIwb7w",
                                "pri": "1",
                                "rep": "0",
                                "docs.count": "40",
                                "docs.deleted": "0",
                                "store.size": "38mb",
                                "pri.store.size": "38mb"
                            }
                        ]"""
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Fehler beim Abruf der Daten aus Elasticsearch",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                        value = """
                        {
                            "timestamp": "2022-11-02T07:42:51.522915300Z",
                            "code": "ELSE_IO_ERROR",
                            "message": "Fehler beim Abrufen der angeforderten ELSE-Daten",
                            "codeNumber": 1301
                        }"""
                    )
                )
            ),
        }
    )
    @PreAuthorize(AuthorityRoles.Backoffice.ES_INDICES)
    public String getElasticSearchDataIndices() {
        log.info("ADMIN: Lade Indizes aus Elasticsearch.");
        try {
            return readConnector.clusterIndices();
        } catch (IOException e) {
            throw new AdminElasticSearchException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                e instanceof ConnectException
                    ? "Fehler beim Herstellen der Verbindung zu Elasticsearch"
                    : "Fehler beim Abrufen der angeforderten ELSE-Daten",
                e
            );
        }
    }

    // elastic search mapping not greater than 10000 must be validated

    @GetMapping(value = "elastic-search/codelists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gibt die Codelisten als Seiten zurück",
        description = "Gibt die Codelisten als Seiten zurück"
    )
    @PreAuthorize(AuthorityRoles.Codelist.READ)
    public ResponseEntity<ResultPage<CodelistEntry>> getCodelists(
        @RequestParam("page") @Valid @Max(200) int page,
        @RequestParam("size") @Valid @Max(9999) int pageSize
    ) {
        log.info("ADMIN: Laden von Codelisteneinträgen.");
        checkPageParameters(page, pageSize, messageSource);
        try {
            PageRequest pageRequest = PageRequest.of(page, pageSize);
            HitsMetadata<CodelistEntry> response = readConnector.searchByIndex(
                IndexNames.CODELISTENTRIES,
                page,
                pageSize,
                null,
                null,
                List.of(),
                CodelistEntry.class
            );
            List<CodelistEntry> entries = response.hits().stream().map(Hit::source).toList();
            return ResponseEntity.ok().body(
                ResultPage.from(entries, response.total().value(), pageRequest)
            );
        } catch (Exception e) {
            // PV-8946 catch-log-return-error
            log.warn("ADMIN - FEHLER: Fehler beim Suchen nach Codelisteneinträgen.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(
        value = "elastic-search/codelists/stats",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Ruft zusammenfassende Infos über den Codelisten-Index ab",
        description = "Ruft zusammenfassende Infos über den Codelisten-Index ab"
    )
    @PreAuthorize(AuthorityRoles.Codelist.STATS)
    public ResponseEntity<String> getCodelistsStatistics() {
        log.info("ADMIN: Laden von Codelisten.");
        try {
            String response = readConnector.getCodeListAggregations();
            return ResponseEntity.ok().body(response);
        } catch (Exception ex) {
            // PV-8946 catch-log-return-error
            log.warn("ADMIN - FEHLER: Fehler beim Laden von Codelisten.", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "geocoding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Test-Zugriff fuer Geocoding",
        description = "Test-Zugriff fuer Geocoding",
        tags = "Temp-API"
    )
    @PreAuthorize(AuthorityRoles.Backoffice.GEOCODING)
    public ResponseEntity<GeoLocation> geocode(
        @RequestParam("city") String city,
        @RequestParam("zip") String zip,
        @RequestParam("number") String number,
        @RequestParam("street") String street
    ) {
        log.info(
            "ADMIN: Stadt {}, Postleitzahl {}, Strasse {}, Hausnummer {} in Geokoordinaten umwandeln.",
            city,
            zip,
            street,
            number
        );
        Address a = Address.builder()
            .street(street)
            .houseNumber(number)
            .zip(zip)
            .city(city)
            .build();
        return geo
            .resolve(a)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
