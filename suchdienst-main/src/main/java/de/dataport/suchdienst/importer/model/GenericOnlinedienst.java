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
public class GenericOnlinedienst
    extends GenericXZuFiObjekt<
        de.xoev.xzufi.v2_2_0.OnlinedienstErweitert,
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert
    >
{

    private final String id;

    public GenericOnlinedienst(de.xoev.xzufi.v2_2_0.OnlinedienstErweitert onlinedienst) {
        super(onlinedienst, null);
        this.id = XZuFiUtil.getIdentificatorString(onlinedienst.getId(), IdentificationType.OD);
    }

    public GenericOnlinedienst(de.xoev.xzufi.v2_3_1.OnlinedienstErweitert onlinedienst) {
        super(null, onlinedienst);
        this.id = de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
            onlinedienst.getId(),
            IdentificationType.OD
        );
    }

    public de.xoev.xzufi.v2_2_0.OnlinedienstErweitert getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> XZuFiKonverter.konvertiere(super.getXZuFi23());
        };
    }

    public de.xoev.xzufi.v2_3_1.OnlinedienstErweitert getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> XZuFiKonverter.konvertiere(super.getXZuFi22());
        };
    }

    public boolean isOeffentlichAnzeigen() {
        return switch (getVersion()) {
            case XZUFI22 -> true;
            case XZUFI23 -> super.getXZuFi23().isKennzeichenOeffentlichAnzeigen();
        };
    }
}
