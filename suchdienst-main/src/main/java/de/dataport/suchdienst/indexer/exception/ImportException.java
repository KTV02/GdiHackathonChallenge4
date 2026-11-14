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
package de.dataport.suchdienst.indexer.exception;

import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;

/**
 * implementation for Import related errors e.g parsing
 */
public class ImportException extends SuchdienstException {

    public ImportException(ErrorCodeEnum code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public ImportException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(code, message, exceptionType);
    }

    public ImportException(
        ErrorCodeEnum code,
        String message,
        ExceptionType exceptionType,
        Throwable cause
    ) {
        super(code, message, exceptionType, cause);
    }
}
