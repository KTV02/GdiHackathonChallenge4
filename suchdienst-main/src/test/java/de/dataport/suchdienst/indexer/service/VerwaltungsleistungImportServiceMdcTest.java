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
package de.dataport.suchdienst.indexer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.model.MdcKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Sichert ab, dass der Verarbeitungskontext auch im Fehlerfall noch am Log-Event hängt.
 */
@ExtendWith({ MockitoExtension.class, MdcCleanupExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class VerwaltungsleistungImportServiceMdcTest {

    @Mock
    private ElasticWriteConnector elasticWriteConnector;

    @InjectMocks
    private VerwaltungsleistungImportService service;

    @Test
    void fehler_log_eines_vl_updates_traegt_bundesland_und_lbid() {
        doThrow(new IllegalStateException("kaputt"))
            .when(elasticWriteConnector)
            .deleteSuggestionByLb("lb-1");

        Logger logger = (Logger) LoggerFactory.getLogger(VerwaltungsleistungImportService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.mediate(new VlUpdateTask("S100002", "producer", "02", "lb-1")).get();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        var fehlerEvent = appender.list
            .stream()
            .filter(event -> event.getMessage().startsWith("Fehler beim Reimport"))
            .findFirst()
            .orElseThrow();
        assertThat(fehlerEvent.getMDCPropertyMap())
            .containsEntry(MdcKeys.BUNDESLAND, "hamburg")
            .containsEntry(MdcKeys.LBID, "lb-1");
        assertThat(MDC.get(MdcKeys.BUNDESLAND)).isNull();
        assertThat(MDC.get(MdcKeys.LBID)).isNull();
    }
}
