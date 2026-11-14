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
package de.dataport.suchdienst.jzufi.controller;

import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_JSON_22;
import static de.dataport.suchdienst.util.XZuFiVersionHeaderUtil.MEDIA_TYPE_JSON_23;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.dataport.suchdienst.jzufi.model.out.JzufiRelationPage23;
import de.dataport.suchdienst.jzufi.model.out.RelationPage22;
import de.dataport.suchdienst.jzufi.model.out.RelationPage23;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.service.RelationService;
import de.dataport.suchdienst.util.PageRequestCheck;
import de.dataport.suchdienst.util.XZuFiVersionHeaderUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "JZuFi",
    description = "Endpunkte zum Laden von JZuFi. JZuFi ist eine JSON-Representation des XZuFi Datenmodells."
)
public class ZustaendigkeitJzufiController implements PageRequestCheck {

    private final MessageSource messageSource;
    private final RelationService relationService;

    @Value("${de.dataport.suchdienst.endpoint.enableDeprecated.v3-relations-ars-jzufi:false}")
    private boolean endpointV3RelationsArsJzufiActive;

    public ZustaendigkeitJzufiController(
        MessageSource messageSource,
        RelationService relationService
    ) {
        this.messageSource = messageSource;
        this.relationService = relationService;
    }

    /**
     * @deprecated Stattdessen /v2/relations/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "2026-10", forRemoval = true)
    @GetMapping(value = "/v1/relations/jzufi-2-3", produces = { MediaType.APPLICATION_JSON_VALUE })
    @Operation(
        summary = "JZuFi der Zuständigkeits-Objekte in der XZuFi-Version 2.3.1",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Zuständigkeits-Objekte in der XZuFi-Version 2.3.1 zurück. Ein Zuständigkeits-Objekt verknüpft Leistungen, Organisationseinheiten und Onlinedienste. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von ZustaendigkeitTransferObjekten.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RelationPage23.class),
                    examples = {
                        @ExampleObject(
                            name = "Zuständigkeiten in JZuFi 2.3.1 Format",
                            description = "Liste mit den gefundenen Zuständigkeiten",
                            value = Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ZUSTAENDIGKEIT_V3
                        ),
                    }
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
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige Pflichtparameter, IDs, Datumsangabe oder Seitengrenzen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public PagedModel<de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt> getZsTrByLbIdAndArs231V1(
        @Parameter(
            description = "Der ARS des Ortes, für den die Zuständigkeiten gelten sollen",
            example = "010000000000",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) final String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Die IDs der Leistungen, für die die Zuständigkeiten gelten sollen",
            example = "[\"L100012.LB.9413106\"]",
            required = true
        ) @RequestParam(name = "lbids") @Size(min = 1, max = 50) List<@Size(
            min = 1,
            max = 255
        ) String> lbIds,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(value = "usearshierarchy", defaultValue = "true") boolean useArsHierarchy,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(Elastic.WINDOW_SIZE - 1) int pageSize
    ) {
        checkPageParameters(page, pageSize, messageSource);
        var resultPair = relationService.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            validDate,
            page,
            pageSize
        );

        var relationStream = resultPair.getRight().stream();
        var totalResults = resultPair.getLeft();
        var pageRequest = PageRequest.of(page, pageSize);

        return new RelationPage23(
            relationStream.map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23).toList(),
            pageRequest,
            totalResults
        );
    }

    @GetMapping(value = "/v1/relations/jzufi-2-2", produces = { MediaType.APPLICATION_JSON_VALUE })
    @Operation(
        summary = "JZuFi der Zuständigkeits-Objekte in der XZuFi-Version 2.2.0",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Zuständigkeits-Objekte in der XZuFi-Version 2.2.0 zurück. Ein Zuständigkeits-Objekt verknüpft Leistungen, Organisationseinheiten und Onlinedienste. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von ZustaendigkeitTransferObjekten.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RelationPage22.class),
                    examples = {
                        @ExampleObject(
                            name = "Zuständigkeiten in JZuFi 2.2.0 Format",
                            description = "Liste mit den gefundenen Zuständigkeiten",
                            value = Example.RESPONSE_EXAMPLE_JZUFI_2_2_0_ZUSTAENDIGKEIT_V3
                        ),
                    }
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
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige Pflichtparameter, IDs, Datumsangabe oder Seitengrenzen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public PagedModel<de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt> getZsTrByLbIdAndArs220V1(
        @Parameter(
            description = "Der ARS des Ortes, für den die Zuständigkeiten gelten sollen",
            example = "010000000000",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) final String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Die IDs der Leistungen, für die die Zuständigkeiten gelten sollen",
            example = "[\"L100012.LB.9413106\"]",
            required = true
        ) @RequestParam(name = "lbids") @Size(min = 1, max = 50) List<@Size(
            min = 1,
            max = 255
        ) String> lbIds,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(value = "usearshierarchy", defaultValue = "true") boolean useArsHierarchy,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(Elastic.WINDOW_SIZE - 1) int pageSize
    ) {
        checkPageParameters(page, pageSize, messageSource);
        var resultPair = relationService.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            validDate,
            page,
            pageSize
        );

        var relationStream = resultPair.getRight().stream();
        var totalResults = resultPair.getLeft();
        var pageRequest = PageRequest.of(page, pageSize);

        return new RelationPage22(
            relationStream.map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi22).toList(),
            pageRequest,
            totalResults
        );
    }

    /**
     * @deprecated Stattdessen /v2/relations/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "11.12.0", forRemoval = true)
    @GetMapping(
        value = "/v3/relations/{ars}/jzufi",
        produces = { MEDIA_TYPE_JSON_22, MEDIA_TYPE_JSON_23, MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi der Zuständigkeits-Objekte in angefragter Version",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) der Zuständigkeits-Objekte in angefragter Version zurück. Ein Zuständigkeits-Objekt verknüpft Leistungen, Organisationseinheiten und Onlinedienste. Es werden Accept-Headers application/json; xzufi-version=2.2.0 oder application/json; xzufi-version=2.3.1 unterstützt. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine Liste von ZustaendigkeitTransferObjekten.",
                content = {
                    @Content(
                        mediaType = MEDIA_TYPE_JSON_22,
                        schema = @Schema(implementation = RelationPage22.class),
                        examples = {
                            @ExampleObject(
                                name = "Zuständigkeiten in JZuFi 2.2.0 Format",
                                description = "Liste mit den gefundenen Zuständigkeiten",
                                value = Example.RESPONSE_EXAMPLE_JZUFI_2_2_0_ZUSTAENDIGKEIT_V3
                            ),
                        }
                    ),
                    @Content(
                        mediaType = MEDIA_TYPE_JSON_23,
                        schema = @Schema(implementation = RelationPage23.class),
                        examples = {
                            @ExampleObject(
                                name = "Zuständigkeiten in JZuFi 2.3.1 Format",
                                description = "Liste mit den gefundenen Zuständigkeiten",
                                value = Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ZUSTAENDIGKEIT_V3
                            ),
                        }
                    ),
                }
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

    // Generic Wildcard ist hier notwendig
    @SuppressWarnings("java:S1452")
    public PagedModel<? extends Serializable> getZsTrByLbIdAndArsV3(
        @Parameter(hidden = true) @RequestHeader(value = "Accept") MediaType accept,
        @PathVariable @Parameter(
            description = "Der ARS des Ortes, für den die Zuständigkeiten gelten sollen",
            example = "010000000000",
            required = true
        ) @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) final String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Die IDs der Leistungen, für die die Zuständigkeiten gelten sollen",
            example = "[\"L100012.LB.9413106\"]",
            required = true
        ) @RequestParam(name = "lbids") @Size(min = 1, max = 50) List<@Size(
            min = 1,
            max = 255
        ) String> lbIds,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(value = "usearshierarchy", defaultValue = "true") boolean useArsHierarchy,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(Elastic.WINDOW_SIZE - 1) int pageSize
    ) {
        if (!endpointV3RelationsArsJzufiActive) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                "Der Endpunkt '/v3/relations/{ars}/jzufi' ist nicht länger aktiv."
            );
        }
        XZuFiVersion version = XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(accept).orElseThrow(
            () ->
                new SuchenException(
                    ErrorCodeEnum.BAD_REQUEST,
                    messageSource.getMessage(
                        "api.400_bad_request_headers",
                        new String[] { XZuFiVersion.class.getSimpleName() },
                        Locale.GERMAN
                    )
                )
        );

        checkPageParameters(page, pageSize, messageSource);
        var resultPair = relationService.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            validDate,
            page,
            pageSize
        );

        var relationStream = resultPair.getRight().stream();
        var totalResults = resultPair.getLeft();
        var pageRequest = PageRequest.of(page, pageSize);

        return switch (version) {
            case XZUFI22 -> new RelationPage22(
                relationStream
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi22)
                    .toList(),
                pageRequest,
                totalResults
            );
            case XZUFI23 -> new RelationPage23(
                relationStream
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23)
                    .toList(),
                pageRequest,
                totalResults
            );
        };
    }

    @GetMapping(value = "/v2/relations/jzufi-2-3", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "JZuFi der Zuständigkeits-Objekte in der XZuFi-Version 2.3.1",
        description = "Liefert eine Seite mit Suchdienst-eigenen JZuFi-Ausgabemodellen. Zuständigkeiten verwenden den Discriminator @type. Daten anderer XZuFi-Versionen werden nach 2.3.1 konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Seite mit ZuständigkeitTransferObjekten",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = JzufiRelationPage23.class),
            examples = @ExampleObject(
                name = "Zuständigkeiten in JZuFi 2.3.1 Format",
                value = Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ZUSTAENDIGKEIT_V2
            )
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige Pflichtparameter, IDs, Datumsangabe oder Seitengrenzen.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Es ist ein interner Fehler aufgetreten",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public JzufiRelationPage23 getZsTrByLbIdAndArs231V2(
        @Parameter(
            description = "Der ARS des Ortes",
            example = "010000000000",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars,
        @Parameter(description = "Die IDs der Leistungen", required = true) @RequestParam(
            name = "lbids"
        ) @Size(min = 1, max = 50) List<@Size(min = 1, max = 255) String> lbIds,
        @Parameter(
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(value = "usearshierarchy", defaultValue = "true") boolean useArsHierarchy,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(Elastic.WINDOW_SIZE - 1) int pageSize
    ) {
        checkPageParameters(page, pageSize, messageSource);
        var resultPair = relationService.getRelationZustaendigkeitTransferObjektXzufiByLbIdAndArs(
            lbIds,
            ars,
            useArsHierarchy,
            null,
            page,
            pageSize
        );

        var relationStream = resultPair.getRight().stream();
        var totalResults = resultPair.getLeft();
        var pageRequest = PageRequest.of(page, pageSize);

        var result = relationStream
            .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23)
            .map(JzufiModelMapper.INSTANCE::map23)
            .toList();

        return new JzufiRelationPage23(result, pageRequest, totalResults);
    }

    private static class Example {

        public static final String RESPONSE_EXAMPLE_JZUFI_2_3_1_ZUSTAENDIGKEIT_V2 = """
        {
          "content": [{
            "uebergeordnetesObjektID": {"value": "OE-1"},
            "zustaendigkeit": [{
              "@type": "ZustaendigkeitOrganisationseinheit",
              "leistungID": [{"value": "LB-1"}],
              "gebietID": [{"value": "010000000000"}],
              "rolle": [{"code": "03"}]
            }]
          }],
          "page": {"number": 0, "size": 10, "totalElements": 1, "totalPages": 1}
        }
        """;

        public static final String RESPONSE_EXAMPLE_JZUFI_2_2_0_ZUSTAENDIGKEIT_V3 = """
        {
          "content": [
            {
              "uebergeordnetesObjektID": {
                "value": "OE_xzufi22DeleteZSOD_77777_0001",
                "schemeAgencyID": "L100042"
              },
              "zustaendigkeit": [
                {
                  "rolle": {
                    "code": "03",
                    "name": "Ansprechpunkt",
                    "listURI": "urn:de:xzufi:codeliste:zustaendigkeitsrolle",
                    "listVersionID": "20210401"
                  },
                  "id": {
                    "value": "LB_xzufi22DeleteZSOD_77777_0001.OE_xzufi22DeleteZSOD_77777_0001.095640000003",
                    "schemeAgencyID": "L100042",
                    "schemeID": "ZustaendigkeitOrganisationseinheit"
                  },
                  "idSekundaer": [
                    {
                      "value": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWJtaXNzaW9uSG9zdCI6InN1Ym1pc3Npb24tYXBpLXRlc3RpbmcuZml0LWNvbm5lY3QuZml0a28uZGV2IiwiaXNzIjoiaHR0cHM6Ly9wb3J0YWwuYXV0aC10ZXN0aW5nLmZpdC1jb25uZWN0LmZpdGtvLmRldiIsInNlcnZpY2VzIjpbeyJnZWJpZXRJRHMiOlsidXJuOmRlOmJ1bmQ6ZGVzdGF0aXM6YmV2b2Vsa2VydW5nc3N0YXRpc3RpazpzY2hsdWVzc2VsOnJzOjEyMzEyMzEyMzEyMyJdLCJsZWlzdHVuZ0lEcyI6WyJ1cm46ZGU6ZmltOmxlaWthOmxlaXN0dW5nOkxCX3h6dWZpMjJEZWxldGVaU09EXzc3Nzc3XzAwMDIiXX1dLCJkZXN0aW5hdGlvbklkIjoiN2I0OWNlYWQtNDQ4My00OTczLWJiZTctODZlYWExNzFjOTkzIiwiaWF0IjoxNjg4NTY1NzcxLCJqdGkiOiI1YmI2ODZmMS1hOGI1LTRiNjItYmU3NC02MDIyZjhmZWIwMmIifQ.scU5RTTc0CeG60VgECB2M3mxVFG5XLltPn8iYYR3XExj_PvkXk9BQoMZNQIHVG07YniAedfoJvJJzhxcnhaVau-20uNgKljtlicANMPaFIFD9CtidUuy04OKf-RvYVwvIM6-8uHvhqV7fzdkzLBlbMni8XQjoeBpHWd6lFYL8Hzak55cAbjU2G1-2LXou2etVBMYCISTQ_K-CvM_f-4R_FFgE9t1nfYjJAZkYaWdJ9iS9xu5rgw_0AJymCkqKAWl1jdvSglPlsycpozXYn7duZjZ3nJNdZ3QmGCtLhRXyF2Y-9aOBCu4U0lYcMbn7rgVJG3KLNCMHGA_axnRDg88Yw",
                      "schemeAgencyID": "urn:de:fitko",
                      "schemeID": "urn:de:fitko:fit-connect:xzufi:destination"
                    }
                  ],
                  "leistungID": [
                    {
                      "value": "LB_xzufi22DeleteZSOD_77777_0002",
                      "schemeAgencyID": "L100042"
                    }
                  ],
                  "gebietID": [
                    {
                      "value": "012012012012",
                      "schemeAgencyID": "DESTATIS",
                      "schemeID": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs",
                      "schemeVersionID": "2018-07-31"
                    }
                  ],
                  "gueltigkeit": []
                }
              ]
            }
          ],
          "page": {
            "number": 0,
            "size": 10,
            "totalElements": 1,
            "totalPages": 1
          }
        }""";

        public static final String RESPONSE_EXAMPLE_JZUFI_2_3_1_ZUSTAENDIGKEIT_V3 = """
        {
          "content": [
            {
              "uebergeordnetesObjektID": {
                "value": "OE_xzufi22DeleteZSOD_77777_0001",
                "schemeAgencyID": "L100042"
              },
              "zustaendigkeit": [
                {
                  "rolle": [
                    {
                      "code": "03",
                      "name": "Ansprechpunkt",
                      "listURI": "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle",
                      "listVersionID": "1"
                    }
                  ],
                  "id": {
                    "value": "LB_xzufi22DeleteZSOD_77777_0001.OE_xzufi22DeleteZSOD_77777_0001.095640000003",
                    "schemeAgencyID": "L100042",
                    "schemeID": "ZustaendigkeitOrganisationseinheit"
                  },
                  "idSekundaer": [
                    {
                      "value": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWJtaXNzaW9uSG9zdCI6InN1Ym1pc3Npb24tYXBpLXRlc3RpbmcuZml0LWNvbm5lY3QuZml0a28uZGV2IiwiaXNzIjoiaHR0cHM6Ly9wb3J0YWwuYXV0aC10ZXN0aW5nLmZpdC1jb25uZWN0LmZpdGtvLmRldiIsInNlcnZpY2VzIjpbeyJnZWJpZXRJRHMiOlsidXJuOmRlOmJ1bmQ6ZGVzdGF0aXM6YmV2b2Vsa2VydW5nc3N0YXRpc3RpazpzY2hsdWVzc2VsOnJzOjEyMzEyMzEyMzEyMyJdLCJsZWlzdHVuZ0lEcyI6WyJ1cm46ZGU6ZmltOmxlaWthOmxlaXN0dW5nOkxCX3h6dWZpMjJEZWxldGVaU09EXzc3Nzc3XzAwMDIiXX1dLCJkZXN0aW5hdGlvbklkIjoiN2I0OWNlYWQtNDQ4My00OTczLWJiZTctODZlYWExNzFjOTkzIiwiaWF0IjoxNjg4NTY1NzcxLCJqdGkiOiI1YmI2ODZmMS1hOGI1LTRiNjItYmU3NC02MDIyZjhmZWIwMmIifQ.scU5RTTc0CeG60VgECB2M3mxVFG5XLltPn8iYYR3XExj_PvkXk9BQoMZNQIHVG07YniAedfoJvJJzhxcnhaVau-20uNgKljtlicANMPaFIFD9CtidUuy04OKf-RvYVwvIM6-8uHvhqV7fzdkzLBlbMni8XQjoeBpHWd6lFYL8Hzak55cAbjU2G1-2LXou2etVBMYCISTQ_K-CvM_f-4R_FFgE9t1nfYjJAZkYaWdJ9iS9xu5rgw_0AJymCkqKAWl1jdvSglPlsycpozXYn7duZjZ3nJNdZ3QmGCtLhRXyF2Y-9aOBCu4U0lYcMbn7rgVJG3KLNCMHGA_axnRDg88Yw",
                      "schemeAgencyID": "urn:de:fitko",
                      "schemeID": "urn:de:fitko:fit-connect:xzufi:destination"
                    }
                  ],
                  "leistungID": [
                    {
                      "value": "LB_xzufi22DeleteZSOD_77777_0002",
                      "schemeAgencyID": "L100042"
                    }
                  ],
                  "gebietID": [
                    {
                      "value": "012012012012",
                      "schemeAgencyID": "DESTATIS",
                      "schemeID": "urn:de:bund:destatis:bevoelkerungsstatistik:schluessel:rs",
                      "schemeVersionID": "2018-07-31"
                    }
                  ],
                  "gueltigkeit": []
                }
              ]
            }
          ],
          "page": {
            "number": 0,
            "size": 10,
            "totalElements": 1,
            "totalPages": 1
          }
        }
        """;
    }
}
