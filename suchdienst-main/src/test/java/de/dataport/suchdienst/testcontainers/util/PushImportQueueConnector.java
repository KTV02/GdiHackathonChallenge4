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
package de.dataport.suchdienst.testcontainers.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestComponent
public class PushImportQueueConnector {

    @Autowired
    public MockMvc mockMvc;

    public String pushXzufi(SaDMessage message) throws Exception {
        String producer = message.getProducer();
        String nachrichtenId = message.getNachrichtenId();
        String transaktionsId = message.getTransaktionsId();
        String transaktionsPosition = message.getTransaktionsPosition();
        String payload = message.getXzufi();
        String schemeAgencyId = message.getSchemeAgencyId();
        String contentType = "application/xml;charset=UTF-8;xzufi-version=%s".formatted(
            message.getXzufiVersion()
        );
        MvcResult mvcResult = mockMvc
            .perform(
                post("/push/xzufi-22/enqueue")
                    .contentType(MediaType.APPLICATION_XML)
                    .header("X-PVOG-NACHRICHTUUID", nachrichtenId)
                    .header("X-PVOG-TRANSAKTIONID", transaktionsId)
                    .header("X-PVOG-TRANSAKTIONPOSITION", transaktionsPosition)
                    .header("X-PVOG-PRODUCER", producer)
                    .header("X-PVOG-AGENCYID", schemeAgencyId)
                    .header("Content-Type", contentType)
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .content(payload)
            )
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");
    }

    public int getQueueStatus(String id, String status) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", String.valueOf(id));
        params.add("status", String.valueOf(status));

        MvcResult mvcResult = mockMvc
            .perform(
                get("/push/xzufi-22/queue")
                    .header("Accept-Language", "de")
                    .accept(MediaType.APPLICATION_JSON)
                    .params(params)
            )
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.totalElements");
    }

    public String getUpToDate() throws Exception {
        MvcResult mvcResult = mockMvc
            .perform(get("/admin/verwaltungsleistung/uptodate").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        return mvcResult.getResponse().getContentAsString();
    }
}
