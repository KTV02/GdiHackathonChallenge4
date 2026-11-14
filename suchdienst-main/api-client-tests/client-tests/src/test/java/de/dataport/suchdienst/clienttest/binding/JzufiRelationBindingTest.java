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
package de.dataport.suchdienst.clienttest.binding;

import static de.dataport.suchdienst.clienttest.support.JzufiContractAssertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.client.model.Jzufi23Formular;
import de.dataport.suchdienst.client.model.Jzufi23Zustaendigkeit;
import de.dataport.suchdienst.client.model.Jzufi23ZustaendigkeitOnlinedienst;
import de.dataport.suchdienst.client.model.Jzufi23ZustaendigkeitOrganisationseinheit;
import de.dataport.suchdienst.client.model.Jzufi23ZustaendigkeitPerson;
import de.dataport.suchdienst.client.model.JzufiRelationPage23;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Testziel: Gemischte Zustaendigkeitslisten behalten Typen, Reihenfolge und Felder in
 * Formularen und Relationsseiten. Seitenmetadaten und das dokumentierte Beispiel
 * bleiben bindbar; ungueltige Typkennungen scheitern auch im Seitenwrapper.
 * Die einzelnen Familien und Untertypfelder prueft {@link JzufiPolymorphismBindingTest}.
 */
class JzufiRelationBindingTest {

    @Test
    void preservesMixedResponsibilitiesThroughFormAndPageRoundTrips() {
        String responsibilities = """
        [
          {"@type":"Zustaendigkeit","leistungID":[{"value":"lb"}],"gebietID":[{"value":"region"}]},
          {"@type":"ZustaendigkeitOrganisationseinheit","leistungID":[{"value":"lb"}],
           "gebietID":[{"value":"region"}],"rolle":[{"code":"02"}],
           "kennzeichenTerminvereinbarungVerpflichtend":true},
          {"@type":"ZustaendigkeitOnlinedienst","leistungID":[{"value":"lb"}],
           "organisationseinheit":[{"organisationseinheitID":{"value":"oe"},"rolle":[{"code":"02"}]}]},
          {"@type":"ZustaendigkeitPerson","leistungID":[{"value":"lb"}],
           "kriterium":[{"codeKriterium":{"code":{"code":"01"}},"auspraegung":["A-Z"]}]}
        ]
        """;
        var expectedTypes = List.of(
            Jzufi23Zustaendigkeit.class,
            Jzufi23ZustaendigkeitOrganisationseinheit.class,
            Jzufi23ZustaendigkeitOnlinedienst.class,
            Jzufi23ZustaendigkeitPerson.class
        );
        var mapper = StrictApiClientFactory.mapper();
        String formJson = "{\"zustaendigkeit\":" + responsibilities + "}";
        var form = mapper.readValue(formJson, Jzufi23Formular.class);
        assertEquals(expectedTypes, form.getZustaendigkeit().stream().map(Object::getClass).toList());
        var formRoundTrip = mapper.readValue(mapper.writeValueAsString(form), Jzufi23Formular.class);
        assertEquals(expectedTypes, formRoundTrip.getZustaendigkeit().stream().map(Object::getClass).toList());
        assertContains(mapper.readTree(formJson), mapper.valueToTree(formRoundTrip));

        String pageJson =
            "{\"content\":[" + formJson + "],\"page\":{\"number\":0,\"size\":10,\"totalElements\":1,\"totalPages\":1}}";
        var page = mapper.readValue(pageJson, JzufiRelationPage23.class);
        var pageRoundTrip = mapper.readValue(mapper.writeValueAsString(page), JzufiRelationPage23.class);
        assertContains(mapper.readTree(pageJson), mapper.valueToTree(pageRoundTrip));
        assertEquals(
            expectedTypes,
            pageRoundTrip.getContent().getFirst().getZustaendigkeit().stream().map(Object::getClass).toList()
        );
    }

    @ParameterizedTest(name = "Ungueltige Typkennung im Seitenwrapper: {0}")
    @ValueSource(strings = { "{}", "{\"@type\":\"unknown\"}" })
    void rejectsInvalidTypesInsideRelationPage(String responsibility) {
        assertThrows(InvalidTypeIdException.class, () ->
            StrictApiClientFactory.mapper().readValue(
                "{\"content\":[{\"zustaendigkeit\":[" + responsibility + "]}]}",
                JzufiRelationPage23.class
            )
        );
    }

    @Test
    void bindsDocumentedRelationExample() {
        var document = YAMLMapper.builder()
            .build()
            .readTree(Path.of(System.getProperty("openapi.spec")).toFile());
        // Der begrenzte Modellexport trägt das Beispiel an der Komponente, der vollständige HTTP-Export an der Antwort.
        var example = document.path("components").path("schemas").path("JzufiRelationPage23").path("example");
        if (example.isMissingNode()) {
            example = document
                .path("paths")
                .path("/v2/relations/jzufi-2-3")
                .at("/get/responses/200/content/application~1json/examples")
                .valueStream()
                .findFirst()
                .orElseThrow()
                .path("value");
        }
        var page = StrictApiClientFactory.mapper().treeToValue(example, JzufiRelationPage23.class);
        var responsibility = assertInstanceOf(
            Jzufi23ZustaendigkeitOrganisationseinheit.class,
            page.getContent().getFirst().getZustaendigkeit().getFirst()
        );
        assertEquals("03", responsibility.getRolle().getFirst().getCode());
    }
}
