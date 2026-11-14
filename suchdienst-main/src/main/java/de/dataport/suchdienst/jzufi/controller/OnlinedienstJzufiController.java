/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.jzufi.controller;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.jzufi.model.mapper.JzufiModelMapper;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "JZuFi",
    description = "Endpunkte zum Laden von JZuFi. JZuFi ist eine JSON-Representation des XZuFi Datenmodells."
)
@Validated
public class OnlinedienstJzufiController {

    public static final String CODE_API_404 = "api.404";
    private final OnlineDienstService onlineDienstService;
    private final MessageSource messageSource;

    public OnlinedienstJzufiController(
        OnlineDienstService onlineDienstService,
        MessageSource messageSource
    ) {
        this.onlineDienstService = onlineDienstService;
        this.messageSource = messageSource;
    }

    /**
     * @deprecated Stattdessen /v2/onlineservices/{id}/jzufi-2-3 verwenden.
     */
    @Deprecated(since = "2026-10", forRemoval = true)
    @GetMapping(
        value = "/v1/onlineservices/{id}/jzufi-2-3",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi des Onlinedienstes in der XZuFi-Version 2.3.1",
        description = "Gibt das ursprüngliche XZuFi als JSON (JZuFi) des Onlinedienstes in der XZuFi-Version 2.3.1 zurück. Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden, werden die ausgelieferten Daten entsprechend konvertiert."
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Onlinedienst für ID gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                        implementation = de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class
                    ),
                    examples = { @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_3_1_ONLINEDIENST) }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Onlinedienst nicht gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public de.xoev.xzufi.v2_3_1.OnlinedienstErweitert getJzufiOd231V1(
        @PathVariable @Parameter(
            description = "Die ID des Onlinedienstes",
            examples = { @ExampleObject("L100042.OD.868") },
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return onlineDienstService
            .getJzufi(id, validDate)
            .map(GenericOnlinedienst::getOrConvertXZuFi23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v1/onlineservices/{id}/jzufi-2-2",
        produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    @Operation(
        summary = "JZuFi des Onlinedienstes in der XZuFi-Version 2.2.0",
        description = """
        Gibt das ursprüngliche XZuFi als JSON (JZuFi) des Onlinedienstes in der XZuFi-Version 2.2.0 zurück.
        Wenn die Auslieferungsversion der Daten von der Version abweicht, in der die Daten an das PVOG geliefert wurden,
        werden die ausgelieferten Daten entsprechend konvertiert.
        Onlinedienste mit kennzeichenOeffentlichAnzeigen=false werden nicht ausgeliefert."""
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Onlinedienst für ID gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OnlinedienstErweitert.class),
                    examples = { @ExampleObject(Example.RESPONSE_EXAMPLE_JZUFI_2_2_0_ONLINEDIENST) }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Onlinedienst nicht gefunden oder nicht zur öffentlichen Anzeige freigegeben",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID oder Datumsangabe.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public OnlinedienstErweitert getJzufiOd220V1(
        @PathVariable @Parameter(
            description = "Die ID des Onlinedienstes",
            examples = { @ExampleObject("L100042.OD.868") },
            required = true
        ) @Size(min = 1, max = 255) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validdate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return onlineDienstService
            .getJzufi(id, validDate)
            .filter(GenericOnlinedienst::isOeffentlichAnzeigen)
            .map(GenericOnlinedienst::getOrConvertXZuFi22)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    @GetMapping(
        value = "/v2/onlineservices/{id}/jzufi-2-3",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "JZuFi des Onlinedienstes in der XZuFi-Version 2.3.1",
        description = "Liefert das Suchdienst-eigene JZuFi-Ausgabemodell mit @type für verschachtelte Zuständigkeiten. Daten anderer XZuFi-Versionen werden nach 2.3.1 konvertiert."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Onlinedienst für ID gefunden",
        content = @Content(
            schema = @Schema(
                implementation = de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert.class
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Onlinedienst nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Ungültige ID.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert getJzufiOd231V2(
        @PathVariable @Parameter(description = "Die ID des Onlinedienstes", required = true) @Size(
            min = 1,
            max = 255
        ) String id
    ) {
        return onlineDienstService
            .getJzufi(id, null)
            .map(GenericOnlinedienst::getOrConvertXZuFi23)
            .map(JzufiModelMapper.INSTANCE::map23)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(CODE_API_404, null, Locale.GERMAN)
                )
            );
    }

    private static class Example {

        public static final String RESPONSE_EXAMPLE_JZUFI_2_2_0_ONLINEDIENST = """
            {
                "id": {
                    "value": "868",
                    "schemeAgencyID": "L100042"
                },
                "idSekundaer": [],
                "name": [
                    {
                        "name": [
                            {
                                "value": "Beispiel-Onlinedienst",
                                "languageCode": "de"
                            }
                        ]
                    }
                ],
                "beschreibung": [],
                "kurzbeschreibung": [],
                "zustaendigkeit": [],
                "synonym": [],
                "sprachversion": [
                    {
                        "languageCode": "de",
                        "sprachbezeichnungDeutsch": "Deutsch",
                        "sprachbezeichnungNativ": "Deutsch"
                    }
                ],
                "gueltigkeit": [
                    {
                        "beginn": "1699-12-31+01:00",
                        "ende": "2050-02-01+01:00",
                        "zusatz": []
                    }
                ]
            }
        """;

        public static final String RESPONSE_EXAMPLE_JZUFI_2_3_1_ONLINEDIENST = """
            {
                "id": {
                    "value": "9137467",
                    "schemeAgencyID": "L100012",
                    "schemeID": "OID"
                },
                "idSekundaer": [],
                "name": [
                    {
                        "name": [
                            {
                                "value": "Beispiel-Onlinedienst",
                                "languageCode": "de"
                            }
                        ]
                    }
                ],
                "beschreibung": [],
                "kurzbeschreibung": [],
                "zustaendigkeit": [],
                "synonym": [],
                "sprachversion": [
                    {
                        "languageCode": "de",
                        "sprachbezeichnungDeutsch": "Deutsch",
                        "sprachbezeichnungNativ": "Deutsch",
                        "kennzeichenMaschinelleUebersetzung": false
                    }
                ],
                "kennzeichenOeffentlichAnzeigen": true,
                "gueltigkeit": []
            }
        """;
    }
}
