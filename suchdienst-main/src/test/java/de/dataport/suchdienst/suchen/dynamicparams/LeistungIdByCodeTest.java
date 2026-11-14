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

import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_2_0.CodeOnlinedienstDynamischerParameterTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeistungIdByCodeTest {

    private static final String LEISTUNG_CODE = "001";
    private static final String PARAM_NAME = "custom_leistung_name";
    private static final String PARAM_VALUE = "SchemeAgency.LB.100100000";
    private LeistungIdByCode leistungIdByCode;
    private DynamicParamContext dynamicParamContext;

    @BeforeEach
    void setUp() {
        leistungIdByCode = new LeistungIdByCode();
        LeistungsbeschreibungVL leistungsbeschreibungVL = new LeistungsbeschreibungVL();
        CodeOnlinedienstDynamischerParameterTyp codeOnlinedienstDynamischerParameterTyp =
            new CodeOnlinedienstDynamischerParameterTyp();
        codeOnlinedienstDynamischerParameterTyp.setCode(LEISTUNG_CODE);
        leistungsbeschreibungVL.setId(PARAM_VALUE);
        dynamicParamContext = DynamicParamContext.builder().lbId(PARAM_VALUE).build();
    }

    @Test
    void testLiefertLeistungId() {
        DynamicParamPair paramPair = leistungIdByCode.fetch(dynamicParamContext, PARAM_NAME);

        assertEquals(LEISTUNG_CODE, paramPair.code());
        assertEquals(PARAM_NAME, paramPair.parameterName());
        assertEquals(
            IdentifikatorUtil.getIdentificatorValueByString(PARAM_VALUE, IdentificationType.LB),
            paramPair.parameterValue()
        );
    }
}
