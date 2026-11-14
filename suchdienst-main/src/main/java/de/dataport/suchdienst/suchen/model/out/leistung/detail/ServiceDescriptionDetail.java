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
package de.dataport.suchdienst.suchen.model.out.leistung.detail;

import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import de.dataport.suchdienst.suchen.model.out.formular.FoOutModel;
import de.dataport.suchdienst.suchen.model.out.leistung.LifeSituationOutModel;
import de.dataport.suchdienst.suchen.model.out.organisation.OeInfoOutModel;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceDescriptionDetail {

    @Schema(
        description = "Inhalt aus dem leikaTextModul mit Code 03, wenn vorhanden, sonst 02. Zusätzlich wird priorisiert der Inhalt aus Zugangskanal 100(Online) gewählt. Falls nicht vorhanden 000 (Online/Offline), oder falls ebenfalls nicht vorhanden als letztes 200(Offline)",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(description = "referenzLeiKa, xzufi:Code.LeiKaLeistung")
    private Set<String> leikakeys;

    @Schema(description = "id, xzufi:Identifikator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Der Sprachcode aus sprachversion.languageCode")
    private String languageCode;

    @Schema(description = "gueltigkeitGebietID, xzufi:Identifikator")
    private Set<String> ars;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "Verfügbare Sprachen aus \"sprachversion\"")
    private Set<String> availableLanguages;

    @Schema(
        description = "Reduzierte Sicht auf Organisationseinheiten mit ID, Titel, Rollen, Kriterien und Sichtbarkeitskennzeichen."
    )
    private Collection<OeInfoOutModel> organisations;

    /**
     * Der Zeitpunkt des neuesten Upserts.
     */
    @Schema(
        description = "Zeitpunkt, an dem der Datensatz im Suchdienst aktualisiert wurde",
        format = "date-time",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant lastUpdate;

    /**
     * Der neueste redaktionelle Update-Zeitpunkt (Versionsinformation aus dem Xzufi).
     */
    @Schema(description = "versionsinformation, xzufi:Versionsinformation")
    private Instant externalLastUpdate;

    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriodOutModel> validityPeriods;

    @ArraySchema(
        schema = @Schema(
            description = "[modulText, xzufi:Textmodul ; modulKosten, xzufi:Kostenmodul ; modulFrist, xzufi:Fristmodul ; modulDokument, xzufi:Dokumentmodul ; modulTextIndividuell, xzufi:TextmodulIndividuell]"
        )
    )
    private List<AbstractServiceModule> details;

    @Schema(description = "Zugehörige Links von Onlinediensten")
    private List<OnlineServiceLink> onlineServiceLinks;

    // Lebenslagen-Wald der Leistung enthält die Roots (Bürger/Unternehmen) und alle Zwischenlagen.
    @Schema(description = "Enthält Lebenslagen")
    private List<LifeSituationOutModel> personalMatters;

    @Schema(description = "Zugehörige Formulare")
    private List<FoOutModel> forms;
}
