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

import static java.util.concurrent.TimeUnit.HOURS;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.util.model.TransaktionsPosition;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ImportStatistic {

    private record Statistic(String transactionId, Instant startTime, Instant endTime) {}

    private Cache<String, Statistic> statistics = Caffeine.newBuilder()
        .expireAfterWrite(24, HOURS)
        .build();

    /**
     * Protokollieren von Zeitmessungen zum Beginn eines Import-Laufs.
     * Es wird nur ein Log protokolliert, wenn die Transaktion als Start markiert ist.
     * </br></br>
     * <b>Hinweis:</b> Die Methoden von logImportStatisticStart und logImportStatisticStop sollten nicht zusammengelegt werden,
     * da es den NachrichtenTyp {@link TransaktionsPosition#START_STOP} gibt,
     * der sowohl die Charakteristik einer Start als auch einer Stop Nachricht hat.
     * Diese Zusammenlegung führte in der Vergangenheit zu einer unerwarteten Protokollierung
     *
     * @param metaData MetaDaten der Transaktion
     */
    public void logImportStatisticStart(TransactionMetadata metaData) {
        var txnPosition = TransaktionsPosition.findByPosition(metaData.transactionPosition());
        if (!txnPosition.isStart()) {
            return;
        }
        log.info(
            "Import START: Importvorgang mit der TransaktionsId {} wurde gestartet.",
            metaData.transactionId()
        );
        startTimeMeasurement(metaData.transactionId());
    }

    /**
     * Protokollieren von Zeitmessungen zum Ende eines Import-Laufs.
     * Es wird nur ein Log protokolliert, wenn die Transaktion als Start markiert ist.
     * </br></br>
     * <b>Hinweis:</b> Die Methoden von logImportStatisticStart und logImportStatisticStop sollten nicht zusammengelegt werden,
     * da es den NachrichtenTyp {@link TransaktionsPosition#START_STOP} gibt,
     * der sowohl die Charakteristik einer Start als auch einer Stop Nachricht hat.
     * Diese Zusammenlegung führte in der Vergangenheit zu einer unerwarteten Protokollierung
     *
     * @param metaData MetaDaten der Transaktion
     */
    public void logImportStatisticStop(TransactionMetadata metaData) {
        var txnPosition = TransaktionsPosition.findByPosition(metaData.transactionPosition());
        if (!txnPosition.isStop()) {
            return;
        }
        String transactionId = metaData.transactionId();
        stopTimeMeasurement(transactionId);
        log.info(
            "Import ENDE: Importvorgang mit der TransaktionsId {} wurde beendet. Dauer: {}.",
            transactionId,
            getDurationInSeconds(transactionId)
        );
        clearTimeMeasurement(transactionId);
    }

    private void startTimeMeasurement(String transactionId) {
        statistics.put(transactionId, new Statistic(transactionId, Instant.now(), null));
    }

    private void stopTimeMeasurement(String transactionId) {
        Instant endTime = Instant.now();
        statistics
            .asMap()
            .computeIfPresent(transactionId, (k, v) ->
                new Statistic(v.transactionId(), v.startTime(), endTime)
            );
    }

    private void clearTimeMeasurement(String transactionId) {
        statistics.invalidate(transactionId);
    }

    private Duration getDuration(String transactionId) {
        Statistic statistic = statistics.getIfPresent(transactionId);
        if (statistic != null && statistic.startTime() != null && statistic.endTime() != null) {
            return Duration.between(statistic.startTime(), statistic.endTime());
        }
        return Duration.ZERO;
    }

    private String getDurationInSeconds(String transactionId) {
        DecimalFormat decimalFormat = new DecimalFormat(
            "###.###",
            new DecimalFormatSymbols(Locale.US)
        );
        float seconds = (float) getDuration(transactionId).toMillis() / 1000;
        return decimalFormat.format(seconds) + "s";
    }
}
