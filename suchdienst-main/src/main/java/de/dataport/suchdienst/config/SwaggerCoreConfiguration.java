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

import static org.springdoc.core.utils.Constants.SPRINGDOC_ENABLED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.annotation.PostConstruct;
import java.util.Iterator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.PolymorphicModelConverter;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguriert die OpenAPI-Modellauflösung mit dem {@link CustomTypeNameResolver}.
 * swagger-core verwendet Jackson 2 und die Modellannotationen; die HTTP-Serialisierung
 * mit Jackson 3 bleibt unabhängig. Diese Konfiguration betrifft nur die
 * API-Beschreibung, nicht JAXB, HTTP-Antworten oder das Elasticsearch-Persistenzformat.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = SPRINGDOC_ENABLED, matchIfMissing = true)
public class SwaggerCoreConfiguration {

    static {
        // swagger-core 2.2.55 kann beim OpenAPI-3.1-Umbau vorhandenes allOf mit Untertypfeldern verlieren.
        // Eigenschaften neben dem allOf-Verweis vermeiden diesen Kopierfehler für alle komponierten Modelle.
        ModelResolver.composedModelPropertiesAsSibling = true;
    }

    private final ObjectMapper objectMapper;
    private final Optional<SpringDocConfigProperties> springDocConfigProperties;

    public SwaggerCoreConfiguration(Optional<SpringDocConfigProperties> springDocConfigProperties) {
        this.objectMapper = openApiObjectMapper();
        this.springDocConfigProperties = springDocConfigProperties;
    }

    /** Mapper für swagger-core mit Typinformationen aus den Modellannotationen. */
    static ObjectMapper openApiObjectMapper() {
        return new ObjectMapper();
    }

    static ModelResolver modelResolver(ObjectMapper mapper, boolean useFqn) {
        return new ModelResolver(mapper, new CustomTypeNameResolver(useFqn)) {
            // swagger-core 2.2.55 teilt den ArraySchema-Rekursionsschutz zwischen Dokumenten.
            @Override
            public synchronized Schema resolve(
                AnnotatedType type,
                ModelConverterContext context,
                Iterator<ModelConverter> chain
            ) {
                return super.resolve(type, context, chain);
            }
        };
    }

    /**
     * Stellt Springdocs Polymorphiekonverter mit einer Ausnahme für lokale JZuFi-v23-Modelle bereit.
     *
     * <p>Bei aktiviertem Springdoc ist diese Bean im Servlet-Webkontext auch ohne den Eintrag
     * {@code springdoc.model-converters.polymorphic-converter.enabled} aktiv.
     * Ein explizites {@code false} verhindert ihre Erstellung; die Property wird hier nur gelesen, nicht gesetzt.
     *
     * <p>Der Konverter löst Java-Typen in OpenAPI-Schemas oder Schema-Referenzen auf, nicht in Nutzdaten.
     * Für das Paket {@code de.dataport.suchdienst.jzufi.model.v23} delegiert er direkt an die übrige
     * Konverterkette, ohne Springdocs automatische {@code oneOf}-Auswahl anzuwenden.
     * Konkrete JZuFi-Basen und ihre Untertypen können dieselbe Antwort beschreiben;
     * ein {@code oneOf} würde jedoch genau einen passenden Zweig verlangen.
     * Die Schemaauflösung anhand der Modellannotationen einschließlich Discriminator und {@code allOf}
     * bleibt erhalten. Für alle anderen Java-Typen gilt Springdocs Standardverhalten.
     *
     * @param mapperProvider Springdocs Mapperanbieter zur Ermittlung des Java-Typs
     * @return Polymorphiekonverter mit der lokalen JZuFi-Ausnahme
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(
        name = "springdoc.model-converters.polymorphic-converter.enabled",
        matchIfMissing = true
    )
    PolymorphicModelConverter polymorphicModelConverter(ObjectMapperProvider mapperProvider) {
        return new PolymorphicModelConverter(mapperProvider) {
            @Override
            public Schema resolve(
                AnnotatedType type,
                ModelConverterContext context,
                Iterator<ModelConverter> chain
            ) {
                var javaType = mapperProvider.jsonMapper().constructType(type.getType());
                if (
                    javaType != null &&
                    javaType
                        .getRawClass()
                        .getPackageName()
                        .equals("de.dataport.suchdienst.jzufi.model.v23")
                ) {
                    // Nur diese Polymorphie-Erweiterung überspringen, nicht die weitere Schemaauflösung.
                    return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
                }
                return super.resolve(type, context, chain);
            }
        };
    }

    /**
     * Registriert beim Initialisieren dieser Konfiguration den Suchdienst-spezifischen Swagger-ModelResolver.
     *
     * <p>Angepasst wird die globale {@link ModelConverters}-Registry für die von Springdoc gewählte
     * OpenAPI-Version. Der neue Resolver verwendet den {@link CustomTypeNameResolver} und berücksichtigt
     * Springdocs FQN-Einstellung. Ein vorhandener ModelResolver wird unter Übernahme seiner
     * Swagger-Konfiguration ersetzt; die übrigen Konverter bleiben registriert.
     *
     * <p>Ohne verfügbare {@link SpringDocConfigProperties} bleibt die Registry unverändert und eine
     * Warnung wird protokolliert. Die Registrierung betrifft ausschließlich die OpenAPI-Schemaerzeugung,
     * nicht die JSON-Mapper für HTTP-Antworten oder Elasticsearch.
     */
    @PostConstruct
    public void registerCustomModelConverter() {
        springDocConfigProperties.ifPresentOrElse(
            config -> {
                var instance = ModelConverters.getInstance(config.isOpenapi31());
                var customConverter = modelResolver(objectMapper, config.isUseFqn());

                instance
                    .getConverters()
                    .stream()
                    .filter(ModelResolver.class::isInstance)
                    .map(ModelResolver.class::cast)
                    .findFirst()
                    .ifPresent(resolver -> {
                        // Die eigene Namensauflösung soll andere Swagger-Optionen nicht zurücksetzen.
                        var newConfig = new io.swagger.v3.core.util.Configuration();
                        BeanUtils.copyProperties(resolver.getConfiguration(), newConfig);
                        customConverter.setConfiguration(newConfig);

                        // Entfernen des Standardkonverters
                        instance.removeConverter(resolver);
                    });

                customConverter.openapi31(config.isOpenapi31());
                instance.addConverter(customConverter);
            },
            () ->
                log.warn(
                    "SpringDocConfigProperties ist nicht verfügbar. Der Dienst wurde entsprechend konfiguriert, " +
                        "sodass keine OpenAPI / Swagger-Funktionalitäten bereitgestellt werden ." +
                        "Dadurch kann die Nutzung bestimmter Funktionen eingeschränkt sein."
                )
        );
    }
}
