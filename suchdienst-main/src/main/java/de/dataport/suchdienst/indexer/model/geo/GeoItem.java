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
package de.dataport.suchdienst.indexer.model.geo;

import org.apache.commons.lang3.StringUtils;

/**
 * Ein Tupel bestehend aus Regionalschlüssel, Postleitzahl und Ortsbezeichnung.
 * Diese Daten gewinnen wir u.a. aus angelieferten HKDE-Daten und ordnen dann die Postleitzahlen zu den bestehenden
 * Einträgen im Index Ars zu.
 *
 * @param regioKey Der Regionalschlüssel kann ein Ars oder Ags sein, dies hängt davon ab in welchem Kontext die GeoItems verarbeitet
 *                 werden. Die GeoItems werden nur temporär während der Verarbeitung von Massendaten aus HKDE in Memory gehalten,
 *                 dann ist jeweils klar um was es sich bei dem regioKey handelt.
 */
public record GeoItem(String regioKey, String zip) {
    private static final int ZIP_LEN = 5;

    @SuppressWarnings("java:S6207")
    public GeoItem(String regioKey, String zip) {
        if (StringUtils.isEmpty(regioKey)) {
            throw new IllegalArgumentException("Regionalschlüssel darf nicht leer sein");
        }
        this.regioKey = regioKey;
        if (zip == null) {
            throw new IllegalArgumentException("Plz darf nicht leer sein");
        }
        if (zip.length() != ZIP_LEN) {
            throw new IllegalArgumentException(
                String.format("Plz '%s' muss Länge '%d haben", zip, ZIP_LEN)
            );
        }
        this.zip = zip;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", regioKey, zip);
    }
}
