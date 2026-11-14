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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import de.dataport.suchdienst.indexer.LanguageCopyable;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fachobjekt, das ein Formular repräsentiert. Dieses Objekt ist Bestandteil einer LeistungsbeschreibungVL,
 * wird ausschließlich innerhalb der LeistungsbeschreibungVL gehalten und in einem Bundesland-Index gespeichert.
 * <p>
 * Mehrsprachigkeit wird im strikten Modus behandelt: Die deutschen Inhalte liegen direkt auf diesem Objekt,
 * weitere Sprachvarianten in {@link #otherLanguages}. Für die Ausgabe wird mit {@link #copyByLanguage(Locale)}
 * eine sprachspezifische Kopie erzeugt. Fehlt die angefragte Sprachvariante oder enthält sie keinen passenden
 * Dateilink, wird das Formular nicht ausgeliefert. Es findet kein Fallback auf Deutsch statt.
 * <p>
 * Die Dateilinks gehören zu den sprachabhängigen Inhalten in {@link FormularContent}. XZuFi 2.3 kann Dateilinks
 * sprachabhängig liefern; XZuFi 2.2-Dateilinks werden beim Parsen unverändert in jede Formularsprache kopiert.
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@Slf4j
public class FormularVL
    extends FormularContent
    implements Identifiable, LanguageCopyable<FormularVL>
{

    @EqualsAndHashCode.Include
    private String id;

    // Gibt an, in welchen der unterstützten Sprachen die sprachabhängigen Felder und Dateilinks des Formulars vorliegen.
    private Set<Locale> languages = new HashSet<>();

    // Sagt für jede vorliegende Sprache aus, ob alle deutschen Texte ebenfalls in dieser
    // Sprache vorliegen.
    private Map<Locale, Boolean> languageComplete = new HashMap<>();

    // Enthält für andere Sprachen als deutsch jeweils die sprachabhängigen Felder des Formular-Fachobjekts.
    // Die deutschen Feldinhalte werden in der Superklasse FormularContent gehalten.
    private Map<Locale, FormularContent> otherLanguages = new HashMap<>();

    /**
     * Eigenständige Formulare:<br>
     * Hier sind die originalen Zuständigkeit ARS gespeichert,
     * welche in der jeweiligen Verwaltungsleistung Partition vorkommen.<br><br>
     * Geschachtelte Formulare:<br>
     * Hier wird die Partitionierung nicht betrachtet.
     * Es finden sich alle Zuständigkeiten von Formularen in einer OE zur Leistung der Partition.
     * Deshalb wird zur Such-Zeit über den Such-Parameter ARS gefiltert.
     */
    private Set<String> ars = new HashSet<>();

    private Instant lastUpdate;
    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    @Override
    public Optional<FormularVL> copyByLanguage(Locale language) {
        if (!languages.contains(language)) {
            log.debug(
                "Formular {} wird fuer Sprache {} nicht ausgeliefert, da keine passende Sprachvariante vorliegt.",
                id,
                language
            );
            return Optional.empty();
        }

        FormularVL formularVL = BusinessObjectMapper.INSTANCE.shallowCopy(this);

        if (language.equals(Locale.GERMAN)) {
            formularVL.getOtherLanguages().clear();
            return withFileLinks(formularVL, language);
        }

        FormularContent otherLanguageContent = otherLanguages.get(language);
        if (otherLanguageContent == null) {
            return Optional.empty();
        }

        BusinessObjectMapper.INSTANCE.copyContent(otherLanguageContent, formularVL);
        formularVL.getOtherLanguages().clear();
        return withFileLinks(formularVL, language);
    }

    private Optional<FormularVL> withFileLinks(FormularVL formularVL, Locale language) {
        if (formularVL.getFileLinks().isEmpty()) {
            log.debug(
                "Formular {} wird fuer Sprache {} nicht ausgeliefert, da kein passender Dateilink vorliegt.",
                id,
                language
            );
            return Optional.empty();
        }

        return Optional.of(formularVL);
    }
}
