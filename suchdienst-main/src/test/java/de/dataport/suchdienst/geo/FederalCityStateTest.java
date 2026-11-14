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
package de.dataport.suchdienst.geo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import de.dataport.suchdienst.geo.service.FederalCityState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FederalCityStateTest {

    @ParameterizedTest
    @CsvSource({
        "0200000000000000,020000000000",
        "0220000000010201,022010201201",
        "0210000000220150,021220150150",
        "1100000000000802,110080008008",
        "1100000000000201,110020002002",
        "1100000000001201,110120012012",
        "0401100000000351,040110351351",
        "0401100000000125,040110125125",
    })
    void transformFromBkgKeyToArsTest(String bkgKey, String xrepositoryArs) {
        assertThat(
            FederalCityState.transformFromBkgKeyToArs(bkgKey).get(),
            equalTo(xrepositoryArs)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "020000000000,0200000000000000",
        "040110000000,0401100000000000",
        "040120000000,0401200000000000",
        "110000000000,11000000000000*",
        "022010201201,0200000000000000",
        "021220150150,0200000000000000",
        "110080008008,11000000000008*",
        "110020002002,11000000000002*",
        "110120012012,11000000000012*",
        "040110351351,0401100000000351",
        "040110111111,0401100000000111",
        "040120000000,0401200000000000",
        "040110125125,0401100000000125",
        "027010701701,0200000000000000",
        "022030210210,0200000000000000",
    })
    void transformFromArsToBkgKeyTest(String xrepositoryArs, String bkgKey) {
        assertThat(FederalCityState.transformFromArsToBkgKey(xrepositoryArs), equalTo(bkgKey));
    }
}
