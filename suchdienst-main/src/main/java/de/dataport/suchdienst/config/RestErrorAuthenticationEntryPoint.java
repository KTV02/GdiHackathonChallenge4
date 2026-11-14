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

import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.util.MdcTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import tools.jackson.databind.ObjectMapper;

@ControllerAdvice
public class RestErrorAuthenticationEntryPoint
    implements AuthenticationEntryPoint, AccessDeniedHandler
{

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public RestErrorAuthenticationEntryPoint(
        ObjectMapper objectMapper,
        MessageSource messageSource
    ) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException e
    ) throws IOException {
        // Weil Authentifizierungsfehler von Keycloak schon in der Filterkette verarbeitet werden, bevor das
        // Dispatcherservlet selbst aktiv wird, kann dieser Fehler nicht mit einem ExceptionHandler erledigt
        // werden. Entsprechend wird hier getan, was für andere Exceptions im RestExceptionHandler getan wird.
        // Wichtig ist, dass ein auf dem RestError basierender JSON-String im body dargestellt wird anstatt
        // des Spring-Standard-HTML-Codes.

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final var restError = new ApiErrorModel(
            ErrorCodeEnum.UNAUTHORIZED,
            messageSource.getMessage("api.401_not_authorized", null, Locale.GERMAN),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        response.getOutputStream().print(objectMapper.writeValueAsString(restError));
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        final var restError = new ApiErrorModel(
            ErrorCodeEnum.FORBIDDEN,
            messageSource.getMessage("api.403_access_denied", null, Locale.GERMAN),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        response.getOutputStream().print(objectMapper.writeValueAsString(restError));
    }
}
