// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Ausgabe einer Kontaktperson mit Sichtbarkeitskennzeichen.
package de.dataport.suchdienst.suchen.model.out.organisation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import lombok.Data;

@Data
public class ContactDetail {

    @Schema(description = "titel, xzufi:String.Localized")
    private String title;

    @Schema(description = "anrede, xzufi:String.Localized")
    private String salutation;

    @Schema(description = "vorname, xoev-lc:String.Latin")
    private String name;

    @Schema(description = "familienname, xoev-lc:String.Latin")
    private String lastname;

    @Schema(description = "raum, xoev-lc:String.Latin")
    private String room;

    @Schema(description = "position, xzufi:String.Localized")
    private String position;

    @Schema(description = "sprechzeiten, xzufi:String.Localized")
    private String speakingTimes;

    @Schema(
        description = "Abbildung der XZuFi-Datenfelder \"anschrift\" und \"erreichbarkeit\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocationDetail location;

    @Schema(description = "internetadresse, xzufi:HyperlinkErweitert")
    private Collection<HyperLink> additionalLinks;

    @Schema(description = "herausgeber, xzufi:Herausgeber")
    private Publisher publisher;

    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean publicDisplayAllowed;
}
