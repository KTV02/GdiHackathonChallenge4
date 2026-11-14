// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Detailausgabe einer Organisationseinheit mit Sichtbarkeitskennzeichen.
package de.dataport.suchdienst.suchen.model.out.organisation;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import de.dataport.suchdienst.suchen.model.out.formular.FoOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
@Schema(description = "Organisationseinheit mit Sichtbarkeitskennzeichen")
public class OrganisationUnitDetail {

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
        description = "Abbildung der XZuFi-Datenfelder \"infoOeffnungszeitenText\" und \"infoOeffnungszeitenStrukturiert\""
    )
    private Collection<AdditionalInformation> additionalInformation;

    @Schema(description = "bankverbindung, xzufi:Bankverbindung")
    private Collection<BankAccount> bankAccounts;

    @Schema(
        description = "Abbildung der XZuFi-Datenfelder \"anschrift\" und \"erreichbarkeit\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocationDetail location;

    @Schema(description = "kommunikationssystem, xzufi:Kommunikationssystem")
    private Set<CommunicationDetail> communicationSystems;

    @Schema(
        description = "Abbildung des XZuFi-Datenfelds \"ZustaendigkeitOrganisationseinheit:rolle\"",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ResponsibilityRole role;

    @Schema(description = "kontaktperson, xzufi:Kontaktperson")
    private Collection<ContactDetail> employees;

    @Schema(description = "herausgeber, xzufi:Herausgeber")
    private Publisher publisher;

    @Schema(description = "Zugehörige Formulare, xzufi:formular")
    private List<FoOutModel> nestedForms;

    @Schema(description = "Internet-Links, xzufi:internetAdresse")
    private List<HyperLink> internetAddresses;

    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean publicDisplayAllowed;
}
