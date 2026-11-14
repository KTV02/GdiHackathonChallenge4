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
package de.dataport.suchdienst.importer.model.jzufi;

import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public final class OnlinedienstEntity23 extends OnlinedienstEntity {

    private OnlinedienstErweitert jzufi;

    public OnlinedienstEntity23(Instant lastUpdate) {
        super(lastUpdate);
    }
}
