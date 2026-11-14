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

import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.config.Profiles;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import de.dataport.suchdienst.util.PageRequestCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
    name = "Verwaltungsleistungen",
    description = "Endpunkte zum Suchen und Finden von Verwaltungsleistungen." +
        " Sobald ein amtlicher Regionalschlüssel (ARS) ermittelt wurde, können Leistungen" +
        " (servicedescriptions) gesucht werden."
)
@Profile(Profiles.VERWALTUNGSLEISTUNG_ACTIVE)
public class VerwaltungsleistungController implements PageRequestCheck {

    private final MessageSource messageSource;
    private final ElasticPublicSearchConnector publicSearchConnector;
    private final PvogRequestContext context;

    public VerwaltungsleistungController(
        ElasticPublicSearchConnector publicSearchConnector,
        MessageSource messageSource,
        PvogRequestContext context
    ) {
        this.publicSearchConnector = publicSearchConnector;
        this.messageSource = messageSource;
        this.context = context;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
    }

    @GetMapping(
        value = "/v1/verwaltungsleistung/{ars}/",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Gibt eine vollständige Leistung.",
        description = "Gibt eine Leistung mit allen Informationen und allen zuständigen Organisationseinheiten zurück"
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "Liefert eine Liste von Leistungen."),
            @ApiResponse(
                responseCode = "404",
                description = "Es wurde kein Ergebnis gefunden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @JsonView(JsonViews.Detail.class)
    public Verwaltungsleistung getVerwaltungsleistung(
        @PathVariable @Parameter(
            description = "Wenn vorhanden dann der ARS, ansonsten der AGS des Ortes für den die Leistung gelten soll",
            required = true
        ) @Size(min = 12, max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*" /*Bewusst keine Verwendung von \d, da \d potenziell mehr Zeichen erlaubt.*/
        ) String ars,
        @Parameter(
            description = "Die ID der Leistung, deren Details geladen werden sollen",
            required = true
        ) @RequestParam(name = "q") @Size(min = 2) String id,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        Locale searchLanguage = context.getLocale();
        boolean excludeOtherThanGerman =
            searchLanguage.equals(Locale.GERMAN) || searchLanguage.equals(Locale.GERMANY);

        return publicSearchConnector
            .searchLbByIdAndArs(id, ars, true, excludeOtherThanGerman, validDate)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", null, Locale.GERMAN)
                )
            );
    }
}
