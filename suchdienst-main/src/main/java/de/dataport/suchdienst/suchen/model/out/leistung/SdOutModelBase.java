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
package de.dataport.suchdienst.suchen.model.out.leistung;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basisklasse der Ausgabemodelle für Leistungsbeschreibungen, von diesen Modellen gibt es mittlerweile zwei Versionen.
 * Hier sind alle gemeinsamen Attribute enthalten.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SdOutModelBase {

    @Schema(
        description = "leikaTextModul mit Code 03, wenn vorhanden, sonst 02",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(description = "leikaTextModul mit Code 06")
    private String description;

    @Schema(description = "referenzLeiKa, xzufi:Code.LeiKaLeistung")
    private Set<String> leikakeys;

    @Schema(description = "informationsbereichSDG, xzufi:Code.SDGInformationsbereich")
    private Set<CodelistEntry> sdgInformation;

    @Schema(description = "id, xzufi:Identifikator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Der Sprachcode aus sprachversion.languageCode")
    private String languageCode;

    @Schema(description = "Zeitpunkt, an dem der Datensatz im Suchdienst erstellt wurde")
    private Instant timestamp;

    @Schema(description = "gueltigkeitGebietID, xzufi:Identifikator")
    private Set<String> ars;

    @Schema(description = "modulUrsprungsportal, xzufi:UrsprungsportalModul")
    private List<SourcePortal> sourcePortals;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "Verfügbare Sprachen aus \"sprachversion\"")
    private Set<String> availableLanguages;

    @Schema(description = "IDs der für diese Leistung zuständigen Organisationseinheiten")
    private Set<String> organisationIds;

    /**
     * Der Zeitpunkt des neuesten Upserts.
     */
    @Schema(
        description = "Zeitpunkt, an dem der Datensatz im Suchdienst empfangen wurde",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant lastUpdate;

    /**
     * Der neueste redaktionelle Update-Zeitpunkt (Versionsinformation aus dem Xzufi).
     */
    @Schema(description = "versionsinformation, xzufi:Versionsinformation")
    private Instant externalLastUpdate;

    @Schema(description = "leistungsadressat, xzufi:Code.Leistungsadressat")
    private Set<String> addressees;

    @Schema(description = "ARS der zugehörigen Bundesländer")
    private Set<String> arsFederalstate;

    /**
     * @deprecated Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated.
     * In neuen OutModels soll es unterdrückt werden, damit es in Zukunft entfernt werden kann.
     */
    @Schema(
        deprecated = true,
        description = "Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated",
        format = "date"
    )
    @Deprecated(forRemoval = true, since = "4.0.9")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    /**
     * @deprecated Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated
     * In neuen OutModels soll es unterdrückt werden, damit es in Zukunft entfernt werden kann.
     */
    @Schema(
        deprecated = true,
        description = "Da es mehr als ein Gültigkeitsdatum geben kann, ist dieses Feld deprecated",
        format = "date"
    )
    @Deprecated(forRemoval = true, since = "4.0.9")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validTo;

    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriodOutModel> validityPeriods;

    @JsonProperty("suggest")
    @Schema(
        description = "Titel aus modulUrsprungsportal, Inhalt für Code 02 und 03 aus modulText, Begriff aus modulBegriffImKontext, zusätzlich gefüllt mit denselben Feldern aus der Spezialisierung"
    )
    private Set<String> suggestions;

    @Schema(
        description = "Titel aus modulUrsprungsportal, Inhalt für Code 02 und 03 aus modulText, Begriff aus modulBegriffImKontext, zusätzlich gefüllt mit denselben Feldern aus der Spezialisierung"
    )
    private Set<String> synonyms;

    @JsonIgnore
    @Schema(description = "Zuständige Onlinedienste, abhängig vom ARS")
    private Set<String> onlineServiceIds;

    @Schema(
        description = "[modulText, xzufi:Textmodul ; modulKosten, xzufi:Kostenmodul ; modulFrist, xzufi:Fristmodul ; modulDokument, xzufi:Dokumentmodul ; modulTextIndividuell, xzufi:TextmodulIndividuell]"
    )
    private List<SeDeBlock> details;

    @Schema(description = "typisierung, xzufi:Code.LeiKaTypisierung")
    private List<LeikaTyping> typing;
}
