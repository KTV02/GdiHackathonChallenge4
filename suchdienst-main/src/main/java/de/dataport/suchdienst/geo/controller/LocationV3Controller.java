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

import de.dataport.suchdienst.config.CaseInsensitiveEnumEditor;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.geo.model.GeoArsDistinctFilters;
import de.dataport.suchdienst.geo.model.GeoArsPropFilters;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.model.internal.HouseAddress;
import de.dataport.suchdienst.geo.model.mapper.LocationMapper;
import de.dataport.suchdienst.geo.model.out.GeoArsOutModelV3;
import de.dataport.suchdienst.geo.model.out.GeoSuggestionOutModel;
import de.dataport.suchdienst.geo.service.GeoService;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "Orte - BKG",
    description = """
    Um den für die Zuständigkeit notwendigen ARS zu ermitteln, kann die Ortssuche des Suchdienstes verwendet werden,
    sofern der ARS des Suchenden sich nicht aus einem anderen Kontext ermitteln lässt.
    Dazu bietet der Suchdienst die folgenden Schnittstellen.

    **Hinweis:** Die in der Version 3 vorliegenden Schnittstellen binden das Bundesamt für Kartographie und
    Geodäsie (BKG) als Datenquelle an.
    Die ARS entsprechen nicht in allen Fällen denen der ARS-Codeliste im
    xRepository (`urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs`).
    Dies hat zur Folge, dass mit solchen ARS am Suchdienst keine Auflösung von Geschäftsobjekten garantiert werden kann.
    """
)
public class LocationV3Controller {

    private static final java.util.regex.Pattern PATTERN = java.util.regex.Pattern.compile(
        "^[\\p{L}\\p{N}]"
    );

    private static final String DESCRIPTION_BAD_REQUEST = """
        {
            "timestamp": "2022-10-11T08:57:53.216502100Z",
            "code": "BAD_REQUEST",
            "message": "Die übermittelten Request-Pfad-Parameter entsprechen nicht den Anforderungen.",
            "codeNumber": 1901
        }
    """;

    private final GeoService geoService;

    public LocationV3Controller(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/v3/locations")
    @Operation(
        summary = "Liefert anhand eines Suchbegriffs Ortsdaten aus dem BKG zurück.",
        description = "Liefert anhand eines Suchbegriffs Ortsdaten aus dem BKG zurück."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Ortsangaben",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = GeoArsOutModelV3.class)),
                    examples = {
                        @ExampleObject(
                            """
                                [
                                    {
                                        "name": "Mannheim",
                                        "ars": "082220000000",
                                        "arsFederalstate": "08",
                                        "gemeinde": "Mannheim"
                                    },
                                    {
                                        "name": "Mannheim 'Rheinau'",
                                        "ars": "082220000000",
                                        "arsFederalstate": "08",
                                        "plz": "68219",
                                        "strasse": "Hauptstraße",
                                        "hausnummer": "2",
                                        "gemeinde": "Mannheim",
                                        "koordinaten": ["8.484444", "49.452778"]
                                    }
                                ]
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die Eingabeparameter sind ungültig.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class),
                    examples = { @ExampleObject(DESCRIPTION_BAD_REQUEST) }
                )
            ),
        }
    )
    public Stream<GeoArsOutModelV3> getLocationsByQuery(
        @Parameter(
            description = "Spezifischer Standort (bevorzugt Ort/Ortsteil oder PLZ, Min: 1, Max: 70 Zeichen). Die Angabe nur des Stadtnamens kann in einzelnen Bundesländern nicht eindeutig interpretiert werden.",
            example = "Neumün",
            required = true
        ) @RequestParam(value = "q") @Size(min = 1, max = 70) final String searchQuery,
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "limit",
            required = false,
            defaultValue = "10"
        ) @Min(1) @Max(25) final Integer limit,
        @Parameter(
            description = "Gibt die Properties an, die immer enthalten (nicht leer) sein sollen."
        ) @RequestParam(
            value = "filterBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsPropFilters> filterBy,
        @Parameter(description = "Gibt die Properties an, die unique sein sollen.") @RequestParam(
            value = "distinctBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsDistinctFilters> distinctBy,
        @Parameter(
            description = """
            Gibt an, ob die Ortsteile in den Ergebnissen ihren eigenen ARS beibehalten sollen (ortsteil=true)
            oder ob die Ortsteil-ARS durch den Bundesland ARS ersetzt werden soll (ortsteil=false).
            """
        ) @RequestParam(
            value = "ortsteil",
            required = false,
            defaultValue = "true"
        ) boolean ortsteil
    ) {
        if (hasSpecialCharacterAtFirstLetter(searchQuery)) {
            return Stream.of();
        }
        var geoLocationArs = geoService.searchByQuery(
            searchQuery,
            limit,
            filterBy,
            distinctBy,
            ortsteil
        );
        return geoLocationArs.stream().map(LocationMapper.INSTANCE::mapGeoArsV3);
    }

    @GetMapping("/v3/locations/details")
    @Operation(
        summary = "Liefert eine Liste von Ortsdaten aus dem BKG zurück.",
        description = """
        Liefert anhand des Wertes der Suchanfrage Ortsdaten aus dem BKG zurück.
        Es muss mindestens einer der Parameter ort oder plz gefüllt sein.

        Bitte Hinweise zu v3 in der Beschreibung beachten."""
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Ortsdaten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = GeoArsOutModelV3.class)),
                    examples = {
                        @ExampleObject(
                            """
                                [
                                    {
                                        "gemeinde": "Mannheim",
                                        "arsFederalstate": "08",
                                        "ars": "082220000000",
                                        "plz": "68219",
                                        "strasse": "Waldblick",
                                        "name": "Mannheim 'Rheinau'",
                                        "koordinaten": ["8.484444", "49.452778"]
                                    }
                                ]
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die Eingabeparameter sind ungültig.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class),
                    examples = { @ExampleObject(DESCRIPTION_BAD_REQUEST) }
                )
            ),
        }
    )
    public Stream<GeoArsOutModelV3> getLocationsByAddress(
        @Parameter(
            description = "Der Name des Ortes/Ortsteils, nach dem gesucht werden soll"
        ) @RequestParam(value = "ort", required = false) @Size(min = 1, max = 70) final String city,
        @Parameter(description = "Die Postleitzahl, nach der gesucht werden soll") @RequestParam(
            value = "plz",
            required = false
        ) @Size(min = 1, max = 5) @SuppressWarnings("java:S6353") @Pattern(
            // Regex Syntax [0-9] bei Fehlermeldungen
            regexp = "[0-9]*"
        ) final String zip,
        @Parameter(description = "Die Straße, nach der gesucht werden soll") @RequestParam(
            value = "strasse",
            required = false
        ) @Size(min = 1, max = 70) final String street,
        @Parameter(description = "Die Hausnummer, nach der gesucht werden soll") @RequestParam(
            value = "hausnummer",
            required = false
        ) @Size(min = 1, max = 70) final String houseNumber,
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "limit",
            required = false,
            defaultValue = "10"
        ) @Min(1) @Max(25) final Integer limit,
        @Parameter(
            description = "Gibt die Properties an, die immer enthalten (nicht leer) sein sollen."
        ) @RequestParam(
            value = "filterBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsPropFilters> filterBy,
        @Parameter(description = "Gibt die Properties an, die unique sein sollen.") @RequestParam(
            value = "distinctBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsDistinctFilters> distinctBy,
        @Parameter(
            description = """
            Gibt an, ob die Ortsteile in den Ergebnissen ihren eigenen ARS beibehalten sollen (ortsteil=true)
            oder ob die Ortsteil-ARS durch den Bundesland ARS ersetzt werden soll (ortsteil=false)."""
        ) @RequestParam(
            value = "ortsteil",
            required = false,
            defaultValue = "true"
        ) boolean ortsteil
    ) {
        checkLocationSearchParameters(city, zip, street, houseNumber);

        if (hasSpecialCharacterAtFirstLetter(city, zip, street, houseNumber)) {
            return Stream.of();
        }

        var geoLocationArs = geoService.searchByAddress(
            new HouseAddress(city, zip, street, houseNumber),
            limit,
            filterBy,
            distinctBy,
            ortsteil
        );
        return geoLocationArs.stream().map(LocationMapper.INSTANCE::mapGeoArsV3);
    }

    @GetMapping("/v3/locations/ars")
    @Operation(
        summary = "Liefert eine Liste von Ortsdaten aus dem BKG anhand des Amtlichen Regionalschlüssels zurück.",
        description = """
        Liefert anhand des Wertes der Suchanfrage Ortsdaten aus dem BKG zurück.

        Bitte Hinweise zu v3 in der Beschreibung beachten."""
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Ortsdaten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = GeoArsOutModelV3.class)),
                    examples = {
                        @ExampleObject(
                            """
                                [
                                    {
                                        "name": "Mannheim",
                                        "ars": "082220000000",
                                        "arsFederalstate": "08",
                                        "gemeinde": "Mannheim"
                                    },
                                    {
                                        "name": "Mannheim 'Rheinau'",
                                        "ars": "082220000000",
                                        "arsFederalstate": "08",
                                        "plz": "68219",
                                        "gemeinde": "Mannheim",
                                        "koordinaten": ["8.484444", "49.452778"]
                                    }
                                ]
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die Eingabeparameter sind ungültig.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class),
                    examples = { @ExampleObject(DESCRIPTION_BAD_REQUEST) }
                )
            ),
        }
    )
    public Stream<GeoArsOutModelV3> getLocationsByArs(
        @Parameter(
            description = "Der Ars des Ortes, nach dem gesucht werden soll.",
            required = true
        ) @RequestParam(value = "ars") @Size(min = 2, max = 12) @SuppressWarnings(
            "java:S6353"
        ) @Pattern(regexp = "[0-9]*") final String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "limit",
            required = false,
            defaultValue = "10"
        ) @Min(1) @Max(25) final Integer limit,
        @Parameter(
            description = "Gibt die Properties an, die immer enthalten (nicht leer) sein sollen."
        ) @RequestParam(
            value = "filterBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsPropFilters> filterBy,
        @Parameter(description = "Gibt die Properties an, die unique sein sollen.") @RequestParam(
            value = "distinctBy",
            required = false,
            defaultValue = ""
        ) final Set<GeoArsDistinctFilters> distinctBy
    ) {
        var geoLocationArs = geoService.searchByArs(ars, limit, filterBy, distinctBy);
        return geoLocationArs.stream().map(LocationMapper.INSTANCE::mapGeoArsV3);
    }

    @GetMapping("/v3/locations/suggestions")
    @Operation(
        summary = "Liefert eine Liste von Vorschlägen zur Autovervollständigung zurück.",
        description = "Liefert Vorschläge zur Autovervollständigung für den Suchbegriff zurück (Präfixsuche)."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Vorschlägen zurück.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                        schema = @Schema(implementation = GeoSuggestionOutModel.class)
                    ),
                    examples = {
                        @ExampleObject(
                            """
                                [
                                    {
                                        "name": "Berlingerode",
                                        "weight": 1.8060234
                                    },
                                    {
                                        "name": "Berlingen",
                                        "weight": 1.7954723
                                    }
                                ]
                            """
                        ),
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Es wurde eine falsche Anfrage gestellt",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class),
                    examples = { @ExampleObject(DESCRIPTION_BAD_REQUEST) }
                )
            ),
        }
    )
    public List<GeoSuggestionOutModel> autocompleteLocations(
        @Parameter(
            description = "Präfix des Ortsnamens (Ort/Ortsteil) oder der PLZ für die Autovervollständigung. Die Angabe nur des Stadtnamens kann in einzelnen Bundesländern nicht eindeutig interpretiert werden.",
            example = "Neumün",
            required = true
        ) @RequestParam(value = "q") @Size(min = 1, max = 70) final String searchQuery,
        @Parameter(description = "Anzahl der Ergebnisse") @RequestParam(
            value = "limit",
            required = false,
            defaultValue = "10"
        ) @Min(1) @Max(25) final Integer limit
    ) {
        if (hasSpecialCharacterAtFirstLetter(searchQuery)) {
            return List.of();
        }
        return geoService.suggest(searchQuery, limit);
    }

    @GetMapping("/v2/locations/reverse")
    @Operation(
        summary = "Liefert die Ortsdaten aus der Suchdienst Datenbank zu einem Breiten- und Längengrad zurück",
        description = "Liefert anhand des Breitengrads und Längengrads einen Ort zurück."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Orten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = GeoArsOutModelV3.class))
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die Eingabeparameter sind ungültig.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class),
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
    public Stream<GeoArsOutModelV3> getArsByCoordinatesV3(
        @Parameter(description = "Der Breitengrad", required = true) @RequestParam(
            "lat"
        ) String lat,
        @Parameter(description = "Der Längengrad", required = true) @RequestParam("lon") String lon
    ) {
        if (!NumberUtils.isParsable(lat) || !NumberUtils.isParsable(lon)) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                "Mindestens einer der Eingabewerte ist nicht numerisch"
            );
        } else if (
            !(Double.parseDouble(lat) <= 90.0 && Double.parseDouble(lat) >= -90.0) ||
            !(Double.parseDouble(lon) <= 180.0 && Double.parseDouble(lon) >= -180.0)
        ) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                "Mindestens einer der Eingabewerte liegt außerhalb des erlaubten Wertebereiches"
            );
        }
        return geoService
            .searchReverse(new GeoLocation(lon, lat))
            .stream()
            .map(LocationMapper.INSTANCE::mapGeoArsV3);
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
            GeoArsDistinctFilters.class,
            new CaseInsensitiveEnumEditor(GeoArsDistinctFilters.class)
        );
        binder.registerCustomEditor(
            GeoArsPropFilters.class,
            new CaseInsensitiveEnumEditor(GeoArsPropFilters.class)
        );
    }

    private void checkLocationSearchParameters(
        String city,
        String zip,
        String street,
        String houseNumber
    ) {
        String message = null;
        if (StringUtils.isBlank(city) && StringUtils.isBlank(zip)) {
            message =
                StringUtils.isBlank(street) && StringUtils.isBlank(houseNumber)
                    ? "Mindestens einer der Parameter ort oder plz muss gefüllt sein."
                    : "Für die Suche mit einer Straße oder Hausnummer muss mindestens einer der Parameter ort oder plz gefüllt sein.";
        } else if (StringUtils.isBlank(street) && StringUtils.isNotBlank(houseNumber)) {
            message = "Für die Suche mit einer Hausnummer muss der Parameter strasse gefüllt sein.";
        }
        if (message != null) {
            throw new SuchenException(ErrorCodeEnum.BAD_REQUEST, message);
        }
    }

    protected boolean hasSpecialCharacterAtFirstLetter(String... queryParameters) {
        return Arrays.stream(queryParameters)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .anyMatch(s -> !PATTERN.matcher(s).find());
    }
}
