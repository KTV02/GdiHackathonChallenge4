/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eine Anschrift einer Organisationseinheit.
 */
public record OrganisationAddressResult(
    @Schema(description = "Typ der Anschrift") AddressType type,
    @Schema(description = "Ort") String city,
    @Schema(description = "Postleitzahl") String zip,
    @Schema(description = "Straße") String street,
    @Schema(description = "Hausnummer") String houseNumber,
    @Schema(description = "Postfach") String postbox
) {}
