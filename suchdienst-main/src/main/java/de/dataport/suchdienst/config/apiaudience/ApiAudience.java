// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Zielgruppe eines API-Endpunkts; steuert OpenAPI-Gruppenzugehörigkeit und Logging.
package de.dataport.suchdienst.config.apiaudience;

/**
 * Zielgruppe eines API-Endpunkts.
 *
 * <p>Der Wert wird über {@link ApiAudienceMarker} an Controller-Klassen oder -Methoden gesetzt und
 * zentral aufgelöst und für folgende Zwecke ausgewertet:
 * <ul>
 *   <li>In {@code WebSecurityConfig}: Endpunkte werden öffentlich freigegeben oder benötigen Authentifizierung.</li>
 *   <li>In {@code OpenApiConfig}: Operationen werden der passenden OpenAPI-Gruppe zugeordnet.</li>
 *   <li>In {@code RequestParameterFilter}: Der Logging-Protokolltyp wird auf Basis der Audience gesetzt.</li>
 * </ul>
 *
 * <p>Die Auflösung zur Laufzeit erfolgt durch {@link ApiAudienceResolver}.
 */
public enum ApiAudience {
    /** Öffentlich zugängliche Endpunkte der stabilen REST-API (OpenAPI-Gruppe "suchen"). */
    PUBLIC,

    /** Experimentelle Endpunkte (Änderungen vorbehalten, OpenAPI-Gruppe "beta"). */
    BETA,

    /** Interne Endpunkte für Administration, Monitoring und Daten-Push (OpenAPI-Gruppe "admin"). */
    INTERNAL,
}
