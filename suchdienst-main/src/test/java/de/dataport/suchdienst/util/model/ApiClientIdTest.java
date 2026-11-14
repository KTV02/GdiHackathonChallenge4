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
package de.dataport.suchdienst.util.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiClientIdTest {

    @Test
    void testApiClientIdCreation() {
        String clientIdValue = UUID.randomUUID().toString();
        ApiClientId apiClientId = new ApiClientId(clientIdValue);
        assertEquals(clientIdValue, apiClientId.value());
        assertEquals(clientIdValue, apiClientId.toString());
    }
}
