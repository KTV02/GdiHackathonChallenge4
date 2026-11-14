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
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.Identifiable;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.NationalityCode;
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
public class OnlinedienstVL
    extends OnlinedienstContent
    implements Identifiable, LanguageCopyable<OnlinedienstVL>
{

    @EqualsAndHashCode.Include
    private String id;

    private List<ValidityPeriod> validityPeriods = new ArrayList<>();
    private Set<Locale> executionLanguages = new HashSet<>();
    private String levelOfConfidence;
    private Set<Locale> languages = new HashSet<>();
    private Map<Locale, Boolean> languageComplete = new HashMap<>();
    private Set<NationalityCode> executionNationalities = new HashSet<>();
    private Set<String> logos = new HashSet<>();
    private Map<Locale, OnlinedienstContent> otherLanguages = new HashMap<>();

    private Set<String> ars = new HashSet<>();

    private boolean oeffentlichAnzeigen = true;
    private CodelistKey onlinedienstStatus;

    private Instant lastUpdate;

    @Override
    public Optional<OnlinedienstVL> copyByLanguage(Locale language) {
        OnlinedienstVL odVL = BusinessObjectMapper.INSTANCE.shallowCopy(this);

        if (language.equals(Locale.GERMAN)) {
            odVL.getOtherLanguages().clear();
            return Optional.of(odVL);
        }

        OnlinedienstContent otherLanguageContent = otherLanguages.get(language);
        if (otherLanguageContent == null) {
            return Optional.empty();
        }

        BusinessObjectMapper.INSTANCE.copyContent(otherLanguageContent, odVL);
        odVL.getOtherLanguages().clear();
        return Optional.of(odVL);
    }
}
