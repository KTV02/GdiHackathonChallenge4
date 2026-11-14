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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.importer.controller.PushImportController;
import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.model.ApiClientId;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MdcCleanupExtension.class)
class RequestParameterFilterTest {

    private RequestParameterFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private ApiAudienceResolver apiAudienceResolver;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn("");
        WebEndpointProperties actuatorProperties = new WebEndpointProperties();
        actuatorProperties.setBasePath("/actuator");
        apiAudienceResolver = Mockito.mock(ApiAudienceResolver.class);

        filter = new RequestParameterFilter(
            servletContext,
            actuatorProperties,
            new ObjectMapper(),
            apiAudienceResolver,
            true
        );
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        chain = Mockito.mock(FilterChain.class);

        logger = (Logger) LoggerFactory.getLogger(RequestParameterFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    private Optional<ILoggingEvent> findApiZugriffEvent() {
        return listAppender.list
            .stream()
            .filter(event -> event.getFormattedMessage().startsWith("API-ZUGRIFF:"))
            .findFirst();
    }

    @Test
    void loggt_bei_oeffentlicher_api_anfrage_alle_geforderten_felder_per_mdc() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(request.getHeader(ApiClientId.HEADER_NAME)).thenReturn("mein-client");
        Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(
            "203.0.113.5, 10.0.0.1, 10.0.0.2"
        );
        Mockito.when(request.getHeader("User-Agent")).thenReturn("JUnit-Test-Agent/1.0");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Mockito.verify(chain).doFilter(request, response);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.ENDPOINT, "/v2/locations");
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.APICLIENTID, "mein-client");
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.CLIENT_IP, "203.0.113.5");
        assertThat(event.getMDCPropertyMap()).containsEntry(
            MdcKeys.USER_AGENT,
            "JUnit-Test-Agent/1.0"
        );
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.RESPONSE_STATUS, "200");
        assertThat(event.getMDCPropertyMap()).containsKey(MdcKeys.REQUEST_TIMESTAMP);
        assertThat(event.getMDCPropertyMap()).containsEntry(
            MdcKeys.PROTOKOLLTYP,
            ProtocolType.PUBLIC.name()
        );

        // Nach dem Request duerfen die MDC-Werte nicht mehr gesetzt sein (kein Leak)
        assertThat(MDC.get(MdcKeys.ENDPOINT)).isNull();
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isNull();
        assertThat(MDC.get(MdcKeys.USER_AGENT)).isNull();
        assertThat(MDC.get(MdcKeys.RESPONSE_STATUS)).isNull();
        assertThat(MDC.get(MdcKeys.REQUEST_TIMESTAMP)).isNull();
    }

    @Test
    void loggt_beta_api_anfragen_ebenfalls_als_oeffentliche_zugriffe() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v1beta2/onlineservices");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(
            MdcKeys.PROTOKOLLTYP,
            ProtocolType.PUBLIC.name()
        );
        assertThat(event.getMDCPropertyMap()).containsEntry(
            MdcKeys.ENDPOINT,
            "/v1beta2/onlineservices"
        );
    }

    @Test
    void loggt_unknown_als_clientId_wenn_kein_header_gesetzt_ist() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(
            ApiClientId.MDC_KEY,
            ApiClientId.UNKNOWN_CLIENT_ID
        );
    }

    @Test
    void faellt_bei_fehlendem_x_forwarded_for_header_auf_remoteAddr_zurueck() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(request.getRemoteAddr()).thenReturn("198.51.100.7");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.CLIENT_IP, "198.51.100.7");
    }

    @Test
    void faellt_bei_fehlendem_user_agent_auf_default_zurueck() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.USER_AGENT, "nicht angegeben");
    }

    @Test
    void loggt_nicht_fuer_nicht_oeffentliche_endpunkte() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/admin/irgendwas");
        Mockito.when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, chain);

        assertThat(findApiZugriffEvent()).isEmpty();
    }

    @Test
    void loggt_nicht_fuer_actuator_endpunkte() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/actuator/health");
        Mockito.when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, chain);

        assertThat(findApiZugriffEvent()).isEmpty();
    }

    @Test
    void enthaelt_response_status_aus_der_antwort() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(response.getStatus()).thenReturn(404);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);

        filter.doFilter(request, response, chain);

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.RESPONSE_STATUS, "404");
    }

    @Test
    void loggt_bei_unbehandelter_exception_status_500_statt_des_initialen_status()
        throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/v2/locations");
        Mockito.when(response.getStatus()).thenReturn(200);
        Mockito.when(apiAudienceResolver.isPubliclyAccessible(request)).thenReturn(true);
        Mockito.doThrow(new ServletException("kaputt")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(
            ServletException.class
        );

        ILoggingEvent event = findApiZugriffEvent().orElseThrow();
        assertThat(event.getFormattedMessage()).endsWith("Status: 500");
        assertThat(event.getMDCPropertyMap()).containsEntry(MdcKeys.RESPONSE_STATUS, "500");
    }

    @Test
    void haelt_import_mdc_bis_zum_ende_der_filter_chain_und_stellt_altwerte_wieder_her()
        throws Exception {
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());
        MDC.put(MdcKeys.TRANSAKTION_ID, "vorherige-transaktion");
        var mockRequest = new MockHttpServletRequest(
            "POST",
            PushImportController.BASE_PATH + "/" + PushImportController.ENQUEUE_PATH
        );
        mockRequest.addHeader(PushImportController.MESSAGE_ID_HEADER, "nachricht-1");
        mockRequest.addHeader(PushImportController.TRANSACTION_ID_HEADER, "transaktion-1");

        filter.doFilter(mockRequest, new MockHttpServletResponse(), (_, _) -> {
            assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNotBlank();
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.IMPORT.name());
            assertThat(MDC.get(MdcKeys.NACHRICHT_UUID)).isEqualTo("nachricht-1");
            assertThat(MDC.get(MdcKeys.TRANSAKTION_ID)).isEqualTo("transaktion-1");
        });

        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
        assertThat(MDC.get(MdcKeys.NACHRICHT_UUID)).isNull();
        assertThat(MDC.get(MdcKeys.TRANSAKTION_ID)).isEqualTo("vorherige-transaktion");
    }

    @Test
    void erhaelt_admin_protokolltyp_nach_dem_sicherheits_prelogging() throws Exception {
        var mockRequest = new MockHttpServletRequest("GET", "/admin/test");

        filter.doFilter(mockRequest, new MockHttpServletResponse(), (_, _) ->
            assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name())
        );

        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isNull();
    }
}
