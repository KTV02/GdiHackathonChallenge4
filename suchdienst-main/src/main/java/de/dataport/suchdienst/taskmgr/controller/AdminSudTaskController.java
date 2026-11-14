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
package de.dataport.suchdienst.taskmgr.controller;

import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import de.dataport.suchdienst.taskmgr.model.out.SudTasksSnapshot;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sudtasks")
@Tag(name = "Admin Endpunkte zum Steuern des SudTaskMgr")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminSudTaskController {

    private final SudTaskMgr sudTaskMgr;

    public AdminSudTaskController(SudTaskMgr sudTaskMgr) {
        this.sudTaskMgr = sudTaskMgr;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Daten über Tasks.",
        description = "Liefert Daten über alle laufenden und wartenden Tasks"
    )
    @PreAuthorize(AuthorityRoles.Backoffice.READ_TASKS)
    public ResponseEntity<SudTasksSnapshot> getTasksSnapshot() {
        return ResponseEntity.status(HttpStatus.OK).body(sudTaskMgr.getTasksSnapshot());
    }
}
