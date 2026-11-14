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
package de.dataport.suchdienst;

import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.ProtocolType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@Slf4j
public class Application {

    public static void main(String[] args) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.SYSTEM)) {
            SpringApplication.run(new Class[] { Application.class }, args);
        }
    }

    /**
     * Der Inhalt des Spring Environment wird nach Schlüssel alphabetisch sortiert als INFO geloggt.
     * Dies wird nach dem Initialisieren der App aufgerufen. Hiermit haben wir also eine zentrale Stelle
     * ziemlich weit vorn im Log um unsere Konfiguration zu überprüfen.
     */
    private void logApplicationProperties(ConfigurableEnvironment env) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.SYSTEM)) {
            logApplicationPropertiesInternal(env);
        }
    }

    private void logApplicationPropertiesInternal(ConfigurableEnvironment env) {
        final List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        final Level defaultLevel;
        final Level severeLevel;
        // Im Rahmen von Unit- bzw. Integrationstests soll das Environment nicht geloggt werden, zu viel Output.
        if (activeProfiles.contains("test")) {
            defaultLevel = severeLevel = Level.DEBUG;
        } else {
            defaultLevel = Level.INFO;
            severeLevel = Level.WARN;
        }
        log.atLevel(defaultLevel).log(
            "Maximal genutzter RAM: {} MB",
            Runtime.getRuntime().maxMemory() / 1024 / 1024
        );
        log.atLevel(defaultLevel).log(
            "Anzahl an Prozessoren laut JVM: {}",
            Runtime.getRuntime().availableProcessors()
        );
        log.atLevel(defaultLevel).log(
            "************************* APPLICATION PROPERTIES ******************************"
        );

        List<MapPropertySource> propertySources = new ArrayList<>();
        for (PropertySource<?> source : env.getPropertySources()) {
            String name = source.getName();
            if (
                source instanceof MapPropertySource mapPropertySource &&
                !"systemEnvironment".equals(name) &&
                !"systemProperties".equals(name) &&
                !"devtools".equals(name)
            ) {
                propertySources.add(mapPropertySource);
            }
        }

        propertySources
            .stream()
            .map(propertySource -> propertySource.getSource().keySet())
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .forEach(key -> logPropertyValue(env, key, defaultLevel, severeLevel));
        log.info(
            "********************************************************************************"
        );
    }

    @EventListener
    public void handleApplicationStartedEvent(ApplicationStartedEvent event) {
        // Das Protokollieren des Environments könnte auch früher geschehen, z.B. beim
        // ApplicationEnvironmentPreparedEvent. Dazu muss aber ein eigenständiger Listener gebaut und in der
        // SpringApplication-Instanz registriert werden. Diese Variante mit der EventListener-Annotation
        // funktioniert erst nach der Erstellung des ApplicationContext.
        final ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();
        logApplicationProperties(env);
    }

    private void logPropertyValue(
        ConfigurableEnvironment env,
        String key,
        Level resolvedPropertyLevel,
        Level unresolvedPropertyLevel
    ) {
        String pattern = "{} = '{}'";
        try {
            String lowerKey = key.toLowerCase();
            String value =
                lowerKey.endsWith(".password") ||
                lowerKey.endsWith(".username") ||
                lowerKey.endsWith(".secret")
                    ? "*****"
                    : env.getProperty(key);
            log.atLevel(resolvedPropertyLevel).log(pattern, key, value);
        } catch (IllegalArgumentException e) {
            log.atLevel(unresolvedPropertyLevel).log(pattern, key, e.getMessage());
        }
    }
}
