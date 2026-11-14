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
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_2_0.CodeOnlinedienstDynamischerParameterTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OeIdByCodeTest {

    private static final String OE_ID_CODE = "003";
    private static final String PARAM_NAME = "custom_oe_name";
    private static final String PARAM_VALUE = "SchemeAgencyId.OE.123456789";
    private OeIdByCode oeIdByCode;
    private DynamicParamContext dynamicParamContext;

    @BeforeEach
    void setUp() {
        oeIdByCode = new OeIdByCode();
        OrganisationseinheitVL organisationseinheitVL = new OrganisationseinheitVL();
        CodeOnlinedienstDynamischerParameterTyp codeOnlinedienstDynamischerParameterTyp =
            new CodeOnlinedienstDynamischerParameterTyp();
        codeOnlinedienstDynamischerParameterTyp.setCode(OE_ID_CODE);
        organisationseinheitVL.setId(PARAM_VALUE);
        dynamicParamContext = DynamicParamContext.builder().oeId(PARAM_VALUE).build();
    }

    @Test
    void testLiefertOeId() {
        DynamicParamPair paramPair = oeIdByCode.fetch(dynamicParamContext, PARAM_NAME);

        assertEquals(OE_ID_CODE, paramPair.code());
        assertEquals(PARAM_NAME, paramPair.parameterName());
        assertEquals(
            IdentifikatorUtil.getIdentificatorValueByString(PARAM_VALUE, IdentificationType.OE),
            paramPair.parameterValue()
        );
    }
}
