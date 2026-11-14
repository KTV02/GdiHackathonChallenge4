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
package suchdienst.integrationstest.tests.bkg.locationsv2;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.LocationsConnector;
import suchdienst.integrationstest.validator.locationsv2.LocationsValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LocationTest {

    @Test
    void fuzzySearch() {
        Response responseDisabled = LocationsConnector.getLocationsV2("Kieel", 10, false);
        LocationsValidator validatorDisabled = new LocationsValidator(responseDisabled);
        validatorDisabled.validateCount(0);

        Response responseEnabled = LocationsConnector.getLocationsV2("Kieel", 10, true);
        LocationsValidator validatorEnabled = new LocationsValidator(responseEnabled);
        validatorEnabled.validateName(0, "Kiel, Landeshauptstadt");
    }

    @DisplayName("searchTown")
    @ParameterizedTest(name = "{displayName} {0}")
    @CsvSource(
        value = {
            "Stein",
            "Weste",
            "Langen",
            "Berg",
            "Lind",
            "Rott",
            "Roth",
            "Hausen",
            "Reich",
            "Buch",
            "Linden",
            "Breit",
            "Karl",
            "Burg",
            "Hamm",
            "Esch",
            "Au",
            "Weil",
            "Wiesen",
            "Wald",
            "Schwarz",
            "Neuenkirchen",
            "Neukirch",
        }
    )
    void searchTown(String town) {
        Response response = LocationsConnector.getLocationsV2(town, 10, true);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateName(0, town).validateZipNotEmpty(0);
    }

    @DisplayName("searchWithUmlaut")
    @ParameterizedTest(name = "{displayName} {0}")
    @CsvSource(value = { "Nuernberg, Nürnberg", "Moelln, Mölln", "Daenischenhagen, Dänischenhagen" })
    void searchWithUmlaut(String town, String expectedTown) {
        Response response = LocationsConnector.getLocationsV2(town, 10, true);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateContainsName(expectedTown);
    }

    @DisplayName("searchZip")
    @ParameterizedTest(name = "{displayName} {0}")
    @CsvSource(
        value = {
            "Weste, 29599",
            "Welterod, 56357",
            "Burg, 54646",
            "Esch, 54585",
            "Au, 79280",
            "Berg, 88276",
            "Berg, 82335",
            "Wald, 88639",
            "Weil, 86947",
            "Wald, 93192",
            "Berg, 95180",
            "Wiesen, 63831",
            "Gingst, 18569",
            "Blesewitz, 17392",
            "Lind, 53506",
            "Stein, 24235",
            "Roth, 55442",
            "Hausen, 55608",
            "Kehrig, 56729",
            "Buch, 56290",
            "Bubach, 56288",
            "Buch, 56357",
            "Herold, 56368",
            "Kirburg, 57629",
            "Immert, 54426",
            "Osann-Monzel, 54518",
            "Karl, 54534",
            "Reich, 55471",
            "Wald, 87616",
            "Schwarz, 17252",
            "Neukirch, 01936",
            "Langen, 49838",
            "Berg, 53505",
        }
    )
    void searchZip(String town, String zip) {
        Response response = LocationsConnector.getLocationsV2(zip, 10, false);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateContainsName(town);
    }

    @DisplayName("searchTownZip")
    @ParameterizedTest(name = "{displayName} {0}")
    @CsvSource(
        value = {
            "Stein 24325, Stein",
            "Weste 29599, Weste",
            "Langen 49838, Langen",
            "Berg 53505, Berg",
            "Lind 53506, Lind",
            "Rott 57632, Rott",
            "Roth 55442, Roth",
            "Hausen 55608, Hausen",
            "Lind 56729, Lind",
            "Buch 56290, Buch",
            "Roth 56288, Roth",
            "Reich 55471, Reich",
            "Berg 56357, Berg",
            "Buch 56357, Buch",
            "Roth 56368, Roth",
            "Linden 57629, Linden",
            "Breit 54426, Breit",
            "Esch 54518, Esch",
            "Karl 54534, Karl",
            "Burg 54646, Burg",
            "Hamm 54636, Hamm",
            "Esch 54585, Esch",
            "Au 79280, Au",
            "Berg 88276, Berg",
            "Berg 82335, Berg",
            "Wald 88639, Wald",
            "Weil 86947, Weil",
            "Wald 93192, Wald",
            "Berg 95180, Berg",
            "Wiesen 63831, Wiesen",
            "Wald 87616, Wald",
            "Schwarz 17252, Schwarz",
            "Neuenkirchen 18569, Neuenkirchen",
            "Neukirch 01936, Neukirch",
            "Neuenkirchen 17392, Neuenkirchen",
        }
    )
    void searchTownZip(String townZip, String town) {
        Response response = LocationsConnector.getLocationsV2(townZip, 10, false);
        LocationsValidator validator = new LocationsValidator(response);
        validator.validateName(0, town);
    }
}
