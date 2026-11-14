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
package de.dataport.suchdienst.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.xoev.xzufi.v2_2_0.Identifikator;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdentifikatorUtilsTest {

    @ParameterizedTest(name = "Identifikator-Wert für {0} kann erfolgreich extrahiert werden")
    @MethodSource("identificationTypes")
    void valueByString(IdentificationType type) {
        String identifikator = "Scheme.Agency.Id.%s.0815.4711".formatted(type.name());
        String actual = IdentifikatorUtil.getIdentificatorValueByString(identifikator, type);
        assertEquals("0815.4711", actual);
    }

    @ParameterizedTest(name = "FullId für {0} kann aus Identifikator erzeugt werden")
    @MethodSource("identificationTypes")
    void getFullId(IdentificationType type) {
        Identifikator identifikator = new Identifikator();
        identifikator.setSchemeAgencyID("Scheme.Agency.Id");
        identifikator.setValue("0815.4711");
        String actual = IdentifikatorUtil.getIdentificatorString(identifikator, type);
        String expected = "Scheme.Agency.Id.%s.0815.4711".formatted(type.name());
        assertEquals(expected, actual);
    }

    static Stream<Arguments> identificationTypes() {
        EnumSet<IdentificationType> enumSet = EnumSet.allOf(IdentificationType.class);
        return enumSet.stream().map(Arguments::of);
    }
}
