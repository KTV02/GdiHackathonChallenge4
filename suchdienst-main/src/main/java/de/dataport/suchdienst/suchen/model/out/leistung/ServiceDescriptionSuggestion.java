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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Eine Leistungsbeschreibung die bei der Autocompletion gefunden wurde. Hier ist zusätzlich der
 * Text enthalten der vervollständigt wurde
 *
 * @author StubbeJa
 */
@Schema(
    description = "Vervollständigungen für 'Ingen'",
    example = """
        {
            "suggestedText": "Ingenieur / Ingenieurin: Erlaubnis zum Führen der Berufsbezeichnung"
        }
    """
)
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceDescriptionSuggestion {

    // Der Text der vervollständigt wurde
    private String suggestedText;
}
