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

import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.util.MdcTemplate;
import java.io.Serial;
import java.util.Map;

/**
 * Diese Exception (und Subklasse ApiWarnException) ist dafür gedacht, fachliche Fehlermeldungen zu
 * generieren, die dann über die API nach draußen gereicht werden.
 * Hier ist darauf zu achten, dass keinesfalls interne technische Informationen in den "message" Argumenten
 * enthalten sind!
 */
public class ApiException extends SuchdienstException {

    @Serial
    private static final long serialVersionUID = 8250599494347365710L;

    private final ApiErrorModel error;

    public ApiException(ErrorCodeEnum code, String message, ExceptionType exceptionType) {
        super(code, message, exceptionType);
        this.error = new ApiErrorModel(
            this.code,
            this.getMessage(),
            exceptionType,
            MdcTemplate.currentRequestId()
        );
    }

    public ApiException(ErrorCodeEnum code, String message, Throwable cause) {
        this(code, message, ExceptionType.FACHLICH, cause);
    }

    public ApiException(
        ErrorCodeEnum code,
        String message,
        ExceptionType exceptionType,
        Throwable cause
    ) {
        super(code, message, exceptionType, cause);
        this.error = new ApiErrorModel(
            this.code,
            this.getMessage(),
            ExceptionType.FACHLICH,
            MdcTemplate.currentRequestId()
        );
    }

    public ApiException(ErrorCodeEnum code, String message, Map<String, String> fields) {
        super(code, message, ExceptionType.FACHLICH);
        this.error = new ApiErrorModel(
            this.code,
            this.getMessage(),
            this.exceptionType,
            fields,
            MdcTemplate.currentRequestId()
        );
    }

    public ApiException(
        ErrorCodeEnum code,
        String message,
        Map<String, String> fields,
        Throwable cause
    ) {
        super(code, message, ExceptionType.FACHLICH, cause);
        this.error = new ApiErrorModel(
            this.code,
            this.getMessage(),
            this.exceptionType,
            fields,
            MdcTemplate.currentRequestId()
        );
    }

    public ApiException set(String name, String value) {
        this.error.addField(name, value);
        return this;
    }

    @Override
    protected ApiErrorModel getModel() {
        return this.error;
    }
}
