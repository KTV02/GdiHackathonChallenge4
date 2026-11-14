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
package de.dataport.suchdienst.importer.model;

import de.dataport.pv_onlinegw.xzufi_konverter.XZuFiKonverter;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import lombok.Getter;

@Getter
public class GenericOrganisationseinheit
    extends GenericXZuFiObjekt<
        de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert,
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert
    >
{

    private final String id;

    public GenericOrganisationseinheit(
        de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert organisationseinheit
    ) {
        super(organisationseinheit, null);
        this.id = XZuFiUtil.getIdentificatorString(
            organisationseinheit.getId(),
            IdentificationType.OE
        );
    }

    public GenericOrganisationseinheit(
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert organisationseinheit
    ) {
        super(null, organisationseinheit);
        this.id = de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
            organisationseinheit.getId(),
            IdentificationType.OE
        );
    }

    public de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> (de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert) XZuFiKonverter.konvertiere(
                super.getXZuFi23()
            );
        };
    }

    public de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> (de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert) XZuFiKonverter.konvertiere(
                super.getXZuFi22()
            );
        };
    }
}
