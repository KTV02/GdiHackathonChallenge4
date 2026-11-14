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
package de.dataport.suchdienst.geo.model;

import de.dataport.suchdienst.indexer.model.mapping.Address;
import org.apache.commons.lang3.StringUtils;

public interface GeoQueryBuilder {
    /**
     * Liefert nur den ersten Teil der Hausnummer, um bei Adressen wie "254-260" die
     * Trefferwahrscheinlichkeit zu erhoehen
     */
    default String extractHouseNo(final Address address) {
        int dash = StringUtils.indexOfAny(address.getHouseNumber(), '-', '/', ',');
        if (dash > 0) {
            return address.getHouseNumber().substring(0, dash - 1);
        }
        return address.getHouseNumber();
    }
}
