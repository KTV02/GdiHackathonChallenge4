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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
public class SpezialisierungVL
    extends SpezialisierungContent
    implements Identifiable, LanguageCopyable<SpezialisierungVL>
{

    @EqualsAndHashCode.Include
    private String id;

    private String idLB;
    private Set<String> ars = new HashSet<>();
    private Map<Locale, Boolean> languageComplete = new HashMap<>();

    private Map<Locale, SpezialisierungContent> otherLanguages = new HashMap<>();

    @Override
    public Optional<SpezialisierungVL> copyByLanguage(Locale language) {
        SpezialisierungVL spVL = BusinessObjectMapper.INSTANCE.shallowCopy(this);

        if (language.equals(Locale.GERMAN)) {
            spVL.getOtherLanguages().clear();
            return Optional.of(spVL);
        }

        SpezialisierungContent otherLanguageContent = otherLanguages.get(language);
        if (otherLanguageContent == null) {
            return Optional.empty();
        }
        BusinessObjectMapper.INSTANCE.copyContent(otherLanguageContent, spVL);
        spVL.getOtherLanguages().clear();
        return Optional.of(spVL);
    }
}
