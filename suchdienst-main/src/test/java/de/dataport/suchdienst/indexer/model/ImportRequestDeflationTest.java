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
package de.dataport.suchdienst.indexer.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.json.JsonMapper;

@JsonTest
class ImportRequestDeflationTest {

    @Autowired
    private JsonMapper mapper;

    @Test
    void testConversion() {
        ImportRequest request = ImportRequest.createOpen(
            null,
            "hallo".getBytes(),
            XZuFiVersion.XZUFI22
        );
        String json = mapper.writeValueAsString(request);
        assertThat(json, containsString("deflate:"));
        ImportRequest readValue = mapper.readValue(json, ImportRequest.class);
        assertThat(readValue, notNullValue());
        assertThat(readValue.getXzufi(), is(request.getXzufi()));
    }

    @Test
    void testReadNonDeflatedJson() {
        String json = "{\"id\":\"id\",\"status\":\"DONE\",\"xzufi\":\"aGFsbG8=\"}";
        ImportRequest readValue = mapper.readValue(json, ImportRequest.class);
        assertThat(readValue, notNullValue());
        assertThat(readValue.getXzufi(), is("hallo".getBytes()));
    }
}
