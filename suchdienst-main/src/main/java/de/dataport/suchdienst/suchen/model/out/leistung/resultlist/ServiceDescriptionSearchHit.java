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
package de.dataport.suchdienst.suchen.model.out.leistung.resultlist;

import de.dataport.suchdienst.suchen.model.out.leistung.detail.OnlineServiceLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Element der Beta-Such-API (/v8beta1/servicedescriptions). Stellt eine Teilmenge von
 * {@link de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceDescriptionDetail} dar und
 * ist auf die für die Visualisierung in einer Trefferliste relevanten Felder reduziert.
 */
@Builder
@Data
public class ServiceDescriptionSearchHit {

    @Schema(description = "id, xzufi:Identifikator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    /**
     * Name der Leistung.
     * <p>
     * Die Ermittlung erfolgt in folgender Reihenfolge:
     * <ol>
     *     <li>Zuerst werden die <b>Spezialisierungen</b> auf die Textmodule angewendet: Anhand der
     *     ARS-Hierarchie wird die passendste Spezialisierung ausgewählt und in die Module
     *     eingearbeitet (Ersetzen bzw. Anreichern des Textes).</li>
     *     <li>Anschließend wird auf den so aufbereiteten Modulen die <b>Code-Priorisierung</b>
     *     angewendet: Es wird der Inhalt aus dem leikaTextModul mit Code 03 verwendet, wenn
     *     vorhanden, sonst Code 02.</li>
     *     <li>Gibt es zu einem Code mehrere Module, entscheidet abschließend die
     *     <b>Zugangskanal-Priorisierung</b>: 100 (Online) vor 000 (Online/Offline) vor
     *     200 (Offline).</li>
     * </ol>
     */
    @Schema(
        description = "Inhalt aus dem leikaTextModul mit Code 03, wenn vorhanden, sonst 02. Zuvor werden Spezialisierungen anhand der ARS-Hierarchie in die Module eingearbeitet. Bei mehreren Modulen zu einem Code wird zusätzlich der Zugangskanal priorisiert: 100 (Online) vor 000 (Online/Offline) vor 200 (Offline).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    /**
     * Kurzer Vorschautext (Snippet) auf die Leistung aus dem leikaTextModul mit Code 24 (Teaser).
     * <p>
     * Die Ermittlung folgt derselben Reihenfolge wie beim {@link #name}:
     * <ol>
     *     <li>Zuerst werden die <b>Spezialisierungen</b> anhand der ARS-Hierarchie in die
     *     Textmodule eingearbeitet.</li>
     *     <li>Anschließend wird das Teaser-Modul mit Code 24 herangezogen.</li>
     *     <li>Existieren dazu mehrere Module, greift die <b>Zugangskanal-Priorisierung</b>:
     *     100 (Online) vor 000 (Online/Offline) vor 200 (Offline).</li>
     * </ol>
     */
    @Schema(
        description = "Kurzer Vorschautext (Snippet) auf die Leistung aus dem leikaTextModul mit Code 24 (Teaser). Zuvor werden Spezialisierungen anhand der ARS-Hierarchie in die Module eingearbeitet. Bei mehreren Modulen greift anschließend die Zugangskanal-Priorisierung: 100 (Online) vor 000 (Online/Offline) vor 200 (Offline)."
    )
    private String teaser;

    @Schema(
        description = "Der 2-stellige ARS des Bundeslandes. Ermöglicht die Unterscheidung zwischen Bundes- und Landesleistungen."
    )
    private Set<String> arsFederalstate;

    @Schema(description = "Vollständigkeit der Felder zu einer Sprache")
    private Map<Locale, Boolean> languageComplete;

    @Schema(description = "Zugehörige Links von Onlinediensten")
    private List<OnlineServiceLink> onlineServiceLinks;

    @Schema(description = "Verfügbare Sprachen der Leistung")
    private Set<String> availableLanguages;
}
