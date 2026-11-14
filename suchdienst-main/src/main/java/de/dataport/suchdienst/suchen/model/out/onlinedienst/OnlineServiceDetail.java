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
package de.dataport.suchdienst.suchen.model.out.onlinedienst;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.common.Publisher;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class OnlineServiceDetail {

    @Schema(description = "id, xzufi:Identifikator")
    private String id;

    @Schema(description = "bezeichnung, xzufi:String.Localized")
    private String name;

    @Schema(description = "beschreibung, xzufi:String.localized")
    private String description;

    @Schema(description = "hilfeText, xzufi:String.localized")
    private String helpText;

    @Schema(description = "hilfeLink, xzufi:HyperlinkErweitert", example = "[]")
    private List<HyperLink> helpLinks;

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
    private List<HyperLink> links;

    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriodOutModel> validityPeriods;

    @Schema(description = "Zeitpunkt, an dem der Datensatz im Suchdienst empfangen wurde")
    private Instant lastUpdate;

    @Schema(description = "EfA-Parameter zur Konfiguration des Dienstes, xzufi:ParameterEfA")
    private List<OnlinedienstParameter> parametersEfa;

    @Schema(
        description = "Individuelle Parameter zur Konfiguration des Onlinedienstes, xzufi:ParameterIndividuell"
    )
    private List<OnlinedienstParameter> parametersCustom;

    @Schema(description = "durchfuehrungSprachen, xzufi:Sprache")
    private Set<String> executionLanguages;

    @Schema(
        description = "durchfuehrungStaatsangehoerigkeit, xzufi:Code.StaatsangehoerigkeitDestatis"
    )
    private Set<NationalityCode> executionNationalities;

    @Schema(description = "vertrauensniveau, xzufi:Code.Vertrauensniveau", example = "00")
    private String levelOfConfidence;

    @Schema(description = "Status des Onlinedienstes")
    private OnlineServiceStatus status;

    @JsonProperty("isPublicDisplayAllowed")
    @Schema(
        description = "kennzeichenOeffentlichAnzeigen, xs:boolean",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean publicDisplayAllowed;

    @Schema(description = "sprachversion, xzufi:Sprachversion")
    private Set<String> languages;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "ARS der zugehörigen Bundesländer")
    private Set<String> arsFederalstate;

    @Schema(description = "zustaendigkeit, xzufi:Zustaendigkeit")
    private Set<OdRelations> relations;

    @Schema(description = "logo, xzufi:Identifikator")
    private Set<String> logos;
}
