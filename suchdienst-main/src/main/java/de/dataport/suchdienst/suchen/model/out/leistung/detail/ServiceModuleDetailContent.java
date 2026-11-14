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
package de.dataport.suchdienst.suchen.model.out.leistung.detail;

import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.KeyValueObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;

public record ServiceModuleDetailContent(
    @Schema(
        description = "Inhalt der Codeliste für den Code in leikaTextmodul",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String title,
    @Schema(description = "inhalt, xzufi:String.Localized") String text,
    @Schema(description = "weiterfuehrenderLink, xzufi:HyperlinkErweitert")
    Collection<HyperLink> links,
    @Schema(description = "Zusatzinformationen aus Modulen")
    Collection<KeyValueObject> additionalInformation
) {}
