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
package de.dataport.suchdienst.suchen.model.views;

/**
 * @deprecated : JsonViews wurden durch dedizierte Out-Modelle abgelöst. Sie existieren lediglich weiter, um den OpenAPI Kontrakt zu erhalten.
 * Bei folgenden API-Versionen sollen sie wegfallen!
 */
@Deprecated(forRemoval = true, since = "9.18.0")
public class JsonViews {

    @Deprecated(forRemoval = true, since = "9.18.0")
    public interface Search {}

    @Deprecated(forRemoval = true, since = "9.18.0")
    public interface Detail extends Search {}

    @Deprecated(forRemoval = true, since = "9.18.0")
    public interface Internal extends Search {}
}
