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
package de.dataport.suchdienst.error.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
@Schema(
    description = "Fehlerinformationen",
    example = """
    {
        "timestamp": "2022-07-19T07:56:29.263780500Z",
        "code": "BAD_REQUEST",
        "message": "Die übermittelten Request-Pfad-Parameter entsprechen nicht den Anforderungen.",
        "codeNumber": 1901
    }"""
)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorModel implements Serializable {

    protected final Instant timestamp;
    protected final ErrorCodeEnum code;
    protected final String message;

    @JsonIgnore
    protected final ExceptionType exceptionType;

    public ErrorModel(
        @NonNull ErrorCodeEnum code,
        @NonNull String message,
        @NonNull ExceptionType exceptionType
    ) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(exceptionType, "exceptionType must not be null");

        this.timestamp = Instant.now();
        this.code = code;
        this.message = message;
        this.exceptionType = exceptionType;
    }

    public int getCodeNumber() {
        return code.getNumber();
    }

    @Override
    public String toString() {
        return (
            "ErrorModel{" +
            "timestamp=" +
            timestamp +
            ", code=" +
            code +
            ", message='" +
            message +
            '\'' +
            ", exceptionType=" +
            exceptionType +
            '}'
        );
    }
}
