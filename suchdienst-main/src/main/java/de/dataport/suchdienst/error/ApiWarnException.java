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

import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import java.io.Serial;

/**
 * Api Exception die nur auf Warn-Level geloggt werden soll.
 * Diese Exception ist dafür gedacht, fachliche Fehlermeldungen zu
 * generieren, die dann über die API nach draußen gereicht werden.
 * Hier ist darauf zu achten, dass keinesfalls interne technische Informationen in den "message" Argumenten
 * enthalten sind!
 */
// Die Exception erbt von sehr vielen Klassen, damit sie zuverlässig gecacht werden kann
@SuppressWarnings("java:S110")
public class ApiWarnException extends ApiException {

    @Serial
    private static final long serialVersionUID = 5196133864243947195L;

    public ApiWarnException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(code, message, exceptionType);
    }
}
