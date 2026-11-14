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
public class GenericFormular
    extends GenericXZuFiObjekt<de.xoev.xzufi.v2_2_0.Formular, de.xoev.xzufi.v2_3_1.Formular>
{

    private final String id;

    public GenericFormular(de.xoev.xzufi.v2_2_0.Formular formular) {
        super(formular, null);
        this.id = XZuFiUtil.getIdentificatorString(formular.getId(), IdentificationType.FO);
    }

    public GenericFormular(de.xoev.xzufi.v2_3_1.Formular formular) {
        super(null, formular);
        this.id = de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
            formular.getId(),
            IdentificationType.FO
        );
    }

    public de.xoev.xzufi.v2_2_0.Formular getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> XZuFiKonverter.konvertiere(super.getXZuFi23());
        };
    }

    public de.xoev.xzufi.v2_3_1.Formular getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> XZuFiKonverter.konvertiere(super.getXZuFi22());
        };
    }
}
