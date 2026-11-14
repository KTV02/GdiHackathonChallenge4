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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hier testen wir, ob das Mapping von Java-Objekte nach JSON funktioniert und insbesondere ob das Lesen von JSON Dokumenten
 * mit unbekannten Properties unkritisch ist.
 * Hintergrund ist der Wunsch, dass wir in unsere ES-Dokumente neue Attribute einfügen können, ohne eine Indexmigration durchzuführen.
 * Beim Deployment bedeutet dies zudem Zeit- und Platzersparnis, evtl noch laufende SuD Slaves, die das neue Attribut noch nicht im
 * Java Code haben, können dennoch die erweiterten JSON-Dokumente laden.
 *
 */
@Slf4j
@SpringBootTest(classes = { JsonMapper.class })
class ElasticSearchConfigurationTest {

    @Autowired
    JsonMapper mapper;

    @Test
    void testJsonMapping() {
        MappingTestClass testObject = new MappingTestClass();
        testObject.setAttr1("attr1-value");
        testObject.setAttr2("attr2-value");
        String testObjectAsString = mapper.writeValueAsString(testObject);
        assertThat(testObjectAsString, is("{\"attr1\":\"attr1-value\",\"attr2\":\"attr2-value\"}"));
    }

    @Test
    void testJsonMappingDoesNotFailOnUnknownProperty() {
        String inputStringWithUnknownProperty =
            "{\"attr1\":\"attr1-value\",\"attr2\":\"attr2-value\",\"attr3\":\"attr3-value\"}";
        MappingTestClass testObject = mapper.readValue(
            inputStringWithUnknownProperty,
            MappingTestClass.class
        );
        assertThat(testObject.getAttr1(), is("attr1-value"));
        assertThat(testObject.getAttr2(), is("attr2-value"));
    }

    @Setter
    @Getter
    private static class MappingTestClass {

        private String attr1;
        private String attr2;
    }
}
