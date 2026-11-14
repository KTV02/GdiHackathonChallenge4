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
package de.dataport.suchdienst.error;

import static java.util.stream.Collectors.joining;

import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Nützliche Hilfsfunktionen zum Loggen von Exceptions.
 */
class ExceptionHandlingUtils {

    private ExceptionHandlingUtils() {}

    static Map<String, String> getBindingErrorsAsMap(BindingResult result) {
        if (result == null) {
            return Collections.emptyMap();
        }
        return result
            .getModel()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }

    /**
     * Liefert eine Log-Ausgabe mit allen wichtigen Daten des WebRequest.
     * Ergebnis sieht ungefähr so aus:
     * GET uri=/theEndpointUri;client=127.0.0.1; params=param1:value1param2=value2; headers=host:localhost:8080,user-agent:curl/7.83.1; locale=de_DE
     */
    private static String webRequestLogString(WebRequest webRequest) {
        var strBuilder = new StringBuilder();
        if (webRequest instanceof ServletWebRequest servletWebRequest) {
            var httpMethod = servletWebRequest.getHttpMethod();
            strBuilder.append(httpMethod).append(" ");
        }

        // Data used for logging should be content-restricted and typed. This can be done by validating the data content or sanitizing it.
        // see javasecurity:S5145
        var description = webRequest.getDescription(true).replaceAll("[\n\r]", "_");
        strBuilder.append(description);

        var parameterMap = webRequest.getParameterMap();
        if (!parameterMap.isEmpty()) {
            var parameterMapStr = parameterMap
                .entrySet()
                .stream()
                .limit(20)
                .map(e -> {
                    var key = e.getKey();
                    var value = e.getValue();
                    final String valueStr;
                    if (value.length == 1) {
                        valueStr = value[0];
                    } else {
                        valueStr = Arrays.toString(value);
                    }
                    return key + ":" + StringUtils.abbreviate(valueStr, 50);
                })
                .collect(joining(","));
            strBuilder.append("; params=").append(parameterMapStr);
        }

        var headerNames = webRequest.getHeaderNames();
        var headerStr = Streams.stream(headerNames)
            .limit(20)
            .map(h -> h + ":" + StringUtils.abbreviate(webRequest.getHeader(h), 50))
            .collect(joining(","));
        if (!headerStr.isEmpty()) {
            strBuilder.append("; headers=").append(headerStr);
        }

        var locale = webRequest.getLocale();
        strBuilder.append("; locale=").append(locale);

        var userPrincipal = webRequest.getUserPrincipal();
        if (userPrincipal != null) {
            strBuilder.append("; userPrincipal=").append(userPrincipal);
        }
        return strBuilder.toString();
    }

    /**
     * Loggt eine Exception zu einem WebRequest auf ERROR level. Die Meldung sieht etwas so aus
     */
    static void logError(Logger logger, Exception ex, WebRequest request) {
        logError("Anfrage fehlgeschlagen", logger, ex, request);
    }

    /**
     * Loggt eine Exception zu einem WebRequest auf WARN level.
     */
    static void logWarn(Logger logger, Exception ex, WebRequest request) {
        logWarn("Anfrage fehlgeschlagen", logger, ex, request);
    }

    /**
     * Loggt eine Exception zu einem WebRequest auf ERROR level.
     */
    static void logError(String msg, Logger logger, Exception ex, WebRequest request) {
        var webRequestLogString = webRequestLogString(request);
        if (logger.isErrorEnabled()) {
            logger.error(formatLogMessage(msg, webRequestLogString, ex), ex);
        }
    }

    /**
     * Loggt eine Exception zu einem WebRequest auf WARN level.
     */
    static void logWarn(String msg, Logger logger, Exception ex, WebRequest request) {
        var webRequestLogString = webRequestLogString(request);
        if (logger.isWarnEnabled()) {
            logger.warn(formatLogMessage(msg, webRequestLogString, ex), ex);
        }
    }

    /**
     * Loggt eine Exception zu einem WebRequest auf INFO level.
     */
    static void logInfo(String msg, Logger logger, Exception ex, WebRequest request) {
        var webRequestLogString = webRequestLogString(request);
        if (logger.isInfoEnabled()) {
            logger.info(formatLogMessage(msg, webRequestLogString, ex), ex);
        }
    }

    private static String formatLogMessage(String msg, String webRequestLogString, Exception ex) {
        return String.format("%s: %s: %s ", msg, webRequestLogString, abbreviatedExceptionMsg(ex));
    }

    /**
     * Liefert den auf 1 KB gekürzten Meldungstext der Exception, sonst Gefahr, dass wir das Log sprengen.
     */
    private static String abbreviatedExceptionMsg(Exception ex) {
        return StringUtils.abbreviate(ex.getMessage(), 1024);
    }
}
