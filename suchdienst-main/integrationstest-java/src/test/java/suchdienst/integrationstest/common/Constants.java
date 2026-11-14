/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.common;

import lombok.AllArgsConstructor;

public class Constants {

    public static final String BASE_PATH = "http://localhost:8080";
    public static final String ELS_BASE_PATH = System.getProperty("elasticsearchurl", "http://localhost:9200");
    public static final String LANGUAGE_DE = "de";
    public static final String LANGUAGE_EN = "en";

    /**
     * Name des Request-Headers, über den sich API-Konsumenten mit einer Client-ID identifizieren
     * können (siehe {@code de.dataport.suchdienst.util.model.ApiClientId#HEADER_NAME}).
     */
    public static final String API_CLIENT_ID_HEADER_NAME = "pvog-sud-api-client-id";

    @AllArgsConstructor
    public enum XzufiVersion {
        V2_2_0("2.2.0"),
        V2_3_1("2.3.1");

        private final String version;

        @Override
        public String toString() {
            return this.version;
        }
    }
}
