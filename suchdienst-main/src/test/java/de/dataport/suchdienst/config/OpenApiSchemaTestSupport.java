/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties.ApiDocs.OpenApiVersion;
import org.springdoc.core.providers.ObjectMapperProvider;

/** Lokale Schemaauflösung und JSON-Schema-Prüfung ohne HTTP-Server oder globalen Testcache. */
final class OpenApiSchemaTestSupport {

    static final String REF = "#/components/schemas/";

    private OpenApiSchemaTestSupport() {}

    /** Isoliert die Modellannotationen mit dem produktiven Swagger-Resolver, aber ohne Springdoc-Konverter. */
    static ModelConverters converters(boolean openapi31) {
        var converters = new ModelConverters(openapi31);
        // Automatisch registrierte Konverter dürfen die isolierte Prüfung des eigenen Resolvers nicht beeinflussen.
        converters.getConverters().forEach(converters::removeConverter);
        converters.addConverter(
            SwaggerCoreConfiguration.modelResolver(
                SwaggerCoreConfiguration.openApiObjectMapper(),
                true
            ).openapi31(openapi31)
        );
        return converters;
    }

    static ObjectMapper schemaMapper(boolean openapi31) {
        return openapi31 ? Json31.mapper() : Json.mapper();
    }

    /** Ergänzt Springdocs automatische Polymorphieauflösung; echte MVC-/Gruppierungstests bleiben separat. */
    static ModelConverters springdocConverters(boolean openapi31) {
        var converters = converters(openapi31);
        var properties = new SpringDocConfigProperties();
        properties
            .getApiDocs()
            .setVersion(openapi31 ? OpenApiVersion.OPENAPI_3_1 : OpenApiVersion.OPENAPI_3_0);
        properties.setUseFqn(true);
        converters.addConverter(
            new SwaggerCoreConfiguration(Optional.of(properties)).polymorphicModelConverter(
                new ObjectMapperProvider(properties)
            )
        );
        return converters;
    }

    /** Löst die Roots in der angegebenen Reihenfolge mit derselben Konverterinstanz auf. */
    static Map<String, Schema> schemas(ModelConverters converters, Iterable<Class<?>> roots) {
        Map<String, Schema> schemas = new LinkedHashMap<>();
        for (var root : roots) schemas.putAll(converters.readAll(root));
        return schemas;
    }

    /**
     * Vergleicht den vollständigen serialisierten Graphen ohne Normalisierung.
     * So bleiben auch Feldverluste im Parallelitätstest sichtbar, unabhängig von internen Swagger-Schemaklassen.
     */
    static void assertSameSchemas(
        Map<String, Schema> expected,
        Map<String, Schema> actual,
        boolean openapi31
    ) {
        assertEquals(expected.keySet(), actual.keySet());
        var mapper = schemaMapper(openapi31);
        JsonNode left = mapper.valueToTree(new Components().schemas(expected));
        JsonNode right = mapper.valueToTree(new Components().schemas(actual));
        left.path("schemas")
            .properties()
            .forEach(entry ->
                assertEquals(
                    entry.getValue(),
                    right.path("schemas").path(entry.getKey()),
                    entry.getKey()
                )
            );
    }

    /**
     * Erzeugt einen Validator für das referenzierte Schema, nicht für eine vollständige HTTP-Antwort.
     *
     * @param name Komponentenname oder Feldpfad darunter, etwa {@code Jzufi23Leistung/properties/struktur}
     */
    static com.networknt.schema.Schema validator(
        Map<String, Schema> schemas,
        String name,
        boolean openapi31
    ) {
        var mapper = schemaMapper(openapi31);
        var document = mapper.createObjectNode().put("$ref", REF + name);
        document.set("components", mapper.valueToTree(new Components().schemas(schemas)));
        // Discriminator-Mappings sind Dispatch-Metadaten; oneOf/allOf und Feldbedingungen validieren.
        return SchemaRegistry.withDefaultDialectId(
            "https://json-schema.org/draft/2020-12/schema",
            builder ->
                builder.schemaRegistryConfig(
                    SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build()
                )
        ).getSchema(document);
    }

    /** Liefert eine veränderbare Kopie der Feldnamen, ohne den Referenzgraphen zu verändern. */
    static Set<String> properties(Schema<?> schema, Map<String, Schema> schemas) {
        return new LinkedHashSet<>(propertySchemas(schema, schemas).keySet());
    }

    /**
     * Sammelt Felddeklarationen über lokale Referenzen und allOf-Eltern.
     * Wiederholte Deklarationen werden zusammengeführt; dies ist keine allgemeine Auflösung widersprüchlicher Constraints.
     */
    static Map<String, Schema> propertySchemas(Schema<?> schema, Map<String, Schema> schemas) {
        return propertySchemas(schema, schemas, new LinkedHashSet<>());
    }

    private static Map<String, Schema> propertySchemas(
        Schema<?> schema,
        Map<String, Schema> schemas,
        Set<String> visited
    ) {
        assertNotNull(schema);
        Map<String, Schema> result = new LinkedHashMap<>();
        if (schema.get$ref() != null && visited.add(schema.get$ref())) {
            Schema<?> referenced = schemas.get(schema.get$ref().substring(REF.length()));
            assertNotNull(referenced, schema.get$ref());
            result.putAll(propertySchemas(referenced, schemas, visited));
        }
        if (schema.getAllOf() != null) {
            schema
                .getAllOf()
                .forEach(parent -> result.putAll(propertySchemas(parent, schemas, visited)));
        }
        // swagger-core kann Elternfelder erneut am Kind deklarieren; die lokale Deklaration hat hier Vorrang.
        if (schema.getProperties() != null) result.putAll(schema.getProperties());
        return result;
    }
}
