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
package de.dataport.suchdienst.suchen.model.out.organisation;

import io.swagger.v3.oas.annotations.media.Schema;

public record BankAccount(
    @Schema(description = "empfaenger, xoev-lx:String.Latin") String receiver,
    @Schema(description = "iban, xoev-lx:String.Latin") String iban,
    @Schema(description = "bankinstitut, xoev-lx:String.Latin") String bankName,
    @Schema(description = "beschreibung, xzufi:String.Localized") String description,
    @Schema(description = "bic, xoev-lx:String.Latin") String bic
) {}
