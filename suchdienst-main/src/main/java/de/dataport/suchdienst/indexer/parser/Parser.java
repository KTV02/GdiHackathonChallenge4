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
package de.dataport.suchdienst.indexer.parser;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Locale;
import java.util.Set;

/**
 * Wandelt ein XZuFi-Modul in ein Modul für die Indexierung um und stellt dessen Sprach- und Codeinformationen bereit.
 *
 * @param <XzufiModule> Typ des eingehenden XZuFi-Moduls
 * @param <IndexerModule> Typ des erzeugten Indexer-Moduls
 */
@SuppressWarnings("java:S119")
public interface Parser<XzufiModule, IndexerModule extends AbstractServiceModule> {
    /**
     * Parst ein XZuFi-Modul für die angegebene Sprache.
     *
     * @param modul zu parsende Quelldaten
     * @param locale Sprache des gewünschten Ergebnisses
     * @return das erzeugte Indexer-Modul oder {@code null}, falls das Modul nicht übernommen wird
     */
    IndexerModule parse(XzufiModule modul, Locale locale);

    /**
     * Ermittelt die im XZuFi-Modul enthaltenen Sprachen.
     *
     * @param module zu untersuchendes XZuFi-Modul
     * @return Sprachkennungen als Sprach-Tags, beispielsweise {@code de-DE}
     */
    Set<String> getIncludedLanguages(XzufiModule module);

    /**
     * Gibt den festen Code des geparsten Modultyps zurück.
     *
     * @return Modulcode oder {@code null}, wenn der Code aus dem XZuFi-Modul ermittelt werden muss
     */
    ServiceModuleCode getCode();
}
