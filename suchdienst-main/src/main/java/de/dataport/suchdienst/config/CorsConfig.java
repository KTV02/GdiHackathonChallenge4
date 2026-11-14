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

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

public interface CorsConfig {
    default UrlBasedCorsConfigurationSource getCorsConfiguration(CustomCorsProperties properties) {
        UrlBasedCorsConfigurationSource config = new UrlBasedCorsConfigurationSource();

        if (properties.isEnabled() && null != properties.getCorsConfiguration()) {
            config.registerCorsConfiguration("/**", properties.getCorsConfiguration());
        }

        return config;
    }
}
