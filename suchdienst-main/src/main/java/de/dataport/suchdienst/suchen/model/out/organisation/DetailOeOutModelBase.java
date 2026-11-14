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

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 * Basisklasse mit allen Feldern zur Ausgabe von OEs über die API. Es gibt pro API-Version eine Subklasse.
 */
@Data
public class DetailOeOutModelBase {

    @Schema(description = "id, xzufi:Identifikator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "sprachversion, xzufi:Sprachversion")
    private Set<String> languages;

    @Schema(
        description = "Zeitpunkt, an dem der Datensatz im Suchdienst empfangen wurde",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant lastUpdate;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "name, xzufi:NameOrganisation")
    @JsonProperty("title")
    private String name;

    @Schema(description = "beschreibung, xzufi:String.Localized")
    private String description;

    @Schema(description = "kurzbeschreibung, xzufi:String.Localized")
    private String shortDescription;

    @Schema(
        description = "Enthält Daten aus XZuFi 2.2.0 \"infoOeffnungszeitenText\" und \"infoOeffnungszeitenStrukturiert\""
    )
    private Collection<AdditionalInformation> additionalInformation;

    @Schema(description = "bankverbindung, xzufi:Bankverbindung")
    private Collection<BankAccount> bankAccounts;

    @Schema(
        description = "Enthält Daten aus XZuFi 2.2.0 \"anschrift\" und \"erreichbarkeit\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Location location;

    @Schema(description = "Enthält Daten aus XZuFi 2.2.0 \"kommunikationssystem\"")
    private Set<Communication> communicationSystems;

    @Schema(
        description = "Kommt aus XZuFi 2.2.0 \"ZustaendigkeitOrganisationseinheit:rolle\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ResponsibilityRole role;

    @Schema(description = "kontaktperson, xzufi:Kontaktperson")
    private Collection<ContactOutModelV4> employees;

    @Schema(description = "herausgeber, xzufi:Herausgeber")
    private Publisher publisher;
}
