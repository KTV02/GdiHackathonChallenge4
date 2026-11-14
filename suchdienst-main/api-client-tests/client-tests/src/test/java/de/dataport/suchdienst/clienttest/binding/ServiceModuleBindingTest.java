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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.client.model.ServiceDescriptionDetail;
import de.dataport.suchdienst.client.model.ServiceModule;
import de.dataport.suchdienst.client.model.ServiceModuleDate;
import de.dataport.suchdienst.client.model.ServiceModuleDetail;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

/** Testziel: Leistungsdetails binden konkrete Module und lehnen unbekannte Typen sowie Untertypfelder ab. */
class ServiceModuleBindingTest {

    @Test
    void bindsConcreteTextAndStructuredModuleTypes() {
        var detail = StrictApiClientFactory.mapper().readValue(
            """
            {"details": [
              {"@type": "_module", "code": "06", "text": "SDK Volltext"},
              {"@type": "_moduleDetail", "code": "13", "text": "SDK Gebühr", "details": []}
            ]}
            """,
            ServiceDescriptionDetail.class
        );
        assertEquals(2, detail.getDetails().size());
        var text = assertInstanceOf(ServiceModule.class, detail.getDetails().getFirst());
        assertEquals("06", text.getCode());
        assertEquals("SDK Volltext", text.getText());
        var fee = assertInstanceOf(ServiceModuleDetail.class, detail.getDetails().get(1));
        assertEquals("13", fee.getCode());
        assertEquals("SDK Gebühr", fee.getText());
    }

    @Test
    void bindsDateModuleAndPreservesItsFieldsOnRoundTrip() {
        var mapper = StrictApiClientFactory.mapper();
        var detail = mapper.readValue(
            """
            {"details": [{
              "@type": "_moduleDate", "code": "35", "title": "Fachliche Freigabe",
              "accessChannel": "ONLINE_OFFLINE", "date": "2026-10-23"
            }]}
            """,
            ServiceDescriptionDetail.class
        );
        var date = assertInstanceOf(ServiceModuleDate.class, detail.getDetails().getFirst());
        assertEquals(LocalDate.of(2026, 10, 23), date.getDate());
        assertEquals("35", date.getCode());
        assertEquals("Fachliche Freigabe", date.getTitle());
        var json = mapper.readTree(mapper.writeValueAsString(detail));
        var module = json.path("details").get(0);
        assertEquals("_moduleDate", module.path("@type").asString());
        assertEquals("2026-10-23", module.path("date").asString());
        assertEquals("ONLINE_OFFLINE", module.path("accessChannel").asString());
        var rebound = mapper.treeToValue(json, ServiceDescriptionDetail.class);
        var reboundDate = assertInstanceOf(ServiceModuleDate.class, rebound.getDetails().getFirst());
        assertEquals(date.getDate(), reboundDate.getDate());
        assertEquals(date.getCode(), reboundDate.getCode());
        assertEquals(date.getTitle(), reboundDate.getTitle());
    }

    @Test
    void rejectsUnknownDiscriminator() {
        assertThrows(JacksonException.class, () ->
            StrictApiClientFactory.mapper().readValue(
                "{\"details\": [{\"@type\": \"unknown-module\"}]}",
                ServiceDescriptionDetail.class
            )
        );
    }

    @Test
    void rejectsUnknownFieldInsideKnownSubtype() {
        assertThrows(JacksonException.class, () ->
            StrictApiClientFactory.mapper().readValue(
                "{\"details\": [{\"@type\": \"_module\", \"unexpected\": true}]}",
                ServiceDescriptionDetail.class
            )
        );
    }
}
