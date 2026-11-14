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

import org.springframework.http.HttpStatus;

/**
 * Vorschlag zu SuD-spezifischen Fehler-Codes; insbesondere interessant für REST-API u. Logs
 * <p>
 * Je "Fehlerquelle" (xzufi, ars, codelists, authentication/authorization, settings, ...) einen
 * reservierten numerischen Bereich, z.B. xzufi -> 1xx, settings -> 2xx usw.
 * <p>
 * Öffentliche Schnittstelle: REST vs Restful -> Vorschlag: Korrekter HTTP Status-Code (mit o.g.
 * (Code-) Zusatz im Body)
 * <p>
 */
public enum ErrorCodeEnum implements ErrorCode {
    ELSE_IO_ERROR(1301),
    JSON_IO_ERROR(1302),
    ELSE_POINT_IN_TIME_ERROR(1303),
    FAILURE_RESPONSE(1304),

    XZUFI_UNEXPECTED_FORMAT(1401),
    XZUFI_IMPORT_ERROR(1402),

    IMPORT_QUEUE(1500),
    IMPORT_QUEUE_STORAGE_FULL(1501),

    SEARCH_UNEXPECTED_RESULT(1502),

    MISSING_RESOURCE(1601),

    UNKNOWN_ERROR(1900),
    BAD_REQUEST(1901),
    BAD_JSON_REQUEST(1902),
    BAD_JSON_PROP_REQUEST(1903),
    NO_MASTER(1904),
    NO_INSTANCETYPE(1905),
    NOT_FOUND(1906),
    INVALID_CSV(1907),
    MISSING_HEADER(1908),
    MESSAGE_NOT_READABLE(1909),

    CODELIST_DOWNLOAD(1908),
    FORBIDDEN(1910),

    VIRUS_DETECTED(1911),
    VIRUS_SCANNER_ERROR(1912),

    INVALID_FILE(1913),

    CLIENT_ABORT(1914),
    CSV_CREATION_ERROR(1915),
    UNAUTHORIZED(1916),
    CONFLICT(1917),
    ENDPOINT_UNAVAILABLE(1918),
    IMPORT_RUNNING(1919),
    THREAD_ERROR(1920),
    GEO_REQUEST_ERROR(1921);

    private final int number;

    ErrorCodeEnum(int number) {
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    public static HttpStatus mapToHttpStatus(ErrorCodeEnum errorCode) {
        return switch (errorCode) {
            case CONFLICT -> HttpStatus.CONFLICT;
            case
                BAD_REQUEST,
                BAD_JSON_REQUEST,
                BAD_JSON_PROP_REQUEST,
                VIRUS_DETECTED,
                INVALID_CSV,
                INVALID_FILE,
                SEARCH_UNEXPECTED_RESULT,
                XZUFI_UNEXPECTED_FORMAT -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ELSE_IO_ERROR, JSON_IO_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
