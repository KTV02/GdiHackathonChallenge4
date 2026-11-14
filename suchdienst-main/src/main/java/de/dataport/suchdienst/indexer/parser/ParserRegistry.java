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

import java.util.Map;

/**
 * Stellt die Zuordnung von XZuFi-Modulklassen zu den passenden {@link Parser}-Implementierungen bereit.
 * Der {@link ParserSelector} verwendet diese Zuordnung zur Auswahl eines Parsers anhand der Modulklasse.
 */
// Generic Wildcard ist hier notwendig
@SuppressWarnings("java:S1452")
public interface ParserRegistry {
    /**
     * Liefert die registrierten Parser für die jeweiligen XZuFi-Modulklassen.
     *
     * @return Zuordnung der Modulklassen zu ihren Parsern
     */
    Map<Class<?>, Parser<?, ?>> getParsers();
}
