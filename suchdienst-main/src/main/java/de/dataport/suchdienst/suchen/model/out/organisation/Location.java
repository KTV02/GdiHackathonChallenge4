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

import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Location {

    private List<AdditionalLink> urls = new ArrayList<>();

    private List<Information> information = new ArrayList<>();

    private List<Address> addresses = new ArrayList<>();

    private List<Communication> communications = new ArrayList<>();
}
