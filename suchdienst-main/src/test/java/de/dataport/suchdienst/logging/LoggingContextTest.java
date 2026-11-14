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
package de.dataport.suchdienst.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.dataport.suchdienst.SuchdienstInitializer;
import de.dataport.suchdienst.config.RequestParameterFilter;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.importer.controller.PushImportController;
import de.dataport.suchdienst.importer.service.QueueImportService;
import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import jakarta.servlet.ServletContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Prüft das MDC-Verhalten gegen einen laufenden Spring-Kontext: echter Filter, echter Controller,
 * echter {@code @ControllerAdvice} und echte Executor-Beans.
 *
 * <p>Das Profil {@code security_off} schaltet {@code WebSecurityConfig} ab, damit die Requests ohne
 * Keycloak-Token durchlaufen. Da dort auch der {@link RequestParameterFilter} registriert wird, hängt
 * ihn dieser Test selbst vor die MockMvc-Kette.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({ "default", "dev", "security_off" })
@MockitoBean(types = SuchdienstInitializer.class)
@ExtendWith(MdcCleanupExtension.class)
class LoggingContextTest {

    private static final String PUSH_PATH =
        PushImportController.BASE_PATH + "/" + PushImportController.ENQUEUE_PATH;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private WebEndpointProperties actuatorProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiAudienceResolver apiAudienceResolver;

    @Autowired
    @Qualifier("ioExecutor")
    private ThreadPoolTaskExecutor ioExecutor;

    @MockitoBean
    private QueueImportService queueImportService;

    private MockMvc mockMvc;
    private Logger rootLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(
                new RequestParameterFilter(
                    servletContext,
                    actuatorProperties,
                    objectMapper,
                    apiAudienceResolver,
                    true
                )
            )
            .build();

        appender = new ListAppender<>();
        appender.start();
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(appender);
        appender.stop();
    }

    /**
     * Ziel: Der Import-Kontext überlebt den Sprung vom Controller in den {@code @ControllerAdvice}.
     * Der Fehler wird erst geloggt, nachdem der Controller verlassen wurde — trägt das Fehler-Log
     * `protokolltyp`, `nachricht_uuid` und `transaktion_id` nicht mehr, ist der Fehler im Betrieb
     * keiner Transaktion des Sammlerdienstes mehr zuzuordnen.
     */
    @Test
    void fehlgeschlagener_push_import_traegt_den_import_kontext_bis_ins_fehler_log()
        throws Exception {
        pushMitUngueltigerTransaktionsposition("nachricht-1", "transaktion-1");

        var empfangen = eventStartingWith("Transfernachricht mit der ID");
        assertThat(empfangen)
            .containsEntry(MdcKeys.PROTOKOLLTYP, ProtocolType.IMPORT.name())
            .containsEntry(MdcKeys.NACHRICHT_UUID, "nachricht-1")
            .containsEntry(MdcKeys.TRANSAKTION_ID, "transaktion-1")
            .containsKey(MdcKeys.REQUEST_ID);

        var fehler = eventWithMdc(MdcKeys.ERROR_CODE);
        assertThat(fehler)
            .containsEntry(MdcKeys.PROTOKOLLTYP, ProtocolType.IMPORT.name())
            .containsEntry(MdcKeys.NACHRICHT_UUID, "nachricht-1")
            .containsEntry(MdcKeys.TRANSAKTION_ID, "transaktion-1")
            .containsEntry(MdcKeys.REQUEST_ID, empfangen.get(MdcKeys.REQUEST_ID));
    }

    /**
     * Ziel: Ein Scope stellt den Vorwert wieder her, statt den Schlüssel zu löschen ("lost write").
     * Der Filter setzt für seine eigene Ausgabe kurzzeitig {@link ProtocolType#SICHERHEIT}; im selben
     * Request muss davor und danach weiter {@link ProtocolType#IMPORT} gelten.
     */
    @Test
    void sicherheits_log_ueberschreibt_den_protokolltyp_nur_fuer_die_eigene_ausgabe()
        throws Exception {
        when(queueImportService.enqueueImportRequest(any(), any(), any())).thenReturn("msg-1");

        mockMvc
            .perform(
                post(PUSH_PATH)
                    .contentType(MediaType.APPLICATION_XML)
                    .header(PushImportController.MESSAGE_ID_HEADER, "nachricht-2")
                    .header(PushImportController.TRANSACTION_ID_HEADER, "transaktion-2")
                    .header("X-PVOG-TRANSAKTIONPOSITION", "0")
                    .header("X-PVOG-AGENCYID", "S100002")
                    .header("X-PVOG-PRODUCER", "test-redaktionssystem")
                    .content("<xzufi/>")
            )
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

        var sicherheit = eventStartingWith("SICHERHEIT: Eine Anfrage");
        assertThat(sicherheit)
            .containsEntry(MdcKeys.PROTOKOLLTYP, ProtocolType.SICHERHEIT.name())
            // Der Request-Kontext gilt weiter, nur der Protokolltyp ist fuer diese Ausgabe ersetzt.
            .containsEntry(MdcKeys.TRANSAKTION_ID, "transaktion-2")
            .containsKey(MdcKeys.REQUEST_ID);

        assertThat(eventStartingWith("Transfernachricht mit der ID")).containsEntry(
            MdcKeys.PROTOKOLLTYP,
            ProtocolType.IMPORT.name()
        );
    }

    /**
     * Ziel: Kein Übersprechen zwischen Requests. Zwei Anfragen auf demselben Thread bekommen eigene
     * `request_id`-Werte und sehen die Header der jeweils anderen nicht.
     */
    @Test
    void aufeinanderfolgende_requests_teilen_keine_mdc_eintraege() throws Exception {
        pushMitUngueltigerTransaktionsposition("nachricht-a", "transaktion-a");
        pushMitUngueltigerTransaktionsposition("nachricht-b", "transaktion-b");

        List<Map<String, String>> empfangen = mdcMaps()
            .stream()
            .filter(mdc -> mdc.containsKey(MdcKeys.NACHRICHT_UUID))
            .filter(mdc -> !mdc.containsKey(MdcKeys.ERROR_CODE))
            .toList();

        assertThat(empfangen).hasSizeGreaterThanOrEqualTo(2);
        assertThat(empfangen.getFirst())
            .containsEntry(MdcKeys.NACHRICHT_UUID, "nachricht-a")
            .containsEntry(MdcKeys.TRANSAKTION_ID, "transaktion-a");
        assertThat(empfangen.getLast())
            .containsEntry(MdcKeys.NACHRICHT_UUID, "nachricht-b")
            .containsEntry(MdcKeys.TRANSAKTION_ID, "transaktion-b");
        // Stelle null-safe sicher, dass die requestIds in der ersten und der letzen Antwort nicht identisch sind
        assertThat(empfangen.getFirst()).doesNotContainEntry(
            MdcKeys.REQUEST_ID,
            empfangen.getLast().get(MdcKeys.REQUEST_ID)
        );
    }

    /**
     * Ziel: Der Filter räumt den Request-Kontext vollständig ab und stellt dabei einen vor dem Request
     * gesetzten Wert wieder her, statt ihn zu löschen.
     */
    @Test
    void der_request_thread_bleibt_nach_dem_request_ohne_mdc_eintraege() throws Exception {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.SYSTEM.name());

        pushMitUngueltigerTransaktionsposition("nachricht-3", "transaktion-3");

        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcKeys.NACHRICHT_UUID)).isNull();
        assertThat(MDC.get(MdcKeys.TRANSAKTION_ID)).isNull();
        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.SYSTEM.name());
    }

    /**
     * Ziel: Der {@code TaskDecorator} der Executor-Beans überträgt den MDC in den Pool-Thread und
     * hinterlässt ihn sauber. Ohne die Übertragung verlieren asynchrone Tasks ihren Kontext, ohne das
     * Aufräumen erbt der nächste, unbeteiligte Task den Kontext seines Vorgängers.
     */
    @Test
    void spring_executor_uebertraegt_den_mdc_und_hinterlaesst_den_pool_thread_sauber()
        throws ExecutionException, InterruptedException {
        Callable<Map<String, String>> kontextAuslesen = MdcTemplate::currentContext;

        Map<String, String> kontextImTask;
        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, "lb-1")) {
            MDC.put(MdcKeys.REQUEST_ID, "request-1");
            kontextImTask = ioExecutor.submit(kontextAuslesen).get();
        }

        assertThat(kontextImTask)
            .containsEntry(MdcKeys.LBID, "lb-1")
            .containsEntry(MdcKeys.REQUEST_ID, "request-1");

        MDC.clear();
        assertThat(ioExecutor.submit(kontextAuslesen).get()).isEmpty();
    }

    /**
     * Ziel: Auch der {@code execute}-Pfad ist dekoriert. {@link CompletableFuture#supplyAsync} nutzt
     * diesen statt {@code submit} — nur deshalb bleiben MDC-Schreibzugriffe innerhalb solcher Tasks
     * (z.B. {@code http_status} im BkgGeoLocationResolver) auf den Pool-Thread beschränkt.
     */
    @Test
    void completable_future_erbt_den_mdc_und_haelt_den_pool_thread_sauber()
        throws ExecutionException, InterruptedException {
        Map<String, String> kontextImTask;
        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, "lb-2")) {
            kontextImTask = CompletableFuture.supplyAsync(() -> {
                MDC.put(MdcKeys.HTTP_STATUS, "200");
                return MdcTemplate.currentContext();
            }, ioExecutor).get();
        }

        assertThat(kontextImTask).containsEntry(MdcKeys.LBID, "lb-2");
        assertThat(MDC.get(MdcKeys.HTTP_STATUS)).isNull();
        assertThat(
            CompletableFuture.supplyAsync(MdcTemplate::currentContext, ioExecutor).get()
        ).doesNotContainKey(MdcKeys.HTTP_STATUS);
    }

    private void pushMitUngueltigerTransaktionsposition(String nachrichtId, String transaktionId)
        throws Exception {
        mockMvc
            .perform(
                post(PUSH_PATH)
                    .contentType(MediaType.APPLICATION_XML)
                    .header(PushImportController.MESSAGE_ID_HEADER, nachrichtId)
                    .header(PushImportController.TRANSACTION_ID_HEADER, transaktionId)
                    .header("X-PVOG-TRANSAKTIONPOSITION", "99")
                    .header("X-PVOG-AGENCYID", "S100002")
                    .header("X-PVOG-PRODUCER", "test-redaktionssystem")
                    .content("<xzufi/>")
            )
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
    }

    private Map<String, String> eventStartingWith(String messagePrefix) {
        return appender.list
            .stream()
            .filter(event -> event.getMessage().startsWith(messagePrefix))
            .findFirst()
            .map(ILoggingEvent::getMDCPropertyMap)
            .orElseThrow(() ->
                new AssertionError("Kein Log-Event mit dem Praefix '" + messagePrefix + "'")
            );
    }

    private Map<String, String> eventWithMdc(String mdcKey) {
        return appender.list
            .stream()
            .map(ILoggingEvent::getMDCPropertyMap)
            .filter(mdc -> mdc.containsKey(mdcKey))
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Kein Log-Event mit dem MDC-Schluessel '" + mdcKey + "'")
            );
    }

    private List<Map<String, String>> mdcMaps() {
        return appender.list.stream().map(ILoggingEvent::getMDCPropertyMap).toList();
    }
}
