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

public class LeikaByCode implements DynamicParamFetcherStrategy {

    public static final String LEIKA_ID_CODE = "004";

    /**
     * Nimmt den Kontext für die Bearbeitung eines dynamischen Parameters und sucht in diesen nach dem Leika Code
     *
     * @param dynamicParamContext Der Bearbeitungskontext
     * @param parameterName       Name des Dynamischen-Parameters, Bestandteil des Query der URL
     * @return Gibt ein gefülltes paramPair mit den gefundenen Daten zurück.
     * Liefert null, wenn der Wert des Dynamischen-Parameters nicht gefunden wurde.
     */
    @Override
    public DynamicParamPair fetch(DynamicParamContext dynamicParamContext, String parameterName) {
        return dynamicParamContext
            .getLeikaId()
            .map(leikaId -> new DynamicParamPair(LEIKA_ID_CODE, parameterName, leikaId))
            .orElse(null);
    }
}
