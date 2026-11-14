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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SchemaRegistry;
import de.dataport.suchdienst.Application;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.configuration.SpringDocSortConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.converters.PolymorphicModelConverter;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Testziel: Alle aktiven produktiven Controller-Signaturen werden durch echte Springdoc-MVC-Ressourcen
 * und produktive Gruppierungs-/Schema-Konfiguration exportiert. Controller- und Advice-Instanzen
 * sind gemockt; weder Fachdienste noch Elasticsearch/Redis werden gestartet oder aufgerufen.
 * Damit wird die API-Beschreibung geprüft, nicht das tatsächliche Antwort-/Fehlerverhalten der Controller.
 * Die produktiven YAML-Defaults müssen Springdocs automatische Auswahl außerhalb von JZuFi
 * erhalten, während JZuFi-Felder direkte Basisreferenzen ohne überlappende oneOf verwenden.
 * Controller und Advices aus Testquellen dürfen in keiner Gruppe erscheinen.
 * Für reproduzierbare Exporte separat ausführen: swagger-core initialisiert Schemaoptionen statisch.
 */
@Isolated("Springdoc registriert Konverter im globalen Swagger-Register")
class SpringdocOpenApiContractTest {

    /**
     * Exportiert Gesamt-, Public-, Beta- und Admin-Dokument mit produktiven Defaults.
     * Die Dateien unter target/docs/springdoc-contract dienen zugleich als frische Eingabe für Offline-Clienttests.
     */
    @Test
    void exportsAllGroupsWithDefaultPolymorphismAndLocalJzufiException() throws IOException {
        var defaults = new YamlPropertySourceLoader().load(
            "application-default",
            new ClassPathResource("application-default.yaml")
        );
        var runner = new WebApplicationContextRunner()
            .withUserConfiguration(ContractConfiguration.class)
            .withPropertyValues(
                "spring.profiles.active=default,dev,beta_api_active,deprecated_api_active,openapi_show_internal,security_off",
                "de.dataport.suchdienst.instance.type=MASTER",
                "keycloak.auth-server-url=http://localhost",
                "keycloak.realm=contract-test"
            )
            .withInitializer(context -> {
                // Testwerte konfigurieren die Umgebung; nachrangig bleiben die produktiven Springdoc-Defaults wirksam.
                defaults.forEach(source ->
                    context.getEnvironment().getPropertySources().addLast(source)
                );
                var scanner = new ClassPathScanningCandidateComponentProvider(
                    false,
                    context.getEnvironment()
                );
                scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
                scanner.addIncludeFilter(new AnnotationTypeFilter(ControllerAdvice.class));
                var beans = (DefaultListableBeanFactory) context.getBeanFactory();
                var productionSource = Application.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
                // Ein Paketfilter reicht nicht: Im Testklassenpfad liegen auch Controller anderer Tests.
                for (var candidate : scanner.findCandidateComponents("de.dataport.suchdienst")) {
                    Class<?> type = ClassUtils.resolveClassName(
                        candidate.getBeanClassName(),
                        context.getClassLoader()
                    );
                    if (
                        !productionSource.equals(
                            type.getProtectionDomain().getCodeSource().getLocation()
                        )
                    ) continue;
                    // Produktiver Bean-Typ für die Signaturanalyse, aber keine Konstruktion seiner Fachabhängigkeiten.
                    var definition = new RootBeanDefinition(type);
                    definition.setInstanceSupplier(() -> mock(type));
                    beans.registerBeanDefinition(type.getName(), definition);
                }
            });
        runner.run(context -> {
            assertTrue(context.isRunning());
            assertEquals(1, context.getBeansOfType(PolymorphicModelConverter.class).size());
            assertTrue(context.getBean(SpringDocConfigProperties.class).isOpenapi31());
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            var mapper = OpenApiSchemaTestSupport.schemaMapper(true);
            var directory = Path.of("target/docs/springdoc-contract");
            Files.createDirectories(directory);
            for (String group : List.of("all", "suchen", "beta", "admin")) {
                String endpoint = "/v3/api-docs" + (group.equals("all") ? "" : "/" + group);
                var response = mvc.perform(get(endpoint)).andExpect(status().isOk()).andReturn();
                var document = mapper.readTree(response.getResponse().getContentAsByteArray());
                assertTrue(document.path("paths").size() > 0, group);
                assertTrue(
                    document
                        .path("paths")
                        .properties()
                        .stream()
                        .noneMatch(path -> path.getKey().startsWith("/audience/")),
                    group + " darf keine Testcontroller exportieren"
                );
                mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(directory.resolve(group + ".json").toFile(), document);
                if (group.equals("all") || group.equals("suchen")) {
                    assertPolymorphicSlots(document);
                    assertJzufiDeprecations(document);
                    assertPublicErrorContracts(document);
                    assertLegacyRelationExamples(document);
                }
                if (group.equals("all") || group.equals("beta")) {
                    assertBetaErrorContracts(document);
                }
                if (!group.equals("admin")) assertApiErrorSchema(document);
            }
        });
    }

    /** Fehlerfelder bleiben auch in view-spezifischen Komponenten vollständig und für strikte Clients bindbar. */
    private static void assertApiErrorSchema(JsonNode document) {
        var properties = document.at("/components/schemas/ApiErrorModel/properties");
        for (String field : List.of(
            "timestamp",
            "code",
            "message",
            "codeNumber",
            "requestId",
            "fields"
        )) {
            assertTrue(properties.has(field), "ApiErrorModel." + field);
        }
        assertEquals("string", properties.at("/requestId/type").asText());
        assertEquals("object", properties.at("/fields/type").asText());
        assertEquals("string", properties.at("/fields/additionalProperties/type").asText());
        document
            .at("/components/schemas")
            .properties()
            .stream()
            .filter(entry -> entry.getKey().startsWith("ApiErrorModel_"))
            .forEach(entry ->
                assertEquals(properties, entry.getValue().path("properties"), entry.getKey())
            );
    }

    /**
     * Prüft eine feste Pfad-/Statusmatrix und den skalaren Suggestions-Parameter.
     * Die Erwartungen werden nicht aus den vorhandenen Antworten abgeleitet, damit fehlende Dokumentation auffällt.
     */
    private static void assertPublicErrorContracts(JsonNode document) {
        assertErrorResponses(
            document,
            List.of(
                "/v3/locations",
                "/v3/locations/details",
                "/v3/locations/ars",
                "/v3/locations/suggestions",
                "/v2/locations/reverse",
                "/v3/servicedescriptions/leikaid",
                "/v2/servicedescriptions/leikahierarchy",
                "/v1/servicedescriptions/suggestions/{ars}",
                "/v1/personalmatters/{ars}",
                "/v2/organisationunits/titles",
                "/v1/organisationunits/{oeId}/ars"
            ),
            "400"
        );
        assertErrorResponses(document, List.of("/v2/onlineservices/detail"), "400", "404");
        for (String resource : List.of(
            "servicedescriptions",
            "organisationunits",
            "onlineservices",
            "relations"
        )) {
            String suffix = resource.equals("relations") ? "" : "/{id}";
            var paths = List.of(
                "/v1/" + resource + suffix + "/jzufi-2-2",
                "/v1/" + resource + suffix + "/jzufi-2-3",
                "/v2/" + resource + suffix + "/jzufi-2-3"
            );
            assertErrorResponses(document, paths, "400");
            if (!resource.equals("relations")) assertErrorResponses(document, paths, "404");
        }
        assertErrorResponses(document, List.of("/v2/relations/jzufi-2-3"), "500");
        var parameters = document
            .path("paths")
            .path("/v1/servicedescriptions/suggestions/{ars}")
            .at("/get/parameters");
        var query = parameters
            .valueStream()
            .filter(parameter -> parameter.path("name").asText().equals("q"))
            .findFirst()
            .orElseThrow();
        assertEquals("query", query.path("in").asText());
        assertTrue(query.path("required").asBoolean());
        assertEquals("string", query.at("/schema/type").asText());
        assertEquals(1, query.at("/schema/minLength").asInt());
        assertEquals(50, query.at("/schema/maxLength").asInt());
        assertFalse(query.path("schema").has("items"));
    }

    private static void assertBetaErrorContracts(JsonNode document) {
        assertErrorResponses(
            document,
            List.of(
                "/v8beta1/servicedescriptions",
                "/v4beta1/servicedescriptions/leikaid",
                "/v3beta1/servicedescriptions/leikahierarchy",
                "/v1beta2/onlineservices"
            ),
            "400"
        );
        assertErrorResponses(
            document,
            List.of("/v3beta1/onlineservices/{odId}/detail"),
            "400",
            "404"
        );
        assertErrorResponses(
            document,
            List.of("/v1beta2/onlineservices/{odId}/organisations"),
            "404"
        );
    }

    private static void assertErrorResponses(
        JsonNode document,
        List<String> paths,
        String... statuses
    ) {
        for (String path : paths) {
            var operation = document.path("paths").path(path).path("get");
            assertTrue(operation.isObject(), path);
            for (String status : statuses) {
                // Personalmatters nutzt die Search-View; deren gleichwertiges Fehlerschema hat einen eigenen Namen.
                assertEquals(
                    "#/components/schemas/" +
                        (path.equals("/v1/personalmatters/{ars}")
                            ? "ApiErrorModel_Search"
                            : "ApiErrorModel"),
                    operation
                        .path("responses")
                        .path(status)
                        .at("/content/application~1json/schema/$ref")
                        .asText(),
                    path + ": " + status
                );
            }
        }
    }

    /** Verhindert, dass die lokalen Typkennungen versehentlich in die dokumentierten Beispiele der Alt-API gelangen. */
    private static void assertLegacyRelationExamples(JsonNode document) {
        var content = document
            .path("paths")
            .path("/v3/relations/{ars}/jzufi")
            .at("/get/responses/200/content");
        assertTrue(content.size() > 0);
        for (var mediaType : content) {
            var examples = mediaType.path("examples");
            assertTrue(examples.size() > 0);
            for (var example : examples) {
                assertTrue(example.hasNonNull("value"));
                assertFalse(example.path("value").toString().contains("@type"));
            }
        }
    }

    private static void assertJzufiDeprecations(JsonNode document) {
        for (String resource : List.of(
            "servicedescriptions",
            "organisationunits",
            "onlineservices",
            "relations"
        )) {
            String suffix = resource.equals("relations") ? "" : "/{id}";
            for (String version : List.of("2-3")) {
                assertDeprecatedOperation(
                    document,
                    "/v1/" + resource + suffix + "/jzufi-" + version,
                    true
                );
            }
            assertDeprecatedOperation(document, "/v2/" + resource + suffix + "/jzufi-2-3", false);
        }
        for (String path : List.of(
            "/v3/servicedescriptions/jzufi",
            "/v2/organisationunits/jzufi",
            "/v3/relations/{ars}/jzufi"
        )) {
            assertDeprecatedOperation(document, path, true);
        }
    }

    private static void assertDeprecatedOperation(
        JsonNode document,
        String path,
        boolean deprecated
    ) {
        var operation = document.path("paths").path(path).path("get");
        assertTrue(operation.isObject(), path);
        assertEquals(deprecated, operation.path("deprecated").asBoolean(), path);
    }

    /**
     * JZuFi-Slots bleiben direkte Basisreferenzen, während andere APIs Springdocs automatische oneOf-Auswahl behalten.
     * Die Payloads prüfen die exportierten Slot-Schemas, nicht das Untertyp-Binding eines Clients.
     */
    private static void assertPolymorphicSlots(JsonNode document) throws Exception {
        var mapper = OpenApiSchemaTestSupport.schemaMapper(true);
        var schemas = document.at("/components/schemas");
        assertFalse(schemas.has("ServiceDescriptionDetailModule"));
        schemas
            .properties()
            .stream()
            .filter(entry -> entry.getKey().startsWith("Jzufi23"))
            .forEach(entry -> {
                assertTrue(entry.getValue().findValues("oneOf").isEmpty(), entry.getKey());
                assertTrue(entry.getValue().findValues("anyOf").isEmpty(), entry.getKey());
            });
        assertEquals(
            "#/components/schemas/Jzufi23Leistungsstruktur",
            schemas.at("/Jzufi23Leistung/properties/struktur/$ref").asText()
        );
        assertEquals(
            "#/components/schemas/Jzufi23Anschrift",
            schemas.at("/Jzufi23Kontaktperson/properties/anschrift/items/$ref").asText()
        );
        assertEquals(
            "#/components/schemas/Jzufi23Zustaendigkeit",
            schemas
                .at("/Jzufi23ZustaendigkeitTransferObjekt/properties/zustaendigkeit/items/$ref")
                .asText()
        );
        assertEquals(
            3,
            schemas.at("/ServiceDescriptionDetail/properties/details/items/oneOf").size()
        );
        assertEquals(2, schemas.at("/SdOutModelV7_Search/properties/details/items/oneOf").size());
        var examples = java.util.Map.of(
            "Jzufi23Leistung/properties/struktur",
            """
            {"@type":"LeistungsstrukturObjektMitVerrichtungUndDetail",
             "leistungsgruppierungLeiKa":{"code":"100"},"leistungsobjektID":{"value":"object"},
             "verrichtungLeiKa":{"code":"001"},"verrichtungsdetail":[{"value":"detail"}]}
            """,
            "Jzufi23Kontaktperson/properties/anschrift/items",
            """
            {"@type":"AnschriftOrganisationseinheit","typ":{"code":"01"},
             "postleitzahl":"12345","ort":"Teststadt","strasse":"Testweg",
             "infoParkplatz":[{"value":"parking"}]}
            """,
            "Jzufi23ZustaendigkeitTransferObjekt/properties/zustaendigkeit/items",
            """
            {"@type":"ZustaendigkeitOrganisationseinheit",
             "leistungID":[{"value":"lb","schemeAgencyID":"test"}],"gebietID":[{"value":"gebiet","schemeAgencyID":"test"}],
             "rolle":[{"code":"02"}],"kennzeichenTerminvereinbarungVerpflichtend":true}
            """,
            "ServiceDescriptionDetail/properties/details/items",
            """
            {"@type":"_moduleDate","title":"Datum","code":"35",
             "accessChannel":"ONLINE_OFFLINE","date":"2026-10-23"}
            """
        );
        for (var entry : examples.entrySet()) {
            // Nur den Slot validieren; Pflichtfelder der umgebenden Antwort gehören nicht zu diesem Nachweis.
            var root = mapper
                .createObjectNode()
                .put("$ref", "#/components/schemas/" + entry.getKey());
            root.set("components", document.path("components"));
            var validator = SchemaRegistry.withDefaultDialectId(
                "https://json-schema.org/draft/2020-12/schema"
            ).getSchema(root);
            var payload = mapper.readTree(entry.getValue());
            var errors = validator.validate(payload);
            assertTrue(errors.isEmpty(), () -> entry.getKey() + ": " + errors);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableConfigurationProperties(SpringDocConfigProperties.class)
    @ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class,
        SpringDocPageableConfiguration.class,
        SpringDocSortConfiguration.class,
        SpringDocSpecPropertiesConfiguration.class,
    })
    @Import({
        OpenApiConfig.class,
        SwaggerCoreConfiguration.class,
        OpenApiJsonViewDiscriminatorCustomizer.class,
    })
    static class ContractConfiguration {

        @Bean
        ApiAudienceResolver apiAudienceResolver() {
            return new ApiAudienceResolver(Optional.empty());
        }
    }
}
