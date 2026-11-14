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
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.util.MdcTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class SuchdienstException extends RuntimeException {

    protected final ErrorCodeEnum code;
    protected final ExceptionType exceptionType;

    /**
     * MDC zum Zeitpunkt des Auftretens, damit das Fehler-Logging im Exception-Handler den
     * Verarbeitungskontext noch kennt, obwohl dessen Scopes bereits geschlossen sind.
     */
    private final Map<String, String> mdcContext;

    protected SuchdienstException(ErrorCodeEnum code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.exceptionType = ExceptionType.TECHNISCH;
        this.mdcContext = copyMdcContext();
    }

    protected SuchdienstException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(message);
        this.code = code;
        this.exceptionType = exceptionType;
        this.mdcContext = copyMdcContext();
    }

    protected SuchdienstException(
        ErrorCodeEnum code,
        String message,
        ExceptionType exceptionType,
        Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.exceptionType = exceptionType;
        this.mdcContext = copyMdcContext();
    }

    private static Map<String, String> copyMdcContext() {
        // Kein Map.copyOf: der MDC darf null-Werte enthalten, eine NPE im Konstruktor waere fatal.
        return Collections.unmodifiableMap(new HashMap<>(MdcTemplate.currentContext()));
    }

    protected ErrorModel getModel() {
        return new ErrorModel(this.code, this.getMessage(), this.exceptionType);
    }
}
