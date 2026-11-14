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
package de.dataport.suchdienst.elasticsearch.api;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public enum AliasNames {
    VERWALTUNGSLEISTUNGEN_SH("alias_suchdienst_verwaltungsleistungen_sh"), // Schleswig-Holstein
    VERWALTUNGSLEISTUNGEN_HH("alias_suchdienst_verwaltungsleistungen_hh"), // Hamburg
    VERWALTUNGSLEISTUNGEN_NI("alias_suchdienst_verwaltungsleistungen_ni"), // Niedersachsen
    VERWALTUNGSLEISTUNGEN_HB("alias_suchdienst_verwaltungsleistungen_hb"), // Bremen
    VERWALTUNGSLEISTUNGEN_NW("alias_suchdienst_verwaltungsleistungen_nw"), // Nordrhein-Westfalen
    VERWALTUNGSLEISTUNGEN_HE("alias_suchdienst_verwaltungsleistungen_he"), // Hessen
    VERWALTUNGSLEISTUNGEN_RP("alias_suchdienst_verwaltungsleistungen_rp"), // Rheinland-Pfalz
    VERWALTUNGSLEISTUNGEN_BW("alias_suchdienst_verwaltungsleistungen_bw"), // Baden-Württemberg
    VERWALTUNGSLEISTUNGEN_BY("alias_suchdienst_verwaltungsleistungen_by"), // Bayern
    VERWALTUNGSLEISTUNGEN_SL("alias_suchdienst_verwaltungsleistungen_sl"), // Saarland
    VERWALTUNGSLEISTUNGEN_BE("alias_suchdienst_verwaltungsleistungen_be"), // Berlin
    VERWALTUNGSLEISTUNGEN_BB("alias_suchdienst_verwaltungsleistungen_bb"), // Brandenburg
    VERWALTUNGSLEISTUNGEN_MV("alias_suchdienst_verwaltungsleistungen_mv"), // Mecklenburg-Vorpommern
    VERWALTUNGSLEISTUNGEN_SN("alias_suchdienst_verwaltungsleistungen_sn"), // Sachsen
    VERWALTUNGSLEISTUNGEN_ST("alias_suchdienst_verwaltungsleistungen_st"), // Sachsen-Anhalt
    VERWALTUNGSLEISTUNGEN_TH("alias_suchdienst_verwaltungsleistungen_th"), // Thüringen

    ONLINEDIENSTE_RELATIONEN("alias_suchdienst_onlinedienste_relationen");

    public static final Map<String, AliasNames> ARS_MAPPING = ImmutableMap.<
        String,
        AliasNames
    >builder()
        .put("01", VERWALTUNGSLEISTUNGEN_SH)
        .put("02", VERWALTUNGSLEISTUNGEN_HH)
        .put("03", VERWALTUNGSLEISTUNGEN_NI)
        .put("04", VERWALTUNGSLEISTUNGEN_HB)
        .put("05", VERWALTUNGSLEISTUNGEN_NW)
        .put("06", VERWALTUNGSLEISTUNGEN_HE)
        .put("07", VERWALTUNGSLEISTUNGEN_RP)
        .put("08", VERWALTUNGSLEISTUNGEN_BW)
        .put("09", VERWALTUNGSLEISTUNGEN_BY)
        .put("10", VERWALTUNGSLEISTUNGEN_SL)
        .put("11", VERWALTUNGSLEISTUNGEN_BE)
        .put("12", VERWALTUNGSLEISTUNGEN_BB)
        .put("13", VERWALTUNGSLEISTUNGEN_MV)
        .put("14", VERWALTUNGSLEISTUNGEN_SN)
        .put("15", VERWALTUNGSLEISTUNGEN_ST)
        .put("16", VERWALTUNGSLEISTUNGEN_TH)
        .build();

    public static final List<AliasNames> VL_ALIASES = List.copyOf(ARS_MAPPING.values());

    public static AliasNames getAliasNameByArs(String ars) {
        // Wenn ein ungültiger Ars genommen wird, dann wollen wir in Bremen suchen, weil es ein kleines Bundesland ist
        return ARS_MAPPING.get(ars) == null ? VERWALTUNGSLEISTUNGEN_HB : ARS_MAPPING.get(ars);
    }

    private final String name;

    AliasNames(String name) {
        this.name = name;
    }

    public static IndexNames getIndexName(AliasNames aliasName) {
        return switch (aliasName) {
            case AliasNames.VERWALTUNGSLEISTUNGEN_SH -> IndexNames.VERWALTUNGSLEISTUNGEN_SH;
            case AliasNames.VERWALTUNGSLEISTUNGEN_HH -> IndexNames.VERWALTUNGSLEISTUNGEN_HH;
            case AliasNames.VERWALTUNGSLEISTUNGEN_NI -> IndexNames.VERWALTUNGSLEISTUNGEN_NI;
            case AliasNames.VERWALTUNGSLEISTUNGEN_HB -> IndexNames.VERWALTUNGSLEISTUNGEN_HB;
            case AliasNames.VERWALTUNGSLEISTUNGEN_NW -> IndexNames.VERWALTUNGSLEISTUNGEN_NW;
            case AliasNames.VERWALTUNGSLEISTUNGEN_HE -> IndexNames.VERWALTUNGSLEISTUNGEN_HE;
            case AliasNames.VERWALTUNGSLEISTUNGEN_RP -> IndexNames.VERWALTUNGSLEISTUNGEN_RP;
            case AliasNames.VERWALTUNGSLEISTUNGEN_BW -> IndexNames.VERWALTUNGSLEISTUNGEN_BW;
            case AliasNames.VERWALTUNGSLEISTUNGEN_BY -> IndexNames.VERWALTUNGSLEISTUNGEN_BY;
            case AliasNames.VERWALTUNGSLEISTUNGEN_SL -> IndexNames.VERWALTUNGSLEISTUNGEN_SL;
            case AliasNames.VERWALTUNGSLEISTUNGEN_BE -> IndexNames.VERWALTUNGSLEISTUNGEN_BE;
            case AliasNames.VERWALTUNGSLEISTUNGEN_BB -> IndexNames.VERWALTUNGSLEISTUNGEN_BB;
            case AliasNames.VERWALTUNGSLEISTUNGEN_MV -> IndexNames.VERWALTUNGSLEISTUNGEN_MV;
            case AliasNames.VERWALTUNGSLEISTUNGEN_SN -> IndexNames.VERWALTUNGSLEISTUNGEN_SN;
            case AliasNames.VERWALTUNGSLEISTUNGEN_ST -> IndexNames.VERWALTUNGSLEISTUNGEN_ST;
            case AliasNames.VERWALTUNGSLEISTUNGEN_TH -> IndexNames.VERWALTUNGSLEISTUNGEN_TH;
            case AliasNames.ONLINEDIENSTE_RELATIONEN -> IndexNames.ONLINEDIENSTE_RELATIONEN;
        };
    }
}
