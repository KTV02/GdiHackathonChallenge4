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

import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
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

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
public class OrganisationseinheitVL extends OrganisationseinheitContent implements Identifiable {

    @EqualsAndHashCode.Include
    private String id;

    private Set<Locale> languages = new HashSet<>();
    private Map<Locale, Boolean> languageComplete = new HashMap<>();
    private Map<Locale, OrganisationseinheitContent> otherLanguages = new HashMap<>();
    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    // Mapt die original angelieferten ARS der Zuständigkeiten (beschränkt auf die übergeordnete Leistung) auf die zugehörigen Rollen
    private Map<String, List<ResponsibilityRole>> relations = new HashMap<>();
    private Map<String, List<ResponsibilityCriterion>> criteria = new HashMap<>();
    private boolean oeffentlichAnzeigen = true;
    private Instant lastUpdate;

    // PV-10974 - geschachtelte Formulare. Leider ist die Property "forms" schon in der Superklasse vergeben.
    private List<FormularVL> nestedForms = new ArrayList<>();

    /**
     * Wendet die angegebene Sprache auf die OE an. Dies geschieht durch Kopieren aller Properties aus der
     * sprachspezifischen Variante der OE, die in otherLanguages gespeichert ist.
     * Es wird auch die Sprache aller geschachtelten Formulare, Kontaktpersonen etc gesetzt.
     *
     * @param language Die Zielsprache
     */
    public void applyLanguageOverride(Locale language) {
        if (!language.equals(Locale.GERMAN)) {
            OrganisationseinheitContent content = otherLanguages.get(language);
            if (content != null) {
                BusinessObjectMapper.INSTANCE.copyContent(content, this);
            }
        }
        nestedForms = nestedForms
            .stream()
            .map(f -> f.copyByLanguage(language))
            .flatMap(Optional::stream)
            .toList();
    }
}
