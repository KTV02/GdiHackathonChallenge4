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
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.Set;
import lombok.Data;

/**
 * Erweitertes Ausgabeformat einer Kontaktperson, an der Oe als "Employee" sichtbar.
 * Mit der Erweiterung in PV-7651 war auch ein erweitertes Ausgabeformat für den neuen Endpunkt /v4/organisationunits/detail nötig.
 */
@Data
public class ContactOutModelV4 {

    @Schema(description = "titel, xzufi:String.Localized")
    protected String title;

    @Schema(description = "anrede, xzufi:String.Localized")
    protected String salutation;

    @Schema(description = "vorname, xoev-lc:String.Latin")
    protected String name;

    @Schema(description = "familienname, xoev-lc:String.Latin")
    protected String lastname;

    @Schema(description = "raum, xoev-lc:String.Latin")
    protected String room;

    @Schema(description = "position, xzufi:String.Localized")
    protected String position;

    @Schema(description = "sprechzeiten, xzufi:String.Localized")
    protected String speakingTimes;

    @Schema(
        description = "Enthält Daten aus XZuFi 2.2.0 \"anschrift\" und \"erreichbarkeit\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected Location location;

    @Schema(description = "zustaendigkeit, xzufi:ZustaendigkeitPerson")
    protected Set<ZustaendigkeitEntity> relations;

    @Schema(description = "internetadresse, xzufi:HyperlinkErweitert")
    protected Collection<AdditionalLink> additionalLinks;

    @Schema(description = "herausgeber, xzufi:Herausgeber")
    private Publisher publisher;
}
