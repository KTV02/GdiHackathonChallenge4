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
package de.dataport.suchdienst.indexer.parser.x22.modul;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_2_0.Bearbeitungsdauermodul;
import de.xoev.xzufi.v2_2_0.FristOhneTyp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("bearbeitungsdauerModulParserX22")
public class BearbeitungsdauerModulParser
    implements Parser<Bearbeitungsdauermodul, ServiceModuleDetail>
{

    private final MessageSource messageSource;
    private final FristHelper fristHelper;

    public BearbeitungsdauerModulParser(
        @Qualifier("fristHelperX22") FristHelper fristHelper,
        MessageSource messageSource
    ) {
        this.fristHelper = fristHelper;
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.BEARBEITUNGSDAUER;
    }

    @Override
    public ServiceModuleDetail parse(Bearbeitungsdauermodul modul, Locale locale) {
        List<ServiceModuleDetailContent> blockDetailsBearbeitungsdauer = new ArrayList<>();

        for (FristOhneTyp frist : modul.getBearbeitungsdauer()) {
            var builder = ServiceModuleDetailContent.builder();
            builder.title(XZuFiUtil.getFirstByLanguage(frist.getBeschreibung(), locale).orElse(""));

            if (frist.getFristauswahl() != null) {
                Optional<String> dauer = fristHelper.calculateDauer(
                    frist.getFristauswahl(),
                    locale
                );
                dauer.ifPresent(builder::text);
            }
            blockDetailsBearbeitungsdauer.add(builder.build());
        }

        return ServiceModuleDetail.builder()
            .title(messageSource.getMessage("bearbeitungsdauer_modul_parser.title", null, locale))
            .text(XZuFiUtil.getFirstByLanguage(modul.getBeschreibung(), locale).orElse(""))
            .details(blockDetailsBearbeitungsdauer)
            .validityPeriods(XZuFiUtil.getValidityPeriods(modul.getGueltigkeit()))
            .links(XZuFiUtil.getHyperlinks(modul.getWeiterfuehrenderLink(), locale))
            .build();
    }

    @Override
    public Set<String> getIncludedLanguages(Bearbeitungsdauermodul modul) {
        Set<String> languages = new HashSet<>(
            XZuFiUtil.getLocalesFromStringLocalized(modul.getBeschreibung())
        );
        modul
            .getBearbeitungsdauer()
            .forEach(m ->
                languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(m.getBeschreibung()))
            );
        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));
        return languages;
    }
}
