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
package de.dataport.suchdienst.suchen.model.out.onlinedienst;

import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Erweitertes OdOutModel, das mit PV-7583 innerhalb von SdOutModel auftritt, wird aber nur innerhalb des neuen Endpunktes
 * "/v4/servicedescriptions/{ars}/detail" verwendet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OdOutModelV4 extends OdOutModel {

    @Schema(description = "identifizierungsmittel, xzufi:Code.Identifizierungsmittel")
    private Set<String> identificationMethods;

    @Schema(description = "zahlungsweise, xzufi:Code.Zahlungsweise")
    private Set<String> paymentMethods;

    @Schema(description = "hilfeText, xzufi:String.Localized")
    private String helpText;

    @Schema(description = "hilfeLink, xzufi:HyperlinkErweitert")
    private List<AdditionalLink> helpLinks;
}
