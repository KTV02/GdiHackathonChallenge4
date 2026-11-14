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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class PvogRequestContextFilter implements Filter {

    private final PvogRequestContext context;

    public PvogRequestContextFilter(@NonNull PvogRequestContext context) {
        this.context = context;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest) {
            context.setLocaleFromRequestHeader(httpServletRequest);
            var locale = context.getLocale();
            if (response instanceof HttpServletResponse httpResponse && locale != null) {
                httpResponse.setHeader(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag());
            }
        }
        chain.doFilter(request, response);
    }
}
