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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Zusammenfassung der Keycloak Properties in eine Klasse. Da die Keycloak-Adapter-Bibliothek nicht mehr verwendet
 * wird, sind hier nicht alle alten Properties vorhanden. In Zukunft sollten die Properties hier auch das
 * 'suchdienst.'-Prefix bekommen, da es jetzt 'nur noch' SuD-spezifische Properties sind.
 */
@Configuration
@Getter
public class KeycloakProperties {

    // Diese OAuth jwk-set-uri ist bei Keycloak-Servern fest so vorgegeben und wird hier aus bereits vorhanden
    // Properties erstellt. Sollte diese URI mal anders lauten, brauchen wir eine neu Property für dieses Pattern
    // oder gleich für die vollständige URL.
    private static final String JWK_SET_URI_PATTERN = "%s/realms/%s/protocol/openid-connect/certs";

    private final String authServerUrl;
    private final String realm;
    private final String resource;
    private final String jwkSetUri;

    public KeycloakProperties(
        @Value("${keycloak.auth-server-url}") String authServerUrl,
        @Value("${keycloak.realm}") String realm,
        @Value("${keycloak.resource}") String resource
    ) {
        // Sicherstellen, dass die gespeicherte URL nicht mit einem Slash endet. So kann sie korrekt im Pattern
        // verwendet werden.
        String tmp = authServerUrl;
        while (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        this.authServerUrl = tmp;
        this.realm = realm;
        this.resource = resource;
        this.jwkSetUri = JWK_SET_URI_PATTERN.formatted(this.authServerUrl, this.realm);
    }
}
