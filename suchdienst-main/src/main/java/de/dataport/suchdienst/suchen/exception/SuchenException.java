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
package de.dataport.suchdienst.suchen.exception;

import de.dataport.suchdienst.error.ApiException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import java.util.Map;

/**
 * Diese Exception ist dafür gedacht, fachliche Fehlermeldungen für die Verwender der Such-API zu
 * generieren.
 * Hier ist darauf zu achten, dass keinesfalls interne technische Informationen in den "message" Argumenten
 * enthalten sind!
 */
// Die Exception erbt von sehr vielen Klassen, damit sie zuverlässig gecacht werden kann
@SuppressWarnings("java:S110")
public class SuchenException extends ApiException {

    public SuchenException(ErrorCodeEnum code, String message) {
        super(code, message, ExceptionType.TECHNISCH);
    }

    public SuchenException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(code, message, exceptionType);
    }

    public SuchenException(ErrorCodeEnum code, String message, Map<String, String> fields) {
        super(code, message, fields);
    }

    public SuchenException(ErrorCodeEnum code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
