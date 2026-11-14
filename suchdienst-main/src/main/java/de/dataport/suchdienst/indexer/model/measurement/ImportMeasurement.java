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
package de.dataport.suchdienst.indexer.model.measurement;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class ImportMeasurement {

    @ToString.Include(rank = 2)
    private MeasurementType type;

    private Instant startTime;
    private Instant endTime;

    @Builder.Default
    private boolean success = true;

    private int objectCount;

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }

        return Duration.ZERO;
    }

    public long getDurationInMillis() {
        return getDuration().toMillis();
    }

    @ToString.Include(name = "duration", rank = 1)
    public String getDurationInSeconds() {
        DecimalFormat decimalFormat = new DecimalFormat(
            "###.###",
            new DecimalFormatSymbols(Locale.US)
        );
        float seconds = (float) getDuration().toMillis() / 1000;
        return decimalFormat.format(seconds) + "s";
    }
}
