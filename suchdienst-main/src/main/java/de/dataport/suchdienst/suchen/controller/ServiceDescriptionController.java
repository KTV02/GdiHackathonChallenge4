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
package de.dataport.suchdienst.suchen.controller;

import static de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode.FULL;
import static de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode.NONE;
import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.service.suggestion.SuggestionHelper;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.leistung.FullLifeSituationAggOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaMeta;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.model.out.leistung.SearchOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.ServiceDescriptionSuggestion;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionCsvService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionSuggestionService;
import de.dataport.suchdienst.util.PageRequestCheck;
import de.dataport.suchdienst.util.ResultPage;
import de.dataport.suchdienst.util.ResultPageWithMeta;
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
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "Leistungen",
    description = "Endpunkte zum Suchen und Finden von Verwaltungsleistungen." +
        " Sobald ein amtlicher Regionalschlüssel (ARS) ermittelt wurde, können Leistungen" +
        " (servicedescriptions) gesucht werden."
)
public class ServiceDescriptionController implements PageRequestCheck {

    private final MessageSource messageSource;
    private final ServiceDescriptionService serviceDescriptionService;
    private final ServiceDescriptionSuggestionService suggestionService;
    private final ServiceDescriptionCsvService csvService;
    private final PvogRequestContext context;

    public static final String DESCR_ARS_SEARCH = "Der ARS der Suche.";
    public static final String DESCR_ARS_LB_WITH_HIERARCHY = """
    Der ARS nach dem gesucht wird. Falls der angegebene ARS kürzer als 12 Zeichen ist, wird der Wert mit Nullen aufgefüllt.
    Im Falle von "useArsHierarchy = true" findet eine Auflösung der ARS-Hierarchie nach oben hin statt. Sucht man bspw. nach Köln, werden auch Zuständigkeiten berücksichtigt, die für ganz NRW definiert wurden.
    Wird "useArsHierarchy = false" verwendet, findet eine exakte Suche im übermittelten ARS statt. Hier werden dann nur Leistungen ausgeliefert, wenn es auch eine exakte Zuständigkeit in dem gesuchten Ort gibt. Sucht man hier beispielsweise nach Köln, werden nur Leistungen ausgeliefert, wenn es eine Zuständigkeit gibt, die in Köln definiert wurde.
    """;
    public static final String DESCR_ARS_LB =
        "Der ARS des Ortes, für den die Leistung gelten soll. Falls der angegebene ARS kürzer als 12 Zeichen ist, wird der Wert mit Nullen aufgefüllt. Der ARS löst die Hierarchie nach oben hin auf.";

    @Value(
        "${de.dataport.suchdienst.endpoint.enableDeprecated.v5-servicedescriptions-ars-detail:false}"
    )
    private boolean endpointV5ServiceDescriptionsArsDetailActive;

    public ServiceDescriptionController(
        ServiceDescriptionService serviceDescriptionService,
        MessageSource messageSource,
        ServiceDescriptionSuggestionService suggestionService,
        ServiceDescriptionCsvService csvService,
        PvogRequestContext context
    ) {
        this.serviceDescriptionService = serviceDescriptionService;
        this.messageSource = messageSource;
        this.suggestionService = suggestionService;
        this.csvService = csvService;
        this.context = context;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
    }

    @GetMapping(
        value = "/v1/servicedescriptions/suggestions/{ars}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Sucht nach Vorschlägen für die Leistungen.",
        description = "Sucht nach Vorschlägen für die Leistungen. Es wird nach dem ARS gefiltert, die Anzahl der Vorschläge ist auf 5 begrenzt."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Vorschlägen.",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(implementation = ServiceDescriptionSuggestion.class)
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Es ist ein interner Fehler aufgetreten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    public List<ServiceDescriptionSuggestion> getLbSuggestions(
        @PathVariable @Parameter(
            description = "Der ARS des Ortes für den der Suchvorschlag gelten soll.",
            example = "01",
            required = true
        ) @Size(min = 2, max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Der Wert, nach dem für die Vervollständigung gesucht werden soll",
            example = "Ingen",
            schema = @Schema(type = "string", minLength = 1, maxLength = 50),
            required = true
        ) @RequestParam(name = "q") @Size(min = 1, max = 50) String searchValue,
        @Parameter(description = "Ob Mehrfachtreffer ignoriert werden sollen") @RequestParam(
            name = "skipDuplicates",
            defaultValue = "false"
        ) boolean skipDuplicates
    ) {
        return suggestionService.searchSuggestions(
            searchValue.toLowerCase(),
            ars,
            skipDuplicates,
            context.getLocale()
        );
    }

    @GetMapping(
        value = "/v5/servicedescriptions/csv",
        produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE }
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                content = @Content(
                    mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    schema = @Schema(implementation = Resource.class)
                ),
                description = "Liefert eine CSV-Liste von Leistungen mit Onlinediensten."
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Es ist ein interner Fehler aufgetreten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Es wurde eine falsche Anfrage gestellt",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @Operation(
        summary = "Liefert alle Leistungen anhand des ARS in einer CSV Datei aus.",
        description = """
        Ermöglicht das Abrufen aller Leistungen in einer CSV Datei, die für den ARS erbracht werden\
         (also für die es eine entsprechende räumliche Zuständigkeit gibt). Zusätzlich zu den Leistungen \
        werden die Onlinedienste geladen. Wenn zu einer Leistung mehrere Onlinedienste vorhanden sind, \
        dann wird je Onlinedienst eine Leistung mit nur einem Onlinedienst ausgegeben. \
        Aufbau der CSV-Datei: \
        <ol>\
        <li>ID der Leistung,</li>\
        <li>LeikaIds der Leistung,</li>\
        <li>Bezeichnung der Leistung (Textmodul 02),</li>\
        <li>Alternative Bezeichnung der Leistung (Textmodul 03),</li>\
        <li>Letzter Updatezeitpunkt,</li>\
        <li>ID des Onlinedienstes,</li>\
        <li>Bezeichnung des Onlinedienstes,</li>\
        <li>Url des Onlinedienstes</li>\
        </ol>"""
    )
    public ResponseEntity<Resource> getAllLbsCsv(
        @Parameter(description = DESCR_ARS_SEARCH, example = "01", required = true) @RequestParam(
            name = "ars"
        ) @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*" /*Bewusst keine Verwendung von \d, da \d potenziell mehr Zeichen erlaubt.*/
        ) String ars, // TODO: Es gibt keine Mindestlänge und die Regex erlaubt einen leeren String, obwohl der Parameter eine Pflichtangabe ist
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        ByteArrayOutputStream out = csvService.searchServiceDescriptionAsCSV(ars, validDate);

        String filename = "leistungsbeschreibungen_" + ars + ".csv";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new ByteArrayResource(out.toByteArray()));
    }

    @GetMapping(
        value = "/v7/servicedescriptions/{ars}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Dieser Endpunkt gibt die Leistung und deren zugeordneten Lebenslagen aus.",
        description = """
        Ausgegebene Leistungen werden nach ARS und einem Suchbegriff oder Lebenslagen gefiltert und mit allen verknüpften Lebenslagen und deren Sublagen ausgegeben.
        Um fachlich korrekte Treffer zu bekommen, sollte nur nach dem ARS von Gemeinden gesucht werden.
        """
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von Leistungen und zählt/aggregiert deren Lebenslagen"
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Es ist ein interner Fehler aufgetreten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "unbekannter Fehler",
                            value = "{\"timestamp\":\"2022-12-06T07:00:00.000000000Z\"," +
                                "\"code\":\"UNKNOWN_ERROR\"," +
                                "\"message\":" +
                                "\"Ein unbekannter Fehler ist aufgetreten.\"," +
                                "\"codeNumber\":1900}"
                        ),
                    },
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Request nicht valide",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "Unzulässige Zeichenanzahl",
                            description = "Die Zeichenkette 'x' ist zu kurz als Wert für den Parameter q.",
                            // PV-9048: Das x in der nächsten Zeile ist eigentlich falsch.
                            value = """
                            {"timestamp":"2022-12-06T07:00:00.000000000Z",\
                            "code":"BAD_REQUEST",\
                            "message":"Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",\
                            "fields":{"x":"Größe muss zwischen 2 und 2147483647 sein"},\
                            "codeNumber":1901}"""
                        ),
                    },
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @JsonView(JsonViews.Search.class)
    public SearchOutModel<SdOutModelV7> getSearchOutModelV7(
        @PathVariable @Parameter(
            description = DESCR_ARS_LB_WITH_HIERARCHY,
            example = "010020000000",
            required = true
        ) @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(regexp = "[0-9]*") String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Codes der Lebenslagen, nach denen gefiltert werden soll."
        ) @RequestParam(name = "personalMatters", required = false) Set<String> personalMatters,
        @Parameter(
            description = "Bewirkt, dass die Schnittstelle zusätzlich zu den Treffern eine Aggregation der zugehörigen Lebenslagen liefert."
        ) @RequestParam(
            value = "facades",
            required = false,
            defaultValue = "false"
        ) boolean facades,
        @Parameter(
            description = "Der Wert, nach dem gesucht werden soll",
            example = "[\"Ingen\"]",
            array = @ArraySchema(schema = @Schema(type = "string"))
            // PV-9369
        ) @RequestParam(name = "q", required = false) @Size(min = 1) List<@Size(
            min = 2
        ) String> searchValue,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) @Max(100) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(9999) int pageSize,
        @Parameter(description = "Erlaubt eine Fuzzy-Suche") @RequestParam(
            value = "fuzzysearch",
            defaultValue = "false"
        ) boolean fuzzysearch,
        @Parameter(
            description = "Ignoriert bestimmte Leika-Typisierungen der Liste urn:de:fim:leika:typisierung"
        ) @RequestParam(value = "ignoreLeikaTypes", defaultValue = "") Set<String> ignoreLeikaTypes,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy,
        @Parameter(
            description = "Schalter für das Erweitern des Onlinedienstlinks um dynamische Parameter. Bei aktivem Flag werden Query-Parameter gemäß dem XZuFi-Standard und dem im Suchdienstkonzept definierten Vorgehen ergänzt."
        ) @RequestParam(
            value = "applyDynamicParams",
            defaultValue = "false"
        ) boolean applyDynamicParams
    ) {
        checkPageParameters(page, pageSize, messageSource);

        // Neu ist hier seit PV-9484, dass eine implizite Suche nach LeikaId stattfindet,
        // wenn es genau ein Suchkriterium gibt, und dieses syntaktisch eine LeikaId ist.
        //
        final Set<String> searchValues;
        final List<String> leikaIds;
        if (searchValue == null) {
            searchValues = Set.of();
            leikaIds = List.of();
        } else if (searchValue.size() == 1 && LeikaKey.isLeikaKey(searchValue.getFirst())) {
            searchValues = Set.of();
            leikaIds = List.of(searchValue.getFirst());
        } else {
            searchValues = SuggestionHelper.addSpaceBetweenWordAndNumber(searchValue);
            leikaIds = List.of();
        }

        PersonalMattersMode personalMattersMode = facades ? FULL : NONE;
        return serviceDescriptionService.search(
            searchValues,
            ars,
            useArsHierarchy,
            personalMatters,
            personalMattersMode,
            page,
            pageSize,
            fuzzysearch,
            ignoreLeikaTypes,
            context.getLocale(),
            leikaIds,
            validDate,
            applyDynamicParams,
            SdOutModelV7.class
        );
    }

    /**
     * PV-8448
     */
    @GetMapping(value = "/v1/personalmatters/{ars}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Sucht nach allen Lebenslagen in einem Gebiet.",
        description = "Gibt alle Lebenslagen gefiltert nach dem ARS aus. Es werden alle Lebenslagen, inklusive Sublagen angezeigt, für die in einem bestimmten Gebiet (ARS) Leistungen vorhanden sind."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Liefert Lebenslagen mit Sublagen und Zählwerten.",
        useReturnTypeSchema = true
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    @JsonView(JsonViews.Search.class)
    public Set<FullLifeSituationAggOutModel> getPersonalMatters(
        @PathVariable @Parameter(
            description = DESCR_ARS_LB,
            example = "090000000000",
            required = true
        ) @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(regexp = "[0-9]*") String ars // Regex Syntax [0-9] bei Fehlermeldungen
    ) {
        final Locale locale = context.getLocale();
        return serviceDescriptionService.searchPersonalMatters(ars, locale);
    }

    @GetMapping(
        value = "/v6/servicedescriptions/{lbid}/detail",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Gibt Detailinformationen zu einer Leistung zurück.",
        description = """
        Gibt eine Leistung mit allen Informationen und allen zuständigen Organisationseinheiten in einem bestimmten Gebiet (ARS) zurück.
        Die Onlinedienst-Links in der Antwortmenge enthalten dynamische Parameter.
        Der Endpunkt liefert zum Zeitpunkt der Anfrage aktuellsten Daten nach Gültigkeitszeitraum zurück.
        Um fachlich korrekte Treffer zu bekommen, sollte nur nach dem ARS von Gemeinden gesucht werden.
        """
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert Details über eine spezielle Leistung",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServiceDescriptionDetail.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                        name = "Nicht gefunden",
                        value = """
                        {
                            "timestamp": "2022-12-06T07:00:00.000000000Z",
                            "code": "NOT_FOUND",
                            "message": "Die angegebene Resource konnte nicht gefunden werden.",
                            "codeNumber": 1906
                        }"""
                    ),
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    public ServiceDescriptionDetail getSdOutModelV6(
        @PathVariable @Parameter(
            description = "Die ID der Leistung, deren Details geladen werden sollen",
            example = "L100012.LB.9413106",
            required = true
        ) @Size(min = 2) String lbid,
        @Parameter(
            description = DESCR_ARS_LB_WITH_HIERARCHY,
            example = "010020000000",
            required = true
        ) @RequestParam(name = "ars") @Size(min = 12, max = 12) @SuppressWarnings(
            "java:S6353"
        ) @Pattern(regexp = "[0-9]*") String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy
    ) {
        Locale searchWithLocale = context.getLocale();
        return serviceDescriptionService
            .fetchDetailV6(lbid, ars, useArsHierarchy, searchWithLocale)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", null, Locale.GERMAN)
                )
            );
    }

    /**
     * @deprecated PV-14411: Dieser Endpunkt wird durch den Endpunkt getSdOutModelV6 ersetzt.
     */
    @Deprecated(forRemoval = true, since = "2026-04")
    @GetMapping(
        value = "/v5/servicedescriptions/{ars}/detail",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Gibt eine vollständige Leistung.",
        description = """
        Gibt eine Leistung mit allen Informationen und allen zuständigen Organisationseinheiten in einem bestimmten Gebiet (ARS) zurück.
        Um fachlich korrekte Treffer zu bekommen, sollte nur nach dem ARS von Gemeinden gesucht werden.
        """
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert Details über eine spezielle Leistung",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SdOutModelV5.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                        name = "Nicht gefunden",
                        value = """
                        {
                            "timestamp": "2022-12-06T07:00:00.000000000Z",
                            "code": "NOT_FOUND",
                            "message": "Die angegebene Resource konnte nicht gefunden werden.",
                            "codeNumber": 1906
                        }"""
                    ),
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @JsonView(JsonViews.Detail.class)
    public SdOutModelV5 getSdOutModelV5(
        @PathVariable @Parameter(
            description = DESCR_ARS_LB_WITH_HIERARCHY,
            example = "010000000000",
            required = true
        ) @Size(min = 12, max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Die ID der Leistung, deren Details geladen werden sollen",
            example = "L100012.LB.9413106",
            required = true
        ) @RequestParam(name = "q") @Size(min = 2) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy,
        @Parameter(
            description = "Schalter für das Erweitern des Onlinedienstlinks um dynamische Parameter. Bei aktivem Flag werden Query-Parameter gemäß dem XZuFi-Standard und dem im Suchdienstkonzept definierten Vorgehen ergänzt."
        ) @RequestParam(
            value = "applyDynamicParams",
            defaultValue = "false"
        ) boolean applyDynamicParams
    ) {
        if (!endpointV5ServiceDescriptionsArsDetailActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v5/servicedescriptions/{ars}/detail' ist nicht länger aktiv."
            );
        }
        Locale searchWithLocale = context.getLocale();
        return serviceDescriptionService
            .fetchDetailV5(
                id,
                ars,
                useArsHierarchy,
                searchWithLocale,
                applyDynamicParams,
                validDate
            )
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v3/servicedescriptions/leikaid",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Sucht mit einem LeiKa-Schlüssel nach Leistungen.",
        description = """
        Sucht nach Leistungen anhand des jeweiligen LeiKa-Schlüssels in einem bestimmten Gebiet (ARS).
        Um fachlich korrekte Treffer zu bekommen, sollte nur nach dem ARS von Gemeinden gesucht werden.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Liefert eine paginierte Trefferliste.",
        useReturnTypeSchema = true
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public ResultPage<SdOutModelV7> findByLeikaIdV3(
        @Parameter(
            description = "Liste von Leika-Schlüsseln",
            example = "[\"99147006067000\"]",
            array = @ArraySchema(schema = @Schema(type = "string")),
            required = true
        ) @RequestParam(value = "leikaIds") List<@Size(min = 14, max = 21) String> leikaIds,
        @Parameter(
            description = "Eine Liste mit zusätzlichen Suchbegriffen",
            example = "[\"Ingenieur\"]",
            array = @ArraySchema(schema = @Schema(type = "string"))
        ) @RequestParam(name = "q", required = false) List<@Size(min = 3) String> searchValue,
        @Parameter(
            description = DESCR_ARS_LB_WITH_HIERARCHY,
            example = "010020000000",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Legt fest, ob die Response organisationIds enthält"
        ) @RequestParam(value = "withOrgIds", defaultValue = "true") boolean withOrgIds,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) @Max(100) int page,
        @Parameter(description = "Anzahl der Ergebnisse ab Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(9999) int pageSize,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy
    ) {
        checkPageParameters(page, pageSize, messageSource);

        final Set<String> searchValues =
            searchValue == null
                ? Set.of()
                : SuggestionHelper.addSpaceBetweenWordAndNumber(searchValue);

        return serviceDescriptionService.searchByLeikaIds(
            searchValues,
            ars,
            useArsHierarchy,
            page,
            pageSize,
            false,
            emptySet(),
            context.getLocale(),
            leikaIds,
            withOrgIds,
            validDate,
            SdOutModelV7.class
        );
    }

    @GetMapping(
        value = "/v2/servicedescriptions/leikahierarchy",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Sucht nach den Leistungen anhand der Hierarchie der Leika-Schlüssel.",
        description = """
        Die übergebenen Leika-Schlüssel müssen alle auf der selben Ebene liegen,\
         also alle entweder Verrichtungsdetail, Verrichtungskennung oder\
         Leistungskennung.\
        <p>Gesucht wird zuerst nach exakt übereinstimmenden Schlüsseln, aber\
         unabhängig von der Länderkennung, sofern der jeweilige Schlüssel\
         nicht den dezentralen Adressraum verwendet. Exakte Treffer werden per 'exact' Flag\
         im 'meta' Objekt der Paginierung-Daten gekennzeichnet.</p>\
        <p>Gibt es keine direkten Treffer, werden spezifischere Leikas als\
         die gesuchten gefunden.</p>\
        <p>Existieren auch diese nicht, wird nach übergeordneten Leikas gesucht.</p>
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Liefert eine paginierte Trefferliste mit LeiKa-Metadaten.",
        useReturnTypeSchema = true
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen, z.B. bei gemischten LeiKa-Hierarchieebenen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public ResultPageWithMeta<SdOutModelV7, LeikaMeta> findByLeikaIdHierarchyV2(
        @Parameter(
            description = "Liste von gültigen Leika-Schlüsseln," +
                " temporäre Schlüssel der Form TMP_XX_... sind nicht erlaubt.",
            required = true
        ) @RequestParam(value = "leikaIds") List<@Size(max = 14, min = 14) String> leikaIds,
        @Parameter(
            description = "Der ARS des Ortes für den die Leistung gelten soll",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Ignoriert bestimmte Leika-Typisierungen der Liste urn:de:fim:leika:typisierung"
        ) @RequestParam(value = "ignoreLeikaTypes", defaultValue = "") Set<String> ignoreLeikaTypes,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) @Max(100) int page,
        @Parameter(description = "Anzahl der Ergebnisse ab Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(9999) int pageSize,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        checkPageParameters(page, pageSize, messageSource);
        return serviceDescriptionService.searchByLeikaHierarchy(
            ignoreLeikaTypes,
            ars,
            page,
            pageSize,
            validDate,
            context.getLocale(),
            leikaIds
        );
    }
}
