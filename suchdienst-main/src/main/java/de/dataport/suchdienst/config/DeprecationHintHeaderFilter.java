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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class DeprecationHintHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpServletResponse) {
            httpServletResponse.setHeader(
                "X-DEPRECATED-DOMAIN",
                "Hinweis: Die APIs unter pvog.fitko.de werden am 01.08.2026 abgeschaltet. Bitte stellen Sie rechtzeitig auf pvog.fitko.net um. Wenn Sie bereits pvog.fitko.net verwenden, ist keine Anpassung erforderlich."
            );
        }
        chain.doFilter(request, response);
    }
}
