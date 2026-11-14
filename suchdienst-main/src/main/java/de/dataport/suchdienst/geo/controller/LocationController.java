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
package de.dataport.suchdienst.geo.controller;

import co.elastic.clients.elasticsearch.core.search.CompletionSuggest;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Iterators;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.geo.model.mapper.LocationMapper;
import de.dataport.suchdienst.geo.model.out.ArsObject;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "Orte",
    description = "Um den für die Zuständigkeit notwendigen ARS zu ermitteln, kann die Ortssuche" +
        " des Suchdienstes verwendet werden, sofern der ARS des Suchenden sich nicht aus einem anderen " +
        "Kontext ermitteln lässt. Dazu bietet der Suchdienst die folgenden Schnittstellen."
)
public class LocationController {

    private final ElasticPublicSearchConnector publicSearchConnector;

    public LocationController(ElasticPublicSearchConnector publicSearchConnector) {
        this.publicSearchConnector = publicSearchConnector;
    }

    @Value("${de.dataport.suchdienst.endpoint.enableDeprecated.v2-locations:false}")
    private boolean endpointV2LocationsActive;

    @Value("${de.dataport.suchdienst.endpoint.enableDeprecated.v2-locations-suggestions:false}")
    private boolean endpointV2LocationsSuggestionsActive;

    /**
     * @deprecated Wird perspektivisch aus der API entfernt
     */
    @Deprecated(forRemoval = true, since = "11.12.0")
    @GetMapping("/v2/locations")
    @Operation(
        summary = "Liefert eine Liste von Ortsdaten aus dem ARS-Index zurück",
        description = "Liefert anhand des Wertes der Suchanfrage Orte aus dem ARS-Index zurück"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Orten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ArsObject.class))
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die Eingabeparameter sind ungültig.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            """
                                {
                                    "timestamp": "2022-10-11T08:57:53.216502100Z",
                                    "code": "BAD_REQUEST",
                                    "message": "Die übermittelten Request-Pfad-Parameter entsprechen nicht den Anforderungen.",
                                    "codeNumber": 1901
                                }
                            """
                        ),
                    }
                )
            ),
        }
    )
    @JsonView(JsonViews.Detail.class)
    public ResponseEntity<List<ArsObject>> getLocationsV2(
        @Parameter(
            description = "Der Name (Ort/Ortsteil), die PLZ oder der ARS, nach der/dem gesucht werden soll. Die Angabe nur des Stadtnamens kann in einzelnen Bundesländern nicht eindeutig interpretiert werden.",
            required = true
        ) @RequestParam(value = "q") @Size(min = 2) final String searchQuery,
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "limit",
            required = false,
            defaultValue = "10"
        ) @Valid @Min(1) @Max(10) final Integer limit,
        @Parameter(
            description = "Gibt an, ob per Fuzzy Search Rechtschreibfehler korrigiert werden sollen."
        ) @RequestParam(
            value = "fuzzysearch",
            required = false,
            defaultValue = "false"
        ) final boolean fuzzysearch
    ) {
        if (!endpointV2LocationsActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v2/locations' ist nicht länger aktiv."
            );
        }
        List<de.dataport.suchdienst.indexer.model.ArsObject> hits =
            publicSearchConnector.searchLocation(searchQuery, 0, limit, fuzzysearch);
        return ResponseEntity.ok(hits.stream().map(LocationMapper.INSTANCE::mapArsObject).toList());
    }

    /**
     * @deprecated Wird perspektivisch aus der API entfernt
     */
    @Deprecated(forRemoval = true, since = "11.12.0")
    @GetMapping("/v2/locations/suggestions")
    @Operation(
        summary = "Liefert eine Liste von Orten",
        description = "Liefert Autovervollständigungs-Vorschläge für den Suchbegriff zurück (Präfixsuche)."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Vorschlägen zurück.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ArsObject.class))
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Es wurde eine falsche Anfrage gestellt",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            """
                                {
                                    "timestamp": "2022-10-11T09:13:41.002318600Z",
                                    "code": "BAD_REQUEST",
                                    "message": "Die übermittelten Request-Pfad-Parameter entsprechen nicht den Anforderungen.",
                                    "codeNumber": 1901
                                }
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Interner Fehler, z.B. bei der Suche in ElasticSearch",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            """
                                {
                                    "timestamp": "2022-10-11T09:15:31.346674500Z",
                                    "code": "ELSE_IO_ERROR",
                                    "message": "Fehler bei der Suche in ElasticSearch",
                                    "codeNumber": 1301
                                }
                            """
                        ),
                    }
                )
            ),
        }
    )
    @JsonView(JsonViews.Search.class)
    public Iterator<ArsObject> autocompleteLocationsV2(
        @Parameter(
            description = "Präfix des Ortsnamens (Ort/Ortsteil) oder der PLZ für die Autovervollständigung. Die Angabe nur des Stadtnamens kann in einzelnen Bundesländern nicht eindeutig interpretiert werden.",
            example = "Neumün",
            required = true
        ) @RequestParam(value = "q") @Size(min = 1, max = 50) final String searchQuery,
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "size",
            required = false,
            defaultValue = "11"
        ) @Min(value = 10) final int size,
        @Parameter(
            description = "Aktiviert die unscharfe Fuzzy-Suche, die auch Teilübereinstimmungen akzeptiert"
        ) @RequestParam(
            value = "fuzzySearch",
            required = false,
            defaultValue = "false"
        ) final boolean fuzzySearch,
        @Parameter(
            description = "Fuzzy-Ergebnisse vor (true) oder nach (false) normalen Treffern einordnen"
        ) @RequestParam(
            value = "fuzzyFirst",
            required = false,
            defaultValue = "false"
        ) final boolean fuzzyFirst
    ) {
        if (!endpointV2LocationsSuggestionsActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v2/locations/suggestions' ist nicht länger aktiv."
            );
        }
        List<CompletionSuggest<de.dataport.suchdienst.indexer.model.ArsObject>> suggestionResponse;
        try {
            suggestionResponse = publicSearchConnector.suggestLocation(
                searchQuery,
                size,
                fuzzySearch
            );
        } catch (ElasticSearchException e) {
            // PV-8946 catch-rethrow
            log.warn(e.getMessage(), e);
            throw new SuchenException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler bei der Suche in ElasticSearch",
                e
            );
        }

        List<de.dataport.suchdienst.indexer.model.ArsObject> suggestions = new ArrayList<>();
        List<de.dataport.suchdienst.indexer.model.ArsObject> exact = new ArrayList<>();
        List<de.dataport.suchdienst.indexer.model.ArsObject> fuzzy = new ArrayList<>();

        for (CompletionSuggest<de.dataport.suchdienst.indexer.model.ArsObject> suggestion : suggestionResponse) {
            for (CompletionSuggestOption<de.dataport.suchdienst.indexer.model.ArsObject> option : suggestion.options()) {
                de.dataport.suchdienst.indexer.model.ArsObject obj = option.source();
                if (obj.getName().equalsIgnoreCase(searchQuery)) {
                    exact.add(obj);
                } else if (
                    fuzzySearch && !StringUtils.startsWithIgnoreCase(obj.getName(), searchQuery)
                ) {
                    fuzzy.add(obj);
                } else {
                    suggestions.add(normalizeZipCode(obj, option.text()));
                }
            }
        }

        List<ArsObject> all = fuzzyFirst
            ? Stream.of(fuzzy, exact, suggestions)
                  .flatMap(Collection::stream)
                  .map(LocationMapper.INSTANCE::mapArsObject)
                  .toList()
            : Stream.of(exact, suggestions, fuzzy)
                  .flatMap(Collection::stream)
                  .map(LocationMapper.INSTANCE::mapArsObject)
                  .toList();
        return Iterators.limit(
            all.iterator(),
            Math.min(size, exact.size() + suggestions.size() + fuzzy.size())
        );
    }

    /**
     * sorgt dafür, dass bei einer Suche nach PLZ auch tatsächlich die gesuchte PLZ geliefert wird und
     * nicht eine andere PLZ des Ortes oder ein Muster
     *
     * @param obj           ein gefundener Ort
     * @param suggestedText der Text, auf den der Suchbegriff gematcht hat
     * @return den gefundenen Ort mit ggf. modifizierter PLZ
     */
    private de.dataport.suchdienst.indexer.model.ArsObject normalizeZipCode(
        de.dataport.suchdienst.indexer.model.ArsObject obj,
        String suggestedText
    ) {
        // wenn der Text mit einer Zahl beginnt, muss es eine (vollst.) PLZ sein
        if (Character.isDigit(suggestedText.charAt(0))) {
            obj.setDisplayZip(suggestedText.substring(0, 5));
        }
        return obj;
    }
}
