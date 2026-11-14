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

/**
 * Gibt an, in welcher Beziehung die gelieferten Treffer zu den angefragten LeiKa-Schlüsseln stehen.
 * <p>
 * Die Suche über die LeiKa-Hierarchie arbeitet mit einer Fallback-Kaskade: Zunächst wird nach den
 * angefragten Schlüsseln gesucht. Bleibt diese Suche ohne Treffer, wird zuerst in der Hierarchie nach
 * unten (detailliertere Leistungen) und anschließend schrittweise nach oben (allgemeinere Leistungen)
 * ausgewichen.
 * </p>
 */
@Schema(
    description = "Beziehung der Treffer zu den angefragten LeiKa-Schlüsseln. Die Suche weicht bei" +
        " leerer Treffermenge zunächst auf detailliertere und anschließend auf allgemeinere Leistungen aus."
)
public enum LeikaMatchType {
    /**
     * Die Treffer tragen exakt die angefragten LeiKa-Schlüssel.
     */
    EXACT,

    /**
     * Zu den angefragten Schlüsseln gab es keine Treffer. Geliefert werden Leistungen aus der Ebene
     * darunter, also detailliertere Leistungen.
     */
    DESCENDANT,

    /**
     * Zu den angefragten Schlüsseln und deren Unterebenen gab es keine Treffer. Geliefert werden
     * Leistungen aus einer übergeordneten Ebene, also allgemeinere Leistungen.
     */
    ANCESTOR,

    /**
     * Weder zu den angefragten Schlüsseln noch an einer anderen Stelle der Hierarchie konnten Treffer
     * ermittelt werden. Die Treffermenge ist leer.
     */
    NONE,
}
