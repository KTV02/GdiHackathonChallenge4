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
package de.dataport.suchdienst.config;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-Controller für den WebMvcConfigTest. Wird von der Application nicht einbezogen.
 * Dieser Test-Code ist NICHT DEPRECATED, hier wird getestet, ob die Annotation Auswirkung auf die Responses hat.
 */
@RestController
@Deprecated(forRemoval = false, since = "1.0")
@ApiAudienceMarker(value = ApiAudience.INTERNAL)
public class DeprecatedTestController {

    @GetMapping(path = "/deprecatedController/methodA")
    @Operation(
        summary = "Endpunkt zum Test des DeprecationHeader-HandlerInterceptor, ganze Klasse ist deprecated"
    )
    public String methodA() {
        return "A";
    }
}
