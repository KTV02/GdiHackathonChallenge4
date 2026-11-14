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
package de.dataport.suchdienst.util;

import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.util.model.ApiClientId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * Util-Methoden für die Arbeit mit ServletRequests.
 */
public class RequestUtils {

    private RequestUtils() {
        // Utility class
    }

    /**
     * Liefert die ApiClientId aus dem Request-Header zurück, falls vorhanden.
     * Liefert ein empty <code>Optional</code>, wenn kein Header gesetzt ist.
     */
    public static Optional<ApiClientId> getApiClientId(HttpServletRequest httpRequest) {
        String headerValue = httpRequest.getHeader(ApiClientId.HEADER_NAME);
        if (StringUtils.isNotEmpty(headerValue)) {
            return Optional.of(new ApiClientId(headerValue));
        }
        return Optional.empty();
    }

    /**
     * Liest die XZuFi-Version aus dem Accept-Header des Requests aus.
     * Wenn keine Version angegeben ist, wird <code>XZuFiVersion.XZUFI22</code> zurückgegeben.
     */
    public static @NonNull XZuFiVersion getXZuFiVersion(HttpServletRequest httpRequest) {
        String acceptHeader = httpRequest.getHeader(HttpHeaders.ACCEPT);
        List<MediaType> mediaTypes;
        try {
            mediaTypes = acceptHeader == null ? null : MediaType.parseMediaTypes(acceptHeader);
        } catch (InvalidMediaTypeException _) {
            mediaTypes = null;
        }
        if (mediaTypes == null) {
            return XZuFiVersion.XZUFI22;
        }
        return mediaTypes
            .stream()
            .flatMap(mediaType ->
                XZuFiVersionHeaderUtil.getXZuFiVersionFromHeader(mediaType).stream()
            )
            .findFirst()
            .orElse(XZuFiVersion.XZUFI22);
    }

    /**
     * Ersetzt potenziell gefährliche Zeichen aus {@code input} durch {@code *}, damit diese weder in Logs noch
     * Responses / Exceptions auftauchen.
     * <p>
     * Erlaubte Zeichen sind:
     * <ul>
     *     <li>Buchstaben und Ziffern</li>
     *     <li>Whitespace</li>
     *     <li>.,-</li>
     * </ul>
     * @param input Vom API-Client empfangene Eingabe
     * @return gefilterte Eingabe, welche keine gefährlichen Zeichen mehr enthält
     */
    public static String sanitizeUserInput(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(input.length());
        input
            .codePoints()
            .forEach(c ->
                result.appendCodePoint(
                    Character.isLetterOrDigit(c) ||
                        Character.isWhitespace(c) ||
                        c == '.' ||
                        c == ',' ||
                        c == '-'
                        ? c
                        : '*'
                )
            );
        return result.toString();
    }
}
