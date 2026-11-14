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
 */
@RestController
@ApiAudienceMarker(value = ApiAudience.INTERNAL)
public class PartiallyDeprecatedTestController {

    @GetMapping(path = "/partiallyDeprecatedController/methodA")
    @Operation(
        summary = "Endpunkt zum Test des DeprecationHeader-HandlerInterceptor, nicht deprecated",
        deprecated = false
    )
    public String methodA() {
        return "A";
    }

    @GetMapping(path = "/partiallyDeprecatedController/methodB")
    @Operation(
        summary = "Endpunkt zum Test des DeprecationHeader-HandlerInterceptor, Operation ist deprecated",
        deprecated = true
    )
    public String methodB() {
        return "B";
    }

    @GetMapping(path = "/partiallyDeprecatedController/methodC")
    @Operation(
        summary = "Endpunkt zum Test des DeprecationHeader-HandlerInterceptor, Methode ist deprecated"
    )
    @Deprecated(forRemoval = true, since = "1.0")
    public String methodC() {
        return "C";
    }
}
