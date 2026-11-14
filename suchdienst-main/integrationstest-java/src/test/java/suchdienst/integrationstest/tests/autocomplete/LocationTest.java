/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.autocomplete;

import io.restassured.response.Response;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.validator.locationssuggestionsv2.LocationsResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LocationTest {

    private static final List<Pair<String, String>> testDataList = List.of(
        Pair.of("24235", "Stein"),
        Pair.of("29599", "Weste"),
        Pair.of("49838", "Langen"),
        Pair.of("53505", "Berg"),
        Pair.of("53506", "Lind"),
        Pair.of("57632", "Rott"),
        Pair.of("55442", "Roth"),
        Pair.of("55608", "Hausen"),
        Pair.of("56729", "Lind"),
        Pair.of("56290", "Buch"),
        Pair.of("56288", "Roth"),
        Pair.of("55471", "Reich"),
        Pair.of("56357", "Berg"),
        Pair.of("56357", "Buch"),
        Pair.of("56368", "Roth"),
        Pair.of("57629", "Linden"),
        Pair.of("54426", "Breit"),
        Pair.of("54518", "Esch"),
        Pair.of("54534", "Karl"),
        Pair.of("54646", "Burg"),
        Pair.of("54636", "Hamm"),
        Pair.of("54585", "Esch"),
        Pair.of("79280", "Au"),
        Pair.of("88276", "Berg"),
        Pair.of("82335", "Berg"),
        Pair.of("88639", "Wald"),
        Pair.of("86947", "Weil"),
        Pair.of("93192", "Wald"),
        Pair.of("95180", "Berg"),
        Pair.of("63831", "Wiesen"),
        Pair.of("87616", "Wald"),
        Pair.of("17252", "Schwarz"),
        Pair.of("18569", "Neuenkirchen"),
        Pair.of("01936", "Neukirch"),
        Pair.of("17392", "Neuenkirchen"),
        Pair.of("90403", "Nürnberg"),
        Pair.of("02699", "Neschwitz / Njeswačidło"),
        Pair.of("91093", "Heßdorf")
    );

    @Test
    void noMatchNotFuzzy() {
        Response response = LocationsConnector.getLocationsSuggestionsV2("Kieel", false);
        LocationsResultListValidator validator = new LocationsResultListValidator(response);
        validator.validateIsEmpty();
    }

    @Test
    void matchWithFuzzy() {
        Response response = LocationsConnector.getLocationsSuggestionsV2("Kieel", true);
        LocationsResultListValidator validator = new LocationsResultListValidator(response);
        validator.validateContains("Kiel, Landeshauptstadt");
    }

    @Test
    void matchWithUmlaut() {
        Response response = LocationsConnector.getLocationsSuggestionsV2("Nuer", false);
        LocationsResultListValidator validator = new LocationsResultListValidator(response);
        validator.validateContains("Nürnberg");

        response = LocationsConnector.getLocationsSuggestionsV2("Moel", false);
        validator = new LocationsResultListValidator(response);
        validator.validateContains("Mölln, Stadt");

        response = LocationsConnector.getLocationsSuggestionsV2("Daeni", false);
        validator = new LocationsResultListValidator(response);
        validator.validateContains("Dänischenhagen");
    }

    @Test
    void matchSmallCity() {
        Response response = LocationsConnector.getLocationsSuggestionsV2("Stein", true);
        LocationsResultListValidator validator = new LocationsResultListValidator(response);
        validator.validateContains("Stein");
    }

    @Test
    void matchByZipAndName() {
        for (Pair<String, String> pairEntry : testDataList) {
            Response response = LocationsConnector.getLocationsSuggestionsV2(
                pairEntry.getLeft() + " " + pairEntry.getRight(),
                true
            );
            LocationsResultListValidator validator = new LocationsResultListValidator(response);
            validator.validateContains(pairEntry.getRight());
        }
    }

    @Test
    void matchByZipFindName() {
        for (Pair<String, String> pairEntry : testDataList) {
            Response response = LocationsConnector.getLocationsSuggestionsV2(pairEntry.getLeft(), false, false, 50);
            LocationsResultListValidator validator = new LocationsResultListValidator(response);
            validator.validateContains(pairEntry.getRight());
        }
    }

    @Test
    void matchByNameFindZip() {
        for (Pair<String, String> pairEntry : testDataList) {
            Response response = LocationsConnector.getLocationsSuggestionsV2(pairEntry.getRight(), false, false, 11);
            LocationsResultListValidator validator = new LocationsResultListValidator(response);
            validator.validateContainsZIP(pairEntry.getLeft());
        }
    }

    @Test
    void matchWithFuzzyFirst() {
        //use size 100 - many fuzzySearch matches - Stiefenhofen is at the end
        Response response1 = LocationsConnector.getLocationsSuggestionsV2("Brei", true, false, 100);
        LocationsResultListValidator validator = new LocationsResultListValidator(response1);
        validator.validateOrderBetween2Items("Breisach am Rhein, Stadt", "Bremen, Stadt");

        Response response2 = LocationsConnector.getLocationsSuggestionsV2("Brei", true, true, 100);
        validator = new LocationsResultListValidator(response2);
        validator.validateOrderBetween2Items("Bremen, Stadt", "Breisach am Rhein, Stadt");
    }
}
