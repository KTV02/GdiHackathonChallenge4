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
package de.dataport.suchdienst.util;

import static org.hamcrest.MatcherAssert.assertThat;

import de.dataport.suchdienst.config.Jackson3Configuration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.xoev.xzufi.v2_2_0.Leistung;
import de.xoev.xzufi.v2_2_0.SchreibeObjekt;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testet Aspekte der JZufi-Konvertierung mit Jackson
 */
@SpringBootTest(
    classes = {
        JacksonAutoConfiguration.class,
        Jackson3Configuration.class,
        SuchdienstProperties.class,
    }
)
class JsonXmlTests {

    @Autowired
    @Qualifier("elasticsearchJsonMapper")
    private JsonMapper mapper;

    private Leistung leistung;

    protected TransferOperation040502 operation;

    @BeforeEach
    void fixture() throws Exception {
        InputStream xmlStream = this.getClass()
            .getClassLoader()
            .getResourceAsStream("XZUFI20181114214018000000000001.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(TransferOperation040502.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        operation = (TransferOperation040502) unmarshaller.unmarshal(xmlStream);
        leistung = ((SchreibeObjekt) operation.getTransferOperation().getFirst()).getLeistung();
    }

    /**
     * Testet ob die Inhalte durch De-/Serialisierung gleich bleiben
     */
    @Test
    void testIdempotence() {
        JsonNode valueToTree = mapper.valueToTree(leistung);

        Leistung reparsed = mapper.readValue(
            mapper.writeValueAsBytes(valueToTree),
            leistung.getClass()
        );

        assertThat(mapper.valueToTree(reparsed), Is.is(valueToTree));
    }

    /**
     * Ohne unsere {@link de.dataport.suchdienst.config.Jackson3Configuration} produziert Jackson bei den Gueltigkeiten falsche Angaben,
     * aus 2018-08-01 wird dann z.B. 2018-07-31T22:00:00.000+0000.
     */
    @Test
    void testDateFormat() {
        JsonNode valueToTree = mapper.valueToTree(leistung);
        JsonNode node = valueToTree.get(1).get("gueltigkeit").get(0).get(1).get("beginn");
        assertThat(node.asString(), Is.is("2018-08-01"));
    }
}
