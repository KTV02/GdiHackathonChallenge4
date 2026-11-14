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

import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public final class OrganisationseinheitEntity22 extends OrganisationseinheitEntity {

    private OrganisationseinheitErweitert jzufi;

    public OrganisationseinheitEntity22(Instant lastUpdate) {
        super(lastUpdate);
    }
}
