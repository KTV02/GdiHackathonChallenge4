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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PvogRequestContextTest {

    private PvogRequestContext pvogRequestContext = new PvogRequestContext();

    @Test
    void testHappyPath() {
        assertRequestLocaleIsSetTo("de", "de");
        assertRequestLocaleIsSetTo("de", "de,en-US;q=0.7,en;q=0.3");
        assertRequestLocaleIsSetTo("en", "de;q=0.5,en-US;q=0.7,en;q=0.3");
    }

    @Test
    void testErrorPathDefaultFallback() {
        assertRequestLocaleIsSetTo("de", "fr,en-US;q=0.7;q=0.5,en;q=0.3");
    }

    @Test
    void testExceptionPathDefaultFallback() {
        assertRequestLocaleIsSetTo("de", "ganz sicher keine gültige locale");
    }

    private void assertRequestLocaleIsSetTo(String expectedLocale, String headerValue) {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader(Mockito.anyString())).thenReturn(headerValue);
        pvogRequestContext.setLocaleFromRequestHeader(mockRequest);
        assertEquals(Locale.of(expectedLocale), pvogRequestContext.getLocale());
    }
}
