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

import static de.dataport.suchdienst.suchen.controller.ServiceDescriptionController.DESCR_ARS_SEARCH;

import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.suchen.model.mapper.OrganisationseinheitMapper;
import de.dataport.suchdienst.suchen.model.out.organisation.DetailOeOutModelV5;
import de.dataport.suchdienst.suchen.model.out.organisation.OeInfoOutModel;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import de.dataport.suchdienst.suchen.service.OrganisationModelConverterService;
import de.dataport.suchdienst.suchen.service.OrganisationUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
    name = "Organisationseinheiten",
    description = "Endpunkte für den Zugriff auf Organisationseinheiten. " +
        "Zumeist Behörden sind diese Organisationen die Orte der Inanspruchnahme der gesuchten Leistung." +
        " Einer gesuchten Leistung können mehrere Organisationseinheiten zugeordnet sein."
)
@Validated
public class OrganisationUnitController {

    private final PvogRequestContext context;
    private final ElasticPublicSearchConnector publicSearchConnector;
    private final OrganisationUnitService organisationUnitService;
    private final OrganisationModelConverterService oeModelConverter;

    public OrganisationUnitController(
        PvogRequestContext context,
        ElasticPublicSearchConnector publicSearchConnector,
        OrganisationUnitService organisationUnitService,
        OrganisationModelConverterService oeModelConverterService
    ) {
        this.context = context;
        this.publicSearchConnector = publicSearchConnector;
        this.organisationUnitService = organisationUnitService;
        this.oeModelConverter = oeModelConverterService;
    }

    public static final String OE_404_EXAMPLE_OBJECT = """
        {
        \t"timestamp": "2022-10-10T09:21:56.440533600Z",
        \t"code": "NOT_FOUND",
        \t"message": "Die angegebene Resource konnte nicht gefunden werden.",
        \t"codeNumber": 1906
        }
    """;

    /**
     * Neue Version des Endpunktes, siehe dazu PV-10975.
     */
    @GetMapping("/v5/organisationunits/detail")
    @Operation(
        summary = "Gibt die Organisationseinheit zurück",
        description = """
        Gibt die Organisationseinheit mit der angeforderten ID zurück. \
        Die Rolle sowie die Kontaktpersonen und Formulare der Organisationseinheit ergeben sich aus der übergebenen 'lbId'. \
        Werden keine 'lbId' und 'ars' übergeben, wird die Rolle auf 'Ansprechpunkt' gesetzt, und es werden keine Kontaktpersonen und Formulare geliefert."""
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Organisationseinheit für ID gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DetailOeOutModelV5.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Organisationseinheit nicht gefunden",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(
                            name = "Organisationseinheit nicht gefunden",
                            value = OE_404_EXAMPLE_OBJECT
                        ),
                    }
                )
            ),
        }
    )
    @JsonView(JsonViews.Detail.class)
    public DetailOeOutModelV5 getDetailOeOutModelV5(
        @Parameter(
            description = "Die ID der Organisationseinheit",
            examples = { @ExampleObject("L100042.OE.868") },
            required = true
        ) @RequestParam(name = "q") @Size(min = 2) String oeId,
        @Parameter(
            description = "Filtert Kontaktpersonen und Formulare anhand der räumlichen Zuständigkeit, konkretisiert die Zuständigkeitsrolle"
        ) @RequestParam(name = "ars", required = false) @Size(min = 2, max = 12) String ars,
        @Parameter(
            description = "Filtert Kontaktpersonen und Formulare anhand der Zuständigkeit für die Leistung, konkretisiert die Zuständigkeitsrolle"
        ) @RequestParam(name = "lbId", required = false) String lbId,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        return organisationUnitService.getOrganisationUnitDetail(
            oeId,
            ars,
            lbId,
            validDate,
            OrganisationseinheitMapper.INSTANCE::mapV5
        );
    }

    /**
     * @deprecated PV-15231: Die Inhalte dieses Endpunktes können stattdessen über den Endpunkt "/v6/servicedescriptions/{lbid}/detail" abgerufen und ausgelesen werden.
     */
    @Deprecated(since = "2026-10")
    @GetMapping("/v2/organisationunits/titles")
    @Operation(
        summary = "Liefert die Titel, Rollen und Kriterien von Organisationseinheiten",
        description = "Gibt die für eine Leistung zuständigen " +
            "Organisationseinheiten mit ihrem Titel, sowie Rollen und Kriterien zurück"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Titel, Rollen und Kriterien der vorhandenen Organisationseinheiten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = OeInfoOutModel.class))
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
                responseCode = "503",
                description = "Dieser Endpunkt ist kurzzeitig nicht verfügbar",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    public List<OeInfoOutModel> getOrganisationUnitTitlesV2(
        @Parameter(description = DESCR_ARS_SEARCH, required = true) @Size(
            max = 12
        ) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*" /*Bewusst keine Verwendung von \d, da \d potenziell mehr Zeichen erlaubt.*/
        ) @RequestParam(name = "ars") String ars, // TODO: Es gibt keine Mindestlänge und die Regex erlaubt einen leeren String, obwohl der Parameter eine Pflichtangabe ist
        @Parameter(
            description = "Die ID der Leistung",
            examples = { @ExampleObject("L100042.LB.27254") },
            required = true
        ) @RequestParam(name = "lbId") String lbId,
        @Parameter(description = "Das Gültigkeitsdatum in der Form yyyy-MM-dd") @RequestParam(
            value = "validDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate validDate
    ) {
        //TODO ValidDate auswerten
        Locale requestedLanguage = context.getLocale();
        final var valuesToInclude = Locale.GERMAN.equals(requestedLanguage)
            ? List.of(
                  "id",
                  "organisationseinheiten.id",
                  "organisationseinheiten.relations",
                  "organisationseinheiten.criteria",
                  "organisationseinheiten.oeffentlichAnzeigen",
                  "organisationseinheiten.name"
              )
            : List.of(
                  "id",
                  "organisationseinheiten.id",
                  "organisationseinheiten.relations",
                  "organisationseinheiten.criteria",
                  "organisationseinheiten.oeffentlichAnzeigen",
                  "organisationseinheiten.name",
                  String.format(
                      "organisationseinheiten.otherLanguages.%s.name",
                      requestedLanguage.getLanguage()
                  )
              );
        List<OrganisationseinheitVL> oes = publicSearchConnector.searchOrganisation(
            lbId,
            ars,
            validDate,
            valuesToInclude
        );
        return oeModelConverter.buildOeInfoOutModels(oes, ars, requestedLanguage);
    }

    @Operation(
        summary = "Finde alle ARS einer Organisationseinheit die Leistungen unter einer LeiKa-ID anbieten.",
        description = """
        Gibt eine Liste von Amtlichen Regionalschlüsseln (ARS) zurück,\s
        für die eine Organisationseinheit eine Leistung anbietet,\s
        welche im Leistungskatalog unter der angegebenen LeiKa-ID geführt wird."""
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liste von Amtlichen Regionalschlüsseln (ARS)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = String.class))
                )
            ),
            @ApiResponse(
                responseCode = "503",
                description = "Dieser Endpunkt ist kurzzeitig nicht verfügbar",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
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
                            value = """
                            {
                              "timestamp": "2025-08-27T07:48:29.766756500Z",
                              "code": "BAD_REQUEST",
                              "message": "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen.",
                              "fields": {
                                "leikaId": "Größe muss zwischen 14 und 21 sein"
                              },
                              "codeNumber": 1901
                            }
                            """
                        ),
                    },
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    @GetMapping("/v1/organisationunits/{oeId}/ars")
    public List<String> findArsByOeIdAndLeikaId(
        @PathVariable @Parameter(
            description = "Die ID der Organisationseinheit",
            example = "L100022.OE.6001887",
            required = true
        ) @Size(min = 2, max = 255) String oeId,
        @Parameter(
            description = "Die LeiKa-ID",
            example = "\"99061006000000\"",
            required = true
        ) @RequestParam @Size(min = 14, max = 21) String leikaId
    ) {
        return organisationUnitService.findArsByOeIdAndLeikaId(leikaId, oeId);
    }
}
