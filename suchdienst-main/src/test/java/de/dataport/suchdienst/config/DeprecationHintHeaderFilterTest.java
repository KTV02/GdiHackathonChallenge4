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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DeprecationHintHeaderFilterTest {

    @Test
    void gibtHinweisAufAbschaltungInResponseHeaderAus() throws ServletException, IOException {
        var response = new MockHttpServletResponse();

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                // no-op
            }
        };

        new DeprecationHintHeaderFilter().doFilter(new MockHttpServletRequest(), response, chain);

        assertEquals(
            "Hinweis: Die APIs unter pvog.fitko.de werden am 01.08.2026 abgeschaltet. Bitte stellen Sie rechtzeitig auf pvog.fitko.net um. Wenn Sie bereits pvog.fitko.net verwenden, ist keine Anpassung erforderlich.",
            response.getHeader("X-DEPRECATED-DOMAIN")
        );
    }
}
