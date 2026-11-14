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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Slf4j
public class ApiErrorModel extends ErrorModel {

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private Map<String, String> fields;

    private final String requestId;

    public ApiErrorModel(
        @NonNull ErrorCodeEnum code,
        @NonNull String message,
        @NonNull ExceptionType exceptionType,
        @NonNull String requestId
    ) {
        super(code, message, exceptionType);
        this.fields = new HashMap<>();
        this.requestId = requestId;
    }

    public ApiErrorModel(
        @NonNull ErrorCodeEnum code,
        @NonNull String message,
        @NonNull ExceptionType exceptionType,
        Map<String, String> fields,
        @NonNull String requestId
    ) {
        this(code, message, exceptionType, requestId);
        if (fields != null && !fields.isEmpty()) {
            this.addFields(fields);
        }
    }

    public void addField(String key, String value) {
        if (this.fields != null) {
            if (key != null) {
                this.fields.putIfAbsent(key, value);
            } else {
                log.info(
                    "Ignoriere NULL-Key in ApiErrorModel#fields: {}",
                    value,
                    new RuntimeException() // stacktrace erzeugen
                );
            }
        }
    }

    public void addFields(Map<String, String> fields) {
        this.fields = Stream.concat(this.fields.entrySet().stream(), fields.entrySet().stream())
            .filter(e -> {
                if (e.getKey() == null) {
                    log.info(
                        "Ignoriere NULL-Key in ApiErrorModel#fields: {}",
                        e,
                        new RuntimeException() // stacktrace
                    );
                    return false;
                }
                return true;
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        return (
            "ApiErrorModel{" +
            "timestamp=" +
            timestamp +
            ", code=" +
            code +
            ", message='" +
            message +
            '\'' +
            ", exceptionType=" +
            exceptionType +
            ", fields=" +
            fields +
            ", requestId=" +
            requestId +
            "}"
        );
    }
}
