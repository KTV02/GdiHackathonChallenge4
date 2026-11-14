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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Enum holding all IndexNames used in PVOG context
 */
@Getter
public enum IndexNames {
    LEISTUNGSBESCHREIBUNG("suchdienst_leistungsbeschreibungen"),
    ORGANISATIONSEINHEITEN("suchdienst_organisationseinheiten"),
    ARS("suchdienst_ars"),
    CODELISTENTRIES("suchdienst_codelistentries"),
    UPDATEHISTORY("suchdienst_updatehistory"),
    SPEZIALISIERUNGEN("suchdienst_spezialisierungen"),
    ZUSTAENDIGKEITEN("suchdienst_zustaendigkeiten"),
    SUGGESTIONS("suchdienst_suggestions"),

    LEISTUNGSKATALOG("suchdienst_leistungskatalog"),
    ONLINEDIENSTE("suchdienst_onlinedienste"),
    ONLINEDIENSTE_RELATIONEN("suchdienst_onlinedienste_relationen"),
    FORMULARE("suchdienst_formulare"),
    IMPORTQUEUE("suchdienst_importqueue"),
    DELETED_OBJECTS("suchdienst_deletedobjects"),
    AFFECTED_LBIDS("suchdienst_affected_lbids"),

    VERWALTUNGSLEISTUNGEN_SH("suchdienst_verwaltungsleistungen_sh"), // 01 Schleswig-Holstein
    VERWALTUNGSLEISTUNGEN_HH("suchdienst_verwaltungsleistungen_hh"), // 02 Hamburg
    VERWALTUNGSLEISTUNGEN_NI("suchdienst_verwaltungsleistungen_ni"), // 03 Niedersachsen
    VERWALTUNGSLEISTUNGEN_HB("suchdienst_verwaltungsleistungen_hb"), // 04 Bremen
    VERWALTUNGSLEISTUNGEN_NW("suchdienst_verwaltungsleistungen_nw"), // 05 Nordrhein-Westfalen
    VERWALTUNGSLEISTUNGEN_HE("suchdienst_verwaltungsleistungen_he"), // 06 Hessen
    VERWALTUNGSLEISTUNGEN_RP("suchdienst_verwaltungsleistungen_rp"), // 07 Rheinland-Pfalz
    VERWALTUNGSLEISTUNGEN_BW("suchdienst_verwaltungsleistungen_bw"), // 08 Baden-Württemberg
    VERWALTUNGSLEISTUNGEN_BY("suchdienst_verwaltungsleistungen_by"), // 09 Bayern
    VERWALTUNGSLEISTUNGEN_SL("suchdienst_verwaltungsleistungen_sl"), // 10 Saarland
    VERWALTUNGSLEISTUNGEN_BE("suchdienst_verwaltungsleistungen_be"), // 11 Berlin
    VERWALTUNGSLEISTUNGEN_BB("suchdienst_verwaltungsleistungen_bb"), // 12 Brandenburg
    VERWALTUNGSLEISTUNGEN_MV("suchdienst_verwaltungsleistungen_mv"), // 13 Mecklenburg-Vorpommern
    VERWALTUNGSLEISTUNGEN_SN("suchdienst_verwaltungsleistungen_sn"), // 14 Sachsen
    VERWALTUNGSLEISTUNGEN_ST("suchdienst_verwaltungsleistungen_st"), // 15 Sachsen-Anhalt
    VERWALTUNGSLEISTUNGEN_TH("suchdienst_verwaltungsleistungen_th"); // 16 Thüringen

    public static final Map<String, IndexNames> ARS_MAPPING = ImmutableMap.<
        String,
        IndexNames
    >builder()
        .put("09", VERWALTUNGSLEISTUNGEN_BY)
        .put("01", VERWALTUNGSLEISTUNGEN_SH)
        .put("02", VERWALTUNGSLEISTUNGEN_HH)
        .put("03", VERWALTUNGSLEISTUNGEN_NI)
        .put("04", VERWALTUNGSLEISTUNGEN_HB)
        .put("05", VERWALTUNGSLEISTUNGEN_NW)
        .put("06", VERWALTUNGSLEISTUNGEN_HE)
        .put("07", VERWALTUNGSLEISTUNGEN_RP)
        .put("08", VERWALTUNGSLEISTUNGEN_BW)
        .put("10", VERWALTUNGSLEISTUNGEN_SL)
        .put("11", VERWALTUNGSLEISTUNGEN_BE)
        .put("12", VERWALTUNGSLEISTUNGEN_BB)
        .put("13", VERWALTUNGSLEISTUNGEN_MV)
        .put("14", VERWALTUNGSLEISTUNGEN_SN)
        .put("15", VERWALTUNGSLEISTUNGEN_ST)
        .put("16", VERWALTUNGSLEISTUNGEN_TH)
        .build();

    public static final List<IndexNames> VL_INDICES = List.copyOf(ARS_MAPPING.values());

    /**
     * Die XZuFi-Indices, die für die Datenübersicht und Producer-Erkennung relevant sind.
     */
    public static final List<IndexNames> XZUFI_INDICES = List.of(
        LEISTUNGSBESCHREIBUNG,
        ORGANISATIONSEINHEITEN,
        ONLINEDIENSTE,
        SPEZIALISIERUNGEN,
        ZUSTAENDIGKEITEN
    );

    public static IndexNames getIndexNameByArs(String ars) {
        // Wenn ein ungültiger Ars genommen wird, dann wollen wir in Bremen suchen weil es ein kleines Bundesland ist
        return ARS_MAPPING.get(ars) == null ? VERWALTUNGSLEISTUNGEN_HB : ARS_MAPPING.get(ars);
    }

    private final String indexName;

    IndexNames(String indexName) {
        this.indexName = indexName;
    }

    public static IndexNames getByName(String name) {
        return Arrays.stream(IndexNames.values())
            .filter(v -> v.indexName.equals(name))
            .findAny()
            .orElse(null);
    }
}
