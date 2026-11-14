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
package de.dataport.suchdienst.indexer.controller;

import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import de.dataport.suchdienst.indexer.service.LeikaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/leistungskatalog")
@Tag(name = "Management des Leistungskatalogs")
@Slf4j
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class LeistungskatalogController {

    private final LeikaService leikaService;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> updateTask;

    private final ImportSchedulingService importSchedulingService;

    public LeistungskatalogController(
        LeikaService leikaService,
        @Qualifier("singleTaskExecutor") TaskScheduler taskScheduler,
        ImportSchedulingService importSchedulingService
    ) {
        this.leikaService = leikaService;
        this.taskScheduler = taskScheduler;
        this.importSchedulingService = importSchedulingService;
    }

    @PostMapping("update")
    @Operation(
        summary = "Startet den Aufbau des Leistungskatalog",
        description = "Der Aufruf zum Aufbau des Leistungskatalogs wird asynchron bearbeitet."
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
    @PreAuthorize(AuthorityRoles.Codelist.UPLOAD)
    public ResponseEntity<HttpStatus> updateLeistungskatalog() {
        if (updateTask == null || updateTask.isDone()) {
            updateTask = taskScheduler.schedule(() -> {
                if (leikaService.importLeistungskatalog()) {
                    importSchedulingService.recalcAllVlIndexesAndWait();
                    log.info(
                        "Update der Verwaltungsleistungen nach Leistungskatalog Update abgeschlossen"
                    );
                }
            }, Instant.now());
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
