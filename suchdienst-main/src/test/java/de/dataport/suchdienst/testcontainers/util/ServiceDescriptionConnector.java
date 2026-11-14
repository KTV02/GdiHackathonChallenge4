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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestComponent
public class ServiceDescriptionConnector {

    @Autowired
    public MockMvc mockMvc;

    public ResultActions getServiceDescriptionsV7(String ars, String q) throws Exception {
        return getServiceDescriptionsV7(ars, List.of(q), 0, 20);
    }

    public ResultActions getServiceDescriptionsV7(
        String ars,
        List<String> q,
        Integer page,
        Integer size
    ) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("q", String.valueOf(q));
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));

        return mockMvc
            .perform(
                MockMvcRequestBuilders.get("/v7/servicedescriptions/{ars}", ars).params(params)
            )
            .andExpectAll(status().isOk());
    }
}
