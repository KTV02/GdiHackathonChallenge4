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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Dieser Handler dient zum Loggen von RequestRejectedExceptions.
 * Wir loggen diese nur als Warnung statt als Fehler (Defaultverhalten).
 * Siehe https://stackoverflow.com/questions/51788764/how-to-intercept-a-requestrejectedexception-in-spring
 * und PV-8945.
 */
@Slf4j
public class SuchdienstRequestRejectedHandler implements RequestRejectedHandler {

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        RequestRejectedException requestRejectedException
    ) throws IOException {
        log.warn(
            "Request '{}' wird abgelehnt: {}",
            request.getRequestURL(),
            requestRejectedException.getMessage()
        );
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
