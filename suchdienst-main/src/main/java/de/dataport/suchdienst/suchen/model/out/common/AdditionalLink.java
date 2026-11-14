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

import de.dataport.suchdienst.suchen.model.out.xzuficopy.OnlinedienstDynamischerParameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Modell zum Abbilden von Onlinedienste-Links zusammen mit dynamischen Parametern.
 * Achtung: Dieses Modell ist veraltet und sollte nicht in neuen APIs verwendet werden.
 * Ferner wird es aktuell an Stellen verwendet die Informationen aus XZuFi abbilden, welche KEINE dynamischen Parameter enthalten.
 *
 * @deprecated Diese Klasse enthält eine Kopie von {@link de.xoev.xzufi.v2_2_0.OnlinedienstDynamischerParameter} und sollte außerhalb von Onlinediensten nicht verwendet werden!
 * Bitte verwendet stattdessen {@link HyperLink} für die Abbildung von {@link de.xoev.xzufi.v2_3_1.HyperlinkErweitert}
 */
@Data
@Deprecated(forRemoval = true)
public class AdditionalLink {

    @Schema(description = "uri, xs:anyURI", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uri;

    @Schema(description = "titel, xs:string")
    private String title;

    @Schema(description = "beschreibung, xs:string")
    private String description;

    @Schema(description = "Ersetzungs-Flag, falls das Objekt aus einer Spezialisierung kommt")
    @EqualsAndHashCode.Exclude
    private final boolean replace = false;

    @EqualsAndHashCode.Exclude
    private Collection<OnlinedienstDynamischerParameter> dynamicParameters = new HashSet<>();
}
