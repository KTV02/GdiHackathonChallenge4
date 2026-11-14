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
package de.dataport.suchdienst.backoffice.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Tolerate;
import org.apache.commons.lang3.StringUtils;

/**
 * Enthält die Daten, die im Backoffice in der Suchdienst "Datenübersicht" in der Tabelle angezeigt werden.
 * Immutables Record — Aufbau über {@link lombok.Builder}.
 * <p>
 * Lombok generiert alle Builder-Setter und {@code build()}. Nur die wenigen Getter,
 * die von {@code deriveState}-Contributors benötigt werden, sind manuell definiert.
 */
@lombok.Builder(builderClassName = "Builder")
public record DataOverviewOutModel(
    @Schema(description = "Der ArsFederalState '00'=Bund, '01'=Schleswig-Holstein etc")
    String fedState,

    @Schema(
        description = "Der Name des liefernden Redaktionssystems oder des Bundeslandes wenn für das Bundesland kein Redaktionssystem angelegt wurde"
    )
    String name,

    @Schema(description = "SchemeAgencyId des liefernden Redaktionssystems") String schemeAgencyId,

    @Schema(description = "Verarbeitungszustand des Bundeslands")
    DataOverviewOutModelProcessState processState,

    @Schema(description = "Anzahl de Xzufi-LBs zum Bundesland") long lbCount,

    @Schema(description = "Anzahl der Xzufi-OEs") long oeCount,

    @Schema(description = "Anzahl der Xzufi-ODs") long odCount,

    @Schema(description = "Anzahl der Xzufi-ZS") long zsCount,

    @Schema(description = "Anzahl der Xzufi-SPs") long spCount,

    @Schema(description = "Anzahl der Verwaltungsleistungen zum Bundesland") long vlCount,

    @Schema(description = "Anzahl der Onlinedienst-Relationen zum Bundesland") long odRelCount,

    @Schema(description = "Anzahl der Elemente in der ImportQueue zum Bundesland")
    long importQueueCount,

    @Schema(description = "Anzahl der ImportTasks zum Bundesland") long importTaskCount,

    @Schema(description = "Anzahl der VlUpdateTasks zum Bundesland") long vlUpdateTaskCount,

    @Schema(description = "Anzahl der VlRecalcTasks zum Bundesland") long vlRecalcTaskCount,

    @Schema(description = "Anzahl der OnlineServiceRelationsRecalcTask zum Bundesland")
    long odRelRecalcTaskCount
) {
    /** Compact Constructor: normalisiert null-Strings zu leeren Strings. */
    public DataOverviewOutModel {
        fedState = fedState != null ? fedState : "";
        name = name != null ? name : "";
        schemeAgencyId = schemeAgencyId != null ? schemeAgencyId : "";
    }

    public boolean hasFedState() {
        return !StringUtils.isEmpty(fedState);
    }

    /**
     * Partielle Builder-Klasse — Lombok generiert Felder, Setter und {@code build()}.
     * <p>
     * Hier sind nur die Getter definiert, die {@code deriveState}-Contributors benötigen,
     * um in Phase 2 auf Werte aus Phase 1 zuzugreifen.
     */
    public static class Builder {

        // --- Getter (für Contributors, die Werte anderer Contributors lesen müssen) ---
        // @Tolerate sorgt dafür, dass Lombok trotz gleichnamiger Getter die Setter generiert.

        @Tolerate
        public String schemeAgencyId() {
            return schemeAgencyId;
        }

        @Tolerate
        public long importQueueCount() {
            return importQueueCount;
        }

        @Tolerate
        public long importTaskCount() {
            return importTaskCount;
        }

        @Tolerate
        public long vlUpdateTaskCount() {
            return vlUpdateTaskCount;
        }

        @Tolerate
        public long vlRecalcTaskCount() {
            return vlRecalcTaskCount;
        }

        @Tolerate
        public long odRelRecalcTaskCount() {
            return odRelRecalcTaskCount;
        }
    }
}
