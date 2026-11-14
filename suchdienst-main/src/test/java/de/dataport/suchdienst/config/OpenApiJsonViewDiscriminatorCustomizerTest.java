// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Regression für View-Referenzen und deklarative polymorphe Modellverträge.
package de.dataport.suchdienst.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV7;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import java.beans.Introspector;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Prüft View-spezifische Referenzen und die davon unabhängigen SeDe-Typ-Constraints. */
@Isolated("Prüft den globalen Swagger-Namensresolver mit beiden FQN-Einstellungen")
class OpenApiJsonViewDiscriminatorCustomizerTest {

    private final OpenApiJsonViewDiscriminatorCustomizer customizer =
        new OpenApiJsonViewDiscriminatorCustomizer();

    /**
     * Mit Springdocs Polymorphiekonverter müssen die Modellannotationen auch die
     * View-spezifischen Untertypen registrieren, nicht nur ihre ungefilterten Varianten.
     */
    @ParameterizedTest
    @CsvSource({
        "false,Search",
        "true,Search",
        "false,Detail",
        "true,Detail",
        "false,Internal",
        "true,Internal",
    })
    void resolvesViewSubtypesFromModelAnnotations(boolean openapi31, String view) throws Exception {
        var converters = OpenApiSchemaTestSupport.springdocConverters(openapi31);
        var resolved = converters.resolveAsResolvedSchema(
            new AnnotatedType(SdOutModelV7.class)
                .jsonViewAnnotation(
                    Views.class
                        .getDeclaredField(Introspector.decapitalize(view))
                        .getAnnotation(JsonView.class)
                )
                .resolveAsRef(true)
        );
        var api = new OpenAPI().components(new Components().schemas(resolved.referencedSchemas));
        customizer.customise(api);
        var schemas = api.getComponents().getSchemas();
        Schema<?> base = schemas.get("SeDeBlock_" + view);
        assertNotNull(base);
        assertNull(base.getOneOf());
        Schema<?> result = schemas.get("SdOutModelV7_" + view);
        Schema<?> items = result.getProperties().get("details").getItems();
        assertEquals(
            Set.of(
                "#/components/schemas/SeDeBlockModule_" + view,
                "#/components/schemas/SeDeBlockGroupedModule_" + view
            ),
            new LinkedHashSet<>(items.getOneOf().stream().map(Schema::get$ref).toList())
        );
        for (var name : List.of("SeDeBlockModule", "SeDeBlockGroupedModule")) {
            Schema<?> child = schemas.get(name + "_" + view);
            assertNotNull(child, name + "_" + view);
            assertTrue(
                child
                    .getAllOf()
                    .stream()
                    .anyMatch(parent ->
                        ("#/components/schemas/SeDeBlock_" + view).equals(parent.get$ref())
                    )
            );
            String discriminator = name.equals("SeDeBlockModule") ? "_module" : "_moduleDetail";
            assertEquals(
                "#/components/schemas/" + name + "_" + view,
                base.getDiscriminator().getMapping().get(discriminator)
            );
        }
        if (!view.equals("Internal")) {
            assertFalse(base.getProperties().containsKey("validityPeriods"));
        }
    }

    // Die alten JSON-Views bleiben bis zur Entfernung ihrer API-Verträge bewusst Teil dieser Regression.
    @SuppressWarnings("java:S5738")
    record Views(
        @JsonView(JsonViews.Search.class) String search,
        @JsonView(JsonViews.Detail.class) String detail,
        @JsonView(JsonViews.Internal.class) String internal
    ) {}

    @ParameterizedTest
    @ValueSource(strings = { "Search", "Detail", "Internal" })
    void mapsSubtypesToTheSameViewAndLeavesOtherReferencesUnchanged(String view) {
        var originalMapping = Map.of(
            "_module",
            "#/components/schemas/SeDeBlockModule",
            "_moduleDetail",
            "#/components/schemas/SeDeBlockGroupedModule",
            "_withoutView",
            "#/components/schemas/SharedModule",
            "_external",
            "https://example.invalid/schema.json"
        );
        var base = new Schema<>().discriminator(
            new Discriminator().propertyName("@type").mapping(originalMapping)
        );
        var viewBase = new Schema<>().discriminator(
            new Discriminator().propertyName("@type").mapping(originalMapping)
        );
        var openApi = new OpenAPI().components(
            new Components()
                .addSchemas("SeDeBlock", base)
                .addSchemas("SeDeBlock_" + view, viewBase)
                .addSchemas("SeDeBlockModule_" + view, new Schema<>())
                .addSchemas("SeDeBlockGroupedModule_" + view, new Schema<>())
                .addSchemas("SharedModule", new Schema<>())
        );

        customizer.customise(openApi);
        customizer.customise(openApi); // Idempotenz: Erneute Anwendung darf korrigierte View-Referenzen nicht verändern.

        assertEquals(
            "#/components/schemas/SeDeBlockModule_" + view,
            viewBase.getDiscriminator().getMapping().get("_module")
        );
        assertEquals(
            "#/components/schemas/SeDeBlockGroupedModule_" + view,
            viewBase.getDiscriminator().getMapping().get("_moduleDetail")
        );
        assertEquals(
            "#/components/schemas/SharedModule",
            viewBase.getDiscriminator().getMapping().get("_withoutView")
        );
        assertEquals(
            "https://example.invalid/schema.json",
            viewBase.getDiscriminator().getMapping().get("_external")
        );
        assertEquals(originalMapping, base.getDiscriminator().getMapping());
    }

    /**
     * Prüft Typ-Constraints allein aus Modellannotationen, auch bei wechselnder globaler FQN-Einstellung.
     * Die View-Referenzkorrektur darf für die Exklusivität der Typkennungen nicht benötigt werden.
     */
    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void declaresSubtypeConstraintsWithoutACustomizer(boolean openApi31, boolean useFqn) {
        boolean previousUseFqn = TypeNameResolver.std.getUseFqn();
        try {
            TypeNameResolver.std.setUseFqn(useFqn);
            var converters = new ModelConverters(openApi31);
            converters.addConverter(
                new ModelResolver(
                    SwaggerCoreConfiguration.openApiObjectMapper(),
                    new CustomTypeNameResolver(true)
                ).openapi31(openApi31)
            );
            var schemas = converters.readAll(SeDeBlockModuleDetail.class);
            for (String name : List.of("SeDeBlockModule", "SeDeBlockGroupedModule")) {
                Schema<?> schema = schemas.get(name);
                assertNotNull(
                    schema,
                    () -> "Schema " + name + " fehlt; vorhanden: " + schemas.keySet()
                );
                String type = name.equals("SeDeBlockModule") ? "_module" : "_moduleDetail";
                assertEquals(
                    "^" + type + "$(?![\\s\\S])",
                    schema.getProperties().get("@type").getPattern()
                );
                assertNull(schema.getProperties().get("@type").getEnum());
                assertTrue(schema.getRequired().contains("@type"));
            }
        } finally {
            // Der Swagger-Standardresolver ist global; nachfolgende Tests müssen ihren ursprünglichen Zustand erhalten.
            TypeNameResolver.std.setUseFqn(previousUseFqn);
        }
    }

    @Test
    void acceptsDocumentsWithoutSchemasOrDiscriminators() {
        assertDoesNotThrow(() -> customizer.customise(new OpenAPI()));
        assertDoesNotThrow(() -> customizer.customise(new OpenAPI().components(new Components())));
        assertDoesNotThrow(() ->
            customizer.customise(
                new OpenAPI().components(
                    new Components()
                        .addSchemas("Plain_Search", new Schema<>())
                        .addSchemas(
                            "Unmapped_Search",
                            new Schema<>().discriminator(new Discriminator())
                        )
                )
            )
        );
    }
}
