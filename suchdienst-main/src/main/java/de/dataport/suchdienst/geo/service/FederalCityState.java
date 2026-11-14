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
package de.dataport.suchdienst.geo.service;

import de.dataport.suchdienst.geo.model.GeoArs;
import de.dataport.suchdienst.indexer.model.ArsObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FederalCityState {

    public static final Map<String, GeoArs> FEDERAL_CITY_STATES = Map.of(
        "02",
        GeoArs.builder()
            .name("Hamburg, Freie und Hansestadt")
            .ars("020000000000")
            .arsFederalstate("02")
            .municipality("Hamburg")
            .longitude("9.73229853")
            .latitude("53.396935")
            .plz("20038") // Postleitzahl aus Gemeindeverzeichnis
            .build(),
        "04011",
        GeoArs.builder()
            .name("Bremen, Stadt")
            .ars("040110000000")
            .arsFederalstate("04")
            .municipality("Bremen")
            .longitude("8.809338")
            .latitude("53.075606")
            .plz("28195") // Postleitzahl aus Gemeindeverzeichnis
            .build(),
        // Bremerhaven ist formal kein Stadtstaat, muss aber aufgrund der ARS in diesem Sonderfall hier so behandelt werden,
        // damit sowohl Bremen als auch Bremerhaven gefunden werden kann.
        "04012",
        GeoArs.builder()
            .name("Bremerhaven, Stadt")
            .ars("040120000000")
            .arsFederalstate("04")
            .municipality("Bremerhaven")
            .longitude("8.524041")
            .latitude("53.485273")
            .plz("27576") // Postleitzahl aus Gemeindeverzeichnis
            .build(),
        "11",
        GeoArs.builder()
            .name("Berlin, Stadt")
            .ars("110000000000")
            .arsFederalstate("11")
            .municipality("Berlin")
            .longitude("13.405538")
            .latitude("52.51767")
            .plz("10178") // Postleitzahl aus Gemeindeverzeichnis
            .build()
    );

    public static Optional<GeoArs> getMatchingFederalCityState(String ars) {
        if (StringUtils.isEmpty(ars) || ars.length() < (ars.startsWith("04") ? 5 : 2)) {
            return Optional.empty();
        }
        String arsFederalState = ars.startsWith("04") ? ars.substring(0, 5) : ars.substring(0, 2);
        return Optional.ofNullable(FEDERAL_CITY_STATES.get(arsFederalState));
    }

    public static boolean isFederalCityStateArs(String ars) {
        return getMatchingFederalCityState(ars).isPresent();
    }

    public static Optional<GeoArs> findMatchingFederalCityState(List<GeoArs> result) {
        return result
            .stream()
            .map(GeoArs::getArs)
            .filter(FederalCityState::isFederalCityStateArs)
            .findFirst()
            .flatMap(FederalCityState::getMatchingFederalCityState);
    }

    /**
     * Sucht in einer GeoArs-Liste, ob ein Stadtstaat vorhanden ist
     * Name = Gemeindename des Stadtstaats und identische ARS.
     */
    public static Optional<GeoArs> findFederalCityEntry(List<GeoArs> arsList, GeoArs fedState) {
        var municipality = Objects.requireNonNull(fedState.getMunicipality());
        return arsList
            .stream()
            .filter(g -> municipality.equals(g.getName()) && fedState.getArs().equals(g.getArs()))
            .findFirst();
    }

    /**
     * Transformiert einen BKG-Schlüssel, also ein ARS Format, welches für Stadtteile in Stadtstaaten nicht funktioniert,
     * in ein Format, welches mit dem PVOG und den XRepository Codelisten kompatibel ist.
     *
     * @param bkgKey BKG API Response 16-21 Stelliger Schlüssel, welcher Informationen zu Stadtteilen und Straßen enthält
     * @return Transformierter PVOG konformer ARS, sofern Umwandlung möglich war
     */
    public static Optional<String> transformFromBkgKeyToArs(String bkgKey) {
        if (bkgKey.length() < 16) {
            return Optional.empty();
        }
        return switch (bkgKey.substring(0, 2)) {
            case "02" -> Optional.of(
                bkgKey.substring(0, 3) +
                    bkgKey.substring(10, 12) +
                    "0" +
                    bkgKey.substring(13, 16).repeat(2)
            );
            case "04" -> Optional.of(
                bkgKey.substring(0, 5) + "0" + bkgKey.substring(13, 16).repeat(2)
            );
            case "11" -> Optional.of(
                "11" + bkgKey.substring(11, 14) + "0" + bkgKey.substring(11, 14).repeat(2)
            );
            default -> Optional.of(bkgKey.substring(0, 12));
        };
    }

    /**
     * Transformiert eine PVOG/Codelisten konforme ARS zu einem BKG-Schlüssel.
     * Die hier erzeugten ARS können für weiterführende Suchanfragen and den BKG verwendet werden.
     *
     * @param ars PVOG ARS
     * @return BKG-Schlüssel mit gewöhnlich 16 Stellen bzw. Wildcard (*) für Berlin (beginnend mit "11"),
     * da die ARS aus der Codeliste nicht bis zu den Ortsteilen auflösen.
     */
    public static String transformFromArsToBkgKey(String ars) {
        if (ars.length() != ArsObject.ARS_LENGTH) {
            return ars;
        }
        return switch (ars.substring(0, 2)) {
            case "02" -> "0200000000000000";
            case "04" -> ars.substring(0, 5) + "0".repeat(8) + ars.substring(9, 12);
            case "11" -> "110000000000" + ars.substring(10, 12) + "*";
            default -> ars;
        };
    }
}
