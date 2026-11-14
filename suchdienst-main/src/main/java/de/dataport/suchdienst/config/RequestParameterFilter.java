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
package de.dataport.suchdienst.config;

import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.importer.controller.PushImportController;
import de.dataport.suchdienst.util.DateUtils;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.RequestUtils;
import de.dataport.suchdienst.util.model.ApiClientId;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.event.Level;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Dieser Filter protokolliert eingehende HTTP-Anfragen und deren Ergebnisse.
 * Dazu loggt er vor und nach der Verarbeitung des Requests durch die Filter-Chain.
 * Zusätzlich legt er bei bestimmten Anfragen den ProtokollTyp im MDC fest, z.b. für Admin-Anfragen
 * den ProtocolType.ADMIN.
 *
 * <p>Als {@link OncePerRequestFilter} greift er pro Request genau einmal.
 */
@Slf4j
@SuppressWarnings("java:S1075") // Hardcode Path Delimiter ist hier ok, da wir eine HTTP-Url nutzen und keinen Dateipfad
public class RequestParameterFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "bearer ";
    private static final String PUSH_IMPORT_PATH =
        PushImportController.BASE_PATH + "/" + PushImportController.ENQUEUE_PATH;
    private static final String AUTH_DESCRIPTION_ATTRIBUTE =
        RequestParameterFilter.class.getName() + ".authDescription";

    private final String contextPath;
    private final String actuatorPath;

    private final ObjectMapper objectMapper;
    private final ApiAudienceResolver apiAudienceResolver;
    private final boolean publicApiAccessLoggingEnabled;

    public RequestParameterFilter(
        ServletContext ctx,
        WebEndpointProperties actuator,
        ObjectMapper objectMapper,
        ApiAudienceResolver apiAudienceResolver,
        boolean publicApiAccessLoggingEnabled
    ) {
        this.contextPath = ctx.getContextPath();
        this.actuatorPath = contextPath + actuator.getBasePath();
        this.objectMapper = objectMapper;
        this.apiAudienceResolver = apiAudienceResolver;
        this.publicApiAccessLoggingEnabled = publicApiAccessLoggingEnabled;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        Instant requestTimestamp = Instant.now();
        try (var _ = MdcTemplate.scoped(createRequestMdc(request))) {
            preLogging(request);
            Exception failure = null;
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException e) {
                failure = e;
                throw e;
            } finally {
                // im finally, damit auch abgebrochene Anfragen protokolliert werden
                int responseStatus = getEffectiveResponseStatus(response, failure);
                postLogging(request, responseStatus, failure);
                logPublicApiRequest(request, responseStatus, requestTimestamp);
            }
        }
    }

    private Map<String, String> createRequestMdc(HttpServletRequest httpRequest) {
        Map<String, String> requestMdc = new HashMap<>();
        requestMdc.put(MdcKeys.REQUEST_ID, UUID.randomUUID().toString());

        if (httpRequest.getRequestURI().startsWith(contextPath + "/admin")) {
            requestMdc.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());
        } else if (httpRequest.getRequestURI().equals(contextPath + PUSH_IMPORT_PATH)) {
            requestMdc.put(MdcKeys.PROTOKOLLTYP, ProtocolType.IMPORT.name());
            putHeaderIfPresent(
                requestMdc,
                MdcKeys.NACHRICHT_UUID,
                httpRequest,
                PushImportController.MESSAGE_ID_HEADER
            );
            putHeaderIfPresent(
                requestMdc,
                MdcKeys.TRANSAKTION_ID,
                httpRequest,
                PushImportController.TRANSACTION_ID_HEADER
            );
        }

        RequestUtils.getApiClientId(httpRequest).ifPresent(apiClientId ->
            requestMdc.put(ApiClientId.MDC_KEY, apiClientId.value())
        );
        return requestMdc;
    }

    /**
     * Ermittelt den für das Logging relevanten Status. Bei einer ungefangenen Exception steht an der
     * Response meist noch der ursprüngliche 200er-Status, weil der Container den Fehlerstatus erst danach
     * setzt; in diesem Fall wird direkt von 500 ausgegangen, statt den veralteten Status zu protokollieren.
     */
    private static int getEffectiveResponseStatus(
        HttpServletResponse httpServletResponse,
        Exception failure
    ) {
        if (failure != null && !httpServletResponse.isCommitted()) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return httpServletResponse.getStatus();
    }

    private static void putHeaderIfPresent(
        Map<String, String> requestMdc,
        String mdcKey,
        HttpServletRequest request,
        String headerName
    ) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            requestMdc.put(mdcKey, headerValue);
        }
    }

    /**
     * Loggt das Ergebnis der Anfrage auf dem SICHERHEIT-Protokoll; Level und Formulierung richten sich
     * nach Status und einer eventuellen Exception (siehe {@link #describeResult}).
     */
    private void postLogging(HttpServletRequest httpRequest, int status, Exception failure) {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.SICHERHEIT)) {
            HttpStatus resolvedStatus = HttpStatus.resolve(status);
            boolean failed = failure != null;
            String ergebnis = describeResult(status, failed);

            Level level;
            if (isPublicApiRequest(httpRequest) || isActuatorRequest(httpRequest)) {
                level = Level.TRACE;
            } else {
                level = failed || isErrorStatus(status) ? Level.WARN : Level.INFO;
            }

            log.atLevel(level)
                .setMessage("SICHERHEIT: Eine Anfrage {}: {} ({}): Endpunkt: {}, Autorisierung: {}")
                .addArgument(ergebnis)
                .addArgument(resolvedStatus != null ? resolvedStatus.name() : "UNBEKANNT")
                .addArgument(status)
                .addArgument(httpRequest.getRequestURI())
                .addArgument(() -> getAuthorizationDescription(httpRequest))
                .log();
        }
    }

    /**
     * Formuliert das Ergebnis für das SICHERHEIT-Log; eine Exception hat dabei Vorrang vor dem Status.
     */
    private String describeResult(int status, boolean failed) {
        if (failed) {
            return "wurde mit einem Fehler abgebrochen";
        }
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return "wurde abgelehnt";
        }
        if (isErrorStatus(status)) {
            return "ist fehlgeschlagen";
        }
        if (status / 100 == 2) {
            return "wurde erfolgreich bearbeitet";
        }
        // 1xx und 3xx, z.B. 304 Not Modified
        return "wurde beantwortet";
    }

    private boolean isErrorStatus(int status) {
        return status / 100 == 4 || status / 100 == 5;
    }

    /**
     * Protokolliert Zugriffe auf öffentliche APIs (siehe {@link #isPublicApiRequest(HttpServletRequest)})
     * auf INFO-Level. Alle relevanten Informationen werden dazu im MDC hinterlegt, damit sie im
     * JSON-Logging (siehe logback-spring.xml) als eigene, durchsuchbare Felder in Elasticsearch landen.
     * <p>
     * Öffentlich aufrufbar sind alle Endpunkte, die mit {@code ApiAudienceMarker(ApiAudience.PUBLIC)}, sowie
     * mit {@code ApiAudienceMarker(ApiAudience.BETA)} annotiert sind.
     */
    private void logPublicApiRequest(
        HttpServletRequest httpRequest,
        int responseStatus,
        Instant requestTimestamp
    ) {
        if (!publicApiAccessLoggingEnabled || !isPublicApiRequest(httpRequest)) {
            return;
        }

        MdcTemplate.runWith(
            createPublicApiAccessMdc(httpRequest, responseStatus, requestTimestamp),
            () ->
                log.info(
                    "API-ZUGRIFF: Anfrage an öffentliche Schnittstelle '{}' wurde bearbeitet, Status: {}",
                    httpRequest.getRequestURI(),
                    responseStatus
                )
        );
    }

    private Map<String, String> createPublicApiAccessMdc(
        HttpServletRequest httpRequest,
        int responseStatus,
        Instant requestTimestamp
    ) {
        // HashMap statt Map.of, weil getRemoteAddr() null liefern kann
        Map<String, String> apiAccessMdc = new HashMap<>();
        apiAccessMdc.put(MdcKeys.PROTOKOLLTYP, ProtocolType.PUBLIC.name());
        apiAccessMdc.put(MdcKeys.ENDPOINT, httpRequest.getRequestURI());
        // createRequestMdc setzt den Schlüssel nur, wenn der Aufrufer eine Client-Id mitschickt
        apiAccessMdc.put(
            MdcKeys.APICLIENTID,
            RequestUtils.getApiClientId(httpRequest)
                .map(ApiClientId::value)
                .orElse(ApiClientId.UNKNOWN_CLIENT_ID)
        );
        apiAccessMdc.put(MdcKeys.CLIENT_IP, getClientIpAddress(httpRequest));
        apiAccessMdc.put(
            MdcKeys.USER_AGENT,
            StringUtils.defaultIfBlank(httpRequest.getHeader("User-Agent"), "nicht angegeben")
        );
        apiAccessMdc.put(
            MdcKeys.REQUEST_TIMESTAMP,
            DateUtils.formatAsDateAndTimeWithMillis(requestTimestamp)
        );
        apiAccessMdc.put(MdcKeys.RESPONSE_STATUS, String.valueOf(responseStatus));
        return apiAccessMdc;
    }

    /**
     * Ermittelt die IP-Adresse des anfragenden Systems. Da der Suchdienst im K8s-Cluster hinter einem
     * Application Layer Gateway läuft, wird zunächst der Header 'X-Forwarded-For' ausgewertet
     * (erster, client-nächster Eintrag). Ist dieser nicht vorhanden, wird auf die direkte
     * Verbindungs-IP ({@link HttpServletRequest#getRemoteAddr()}) zurückgefallen.
     */
    private String getClientIpAddress(HttpServletRequest httpRequest) {
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private void preLogging(HttpServletRequest httpRequest) {
        // Aktionen hier finden vor dem Rest der filter chain statt
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.SICHERHEIT)) {
            Level level =
                isActuatorRequest(httpRequest) || isPublicApiRequest(httpRequest)
                    ? Level.TRACE
                    : Level.INFO;
            log.atLevel(level)
                .setMessage(
                    "SICHERHEIT: Eine Anfrage wurde registriert: Endpunkt: {}, Autorisierung: {}"
                )
                .addArgument(httpRequest.getRequestURI())
                .addArgument(() -> getAuthorizationDescription(httpRequest))
                .log();

            if (log.isDebugEnabled()) {
                log.debug("HttpRequest: Method: {}", httpRequest.getMethod());
                log.debug("HttpRequest: URL: {}", httpRequest.getRequestURL());
                log.debug("HttpRequest: Query String: {}", httpRequest.getQueryString());
                log.debug("HttpRequest: AuthType: {}", httpRequest.getAuthType());
                log.debug(
                    "HttpRequest: Header 'Authorization': {}",
                    getAuthorizationDescription(httpRequest)
                );
                RequestUtils.getApiClientId(httpRequest).ifPresent(apiClientId ->
                    log.debug("HttpRequest: ApiClientId: {}", apiClientId.value())
                );
                log.debug(
                    "HttpRequest: other Headers: 'Expect': {}, 'Forwarded': {}, 'X-Forwarded-Proto': {}," +
                        " 'X-Forwarded-Host': {}, 'X-Forwarded-For': {}, 'Via': {}, 'Location': {}",
                    httpRequest.getHeader("Expect"),
                    httpRequest.getHeader("Forwarded"),
                    httpRequest.getHeader("X-Forwarded-Proto"),
                    httpRequest.getHeader("X-Forwarded-Host"),
                    httpRequest.getHeader("X-Forwarded-For"),
                    httpRequest.getHeader("Via"),
                    httpRequest.getHeader("Location")
                );
            }
        }
    }

    private boolean isPublicApiRequest(HttpServletRequest httpRequest) {
        return apiAudienceResolver.isPubliclyAccessible(httpRequest);
    }

    private boolean isActuatorRequest(HttpServletRequest httpRequest) {
        // vorerst auch Requests auf den Root als actuator werden, weil die im k8s
        // als health-check verwendet werden, vgl. PV-7160
        if (
            Strings.CS.equals(
                Strings.CS.removeEnd(httpRequest.getRequestURI(), "/"),
                this.contextPath
            )
        ) {
            return true;
        }
        return Strings.CS.startsWith(httpRequest.getRequestURI(), actuatorPath);
    }

    /**
     * Erzeugt eine Beschreibung des Authorization Headers. Zerlegt, falls vorhanden, das JSON Web Token
     * von keycloak, für Informationen zur client_id und dem Ablaufdatum. Das Ergebnis wird am Request
     * zwischengespeichert, weil pre- und postLogging dieselbe Beschreibung brauchen.
     */
    private String getAuthorizationDescription(HttpServletRequest request) {
        if (request.getAttribute(AUTH_DESCRIPTION_ATTRIBUTE) instanceof String cached) {
            return cached;
        }
        String description = describeAuthorization(request);
        request.setAttribute(AUTH_DESCRIPTION_ATTRIBUTE, description);
        return description;
    }

    private String describeAuthorization(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (StringUtils.isBlank(authHeader)) {
            return "kein Authorization Header vorhanden";
        }

        byte[] payload;
        try {
            payload = getDecodedPayload(authHeader);
            if (payload.length > 0) {
                String clientDescription = getClientDescription(payload);
                if (clientDescription != null) {
                    return clientDescription;
                }
            }
        } catch (Exception _) {
            // PV-8946 catch-return
            // ignorieren, irgendwas Kaputtes wurde geliefert
        }

        // Nur das Schema loggen: der Wert kann Zugangsdaten enthalten (z.B. bei 'Basic')
        String authScheme = StringUtils.substringBefore(authHeader, " ");
        if (authScheme.length() == authHeader.length()) {
            return "unerwarteter Authorization Header ohne Schema";
        }
        return (
            "unerwarteter Authorization Header, Schema: '" +
            StringUtils.abbreviate(authScheme, 20) +
            "'"
        );
    }

    private String getClientDescription(byte[] payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String clientDescription = "ohne Hinweis auf Client";
            if (jsonNode.has("client_id")) {
                clientDescription = String.format(
                    "service (client_id): '%s'",
                    jsonNode.get("client_id").asString()
                );
            } else if (jsonNode.has("sub")) {
                clientDescription = String.format(
                    "user id (sub): '%s'",
                    jsonNode.get("sub").asString()
                );
            } else if (jsonNode.has("azp")) {
                clientDescription = String.format(
                    "authorized party (azp): '%s'",
                    jsonNode.get("azp").asString()
                );
            }

            String expirationDescription = "ohne Ablaufdatum (exp)";
            JsonNode expireNode = jsonNode.get("exp");
            if (expireNode != null && expireNode.isNumber()) {
                long secondsSinceEpoch = expireNode.asLong();
                var expireInstant = Instant.ofEpochSecond(secondsSinceEpoch);
                expirationDescription = String.format(
                    "Ablaufdatum (exp): %s",
                    DateUtils.formatAsDateAndTime(expireInstant)
                );
            }

            return String.format(
                "Bearer Token (JWT): %s, %s",
                clientDescription,
                expirationDescription
            );
        } catch (Exception _) {
            // PV-8946 catch-return
            // ignorieren, Kurz-Header wird geliefert
        }
        return null;
    }

    /**
     * json web token bestehen aus header, payload und signature, durch Punkte getrennt. Jeder Teil ist
     * in Base64URL codiert. Diese Methode liefert den decodierten payload, oder ein leeres byte-Array,
     * falls der authHead nicht mit dem BEARER_PREFIX beginnt oder kein json web token enthält.
     *
     * @param authHeader der Authorization header des Request
     * @return den decodierten payload, falls authHeader ein json web token enthält
     */
    private byte[] getDecodedPayload(String authHeader) {
        if (authHeader.length() > BEARER_PREFIX.length()) {
            String authHeaderHead = authHeader.substring(0, BEARER_PREFIX.length());
            if (BEARER_PREFIX.equalsIgnoreCase(authHeaderHead)) {
                String jsonWebToken = authHeader.substring(BEARER_PREFIX.length());
                String[] parts = jsonWebToken.split("\\.");
                if (parts.length == 3) {
                    return Base64.getUrlDecoder().decode(parts[1]);
                }
            }
        }
        return new byte[0];
    }
}
