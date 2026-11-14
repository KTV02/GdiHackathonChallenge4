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
package de.dataport.suchdienst.taskmgr.exception;

import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;

/**
 * Diese Exception wird geworfen, wenn ein SudTask irregulär terminiert, siehe SudTaskMgr.
 */
public class SudTaskException extends SuchdienstException {

    public SudTaskException(ErrorCodeEnum code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public SudTaskException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(code, message, exceptionType);
    }

    public SudTaskException(
        ErrorCodeEnum code,
        String message,
        ExceptionType exceptionType,
        Throwable cause
    ) {
        super(code, message, exceptionType, cause);
    }
}
