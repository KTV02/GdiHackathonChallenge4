// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Beta-Endpunkt zur Detailausgabe von Organisationseinheiten.
package de.dataport.suchdienst.suchen.controller.beta;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.suchen.model.mapper.OrganisationseinheitBetaMapper;
import de.dataport.suchdienst.suchen.model.out.organisation.OrganisationUnitDetail;
import de.dataport.suchdienst.suchen.service.OrganisationUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@ApiAudienceMarker(ApiAudience.BETA)
@Tag(
    name = "Beta Organisationseinheiten",
    description = "Endpunkte für den Zugriff auf Organisationseinheiten."
)
@Validated
public class OrganisationUnitBetaController {

    private final OrganisationUnitService organisationUnitService;

    public OrganisationUnitBetaController(OrganisationUnitService organisationUnitService) {
        this.organisationUnitService = organisationUnitService;
    }

    @GetMapping("/v6beta1/organisationunits/{oeid}/detail")
    @Operation(
        summary = "Beta: Gibt die Organisationseinheit zurück",
        description = """
        Gibt die Organisationseinheit mit der angeforderten ID zurück. \
        Der Endpunkt liefert die zum Zeitpunkt der Anfrage nach Gültigkeitszeitraum aktuellen Daten zurück. \
        Die Rolle sowie die Kontaktpersonen und Formulare der Organisationseinheit ergeben sich aus der übergebenen 'lbId'. \
        Werden keine 'lbId' und 'ars' übergeben, wird die Rolle auf 'Ansprechpunkt' gesetzt, und es werden keine Kontaktpersonen und Formulare geliefert. \
        Organisationseinheit, Kontaktpersonen und Kommunikationssysteme enthalten das Kennzeichen 'isPublicDisplayAllowed' \
        aus 'kennzeichenOeffentlichAnzeigen'. Das Kennzeichen wird auch für Kommunikationssysteme in Ortsangaben ausgegeben; \
        nicht öffentliche Einträge werden nicht anhand dieses Kennzeichens ausgefiltert."""
    )
    @ApiResponse(
        responseCode = "200",
        description = "Organisationseinheit für ID gefunden",
        content = @Content(schema = @Schema(implementation = OrganisationUnitDetail.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Organisationseinheit nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiErrorModel.class))
    )
    public OrganisationUnitDetail getOrganisationUnitDetail(
        @Parameter(
            description = "Die ID der Organisationseinheit",
            example = "L100042.OE.868",
            required = true
        ) @PathVariable(name = "oeid") @Size(min = 2) String oeId,
        @Parameter(
            description = "Filtert Kontaktpersonen und Formulare anhand der räumlichen Zuständigkeit, konkretisiert die Zuständigkeitsrolle"
        ) @RequestParam(name = "ars", required = false) @Size(min = 2, max = 12) String ars,
        @Parameter(
            description = "Filtert Kontaktpersonen und Formulare anhand der Zuständigkeit für die Leistung, konkretisiert die Zuständigkeitsrolle"
        ) @RequestParam(name = "lbId", required = false) String lbId
    ) {
        return organisationUnitService.getOrganisationUnitDetail(
            oeId,
            ars,
            lbId,
            LocalDate.now(ZoneId.systemDefault()),
            OrganisationseinheitBetaMapper.INSTANCE::mapV6Beta1
        );
    }
}
