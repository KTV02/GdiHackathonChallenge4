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
package de.dataport.suchdienst.suchen.dynamicparams;

import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.IdentifikatorUtil;

public class OeIdByCode implements DynamicParamFetcherStrategy {

    public static final String OE_ID_CODE = "003";

    /**
     * Nimmt den Kontext für die bearbeitung eines dynamischen Parameters und sucht in diesen nach der
     * Organisationseinheit
     *
     * @param dynamicParamContext Der Bearbeitungskontext
     * @param parameterName       Name des Dynamischen-Parameters, Bestandteil des Query der URL
     * @return Gibt ein gefülltes paramPair mit den gefundenen Daten zurück.
     * Liefert null, wenn der Wert des Dynamischen-Parameters nicht gefunden wurde.
     */
    @Override
    public DynamicParamPair fetch(DynamicParamContext dynamicParamContext, String parameterName) {
        return dynamicParamContext
            .getOrganisationseinheitId()
            .map(OeIdByCode::toXZuFiOeId)
            .map(oeId -> new DynamicParamPair(OE_ID_CODE, parameterName, oeId))
            .orElse(null);
    }

    private static String toXZuFiOeId(String oeId) {
        return IdentifikatorUtil.getIdentificatorValueByString(oeId, IdentificationType.OE);
    }
}
