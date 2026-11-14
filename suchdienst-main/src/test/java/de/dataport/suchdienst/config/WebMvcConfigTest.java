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
package de.dataport.suchdienst.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = { DeprecatedTestController.class, PartiallyDeprecatedTestController.class }
)
@MockitoBean(types = PvogRequestContext.class)
@Import({
    WebMvcConfig.class,
    CustomCorsProperties.class,
    DevSecurityConfig.class,
    ApiAudienceResolver.class,
})
class WebMvcConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName(
        "Ein Endpunkt ohne irgendeine Deprecation-Annotation setzt keinen Deprecated-Header in die Antwort."
    )
    void callEndpoint_NoDeprecation() throws Exception {
        mockMvc
            .perform(get("/partiallyDeprecatedController/methodA"))
            .andExpect(content().string("A"))
            .andExpect(header().doesNotExist(WebMvcConfig.DEPRECATED_HEADER));
    }

    @Test
    @DisplayName(
        "Ein Endpunkt mit einer Operation-Annotation mit der Property deprecated=true setzt den Deprecated-Header in die Antwort."
    )
    void callEndpoint_OperationDeprecation() throws Exception {
        mockMvc
            .perform(get("/partiallyDeprecatedController/methodB"))
            .andExpect(content().string("B"))
            .andExpect(header().string(WebMvcConfig.DEPRECATED_HEADER, String.valueOf(true)));
    }

    @Test
    @DisplayName(
        "Ein Endpunkt mit Deprecated-Annotation an seiner Methode setzt den Deprecated-Header in die Antwort."
    )
    void callEndpoint_MethodDeprecation() throws Exception {
        mockMvc
            .perform(get("/partiallyDeprecatedController/methodC"))
            .andExpect(content().string("C"))
            .andExpect(header().string(WebMvcConfig.DEPRECATED_HEADER, String.valueOf(true)));
    }

    @Test
    @DisplayName(
        "Ein Endpunkt mit Deprecated-Annotation an seiner Klasse setzt den Deprecated-Header in die Antwort."
    )
    void callEndpoint_ClassDeprecation() throws Exception {
        mockMvc
            .perform(get("/deprecatedController/methodA"))
            .andExpect(content().string("A"))
            .andExpect(header().string(WebMvcConfig.DEPRECATED_HEADER, String.valueOf(true)));
    }
}
