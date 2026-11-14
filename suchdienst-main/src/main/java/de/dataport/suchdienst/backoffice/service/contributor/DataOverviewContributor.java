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
package de.dataport.suchdienst.backoffice.service.contributor;

import de.dataport.suchdienst.backoffice.model.out.DataOverviewOutModel;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Liefert einen bestimmten Aspekt der Datenübersicht.
 * <p>
 * Die Ausführung erfolgt in zwei klar getrennten Phasen:
 * <ol>
 *   <li><b>Phase 1 – {@link #collectData}</b>: Rohe Zähler und Fakten aus externen Quellen
 *       (Elasticsearch, Datenbank …) werden unabhängig voneinander in die Builder geschrieben.
 *       Contributors dieser Phase haben <em>keine</em> Abhängigkeit zueinander.</li>
 *   <li><b>Phase 2 – {@link #deriveState}</b>: Abgeleitete Zustände (z. B. {@code processState})
 *       werden auf Basis der in Phase 1 geschriebenen Werte berechnet.
 *       In dieser Phase dürfen Builder-Getter genutzt werden.</li>
 * </ol>
 * <p>
 * Um eine neue Datenquelle hinzuzufügen, genügt es, eine weitere {@code @Component}-Klasse
 * zu erstellen, die dieses Interface implementiert — ohne bestehende Contributors oder den
 * Orchestrator-Service anzupassen.
 */
public interface DataOverviewContributor {
    /**
     * Phase 1: Schreibt rohe Zähler und Fakten aus der jeweiligen Datenquelle in die Builder.
     * <p>
     * Darf <em>keine</em> Werte anderer Contributors lesen (Builder-Getter sind in dieser Phase
     * noch nicht zuverlässig befüllt).
     *
     * @param builders Map von SchemeAgencyId → Builder. Darf mutiert werden.
     */
    default void collectData(Map<String, DataOverviewOutModel.Builder> builders) {}

    /**
     * Phase 1 (parallelisierbar): Sammelt rohe Daten ohne direkten Schreibzugriff auf die
     * gemeinsamen Builder.
     * <p>
     * Gibt eine Merge-Funktion zurück, die später sequentiell auf die Builder angewendet wird.
     * Damit bleiben parallele Datenerhebung und thread-sicheres Mergen sauber getrennt.
     *
     * @param schemeAgencyIds Alle bekannten SchemeAgencyIds der aktuellen Datenübersicht.
     * @return Merge-Funktion zum Anwenden der gesammelten Daten auf die Builder.
     */
    default Consumer<Map<String, DataOverviewOutModel.Builder>> collectDataParallel(
        Set<String> schemeAgencyIds
    ) {
        return this::collectData;
    }

    /**
     * Phase 2: Leitet aggregierte Zustände aus den in Phase 1 gesammelten Daten ab.
     * <p>
     * Wird erst aufgerufen, nachdem alle {@link #collectData}-Aufrufe abgeschlossen sind.
     * Builder-Getter dürfen hier bedenkenlos genutzt werden.
     *
     * @param builders Map von SchemeAgencyId → Builder. Darf gelesen und mutiert werden.
     */
    default void deriveState(Map<String, DataOverviewOutModel.Builder> builders) {}
}
