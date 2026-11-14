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
package de.dataport.suchdienst.elasticsearch.api.exception;

import de.dataport.suchdienst.error.SuchdienstException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;

/**
 * Elasticsearch related exceptions ( interaction with elastic API )
 */
public class ElasticSearchException extends SuchdienstException {

    public ElasticSearchException(ErrorCodeEnum code, String message) {
        super(code, message, ExceptionType.TECHNISCH);
    }

    public ElasticSearchException(ErrorCodeEnum code, String message, Throwable cause) {
        super(code, message, ExceptionType.TECHNISCH, cause);
    }

    public ElasticSearchException(
        ErrorCodeEnum code,
        String message,
        ExceptionType exceptionType,
        Throwable cause
    ) {
        super(code, message, exceptionType, cause);
    }
}
