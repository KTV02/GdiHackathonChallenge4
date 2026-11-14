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
package de.dataport.suchdienst.indexer.parser.x22;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Validiert den Builder von {@link ServiceModuleDetailContent} und {@link ServiceModuleDetail} (x22):
 *  - Titel/Text/Links/Zusatzinfos werden korrekt gesetzt
 *  - Zusammengesetztes Detail-Modul enthält seine Kinder.
 */
class DetailsBlockCreationTest {

    private final List<Link> links = List.of(
        Link.of("uri", "title", "description"),
        Link.of("uri", "title", "description")
    );

    private final List<KeyValueObject> addInfo = List.of(
        new KeyValueObject("key", "text"),
        new KeyValueObject("key", "text")
    );

    @Test
    void testBlockModule() {
        ServiceModuleDetailContent module = buildBlockModul();

        assertNotNull(module);
        assertEquals("title", module.getTitle());
        assertEquals("text", module.getText());
        assertEquals(links, module.getLinks());
        assertEquals(addInfo, module.getAdditionalInformation());
    }

    @Test
    void testBlockModuleDetail() {
        List<Link> myLinks = List.of(
            Link.of("uri", "title", "description"),
            Link.of("uri", "title", "description")
        );
        List<ServiceModuleDetailContent> details = List.of(buildBlockModul(), buildBlockModul());

        ServiceModuleDetail detailModule = ServiceModuleDetail.builder()
            .title("title")
            .text("text")
            .links(myLinks)
            .details(details)
            .build();

        assertNotNull(detailModule);
        assertEquals("title", detailModule.getTitle());
        assertEquals("text", detailModule.getText());
        assertEquals(myLinks, detailModule.getLinks());
        assertEquals(details, detailModule.getDetails());
    }

    private ServiceModuleDetailContent buildBlockModul() {
        return ServiceModuleDetailContent.builder()
            .title("title")
            .text("text")
            .links(links)
            .additionalInformation(addInfo)
            .build();
    }
}
