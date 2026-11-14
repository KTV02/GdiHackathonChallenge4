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
package de.dataport.suchdienst.taskmgr.model;

import static de.dataport.suchdienst.taskmgr.model.SudTaskType.OD_RE_RECALC;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class OdRelRecalcTask extends SchemeAgencyIdTask<Void> {

    public OdRelRecalcTask(String schemeAgencyId, String producer) {
        super(OD_RE_RECALC, schemeAgencyId, producer);
    }

    @Override
    public Class<Void> getFutureType() {
        return Void.class;
    }
}
