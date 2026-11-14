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

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import de.dataport.suchdienst.backoffice.service.AdminDataOverviewService;
import de.dataport.suchdienst.config.AuthorityRoles;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dataoverview")
@Slf4j
@Tag(name = "Admin Endpunkte zum Abrufen einer Datenübersicht")
@ApiAudienceMarker(ApiAudience.INTERNAL)
@Conditional(OnMasterCondition.class)
@SecurityRequirement(name = "pvog_auth")
public class AdminDataOverviewController {

    private final AdminDataOverviewService dataOverviewService;

    public AdminDataOverviewController(AdminDataOverviewService dataOverviewService) {
        this.dataOverviewService = dataOverviewService;
    }

    @GetMapping
    @Operation(
        summary = "Gibt eine Übersicht über den Zustand der Daten des Suchdienstes zurück.",
        description = "Gibt eine Übersicht über den Zustand der Daten des Suchdienstes zurück. Dies umfasst die Azahl an Geschäftsobjekten in den XZuFi-Indizes, die Verwaltungsleistungen und eine Sicht auf die verschiedenen Warteschlangen."
    )
    @PreAuthorize(AuthorityRoles.Backoffice.READ_DATA_OVERVIEW)
    public ResponseEntity<List<DataOverviewOutModel>> getDataOverview() {
        var outModels = dataOverviewService.getDataOverview();
        return ResponseEntity.ok(outModels);
    }
}
