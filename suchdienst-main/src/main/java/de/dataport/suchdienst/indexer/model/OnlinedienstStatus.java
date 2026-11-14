/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.model;

import java.util.Arrays;

public enum OnlinedienstStatus {
    UNDEFINED("00"),
    PLANNING("10"),
    INITIALIZATION("20"),
    TEST("30"),
    PRODUCTION("40"),
    MAINTENANCE("50"),
    DEACTIVATION("99");

    public static final String CODELIST_URI = "urn:xoev-de:fim:codeliste:xzufi.onlinedienststatus";
    public static final String CODELIST_VERSION = "1";

    private final String code;

    OnlinedienstStatus(String code) {
        this.code = code;
    }

    public static OnlinedienstStatus from(CodelistKey key) {
        if (
            key == null ||
            !CODELIST_URI.equals(key.uri()) ||
            !CODELIST_VERSION.equals(key.version())
        ) {
            return UNDEFINED;
        }

        return Arrays.stream(values())
            .filter(status -> status.code.equals(key.code()))
            .findFirst()
            .orElse(UNDEFINED);
    }

    public static boolean isKnown(CodelistKey key) {
        return (
            key != null &&
            CODELIST_URI.equals(key.uri()) &&
            CODELIST_VERSION.equals(key.version()) &&
            Arrays.stream(values()).anyMatch(status -> status.code.equals(key.code()))
        );
    }
}
