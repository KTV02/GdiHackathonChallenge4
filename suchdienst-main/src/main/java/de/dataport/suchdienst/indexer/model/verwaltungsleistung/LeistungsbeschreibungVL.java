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
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapping.LeikaTyping;
import de.dataport.suchdienst.indexer.model.mapping.LifeSituation;
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
public class LeistungsbeschreibungVL
    extends LeistungsbeschreibungContent
    implements Identifiable, LanguageCopyable<Map<String, List<AbstractServiceModule>>>
{

    @EqualsAndHashCode.Include
    private String id;

    private String languageCode;
    private Instant lastUpdate;
    private boolean isGemeinsamerVollzug;

    /**
     * Der neueste redaktionelle Update-Zeitpunkt (Versionsinformation aus dem Xzufi).
     */
    private Instant externalLastUpdate;

    private Set<String> addressees = new HashSet<>();
    private Set<String> arsFederalstate = new HashSet<>();
    private Set<String> ars = new HashSet<>();

    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    private Set<String> leikakeys = new HashSet<>();

    private Set<String> leikaHierarchyKeys = new HashSet<>();

    private Set<CodelistEntry> sdgInformation = new HashSet<>();

    private Set<CodelistEntry> sdgInformation2 = new HashSet<>();

    private Set<Locale> availableLanguages = new HashSet<>();

    private Map<Locale, LeistungsbeschreibungContent> otherLanguages = new HashMap<>();

    private List<LeikaTyping> typing = new ArrayList<>();

    private Map<Locale, Boolean> languageComplete = new HashMap<>();

    private Set<LifeSituation> lifeSituations = new HashSet<>();

    // Redundant halten wir die Codes der Lebenslagen hier noch in einer einfachen Liste um damit eine effiziente, einfache Suchanfrage
    // auf Lebenslagen zu ermöglichen, die statt einem Lookup in allen Baum-Hierarchie-Ebenen einfach flat in dieser Liste sucht.
    private Set<String> lifeSituationCodes = new HashSet<>();

    // Menge der relevanten Lebenslagen Versionen zum Abrufen aus dem xRepository.
    // Wenn im Lebenslagenkatalog die Lebenslagen für die der Leistung betreffende LeiKa gefunden wurden, entspricht die Menge
    // aller Versionen, die im Leistungskatalog enthalten sind.
    // Wenn die Lebenslagen nicht über den Katalog aufgelöst werden konnten, sondern über das XML, dann entspricht die Menge
    // genau der Versionen, welcher im XML zu dieser Leistung hinterlegt wurden.
    private Set<String> lifeSituationVersions = new HashSet<>();

    @Override
    public Optional<Map<String, List<AbstractServiceModule>>> copyByLanguage(Locale language) {
        if (language.equals(Locale.GERMAN)) {
            return Optional.of(new HashMap<>(this.getDetails()));
        }

        LeistungsbeschreibungContent otherLanguageContent = otherLanguages.get(language);
        if (otherLanguageContent == null) {
            return Optional.empty();
        }

        return Optional.of(new HashMap<>(otherLanguageContent.getDetails()));
    }
}
