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
import de.dataport.suchdienst.util.Bundeslaender;
import lombok.EqualsAndHashCode;

/**
 * Ein Task, um für ein Bundesland einen neuen Verwaltungsleistungen-Index anzulegen und zu füllen.
 */
@EqualsAndHashCode(callSuper = true)
public class VlRecalcTask extends BundeslandTask<Void> {

    public VlRecalcTask(Bundeslaender bundesland) {
        super(SudTaskType.VL_RECALC, bundesland);
    }

    @Override
    public Class<Void> getFutureType() {
        return Void.class;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass().getSimpleName())
            .add("bundesland", bundesland)
            .toString();
    }
}
