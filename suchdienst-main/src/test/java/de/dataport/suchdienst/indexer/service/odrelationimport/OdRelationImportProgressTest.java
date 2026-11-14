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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OdRelationImportProgressTest {

    @Test
    void recordBatchTracksCumulativeState() {
        Instant importStart = Instant.parse("2026-03-07T10:15:30Z");
        OdRelationImportProgress progress = new OdRelationImportProgress(importStart, 42);

        OdRelationImportProgress.BatchSnapshot firstBatch = progress.recordBatch(
            new OdRelationImportService.ImportPageResult("od-2", 2),
            importStart.plusSeconds(5),
            importStart.plusSeconds(8)
        );
        OdRelationImportProgress.BatchSnapshot secondBatch = progress.recordBatch(
            new OdRelationImportService.ImportPageResult("od-3", 1),
            importStart.plusSeconds(10),
            importStart.plusSeconds(14)
        );

        assertThat(firstBatch.batchNumber()).isEqualTo(1);
        assertThat(firstBatch.totalImported()).isEqualTo(2);
        assertThat(firstBatch.totalDocs()).isEqualTo(42);
        assertThat(firstBatch.batchDuration()).isEqualTo(Duration.ofSeconds(3));
        assertThat(secondBatch.batchNumber()).isEqualTo(2);
        assertThat(secondBatch.totalImported()).isEqualTo(3);
        assertThat(secondBatch.totalDuration()).isEqualTo(Duration.ofSeconds(14));
    }

    @Test
    void finishReportsEmptyImport() {
        Instant importStart = Instant.parse("2026-03-07T10:15:30Z");
        OdRelationImportProgress progress = new OdRelationImportProgress(importStart, 0);

        OdRelationImportProgress.CompletionSnapshot completion = progress.finish(
            importStart.plusSeconds(61)
        );

        assertThat(completion.isEmpty()).isTrue();
        assertThat(completion.batchCount()).isZero();
        assertThat(completion.totalDuration()).isEqualTo(Duration.ofSeconds(61));
    }

    @Test
    void formatsDurationAndThroughputForLogging() {
        assertThat(OdRelationImportProgress.formatThroughput(5, Duration.ofSeconds(2))).isEqualTo(
            "2.5"
        );
        assertThat(OdRelationImportProgress.formatThroughput(0, Duration.ofSeconds(2))).isEqualTo(
            "0.0"
        );
        assertThat(OdRelationImportProgress.formatDuration(Duration.ofSeconds(59))).isEqualTo(
            "59s"
        );
        assertThat(OdRelationImportProgress.formatDuration(Duration.ofSeconds(61))).isEqualTo(
            "1m 1s"
        );
        assertThat(OdRelationImportProgress.formatDuration(Duration.ofSeconds(3661))).isEqualTo(
            "1h 1m 1s"
        );
    }
}
