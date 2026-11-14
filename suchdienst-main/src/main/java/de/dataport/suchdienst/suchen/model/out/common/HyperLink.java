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
package de.dataport.suchdienst.suchen.model.out.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Einfache Representation eines {@link de.xoev.xzufi.v2_3_1.HyperlinkErweitert}.
 * Achtung: Dieses Modell eignet sich nicht für Onlinedienste die zusammen mit dynamischen Parametern ausgegeben werden sollen.
 * Diese werden aktuell über {@link AdditionalLink} abgebildet. Dieses Modell wird allerdings nicht weiter fortgeführt und muss bei Schaffung neuer APIs ersetzt werden.
 * TODO: Dieses OutModel auch an den anderen APIs verwenden, an denen bisher direkt AdditionalLinks heraus gereicht werden.
 *  Bedenken aber: dadurch entstehen neue API-Versionen.
 */
public record HyperLink(
    @Schema(description = "uri, xs:anyURI", requiredMode = Schema.RequiredMode.REQUIRED) String uri,
    @Schema(description = "titel, xs:string") String title,
    @Schema(description = "beschreibung, xs:string") String description
) {}
