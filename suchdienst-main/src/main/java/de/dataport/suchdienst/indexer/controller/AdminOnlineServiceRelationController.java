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
package de.dataport.suchdienst.indexer.controller;

import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.indexer.service.ImportSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/onlineservices/relations")
@Slf4j
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminOnlineServiceRelationController {

    private final ImportSchedulingService importSchedulingService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> calculateAllTask;

    public AdminOnlineServiceRelationController(
        ImportSchedulingService importSchedulingService,
        @Qualifier("singleTaskExecutor") TaskScheduler taskScheduler
    ) {
        this.importSchedulingService = importSchedulingService;
        this.taskScheduler = taskScheduler;
    }

    @PostMapping(value = "/calculate/all")
    @PreAuthorize(AuthorityRoles.OnlineServiceRelations.CALCULATE_ALL)
    public synchronized ResponseEntity<Void> calculateAll() {
        if (calculateAllTask == null || calculateAllTask.isDone()) {
            calculateAllTask = taskScheduler.schedule(() -> {
                importSchedulingService.recalcAllOdRelIndexesAndWait();
                log.info("Update der Onlinedienst-Relationen abgeschlossen");
            }, Instant.now());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping(value = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AuthorityRoles.OnlineServiceRelations.CALCULATE)
    @Operation(
        summary = "Berechnet Onlinedienste-Relationen",
        description = "Berechnet Onlinedienste-Relationen"
    )
    public ResponseEntity<Void> calculateByBody(
        @RequestBody @Valid AdminOnlineServiceRelationController.CalculateOnlineServicesRelationsRequest body
    ) {
        if (
            importSchedulingService.importOnlineServiceRelationsAsync(
                body.schemeAgencyId,
                body.producer
            )
        ) {
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public record CalculateOnlineServicesRelationsRequest(
        @NotNull String schemeAgencyId,
        @NotNull String producer
    ) {}
}
