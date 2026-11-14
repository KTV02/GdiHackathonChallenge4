/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.clienttest.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.client.ApiClient;
import de.dataport.suchdienst.client.model.ErrorModel;
import de.dataport.suchdienst.client.model.FederalStateServices;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testziel: Generierte Modelle binden bekannte JSON-Werte korrekt und lehnen unbekannte Felder,
 * ungültige Enum-Werte und Zeitstempel ab, damit Vertragsabweichungen nicht unbemerkt bleiben.
 */
class StrictJsonBindingTest {

    private final JsonMapper mapper = StrictApiClientFactory.mapper();

    @Test
    void generatedDefaultMapperAlreadyRejectsUnknownProperties() {
        JsonMapper generatedMapper = new ApiClient().getJsonMapper();
        assertTrue(generatedMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertThrows(JacksonException.class, () ->
            generatedMapper.readValue("{\"unexpected\": true}", FederalStateServices.class)
        );
    }

    @Test
    void bindsStatisticsIncludingNestedModelsAndDates() throws IOException {
        String json = new ClassPathResource("binding/statistics.json").getContentAsString(StandardCharsets.UTF_8);
        FederalStateServices statistics = mapper.readValue(json, FederalStateServices.class);
        assertEquals("2026-10-05T10:15:30Z", statistics.getTimestamp().toInstant().toString());
        assertNotNull(statistics.getStateServices());
        assertEquals(1, statistics.getStateStatistics().size());
        var state = statistics.getStateStatistics().iterator().next();
        assertEquals("01", state.getArs());
        assertEquals(2L, state.getLbs().getCount());
        assertNotNull(state.getLbs().getLastUpdate());
        assertEquals(1L, state.getOes().getCount());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"unexpected\": true}",
            "{\"stateStatistics\": [{\"ars\": \"01\", \"unexpected\": true}]}",
            "{\"stateStatistics\": [{\"lbs\": {\"count\": 1, \"unexpected\": true}}]}",
        }
    )
    void rejectsUnknownPropertiesAtEveryModelLevel(String json) {
        assertThrows(JacksonException.class, () -> mapper.readValue(json, FederalStateServices.class));
    }

    @Test
    void bindsDocumentedEnumValue() {
        ErrorModel error = mapper.readValue("{\"code\": \"BAD_REQUEST\"}", ErrorModel.class);
        assertNotNull(error.getCode());
        assertEquals("BAD_REQUEST", error.getCode().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"NOT_A_DOCUMENTED_ERROR\"", "\"bad_request\"", "999" })
    void rejectsUnknownOrInvalidEnumValues(String value) {
        assertThrows(JacksonException.class, () -> mapper.readValue("{\"code\": " + value + "}", ErrorModel.class));
    }

    @Test
    void rejectsInvalidTimestamp() {
        assertThrows(JacksonException.class, () ->
            mapper.readValue("{\"timestamp\": \"not-a-timestamp\"}", FederalStateServices.class)
        );
    }

    @Test
    void acceptsDynamicKeysInExplicitMapsWithoutRelaxingModelBinding() {
        var statistics = mapper.readValue(
            "{\"stateServices\": {\"01\": 2, \"sdk-dynamic-key\": 3}}",
            FederalStateServices.class
        );
        assertEquals(Map.of("01", 2, "sdk-dynamic-key", 3), statistics.getStateServices());
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-10-05T10:15:30Z", "2026-10-05T12:15:30+02:00", "2026-10-05T05:15:30-05:00" })
    void bindsEquivalentTimestampOffsetsToTheSameInstant(String timestamp) {
        var statistics = mapper.readValue("{\"timestamp\": \"" + timestamp + "\"}", FederalStateServices.class);
        assertEquals(Instant.parse("2026-10-05T10:15:30Z"), statistics.getTimestamp().toInstant());
    }
}
