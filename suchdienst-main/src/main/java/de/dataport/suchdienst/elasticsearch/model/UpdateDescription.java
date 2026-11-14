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
package de.dataport.suchdienst.elasticsearch.model;

import java.time.Instant;
import lombok.Data;

@Data
public class UpdateDescription {

    private int version;
    private UpdateStatus status;
    private String description;
    private Instant timestamp;
    private String indexName;
}
