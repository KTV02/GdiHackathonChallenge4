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
import de.xoev.xzufi.v2_2_0.Code;
import de.xoev.xzufi.v2_3_1.Identifikator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class GenericLeistung
    extends GenericXZuFiObjekt<de.xoev.xzufi.v2_2_0.Leistung, de.xoev.xzufi.v2_3_1.Leistung>
{

    private final String id;
    private final Set<String> arsFederalState;
    private final Set<String> leikaId;

    public GenericLeistung(de.xoev.xzufi.v2_2_0.Leistung leistung) {
        super(leistung, null);
        this.id = XZuFiUtil.getIdentificatorString(leistung.getId(), IdentificationType.LB);
        this.arsFederalState = extractArsFederalState();
        this.leikaId = extractLeikaId();
    }

    public GenericLeistung(de.xoev.xzufi.v2_3_1.Leistung leistung) {
        super(null, leistung);
        this.id = de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
            leistung.getId(),
            IdentificationType.LB
        );
        this.arsFederalState = extractArsFederalState();
        this.leikaId = extractLeikaId();
    }

    public de.xoev.xzufi.v2_2_0.Leistung getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> XZuFiKonverter.konvertiere(super.getXZuFi23());
        };
    }

    public de.xoev.xzufi.v2_3_1.Leistung getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> XZuFiKonverter.konvertiere(super.getXZuFi22());
        };
    }

    private Set<String> extractArsFederalState() {
        return switch (this.getVersion()) {
            case XZUFI22 -> XZuFiUtil.getArsFederalState(getXZuFi22().getGueltigkeitGebietID());
            case XZUFI23 -> de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getArsFederalState(
                getXZuFi23().getGueltigkeitGebietID()
            );
        };
    }

    private Set<String> extractLeikaId() {
        return switch (this.getVersion()) {
            case XZUFI22 -> getXZuFi22().getReferenzLeiKa() != null
                ? getXZuFi22()
                      .getReferenzLeiKa()
                      .stream()
                      .map(Code::getCode)
                      .collect(Collectors.toSet())
                : Set.of();
            case XZUFI23 -> getXZuFi23().getReferenzLeistungsschluessel() != null
                ? getXZuFi23()
                      .getReferenzLeistungsschluessel()
                      .stream()
                      .map(Identifikator::getValue)
                      .collect(Collectors.toSet())
                : Set.of();
        };
    }
}
