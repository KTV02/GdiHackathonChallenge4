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
package de.dataport.suchdienst.clienttest.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.client.model.Jzufi23Versionsinformation;
import de.dataport.suchdienst.client.model.Jzufi23Zeitraum;
import de.dataport.suchdienst.client.model.Jzufi23Zeitserie;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testziel: XML-Datum und reine Uhrzeit bleiben Strings mit ihrem Offset und ihrer
 * Praezision. Zeitstempel binden als OffsetDateTime ohne Aenderung des Zeitpunkts.
 */
class JzufiTemporalBindingTest {

    @ParameterizedTest(name = "XML-Datum: {0}")
    @ValueSource(strings = { "1699-12-31+01:00", "2026-01-01+14:00", "2026-03-29" })
    void bindsXmlDatesAsStringsWithOptionalTimezone(String date) {
        var period = StrictApiClientFactory.mapper().readValue("{\"beginn\":\"" + date + "\"}", Jzufi23Zeitraum.class);
        assertEquals(date, period.getBeginn());
    }

    @ParameterizedTest(name = "XML-Uhrzeit: {0}")
    @ValueSource(strings = { "08:30:00", "00:15:30.123456+14:00", "23:59:59.987654-03:30" })
    void bindsXmlTimesAsStringsWithoutInventingADate(String time) {
        var series = StrictApiClientFactory.mapper().readValue("{\"beginn\":\"" + time + "\"}", Jzufi23Zeitserie.class);
        assertEquals(time, series.getBeginn());
    }

    @Test
    void bindsXmlTimestampsWithoutChangingTheInstant() {
        String value = "2026-01-01T00:15:30.123+14:00";
        var timestamp = StrictApiClientFactory.mapper()
            .readValue("{\"erstelltDatumZeit\":\"" + value + "\"}", Jzufi23Versionsinformation.class)
            .getErstelltDatumZeit();
        assertEquals(OffsetDateTime.parse(value).toInstant(), timestamp.toInstant());
    }
}
