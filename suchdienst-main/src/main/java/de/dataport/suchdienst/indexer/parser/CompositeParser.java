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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Erweiterung des {@link Parser}-Interfaces für Parser, die aus einem einzelnen XZuFi-Modul
 * mehrere {@link AbstractServiceModule}-Ergebnisse erzeugen.
 * <p>
 * Die reguläre {@link Parser#parse}-Methode liefert weiterhin das primäre Ergebnis.
 * Über {@link #parseComposite} werden alle erzeugten Module als Code-Modul-Paare zurückgegeben.
 */
@SuppressWarnings("java:S119")
public interface CompositeParser<
    XzufiModule,
    IndexerModule extends AbstractServiceModule
> extends Parser<XzufiModule, IndexerModule> {
    /**
     * Parst ein XZuFi-Modul und gibt eine Liste von Code-Modul-Paaren zurück.
     * Im Gegensatz zu {@link Parser#parse} übernimmt diese Methode sowohl das Parsing
     * als auch die Code-Zuordnung der erzeugten Module.
     * Wird vom {@link ParserSelector} aufgerufen, wenn der Parser mehrere Module erzeugt.
     */
    List<Map.Entry<ServiceModuleCode, AbstractServiceModule>> parseComposite(
        XzufiModule modul,
        Locale locale
    );
}
