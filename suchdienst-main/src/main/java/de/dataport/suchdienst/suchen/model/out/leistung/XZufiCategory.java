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
package de.dataport.suchdienst.suchen.model.out.leistung;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(enumAsRef = true)
public enum XZufiCategory {
    @JsonProperty("BEARBEITUNGSDAUER")
    BEARBEITUNGSDAUER(95),

    @JsonProperty("BEGRIFF_IM_KONTEXT")
    BEGRIFF_IM_KONTEXT(-1),

    @JsonProperty("DOKUMENT")
    DOKUMENT(96),

    @JsonProperty("FRIST")
    FRIST(97),

    @JsonProperty("KOSTEN")
    KOSTEN(98),

    @JsonProperty("TEXT_MODUL_INDIVIDUELL")
    TEXT_MODUL_INDIVIDUELL(30),

    @JsonProperty("TEXT_MODUL")
    TEXT_MODUL(-1),

    @JsonProperty("FACHLICHE_FREIGABE")
    FACHLICHE_FREIGABE(93),

    @JsonProperty("URSPRUNGSPORTAL_MODUL")
    URSPRUNGSPORTAL_MODUL(94);

    private final int blockPosition;

    XZufiCategory(int blockPosition) {
        this.blockPosition = blockPosition;
    }
}
