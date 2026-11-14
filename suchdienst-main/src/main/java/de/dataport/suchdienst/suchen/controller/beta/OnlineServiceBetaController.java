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
package de.dataport.suchdienst.suchen.controller.beta;

import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OnlineServiceIteratorResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResponse;
import de.dataport.suchdienst.suchen.service.OnlineDienstService;
import de.dataport.suchdienst.util.PageRequestCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Beta Onlinedienste", description = "Endpunkte zum Abrufen der Onlinedienste via ARS.")
@Validated
@Slf4j
@ApiAudienceMarker(ApiAudience.BETA)
public class OnlineServiceBetaController implements PageRequestCheck {

    private final OnlineDienstService onlineDienstService;

    public OnlineServiceBetaController(OnlineDienstService onlineDienstService) {
        this.onlineDienstService = onlineDienstService;
    }

    @GetMapping("/v3beta1/onlineservices/{odId}/detail")
    @Operation(
        summary = "Beta: Liefert einen Onlinedienst",
        description = "Liefert einen Onlinedienst mit der angeforderten ID zurück."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Onlinedienst für ID gefunden",
        content = @Content(schema = @Schema(implementation = OnlineServiceDetail.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Onlinedienst für ID nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public OnlineServiceDetail getOnlineServiceDetail(
        @PathVariable @Parameter(description = "Die ID des Onlinedienstes", required = true) @Size(
            min = 2
        ) String odId
    ) {
        return onlineDienstService.getOnlineServiceV3Beta1ById(odId);
    }

    @GetMapping("/v1beta2/onlineservices")
    @Operation(
        summary = "Liefert Onlinedienste mit Relationen zu Leistungen innerhalb eines Gebiets (ARS-Bereichs)",
        description = """
        Liefert Onlinedienste innerhalb des angegebenen ARS-Bereichs.
        Die Filterung erfolgt entlang der ARS-Hierarchie sowohl nach oben als auch nach unten.

        Filterung nach oben: Bei der Suche nach der ARS `091` (Oberbayern) werden Onlinedienste mit Zuständigkeit für `091` sowie für übergeordnete Ebenen geliefert, z.B. Bayern (`09`) oder Bund (`00`).

        Filterung nach unten: Bei der Suche nach `091` werden zusätzlich Onlinedienste für untergeordnete Ebenen bis hinunter zur Gemeindeebene geliefert, z.B. München (`091620000000`).
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Onlinedienste innerhalb eines ARS",
        content = @Content(schema = @Schema(implementation = OnlineServiceIteratorResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public OnlineServiceIteratorResponse getOnlineServiceWithRelations(
        @Parameter(
            name = "ars",
            in = ParameterIn.QUERY,
            description = "ARS",
            required = true,
            schema = @Schema(
                type = "string",
                pattern = "(?!00)\\d{2,12}",
                minLength = 2,
                maxLength = 12
            )
        ) @RequestParam(name = "ars") @Pattern(regexp = "(?!00)\\d{2,12}") String ars,
        @Parameter(
            name = "leikaIds",
            in = ParameterIn.QUERY,
            description = "Liste von Leika-Schlüsseln",
            array = @ArraySchema(schema = @Schema(type = "string"))
        ) @RequestParam(value = "leikaIds", required = false) List<@Size(
            min = 14,
            max = 21
        ) String> leikaIds,
        @RequestParam(name = "nextPageToken", required = false) String nextPageToken
    ) {
        String trimmedArs = ARS.of(ars).hierarchyTrim();
        return onlineDienstService.getOnlineServicesByArsViaOdRelationIndex(
            trimmedArs,
            leikaIds,
            nextPageToken
        );
    }

    @GetMapping("/v1beta2/onlineservices/{odId}/organisations")
    @Operation(
        summary = "Liefert Organisationseinheiten, die für einen Onlinedienst zuständig sind",
        description = """
        Liefert alle Organisationseinheiten mit ihren Rollen, die für den
        angegebenen Onlinedienst zuständig sind. Die Zuordnung erfolgt über
        die OE-Zuständigkeiten (ZS-OE), gefiltert entlang der ARS-Hierarchie.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheiten des Onlinedienstes",
        content = @Content(schema = @Schema(implementation = OrganisationResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Kein Onlinedienst mit der angegebenen ID gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public OrganisationResponse getOrganisationsByOnlineService(
        @Parameter(
            name = "odId",
            in = ParameterIn.PATH,
            description = "ID des Onlinedienstes",
            required = true
        ) @PathVariable String odId
    ) {
        return onlineDienstService.getOrganisationsByOdId(odId);
    }
}
