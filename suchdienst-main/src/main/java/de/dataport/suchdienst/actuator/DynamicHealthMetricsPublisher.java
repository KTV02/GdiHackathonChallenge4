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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Diese Komponente registriert dynamisch Gauges für alle Komponenten des Health Endpoints.
 * Jedes Mal, wenn die Metrik abgefragt wird, wird live Status der zugehörigen Komponente ermittelt.
 * Die Gauges haben den Namen "app_health_component_status" und geben 1 für "UP" und 0 für "DOWN" zurück.
 * Sie sind mit den Tags "component" und "application" versehen.
 * <p>
 * Beispiel:
 * <p>
 * <pre>app_health_component_status{application="suchdienst",component="database"} 1.0</pre>
 */
@Component
public class DynamicHealthMetricsPublisher {

    private final MeterRegistry registry;
    private final HealthEndpoint healthEndpoint;

    public DynamicHealthMetricsPublisher(MeterRegistry registry, HealthEndpoint healthEndpoint) {
        this.registry = registry;
        this.healthEndpoint = healthEndpoint;
    }

    @PostConstruct
    public void init() {
        // Initiale Abfrage des Health Endpoints, um die Komponenten zu registrieren:
        CompositeHealthDescriptor initialCompositeHealth =
            (CompositeHealthDescriptor) healthEndpoint.health();
        var components = initialCompositeHealth.getComponents();
        if (components != null) {
            for (String componentName : components.keySet()) {
                // Erstelle einen Gauge für jede Komponente, dieser fragt live den Status ab:
                Gauge.builder("app_health_component_status", () -> {
                    CompositeHealthDescriptor dynamicCompositeHealth =
                        (CompositeHealthDescriptor) healthEndpoint.health();
                    var dynamicComponents = dynamicCompositeHealth.getComponents();
                    if (dynamicComponents == null) {
                        return 0.0;
                    }
                    Object detail = dynamicComponents.get(componentName);
                    if (detail instanceof HealthDescriptor health) {
                        return statusToValue(health.getStatus());
                    }
                    return 0.0;
                })
                    .description("Health status of component: 1=UP, 0=DOWN")
                    .tag("component", componentName)
                    .tag("application", "suchdienst")
                    .register(registry);
            }
        }
    }

    private double statusToValue(Status status) {
        return "UP".equalsIgnoreCase(status.getCode()) ? 1.0 : 0.0;
    }
}
