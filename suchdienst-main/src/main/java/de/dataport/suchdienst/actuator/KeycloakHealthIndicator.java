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
package de.dataport.suchdienst.actuator;

import de.dataport.suchdienst.config.KeycloakProperties;
import de.dataport.suchdienst.util.model.ProtocolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Component
public class KeycloakHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakHealthIndicator.class);
    private final RestTemplate restTemplate = new RestTemplate();

    private final JsonMapper mapper;
    private final String resourceUrl;

    public KeycloakHealthIndicator(KeycloakProperties keycloakProperties, JsonMapper mapper) {
        // Der health-Endpunkt ist auch im Keycloak hart verdrahtet
        resourceUrl = keycloakProperties.getAuthServerUrl() + "/health";
        LOGGER.info(
            "{}: URL für Keycloak Server Health Endpoint: {}",
            ProtocolType.SYSTEM,
            resourceUrl
        );
        this.mapper = mapper;
    }

    @Override
    public Health health() {
        // Stand Spring Boot 2.7: Die Aggregation von Status über mehrere Komponenten propagiert den worst case,
        // zumindest beim Standard Status Propagator. Wäre eine (vielleicht optionale) Komponente DOWN, wäre
        // die ganze Applikation DOWN und würde von Kubernetes abgeschossen werden.
        // Wenn das nicht gewünscht ist, muss der Status UP oder UNKNOWN sein.
        Health.Builder health = Health.unknown();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);
            // Http-Status muss OK sein, sonst würde eine HttpStatusCodeException geworfen
            var jsonNode = mapper.readTree(response.getBody());
            if (
                jsonNode.hasNonNull("status") &&
                "UP".equalsIgnoreCase(jsonNode.get("status").asString())
            ) {
                health.up();
            }
        } catch (Exception _) {
            // Mögliche Exceptions: HttpStatusCodeException, RestClientException, JsonProcessingException
            // Aber Details sind nicht wichtig, es soll kein Fehler protokolliert werden.
            // Status bleibt 'unknown'
        }

        return health.build();
    }
}
