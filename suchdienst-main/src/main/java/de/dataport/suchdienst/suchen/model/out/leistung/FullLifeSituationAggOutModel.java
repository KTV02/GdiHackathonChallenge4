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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PV-9431: Wie {@link AbstractLifeSituationAggOutModel}, aber mit Kommentar-Feld.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "Ein Lebenslageneintrag",
    example = """
    {
        "code": "2000000",
        "name": "Geschäftslagen für Unternehmen",
        "count": 2,
        "children": [
            {
                "code": "2030000",
                "name": "Arbeitgeber sein",
                "count": 2,
                "children": [
                    {
                        "code": "2030700",
                        "name": "Sonderregelungen der Arbeitszeit",
                        "count": 2,
                        "comment": "Neben den gewöhnlichen Arbeitszeitmodellen gibt es einige Sonderregelungen. Die Altersteilzeit oder das Wintergeld für Beschäftigte in witterungsabhängigen Branchen sind Beispiele hierfür. Informationen und Services zu diesen Regelungen haben wir hier für Sie zusammengefasst."
                    }
                ],
                "comment": "Sie leiten einen Betrieb mit Beschäftigten? Wie sieht es mit der Sozialversicherung aus? Wie bezahlt man die Lohnsteuer und welche Meldepflichten gibt es? Damit Sie als Arbeitgeberin oder Arbeitgeber den Überblick über Ihre Rechte und Pflichten behalten, finden Sie hier umfangreiche Informationen über arbeitsrechtliche Aspekte."
            }
        ],
        "comment": "Informationen und Services für Unternehmen in allen geschäftlichen Belangen."
    }"""
)
public final class FullLifeSituationAggOutModel
    extends AbstractLifeSituationAggOutModel<FullLifeSituationAggOutModel>
{

    /**
     * Der für den Client internationalisierte Kommentar der Lebenslage
     */
    @Schema(description = "Der für den Client internationalisierte Kommentar der Lebenslage")
    private String comment;
}
