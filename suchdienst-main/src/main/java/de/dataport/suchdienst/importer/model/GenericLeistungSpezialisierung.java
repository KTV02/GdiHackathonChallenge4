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
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import java.util.Set;
import lombok.Getter;

@Getter
@SuppressWarnings("java:S1301") // If statements würden die Lesbarkeit in diesem Fall nicht verbessern
public class GenericLeistungSpezialisierung
    extends GenericXZuFiObjekt<
        de.xoev.xzufi.v2_2_0.LeistungSpezialisierung,
        de.xoev.xzufi.v2_3_1.LeistungSpezialisierung
    >
{

    private final String id;
    private final String lbId;
    private final Set<String> arsFederalState;

    public GenericLeistungSpezialisierung(
        de.xoev.xzufi.v2_2_0.LeistungSpezialisierung leistungSpezialisierung
    ) {
        super(leistungSpezialisierung, null);
        this.id = extractIdentifikatorString();
        this.lbId = extractLeistungIdentifikatorString();
        this.arsFederalState = extractArsFederalState();
    }

    public GenericLeistungSpezialisierung(
        de.xoev.xzufi.v2_3_1.LeistungSpezialisierung leistungSpezialisierung
    ) {
        super(null, leistungSpezialisierung);
        this.id = extractIdentifikatorString();
        this.lbId = extractLeistungIdentifikatorString();
        this.arsFederalState = extractArsFederalState();
    }

    public de.xoev.xzufi.v2_2_0.LeistungSpezialisierung getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> XZuFiKonverter.konvertiere(super.getXZuFi23());
        };
    }

    public de.xoev.xzufi.v2_3_1.LeistungSpezialisierung getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> XZuFiKonverter.konvertiere(super.getXZuFi22());
        };
    }

    private String extractIdentifikatorString() {
        return switch (getVersion()) {
            case XZUFI22 -> {
                LeistungSpezialisierung bo = getXZuFi22();

                Identifikator spId;
                if (
                    bo.getId() == null ||
                    bo.getId().getValue() == null ||
                    bo.getId().getValue().isEmpty()
                ) {
                    spId = bo.getIdGebiet().getFirst();
                } else {
                    spId = bo.getId();
                }
                yield XZuFiUtil.getIdentificatorString(spId, IdentificationType.SP);
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.LeistungSpezialisierung bo = getXZuFi23();

                de.xoev.xzufi.v2_3_1.Identifikator spId;
                if (
                    bo.getId() == null ||
                    bo.getId().getValue() == null ||
                    bo.getId().getValue().isEmpty()
                ) {
                    spId = bo.getGebietID().getFirst();
                } else {
                    spId = bo.getId();
                }
                yield de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
                    spId,
                    IdentificationType.SP
                );
            }
        };
    }

    private String extractLeistungIdentifikatorString() {
        String identifikatorString = "";

        switch (getVersion()) {
            case XZUFI22 -> {
                LeistungSpezialisierung bo = getXZuFi22();
                identifikatorString = XZuFiUtil.getIdentificatorString(
                    bo.getIdLeistung(),
                    IdentificationType.LB
                );
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.LeistungSpezialisierung bo = getXZuFi23();
                identifikatorString =
                    de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
                        bo.getLeistungID(),
                        IdentificationType.LB
                    );
            }
        }

        return identifikatorString;
    }

    private Set<String> extractArsFederalState() {
        return switch (getVersion()) {
            case XZUFI22 -> XZuFiUtil.getArsFederalState(getXZuFi22().getIdGebiet());
            case XZUFI23 -> de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getArsFederalState(
                getXZuFi23().getGebietID()
            );
        };
    }
}
