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
package de.dataport.suchdienst.indexer.model.mapping;

import java.util.Optional;
import lombok.Getter;

@Getter
public enum CodeServiceDescription {
    NAME("02"),
    NAME_OTHER_DESCRIPTION("03"),
    KURZTEXT("05"),
    VOLLTEXT("06"),
    SOURCE_PORTAL("18"),
    TEASER("24");

    private final String code;

    CodeServiceDescription(String code) {
        this.code = code;
    }

    public boolean isCode(String code) {
        return getCode().equals(code);
    }

    public static Optional<CodeServiceDescription> getByCode(String code) {
        for (CodeServiceDescription csd : values()) {
            if (csd.getCode().equals(code)) {
                return Optional.of(csd);
            }
        }

        return Optional.empty();
    }
}
