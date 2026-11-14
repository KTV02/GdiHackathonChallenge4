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
import de.dataport.suchdienst.indexer.util.TypeUtil;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_2_0.LoescheObjekt;
import lombok.Getter;

@Getter
public class GenericLoescheObjekt implements GenericXZuFiOperation {

    private final Object data;
    private final IdentificationType klasse;
    private final String identifikatorString;

    public GenericLoescheObjekt(LoescheObjekt loescheObjekt) {
        this.data = loescheObjekt;
        this.klasse = TypeUtil.identificationTypeFromKlasse(
            loescheObjekt.getKlasse(),
            loescheObjekt.getId().getSchemeID()
        );
        this.identifikatorString = IdentifikatorUtil.getIdentificatorString(
            loescheObjekt.getId(),
            klasse
        );
    }

    public GenericLoescheObjekt(de.xoev.xzufi.v2_3_1.LoescheObjekt loescheObjekt) {
        this.data = loescheObjekt;
        this.klasse = TypeUtil.identificationTypeFromCode(
            loescheObjekt.getKlasse().getCode(),
            loescheObjekt.getId().getSchemeID()
        );
        this.identifikatorString = IdentifikatorUtil.getIdentificatorString(
            loescheObjekt.getId(),
            klasse
        );
    }

    public XZuFiVersion getVersion() {
        String packageName = getData().getClass().getPackageName();
        if (packageName.equals(de.xoev.xzufi.v2_2_0.LoescheObjekt.class.getPackageName())) {
            return XZuFiVersion.XZUFI22;
        } else if (packageName.equals(de.xoev.xzufi.v2_3_1.LoescheObjekt.class.getPackageName())) {
            return XZuFiVersion.XZUFI23;
        }
        throw new IllegalStateException("XZUFI Version konnte nicht bestimmt werden");
    }

    public de.xoev.xzufi.v2_2_0.LoescheObjekt getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> (LoescheObjekt) data;
            case XZUFI23 -> XZuFiKonverter.konvertiere((de.xoev.xzufi.v2_3_1.LoescheObjekt) data);
        };
    }

    public de.xoev.xzufi.v2_3_1.LoescheObjekt getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> (de.xoev.xzufi.v2_3_1.LoescheObjekt) data;
            case XZUFI22 -> XZuFiKonverter.konvertiere((LoescheObjekt) data);
        };
    }
}
