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
package de.dataport.suchdienst.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gemeinsam genutzte Datums-Methoden.
 */
public class DateUtils {

    private static final DateTimeFormatter FMT_DATE_TIME = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss"
    );

    private static final DateTimeFormatter FMT_DATE_TIME_WITH_MILLIS = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS"
    );

    private DateUtils() {
        // privater Konstruktor, da es eine Utility Klasse ist.
    }

    /**
     * Formatiert Instant mit Ausgabe von Datum und Uhrzeit.
     */
    public static String formatAsDateAndTime(Instant instant) {
        return formatAsDateAndTime(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Formatiert ZonedDateTime mit Ausgabe von Datum und Uhrzeit.
     */
    public static String formatAsDateAndTime(ZonedDateTime zonedDateTime) {
        return FMT_DATE_TIME.format(zonedDateTime);
    }

    /**
     * Formatiert Instant mit Ausgabe von Datum und Uhrzeit inklusive Millisekunden.
     */
    public static String formatAsDateAndTimeWithMillis(Instant instant) {
        return FMT_DATE_TIME_WITH_MILLIS.format(instant.atZone(ZoneId.systemDefault()));
    }
}
