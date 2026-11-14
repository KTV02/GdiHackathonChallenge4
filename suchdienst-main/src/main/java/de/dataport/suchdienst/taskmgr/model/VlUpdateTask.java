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
 * Ein Task, um in einem Verwaltungsleistungen-Index die Verwaltungsleistungen für eine LB neu zu berechnen.
 * Der Task beschreibt nur diesen einen Index. Falls es sich um eine LigV handelt, wird ein Task pro
 * Index generiert.
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class VlUpdateTask extends SchemeAgencyIdTask<Void> {

    // Der zu diesem Gebiet gehörende Index wird beschrieben.
    private final String targetArsFederalState;

    // Zu dieser LB werden die VLs berechnet
    private final String lbId;

    public VlUpdateTask(
        String schemeAgencyId,
        String producer,
        String targetArsFederalState,
        String lbId
    ) {
        super(SudTaskType.VL_UPDATE, schemeAgencyId, producer);
        this.lbId = lbId;
        this.targetArsFederalState = targetArsFederalState;
    }

    @Override
    public Class<Void> getFutureType() {
        return Void.class;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass().getSimpleName())
            .add("schemeAgencyId", schemeAgencyId)
            .add("lbId", lbId)
            .add("targetArsFederalState", targetArsFederalState)
            .toString();
    }
}
