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
package de.dataport.suchdienst.indexer.model.mapping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Schema(description = "empfaenger, xoev-lx:String.Latin")
    private String receiver;

    @Schema(description = "iban, xoev-lx:String.Latin")
    private String iban;

    @Schema(description = "bankinstitut, xoev-lx:String.Latin")
    private String bankName;

    @Schema(description = "beschreibung, xzufi:String.Localized")
    private String description;

    @Schema(description = "bic, xoev-lx:String.Latin")
    private String bic;

    public BankAccount(
        String receiver,
        String iban,
        String bankName,
        String description,
        String bic
    ) {
        super();
        this.receiver = receiver;
        this.iban = iban;
        this.bankName = bankName;
        this.description = description;
        this.bic = bic;
    }
}
