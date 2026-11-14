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
package de.dataport.suchdienst.backoffice.controller;

import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/verwaltungsleistung")
@Tag(name = "Admin Endpunkte zum Steuern der Verwaltungsleistungen")
@Slf4j
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminVerwaltungsleistungController {

    private static final String REGEX = "^(00|01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16)$";

    private final ImportSchedulingService importSchedulingService;
    private final SudTaskMgr sudTaskMgr;

    private final LeikaService leikaService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> calculateAllTask;

    public AdminVerwaltungsleistungController(
        ImportSchedulingService importSchedulingService,
        SudTaskMgr sudTaskMgr,
        LeikaService leikaService,
        @Qualifier("singleTaskExecutor") TaskScheduler taskScheduler
    ) {
        this.importSchedulingService = importSchedulingService;
        this.sudTaskMgr = sudTaskMgr;
        this.leikaService = leikaService;
        this.taskScheduler = taskScheduler;
    }

    @GetMapping("uptodate")
    @Operation(
        summary = "Gibt an ob die Queue des internen Task-Managers des Suchdienstes leer ist.",
        description = "Schnittstelle zum bedingten Warten im Test. Gibt Einsicht darüber ob die Queue des internen Task-Managers des Suchdienstes leer ist und damit keine weiteren Verarbeitungen zu erwarten sind."
    )
    @PreAuthorize(AuthorityRoles.Verwaltungsleistung.READ_UPTODATE)
    public ResponseEntity<Boolean> isUpToDate() {
        var isUpToDate = sudTaskMgr.isEmpty();
        return ResponseEntity.status(HttpStatus.OK).body(isUpToDate);
    }

    /**
     * @deprecated Wird perspektivisch aus der API entfernt
     */
    @Hidden
    @PostMapping(value = "/calculate/all", params = { "updateLeistungskatalog" })
    @PreAuthorize(AuthorityRoles.Verwaltungsleistung.CALCULATE_ALL)
    @Operation(
        summary = "Verwaltungsleistungen Berechnung.",
        description = "Berechnet Verwaltungsleistungen aus der alten Struktur, optional mit Update des Leistungskatalogs",
        operationId = "calculateAllVerwaltungsleitungenByParams",
        deprecated = true
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Auftrag übernommen. Bearbeitung erfolgt in Kürze",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    oneOf = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Vorgang bereits gestartet",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
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
    @Deprecated(since = "11.10.1", forRemoval = true)
    public ResponseEntity<Void> calculateAllByParams(
        @Parameter(
            description = "Steuert, ob vor der Neuberechnung ein Update des Leistungskatalogs stattfindet"
        ) @RequestParam(
            name = "updateLeistungskatalog",
            defaultValue = "true"
        ) boolean updateLeistungskatalog
    ) {
        return calculateAll(updateLeistungskatalog);
    }

    @PostMapping(value = "/calculate/all")
    @PreAuthorize(AuthorityRoles.Verwaltungsleistung.CALCULATE_ALL)
    @Operation(
        summary = "Verwaltungsleistungen Berechnung.",
        description = "Berechnet Verwaltungsleistungen aus der alten Struktur, optional mit Update des Leistungskatalogs",
        operationId = "calculateAllVerwaltungsleitungenByBody"
    )
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Auftrag übernommen. Bearbeitung erfolgt in Kürze",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    oneOf = @Schema(implementation = String.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Vorgang bereits gestartet",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
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
    public ResponseEntity<Void> calculateAllByBody(
        @RequestBody CalculateAllVerwaltungsleistungenRequest body
    ) {
        return calculateAll(body.updateLeistungskatalog);
    }

    private ResponseEntity<Void> calculateAll(boolean updateLeistungskatalog) {
        if (calculateAllTask == null || calculateAllTask.isDone()) {
            calculateAllTask = taskScheduler.schedule(() -> {
                if (updateLeistungskatalog) {
                    leikaService.importLeistungskatalog();
                }
                importSchedulingService.recalcAllVlIndexesAndWait();
                log.info(
                    "Update der Verwaltungsleistungen nach Leistungskatalog Update abgeschlossen"
                );
            }, Instant.now());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping(value = "/calculate")
    @Operation(
        summary = "Berechnet Verwaltungsleistungen aus der alten Struktur",
        description = "Berechnet Verwaltungsleistungen aus der alten Struktur",
        operationId = "calculateVerwaltungsleitungenByBody"
    )
    @PreAuthorize(AuthorityRoles.Verwaltungsleistung.CALCULATE)
    public ResponseEntity<Void> calculateByBody(
        @RequestBody @Valid AdminVerwaltungsleistungController.CalculateVerwaltungsleistungenRequest body
    ) {
        return calculate(body.arsFederalState);
    }

    /**
     * @deprecated Wird perspektivisch aus der API entfernt
     */
    @Hidden
    @PostMapping(value = "/calculate", params = { "arsFederalState" })
    @Operation(
        summary = "Berechnet Verwaltungsleistungen aus der alten Struktur",
        description = "Berechnet Verwaltungsleistungen aus der alten Struktur",
        operationId = "calculateVerwaltungsleitungenByParams",
        deprecated = true
    )
    @PreAuthorize(AuthorityRoles.Verwaltungsleistung.CALCULATE)
    @Deprecated(since = "11.10.1", forRemoval = true)
    public ResponseEntity<Void> calculateByParams(
        @RequestParam(name = "arsFederalState") @Size(min = 2, max = 2) @Pattern(
            regexp = REGEX
        ) String arsFederalState
    ) {
        return calculate(arsFederalState);
    }

    private ResponseEntity<Void> calculate(String arsFederalState) {
        if (importSchedulingService.importIndexAsync(arsFederalState)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record CalculateVerwaltungsleistungenRequest(
        @Size(min = 2, max = 2) @Pattern(regexp = REGEX) String arsFederalState
    ) {}

    public record CalculateAllVerwaltungsleistungenRequest(
        @Schema(
            name = "updateLeistungskatalog",
            description = "Steuert, ob vor der Neuberechnung ein Update des Leistungskatalogs stattfindet",
            defaultValue = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        boolean updateLeistungskatalog
    ) {}
}
