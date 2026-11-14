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

/**
 * Enthält die Informationen, die gemeinsam den Identifikator für einen Codelisten-Eintrag bilden.
 */
public record CodelistKey(
    // Die URI, wie sie in XRepository hinterlegt ist, Aufbau ist typischerweise {@literal urn:...}.
    String uri,

    // Version der Codeliste, kann beliebige(?) alphanumerische Zeichenketten enthalten.
    String version,

    //  Der eigentliche Code, also der Wert der identifizierten Schluessel-Spalte (ueblicherweise in XML
    //  mit {@literal Key Id="codeKey"} ausgezeichnet).
    String code
) {}
