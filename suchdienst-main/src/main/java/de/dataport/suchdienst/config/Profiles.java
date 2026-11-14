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

public class Profiles {

    public static final String PRODUCTION = "production";
    public static final String DEVELOPMENT = "development";
    public static final String INTEGRATION_TEST = "integration-test";
    public static final String DEPRECATED_API_ACTIVE = "deprecated_api_active"; // NOSONAR - bewusst für spätere Nutzung behalten
    public static final String BETA_API_ACTIVE = "beta_api_active";
    public static final String OPENAPI_SHOW_INTERNAL = "openapi_show_internal";
    public static final String VERWALTUNGSLEISTUNG_ACTIVE = "verwaltungsleistungActive";
    public static final String SECURITY_OFF = "security_off";

    private Profiles() {}
}
