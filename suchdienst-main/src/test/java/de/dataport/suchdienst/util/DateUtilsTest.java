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
package de.dataport.suchdienst.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void formatAsDateAndTime_liefert_datum_und_uhrzeit_ohne_millisekunden() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2026,
            8,
            6,
            17,
            17,
            40,
            789_000_000,
            ZoneId.systemDefault()
        );

        String formatted = DateUtils.formatAsDateAndTime(zonedDateTime.toInstant());

        assertThat(formatted).isEqualTo("2026-08-06 17:17:40");
    }

    @Test
    void formatAsDateAndTimeWithMillis_liefert_datum_und_uhrzeit_inklusive_millisekunden() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2026,
            8,
            6,
            17,
            17,
            40,
            789_000_000,
            ZoneId.systemDefault()
        );

        String formatted = DateUtils.formatAsDateAndTimeWithMillis(zonedDateTime.toInstant());

        assertThat(formatted).isEqualTo("2026-08-06 17:17:40.789");
    }
}
