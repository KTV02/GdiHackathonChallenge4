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
package de.dataport.suchdienst.taskmgr.model;

import com.google.common.base.MoreObjects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Ein Task, um ein Element aus der Nachrichten-Queue zu importieren.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ImportTask extends SchemeAgencyIdTask<Void> {

    // Nachrichten-UUID vom SaD
    private final String requestId;

    public ImportTask(String schemeAgencyId, String producer, String requestId) {
        super(SudTaskType.IMPORT, schemeAgencyId, producer);
        this.requestId = requestId;
    }

    @Override
    public Class<Void> getFutureType() {
        return Void.class;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass().getSimpleName())
            .add("requestId", requestId)
            .add("schemeAgencyId", schemeAgencyId)
            .toString();
    }
}
