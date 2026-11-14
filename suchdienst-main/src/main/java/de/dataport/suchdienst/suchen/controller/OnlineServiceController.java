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

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceOutModelV2;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Onlinedienste", description = "Endpunkte zum separaten Abrufen der Onlinedienste.")
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
public class OnlineServiceController {

    private final OnlineDienstService onlineDienstService;

    public OnlineServiceController(OnlineDienstService onlineDienstService) {
        this.onlineDienstService = onlineDienstService;
    }

    @GetMapping("/v2/onlineservices/detail")
    @Operation(
        summary = "Liefert einen Onlinedienst",
        description = "Liefert einen Onlinedienst mit der angeforderten ID zurück"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Onlinedienst für ID gefunden",
        content = @Content(
            examples = {
                @ExampleObject(
                    name = "Onlinedienst",
                    value = """
                    {
                        "id": "L100042.OD.OD_xzufi22DeleteZSOD_77777_0001",
                        "name": "Link bzw. URL zum Online-Dienst",
                        "helpLinks": [],
                        "paymentMethods": [],
                        "identificationMethods": [],
                        "links": [
                            {
                                "uri": "https://lmgtfy.com/?q=OD_xzufi22DeleteZSOD_77777_0001",
                                "title": "OD_xzufi22DeleteZSOD_77777_0001_Title",
                                "replace": false,
                                "dynamicParameters": []
                            }
                        ],
                        "validityPeriods": [
                            {
                                "gte": "1970-01-01",
                                "lte": "2999-12-31"
                            }
                        ],
                        "lastUpdate": "2022-10-06T07:24:24.356725600Z",
                        "parameters": [],
                        "executionLanguages": [],
                        "executionNationalities": [],
                        "levelOfConfidence": "00",
                        "languages": [
                            "de",
                            "en"
                        ],
                        "languageComplete": {
                            "en": true
                        },
                        "arsFederalstate": [
                            "12"
                        ],
                        "relations": [
                            {
                                "id": "L100042.ZSOD.LB_xzufi22DeleteZSOD_77777_0003.OD_xzufi22DeleteZSOD_77777_0001.095620000000",
                                "lbId": "L100042.LB.LB_xzufi22DeleteZSOD_77777_0001",
                                "ars": "012012012012",
                                "parentId": "L100042.OD.OD_xzufi22DeleteZSOD_77777_0001",
                                "typ": "ONLINESERVICE"
                            }
                        ],
                        "logos": []
                    }"""
                ),
            },
            schema = @Schema(implementation = OnlineServiceOutModelV2.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Onlinedienst für ID nicht gefunden",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ApiErrorModel.class)
        )
    )
    public OnlineServiceOutModelV2 getDetailOdV2(
        @Parameter(description = "Die ID des Onlinedienstes", required = true) @RequestParam(
            name = "q"
        ) @Size(min = 2) String searchValue,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return onlineDienstService.getOnlineServiceV2ById(searchValue, validDate);
    }
}
