// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Ortsangaben mit Kommunikationssystemen im neuen Detailformat.
package de.dataport.suchdienst.suchen.model.out.organisation;

import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class LocationDetail {

    private List<HyperLink> urls = new ArrayList<>();

    private List<Information> information = new ArrayList<>();

    private List<Address> addresses = new ArrayList<>();

    private List<CommunicationDetail> communications = new ArrayList<>();
}
