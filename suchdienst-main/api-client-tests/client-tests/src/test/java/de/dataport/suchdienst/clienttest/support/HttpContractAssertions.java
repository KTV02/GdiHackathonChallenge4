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
package de.dataport.suchdienst.clienttest.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.model.ApiErrorModel;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/** Gemeinsame Assertions; die auszuführenden API-Aufrufe bleiben sichtbar in den Operationsklassen. */
public final class HttpContractAssertions {

    private HttpContractAssertions() {}

    public static <T> T assertJsonSuccess(ResponseEntity<T> response) {
        assertEquals(200, response.getStatusCode().value());
        assertJson(response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    /**
     * Erwartet einen echten HTTP-Fehler. Pflichtparameterfälle senden deshalb teilweise rohe HTTP-Aufrufe:
     * Eine lokale Nullprüfung des generierten Clients wäre kein Nachweis der Servervalidierung.
     */
    public static ApiErrorModel assertApiError(int status, Executable read) {
        var exception = assertThrows(RestClientResponseException.class, read);
        assertEquals(status, exception.getStatusCode().value());
        assertNotNull(exception.getResponseHeaders());
        assertJson(exception.getResponseHeaders().getContentType());
        var error = StrictApiClientFactory.mapper().readValue(exception.getResponseBodyAsString(), ApiErrorModel.class);
        assertEquals(
            status == 404 ? ApiErrorModel.CodeEnum.NOT_FOUND : ApiErrorModel.CodeEnum.BAD_REQUEST,
            error.getCode()
        );
        assertNotNull(error.getTimestamp());
        assertNotNull(error.getMessage());
        assertFalse(error.getMessage().isBlank());
        assertNotNull(error.getRequestId());
        assertFalse(error.getRequestId().isBlank());
        return error;
    }

    public static void assertJson(MediaType contentType) {
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(contentType), contentType.toString());
    }
}
