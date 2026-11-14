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

/**
 * @param destatisCode Der Code vom Destatis, der im XZuFI geliefert wird.
 * @param iso2Code     Der zugehörige ISO2-Code, sofern vorhanden. Kann leer sein, weil es in der Destatis-Liste
 *                     Einträge gibt, die keinen ISO-Code haben (z.B. Palästina) oder weil der Code nicht aufgelöst
 *                     werden konnte.
 */
public record NationalityCode(String destatisCode, String iso2Code) {}
