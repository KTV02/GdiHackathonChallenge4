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
package de.dataport.suchdienst.geo.util;

import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.common.model.ArsHierarchyEbene;
import de.dataport.suchdienst.geo.model.internal.SearchARS;
import de.dataport.suchdienst.geo.service.FederalCityState;
import de.dataport.suchdienst.indexer.model.ArsObject;
import java.util.Map;

public final class ArsSearchUtils {

    private ArsSearchUtils() {
        // Utility class
    }

    /**
     * Nutzt Wildcards (*) der BKG-Suche, um die Eingabe in eine Form umzuwandeln, welche ggf.
     * zu eher erwarteten Suchergebnissen führt (TODO es braucht Testfälle, um das zu beweisen sowie
     * die Überlegung, ob das ein Feature ist, das überhaupt verwendet/angefragt wird). Außerdem werden
     * Stadtstaaten-ARS in ein BKG Äquivalent überführt.
     *
     * @param ars Eingabe-ARS. Darf nur aus Ziffern bestehen. Darf kürzer als 12 Zeichen sein
     * @return Such-ARS für die BKG-API.
     */
    public static SearchARS buildBkgArsSearchParam(String ars, Map<String, String> arsList) {
        var isFedCityState = FederalCityState.isFederalCityStateArs(ars);
        var searchArs = new SearchARS(ars);

        // Anhängende Nullen werden als Platzhalter für höhere ARS-Hierarchien angesehen und deshalb hier nach Hierarchie entfernt
        var trimmedArs = new ARS(ars).hierarchyTrim();
        var arsIsTrimmed = trimmedArs.length() < ArsObject.ARS_LENGTH;

        // Der Stern gilt als Wildcard in der BKG-Suche. Damit wird sichergestellt, dass die Ergebnisse auch mit dem eingegebenen ARS-Teil beginnen.
        // Ohne die Wildcard kann es zu Ergebnissen aus Teilstrings kommen, was für den Anwender unerwartet sein könnte.
        searchArs.setProcessedArs(arsIsTrimmed ? trimmedArs + "*" : trimmedArs);

        if (trimmedArs.length() == 2) {
            searchArs.setArsEbene(ArsHierarchyEbene.BUNDESLAND);
        }

        if (isFedCityState && !arsIsTrimmed && arsList.containsKey(ars)) {
            // Der ARS ist vollständig (nicht gekürzt, also ein ARS mit OT), ein Stadtstaat-ARS
            // und befindet sich in der XRepository-Liste,
            // also transformieren wir ihn zu einem 16-stelligen BKG-Schlüssel.
            searchArs.setSchluessel(FederalCityState.transformFromArsToBkgKey(ars));
            searchArs.setArsEbene(ArsHierarchyEbene.ORTSTEIL);
            // Ein transformierter BL-ARS führt zu einem Schlüssel, der für HH unerwartete Treffer liefert.
            // Deshalb muss in dem 'schluessel'-Feld anstatt in dem 'rs'-Feld gesucht werden.
            return searchArs;
        }

        // Der Such-ARS
        // - ist keinem Stadtstaat zugeordnet, der im XRepository definiert wurde
        // - enthält keine endenden Nullen in kompletten Ebenen
        return searchArs;
    }
}
