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
package de.dataport.suchdienst.indexer.service.odrelationimport;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Hält den Fortschritt eines OD-Relation-Imports und erzeugt logbare Snapshots.
 */
final class OdRelationImportProgress {

    private final Instant startedAt;
    private final long totalDocs;
    private int totalImported;
    private int batchCount;

    OdRelationImportProgress(Instant startedAt, long totalDocs) {
        this.startedAt = startedAt;
        this.totalDocs = totalDocs;
    }

    /**
     * Fortschritt nach einem abgeschlossenen Batch.
     */
    BatchSnapshot recordBatch(
        OdRelationImportService.ImportPageResult result,
        Instant batchStartedAt,
        Instant batchCompletedAt
    ) {
        batchCount++;
        totalImported += result.importedCount();
        return new BatchSnapshot(
            batchCount,
            result.importedCount(),
            totalImported,
            totalDocs,
            result.lastId(),
            Duration.between(batchStartedAt, batchCompletedAt),
            Duration.between(startedAt, batchCompletedAt)
        );
    }

    /**
     * Abschlusssnapshot nach Ende des Imports.
     */
    CompletionSnapshot finish(Instant completedAt) {
        return new CompletionSnapshot(
            totalImported,
            batchCount,
            Duration.between(startedAt, completedAt)
        );
    }

    /**
     * Formatiert den mittleren Durchsatz pro Sekunde für Logging-Ausgaben.
     */
    static String formatThroughput(int count, Duration duration) {
        long durationMillis = duration.toMillis();
        if (count <= 0 || durationMillis <= 0) {
            return "0.0";
        }
        double throughputPerSecond = count / (durationMillis / 1_000.0);
        return String.format(Locale.ROOT, "%.1f", throughputPerSecond);
    }

    /**
     * Formatiert eine Dauer menschenlesbar für Logging-Ausgaben.
     */
    static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        }
        return String.format("%ds", seconds);
    }

    record BatchSnapshot(
        int batchNumber,
        int importedCount,
        int totalImported,
        long totalDocs,
        String lastId,
        Duration batchDuration,
        Duration totalDuration
    ) {}

    record CompletionSnapshot(int totalImported, int batchCount, Duration totalDuration) {
        boolean isEmpty() {
            return totalImported == 0;
        }
    }
}
