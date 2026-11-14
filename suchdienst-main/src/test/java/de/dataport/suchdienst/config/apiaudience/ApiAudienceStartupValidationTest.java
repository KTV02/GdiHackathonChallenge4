// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Tests der Audience-Pfadkonventionen.
package de.dataport.suchdienst.config.apiaudience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiAudienceStartupValidationTest {

    @ParameterizedTest
    @CsvSource({
        "PUBLIC, /v1, true",
        "PUBLIC, /v9/locations, true",
        "PUBLIC, /v00/locations, true",
        "PUBLIC, /v01/locations, true",
        "PUBLIC, /v99/locations, true",
        "PUBLIC, /v1beta2/onlineservices, false",
        "PUBLIC, /v100/locations, false",
        "BETA, /v1beta2, true",
        "BETA, /v9beta9/onlineservices, true",
        "BETA, /v01beta00/onlineservices, true",
        "BETA, /v99beta99/onlineservices, true",
        "BETA, /v1/onlineservices, false",
        "BETA, /v100beta1/onlineservices, false",
        "BETA, /v1beta100/onlineservices, false",
        "INTERNAL, /admin/xzufi, true",
        "INTERNAL, /monitoring/oe, true",
        "INTERNAL, /v2/locations, false",
        "INTERNAL, /v8beta1/servicedescriptions, false",
    })
    void pathMustMatchAudience(ApiAudience audience, String path, boolean expected) {
        assertThat(ApiAudienceStartupValidation.matchesAudience(path, audience)).isEqualTo(
            expected
        );
    }
}
