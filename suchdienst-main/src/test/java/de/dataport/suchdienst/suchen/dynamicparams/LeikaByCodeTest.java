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
package de.dataport.suchdienst.suchen.dynamicparams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.xoev.xzufi.v2_2_0.CodeOnlinedienstDynamischerParameterTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeikaByCodeTest {

    private static final String LEIKA_CODE = "004";
    private static final String PARAM_NAME = "custom_leika_name";
    private LeikaByCode leikaByCode;
    private DynamicParamContext dynamicParamContext;

    @BeforeEach
    void setUp() {
        leikaByCode = new LeikaByCode();

        CodeOnlinedienstDynamischerParameterTyp codeOnlinedienstDynamischerParameterTyp =
            new CodeOnlinedienstDynamischerParameterTyp();
        codeOnlinedienstDynamischerParameterTyp.setCode(LEIKA_CODE);
        dynamicParamContext = DynamicParamContext.builder().leikaId("Leika").build();
    }

    @Test
    void testLiefertLeika() {
        DynamicParamPair paramPair = leikaByCode.fetch(dynamicParamContext, PARAM_NAME);

        assertEquals(LEIKA_CODE, paramPair.code());
        assertEquals(PARAM_NAME, paramPair.parameterName());
        assertEquals("Leika", paramPair.parameterValue());
    }
}
