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
package de.dataport.suchdienst.suchen.controller.beta;

import static de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode.FULL;
import static de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode.NONE;

import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.config.Profiles;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.elasticsearch.api.model.PersonalMattersMode;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.service.suggestion.SuggestionHelper;
import de.dataport.suchdienst.suchen.controller.ServiceDescriptionController;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionHierarchySearchResult;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchHit;
import de.dataport.suchdienst.suchen.model.out.leistung.resultlist.ServiceDescriptionSearchResult;
import de.dataport.suchdienst.suchen.service.ServiceDescriptionService;
import de.dataport.suchdienst.util.PageRequestCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("java:S1168") // Null als Return für eine Liste ist sehr ungewöhnlich. Hier ist es
// gewollt, die Koordinaten sind bei den verwendenden Stellen (in einem Outmodel) explizit @Nullable.
@RestController
@Validated
@ApiAudienceMarker(ApiAudience.BETA)
@Tag(
    name = "Beta Leistungen",
    description = "Beta Endpunkte zum Suchen und Finden von Verwaltungsleistungen." +
        " Sobald ein amtlicher Regionalschlüssel (ARS) ermittelt wurde, können Leistungen" +
        " (servicedescriptions) gesucht werden."
)
@Profile(Profiles.BETA_API_ACTIVE)
public class ServiceDescriptionBetaController implements PageRequestCheck {

    private final ServiceDescriptionService serviceDescriptionService;
    private final MessageSource messageSource;
    private final PvogRequestContext context;

    public ServiceDescriptionBetaController(
        ServiceDescriptionService serviceDescriptionService,
        MessageSource messageSource,
        PvogRequestContext context
    ) {
        this.serviceDescriptionService = serviceDescriptionService;
        this.messageSource = messageSource;
        this.context = context;
    }

    @GetMapping(value = "/v8beta1/servicedescriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Beta: Sucht nach Leistungen in einem Gebiet und liefert eine Trefferliste.",
        description = """
        Beta-Endpunkt der Such-API. Liefert eine paginierte Trefferliste von Leistungen als reduzierte Sicht auf die Detaildaten einer Leistung.
        Onlinedienst-Links enthalten dynamische Parameter, die Suche ist unschärfetolerant (Fuzzy-Suche) und der Gültigkeitszeitraum wird auf den Zeitpunkt der Anfrage gesetzt.
        Um fachlich korrekte Treffer zu bekommen, sollte nur nach dem ARS von Gemeinden gesucht werden.
        Achtung: Es handelt sich um eine Beta-Schnittstelle, deren Datenmodell und Verhalten sich noch ändern können.
        """
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine paginierte Trefferliste inklusive Facetten.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServiceDescriptionSearchResult.class)
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
        }
    )
    public ServiceDescriptionSearchResult getServiceDescriptions(
        @Parameter(
            description = ServiceDescriptionController.DESCR_ARS_LB_WITH_HIERARCHY,
            example = "010020000000",
            required = true
        ) @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars, // Regex Syntax [0-9] bei Fehlermeldungen
        @Parameter(
            description = "Der Wert, nach dem gesucht werden soll",
            example = "[\"Ingen\"]",
            array = @ArraySchema(schema = @Schema(type = "string"))
        ) @RequestParam(name = "q", required = false) @Size(min = 1) List<@Size(
            min = 2
        ) String> searchValue,
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
            description = "Ob die Ars-Hierarchie bei der Suche berücksichtigt werden soll"
        ) @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy,
        @Parameter(description = "Ab Seite") @RequestParam(value = "page", defaultValue = "0") @Min(
            0
        ) @Max(100) int page,
        @Parameter(description = "Anzahl der Ergebnisse pro Seite") @RequestParam(
            value = "size",
            defaultValue = "10"
        ) @Min(5) @Max(9999) int pageSize
    ) {
        checkPageParameters(page, pageSize, messageSource);

        if (searchValue == null && (personalMatters == null || personalMatters.isEmpty())) {
            throw new SuchenException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_request_search_params",
                    null,
                    context.getLocale()
                )
            );
        }

        // Implizite Suche nach LeikaId, wenn es genau ein Suchkriterium gibt und dieses syntaktisch
        // eine LeikaId ist (analog zur V7-API, PV-9484).
        final Set<String> searchValues;
        final List<String> leikaIds;
        if (searchValue == null || searchValue.isEmpty()) {
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
        return serviceDescriptionService.searchHits(
            searchValues,
            ars,
            useArsHierarchy,
            personalMatters,
            personalMattersMode,
            page,
            pageSize,
            context.getLocale(),
            leikaIds
        );
    }

    @GetMapping(
        value = "/v4beta1/servicedescriptions/leikaid",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Beta: Sucht mit LeiKa-Schlüsseln nach Leistungen.")
    @ApiResponse(
        responseCode = "200",
        description = "Liefert eine paginierte Trefferliste für die angefragten LeiKa-Schlüssel.",
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
    public PagedModel<ServiceDescriptionSearchHit> findByLeikaId(
        @RequestParam(value = "leikaIds") List<@Size(min = 14, max = 21) String> leikaIds,
        @RequestParam(name = "q", required = false) List<@Size(min = 3) String> searchValue,
        @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) @Max(100) int page,
        @RequestParam(value = "size", defaultValue = "10") @Min(5) @Max(9999) int pageSize,
        @RequestParam(
            value = "useArsHierarchy",
            required = false,
            defaultValue = "true"
        ) boolean useArsHierarchy
    ) {
        checkPageParameters(page, pageSize, messageSource);
        Set<String> searchValues =
            searchValue == null
                ? Set.of()
                : SuggestionHelper.addSpaceBetweenWordAndNumber(searchValue);
        return serviceDescriptionService.searchHitsByLeikaId(
            searchValues,
            ars,
            useArsHierarchy,
            page,
            pageSize,
            context.getLocale(),
            leikaIds
        );
    }

    @GetMapping(
        value = "/v3beta1/servicedescriptions/leikahierarchy",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Beta: Sucht anhand der Hierarchie von LeiKa-Schlüsseln.",
        description = """
        Beta-Endpunkt der Such-API. Sucht zunächst nach Leistungen mit exakt den angefragten LeiKa-Schlüsseln.
        Bleibt diese Suche ohne Treffer, wird in der LeiKa-Hierarchie ausgewichen: zuerst eine Ebene nach unten
        (detailliertere Leistungen), anschließend schrittweise nach oben (allgemeinere Leistungen), bis Treffer
        gefunden werden.
        Welche Stufe gegriffen hat, wird im Objekt 'leikaMatch' über 'matchType' (EXACT, DESCENDANT, ANCESTOR, NONE)
        gekennzeichnet. 'effectiveLeikaIds' enthält die vollständigen, 14-stelligen Schlüssel, für die die Treffer
        tatsächlich ermittelt wurden.
        Achtung: Es handelt sich um eine Beta-Schnittstelle, deren Datenmodell und Verhalten sich noch ändern können.
        """
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert eine paginierte Trefferliste inklusive Angabe, wie die Treffer" +
                    " zu den angefragten LeiKa-Schlüsseln ermittelt wurden.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServiceDescriptionHierarchySearchResult.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen," +
                    " z.B. wenn die LeiKa-Schlüssel unterschiedliche Hierarchie-Ebenen betreffen.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorModel.class)
                )
            ),
        }
    )
    public ServiceDescriptionHierarchySearchResult findByLeikaHierarchy(
        @RequestParam(value = "leikaIds") List<@Size(max = 14, min = 14) String> leikaIds,
        @RequestParam(name = "ars") @Size(max = 12) @SuppressWarnings("java:S6353") @Pattern(
            regexp = "[0-9]*"
        ) String ars,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) @Max(100) int page,
        @RequestParam(value = "size", defaultValue = "10") @Min(5) @Max(9999) int pageSize
    ) {
        checkPageParameters(page, pageSize, messageSource);
        return serviceDescriptionService.searchHitsByLeikaHierarchy(
            ars,
            page,
            pageSize,
            context.getLocale(),
            leikaIds
        );
    }
}
