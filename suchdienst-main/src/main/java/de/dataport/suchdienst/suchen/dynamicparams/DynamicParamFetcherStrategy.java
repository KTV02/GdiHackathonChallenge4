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

/**
 * Implementierung für verschiedene Suchstrategien von key-value Paaren, die dynamische Parameter wiederspiegeln.
 */
public interface DynamicParamFetcherStrategy {
    /**
     * Das Interface muss eine fetch Methode implementieren, welche ein mit key-value Paar gefülltes
     * DynamicParamContext liefert. Ist der Parametername nicht aufzufinden, wird das Paar verworfen. Ein
     * leerer Wert ist möglich.
     *
     * @param dynamicParamContext      Kontext der Suche zum Finden der Informationen anhand des Codes
     * @param dynamischerParameterName Name des Dynamischen-Parameters, Bestandteil des Query der URL
     * @return Gefülltes Parameterpaar
     */
    DynamicParamPair fetch(
        DynamicParamContext dynamicParamContext,
        String dynamischerParameterName
    );
}
