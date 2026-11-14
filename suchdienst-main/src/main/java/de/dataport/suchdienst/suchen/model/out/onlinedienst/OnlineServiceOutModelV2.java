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

import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class OnlineServiceOutModelV2 {

    @Schema(description = "id, xzufi:Identifikator")
    private String id;

    @Schema(description = "bezeichnung, xzufi:String.Localized")
    private String name;

    @Schema(description = "beschreibung, xzufi:String.localized")
    private String description;

    @Schema(description = "hilfeText, xzufi:String.localized")
    private String helpText;

    @Schema(description = "hilfeLink, xzufi:HyperlinkErweitert", example = "[]")
    private List<AdditionalLink> helpLinks;

    @Schema(description = "herausgeber, xzufi:Herausgeber")
    private Publisher publisher;

    @Schema(description = "zahlungsweise, xzufi:Code.Zahlungsweise", example = "[]")
    private Set<String> paymentMethods;

    @Schema(
        description = "identifizierungsmittel, xzufi:Code.Identifizierungsmittel",
        example = "[]"
    )
    private Set<String> identificationMethods;

    @Schema(description = "link, xzufi:OnlinedienstLink")
    private List<AdditionalLink> links;

    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriod> validityPeriods;

    @Schema(description = "Zeitpunkt, an dem der Datensatz im Suchdienst empfangen wurde")
    private Instant lastUpdate;

    @Schema(description = "parameter, xzufi:OnlinedienstParameter")
    private Set<OnlinedienstParameter> parameters;

    @Schema(description = "durchfuehrungSprachen, xzufi:Sprache")
    private Set<String> executionLanguages;

    @Schema(
        description = "durchfuehrungStaatsangehoerigkeit, xzufi:Code.StaatsangehoerigkeitDestatis"
    )
    private Set<NationalityCode> executionNationalities;

    @Schema(description = "vertrauensniveau, xzufi:Code.Vertrauensniveau", example = "00")
    private String levelOfConfidence;

    @Schema(description = "sprachversion, xzufi:Sprachversion")
    private Set<String> languages;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "ARS der zugehörigen Bundesländer")
    private Set<String> arsFederalstate;

    @Schema(description = "zustaendigkeit, xzufi:Zustaendigkeit")
    private Set<OdRelationOutModel> relations;

    @Schema(description = "logo, xzufi:Identifikator")
    private final Set<String> logos;
}
