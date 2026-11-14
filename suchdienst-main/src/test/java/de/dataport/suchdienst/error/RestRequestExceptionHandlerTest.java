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
package de.dataport.suchdienst.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.config.ResourceBundleConfig;
import de.dataport.suchdienst.importer.controller.PushImportController;
import de.dataport.suchdienst.importer.service.DownloadImportRequestTokenService;
import de.dataport.suchdienst.importer.service.QueueImportService;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PushImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ ResourceBundleConfig.class })
@MockitoBean(types = { QueueImportService.class, DownloadImportRequestTokenService.class })
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class RestRequestExceptionHandlerTest {

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public ObjectMapper objectMapper;

    @MockitoBean
    PvogRequestContext pvogRequestContext;

    @ParameterizedTest
    @CsvSource({
        "de," +
            "Die übermittelten Request-Parameter entsprechen nicht den Anforderungen. " +
            "Der Parameter pageSize muss vom Typ int sein.",
        "en," +
            "The transmitted request parameters do not meet the requirements. " +
            "The parameter pageSize must be of type int.",
    })
    void handleTypeMismatch(String language, String expectedMessage) throws Exception {
        Mockito.when(pvogRequestContext.getLocale()).thenReturn(Locale.of(language));
        MvcResult mvcResult = mockMvc
            .perform(get("/push/xzufi-22/queue?page=0&pageSize=nonumb"))
            .andExpect(status().isBadRequest())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        List<String> message = objectMapper
            .readTree(response.getContentAsString(Charset.defaultCharset()))
            .findValuesAsString("message");

        assertFalse(message.isEmpty());
        assertEquals(expectedMessage, message.getFirst());
    }
}
