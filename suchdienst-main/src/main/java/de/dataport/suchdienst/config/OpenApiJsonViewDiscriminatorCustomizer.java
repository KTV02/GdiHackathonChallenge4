// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: View-konsistente Discriminator-Referenzen für generierte API-Clients.
package de.dataport.suchdienst.config;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.LinkedHashMap;
import java.util.List;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Korrigiert lokale Discriminator-Referenzen auf vorhandene Subtypen derselben JSON-View.
 * Die Modellannotationen beschreiben die Typ-Constraints; JSON-Antworten bleiben unverändert.
 */
@Component
public class OpenApiJsonViewDiscriminatorCustomizer implements GlobalOpenApiCustomizer {

    private static final String SCHEMA_PREFIX = "#/components/schemas/";
    private static final List<String> VIEW_SUFFIXES = List.of("_Search", "_Detail", "_Internal");

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        var schemas = openApi.getComponents().getSchemas();
        schemas.forEach((name, schema) -> {
            var discriminator = schema.getDiscriminator();
            if (discriminator == null || discriminator.getMapping() == null) {
                return;
            }
            VIEW_SUFFIXES.stream()
                .filter(name::endsWith)
                .findFirst()
                .ifPresent(suffix -> {
                    var mapping = new LinkedHashMap<>(discriminator.getMapping());
                    mapping.replaceAll((type, reference) -> {
                        if (reference != null && reference.startsWith(SCHEMA_PREFIX)) {
                            String viewSubtype =
                                reference.substring(SCHEMA_PREFIX.length()) + suffix;
                            if (schemas.containsKey(viewSubtype)) {
                                return SCHEMA_PREFIX + viewSubtype;
                            }
                        }
                        return reference;
                    });
                    discriminator.setMapping(mapping);
                });
        });
    }
}
