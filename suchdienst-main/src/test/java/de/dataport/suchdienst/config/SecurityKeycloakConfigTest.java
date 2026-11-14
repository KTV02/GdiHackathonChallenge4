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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SecurityKeycloakConfigTest {

    @Test
    @DisplayName("Berechnung der Endpoint-Patterns zu den Swagger-Pfaden")
    void getSwaggerEndpointPatterns() {
        WebSecurityConfig config = Mockito.mock(WebSecurityConfig.class, CALLS_REAL_METHODS);

        // Zu den Swagger Properties springdoc.api-docs.path (default '/v3/api-docs') und springdoc.swagger-ui.path
        // (default: '/swagger-ui.html') müssen drei Pfad-Patterns für die Freigabe generiert werden. Bei
        // unveränderten property values sind das '/v3/api-docs/**', '/swagger-ui.html/**' und '/swagger-ui/**'.
        // Wichtig ist, dass zu einem swaggerUiPath der Form '/a/b/c' von Swagger auch immer Pfade der Art
        // '/a/b/swagger-ui' gebildet werden. Die Patterns werden dann aus mit Pfaden mit angehängtem Wildcard "/**"
        // gebildet, mit oder ohne Slash, je nachdem, ob die Pfad-Properties mit einem Slash enden.

        assertThat(
            config.getSwaggerEndpointPatterns("/v3/api-docs", "/swagger-ui.html"),
            hasItems("/v3/api-docs/**", "/swagger-ui.html/**", "/swagger-ui/**")
        );

        assertThat(
            config.getSwaggerEndpointPatterns("/apidocs", null),
            hasItems("/apidocs/**", "/swagger-ui.html/**", "/swagger-ui/**")
        );

        assertThat(
            config.getSwaggerEndpointPatterns("/apidocs", ""),
            hasItems("/apidocs/**", "/swagger-ui.html/**", "/swagger-ui/**")
        );

        assertThat(
            config.getSwaggerEndpointPatterns("/apidocs", "/a/b"),
            hasItems("/apidocs/**", "/a/b/**", "/a/swagger-ui/**")
        );

        assertThat(
            config.getSwaggerEndpointPatterns("/apidocs/", "/a/b/"),
            hasItems("/apidocs/**", "/a/b/**", "/a/b/swagger-ui/**")
        );

        // Die Eingaben "", "/" und alles mit einem "*" sind nicht erlaubt, da man damit alle oder viele Endpunkte
        // freischalten würde. Entsprechend wird die Methode ein Warnung loggen und eine leere Liste
        // zurück geben.
        assertThat(config.getSwaggerEndpointPatterns("", "/a/b/"), Matchers.hasSize(0));

        assertThat(config.getSwaggerEndpointPatterns("/", "/a/b/"), Matchers.hasSize(0));

        assertThat(config.getSwaggerEndpointPatterns("/x/*/y", "/a/b/"), Matchers.hasSize(0));

        assertThat(config.getSwaggerEndpointPatterns("/a", "/"), Matchers.hasSize(0));

        assertThat(config.getSwaggerEndpointPatterns("/a", "*/ab"), Matchers.hasSize(0));
    }
}
