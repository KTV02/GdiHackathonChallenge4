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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Data;

/**
 * Repräsentiert einen einzelnen Eintrag (also einen konkreten Code) einer XoeV-Codeliste (von
 * <a href="https://xrepository.de/">https://xrepository.de/</a>).
 * <p>
 * Im XRepository werden Codelisten anhand ihrer URI und Versionsnummer identifiziert. Wir gehen
 * davon aus, dass alle (für PVOG relevanten) Codelisten eine eindeutige Schlüssel-Spalte ("Code")
 * enthalten, mit dem dann jeder Eintrag identifiziert werden kann.
 */
@Data
public class CodelistEntry {

    /**
     * interne ID, entspricht der Konkatenation von {@link #uri} + '_' + {@link #version} + '_' + {@link #code}
     */
    private final String id;

    /**
     * die URI, wie sie in XRepository hinterlegt ist, Aufbau ist typischerweise {@literal urn:...}
     */
    private final String uri;
    /**
     * Version der Codeliste kann beliebige(?) alphanumerische Zeichenketten enthalten
     */
    private final String version;
    /**
     * der eigentliche Code, also der Wert der identifizierten Schlüssel-Spalte (unüblicherweise in XML
     * mit {@literal Key Id="codeKey"} ausgezeichnet).
     */
    private final String code;
    /**
     * Enthält die (sprach-abhängigen) Klartexte. Der deutsche Klartext steht im XML in der Spalte,
     * die unüblicherweise mit {@literal <Key Id="codenameKey">} ausgezeichnet ist.
     */
    private final Map<Locale, String> texts = new HashMap<>();

    /**
     * PV-9431: Enthält die (sprach-abhängigen) Kommentare.
     */
    private final Map<Locale, String> comments = new HashMap<>();

    private final List<CodeListValue> values = new ArrayList<>();

    private final CodelistKey key;
}
