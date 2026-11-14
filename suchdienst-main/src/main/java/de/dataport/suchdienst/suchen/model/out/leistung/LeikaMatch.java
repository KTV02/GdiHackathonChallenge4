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
package de.dataport.suchdienst.suchen.model.out.leistung;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Set;

/**
 * Beschreibt, wie die gelieferte Treffermenge zu den angefragten LeiKa-Schlüsseln zustande gekommen
 * ist.
 * <p>
 * Ermöglicht Konsumenten die Unterscheidung zwischen exakten Treffern und Ergebnissen, die über die
 * LeiKa-Hierarchie ausgewichen sind, sodass die Qualität der Trefferliste kenntlich gemacht werden
 * kann.
 * </p>
 *
 * @param matchType         Beziehung der Treffer zu den angefragten Schlüsseln
 * @param effectiveLeikaIds die Schlüssel, mit denen die letztlich erfolgreiche Suche ausgeführt wurde.
 *                          Immer vollständige, 14-stellige LeiKa-Schlüssel. Bei
 *                          {@link LeikaMatchType#EXACT} und {@link LeikaMatchType#DESCENDANT}
 *                          entsprechen sie den angefragten Schlüsseln, bei
 *                          {@link LeikaMatchType#ANCESTOR} den übergeordneten und geben damit an, auf
 *                          welche Ebene der Hierarchie ausgewichen wurde. Bei
 *                          {@link LeikaMatchType#NONE} ist die Menge leer.
 *                          <p>
 *                          <b>Achtung:</b> Es handelt sich um die Schlüssel der Suchanfrage, nicht um
 *                          die Schlüssel der gefundenen Leistungen. Bei bundeseinheitlichen Schlüsseln
 *                          wird die zweistellige Instanz bei der Suche ignoriert, sodass eine Anfrage
 *                          nach {@code 77008001001001} auch eine Leistung mit dem Schlüssel
 *                          {@code 99008001001001} liefert. Zurückgegeben wird dann der angefragte
 *                          Schlüssel {@code 77008001001001}.
 *                          </p>
 */
public record LeikaMatch(
    @Schema(
        description = "Beziehung der Treffer zu den angefragten LeiKa-Schlüsseln.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    LeikaMatchType matchType,

    @Schema(
        description = "Die vollständigen, 14-stelligen LeiKa-Schlüssel, mit denen die letztlich" +
            " erfolgreiche Suche ausgeführt wurde. Bei matchType ANCESTOR weichen sie von den" +
            " angefragten Schlüsseln ab und geben an, auf welche Ebene der Hierarchie ausgewichen" +
            " wurde; bei NONE ist die Menge leer. Es handelt sich um die Schlüssel der Suchanfrage," +
            " nicht um die der gefundenen Leistungen: Da die zweistellige Instanz bei" +
            " bundeseinheitlichen Schlüsseln ignoriert wird, kann eine gefundene Leistung einen" +
            " abweichenden Instanz-Präfix tragen.",
        example = "[\"99008001000000\"]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Set<String> effectiveLeikaIds
) implements Serializable {
    /**
     * Ergebnis, wenn an keiner Stelle der Hierarchie Treffer ermittelt werden konnten.
     */
    public static LeikaMatch none() {
        return new LeikaMatch(LeikaMatchType.NONE, Set.of());
    }
}
