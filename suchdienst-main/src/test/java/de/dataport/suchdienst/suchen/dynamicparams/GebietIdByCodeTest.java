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

class GebietIdByCodeTest {

    private static final String GEBIET_CODE = "002";
    private static final String PARAM_NAME = "custom_ars_name";
    private static final String PARAM_VALUE = "0123456789012";
    private GebietIdByCode gebietIdByCode;
    private DynamicParamContext dynamicParamContext;

    @BeforeEach
    void setUp() {
        gebietIdByCode = new GebietIdByCode();
        CodeOnlinedienstDynamischerParameterTyp codeOnlinedienstDynamischerParameterTyp =
            new CodeOnlinedienstDynamischerParameterTyp();
        codeOnlinedienstDynamischerParameterTyp.setCode(GEBIET_CODE);
        dynamicParamContext = DynamicParamContext.builder().ars(PARAM_VALUE).build();
    }

    @Test
    void testLiefertArs() {
        DynamicParamPair paramPair = gebietIdByCode.fetch(dynamicParamContext, PARAM_NAME);

        assertEquals(GEBIET_CODE, paramPair.code());
        assertEquals(PARAM_NAME, paramPair.parameterName());
        assertEquals(PARAM_VALUE, paramPair.parameterValue());
    }
}
