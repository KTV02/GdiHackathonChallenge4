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

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.SuppressedTextmodule;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_2_0.Textmodul;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service("textModulParserX22")
public class TextModulParser implements Parser<Textmodul, ServiceModule> {

    private final CodelistService codelistService;

    public TextModulParser(CodelistService codelistService) {
        this.codelistService = codelistService;
    }

    @Override
    public ServiceModuleCode getCode() {
        return null;
    }

    @Override
    public ServiceModule parse(Textmodul textmodul, Locale locale) {
        String textModulCode = textmodul.getLeikaTextmodul().getCode();

        return Optional.ofNullable(textModulCode)
            .filter(SuppressedTextmodule::isCodeAllowed)
            .map(code -> {
                String title = getTitle(textmodul, code, locale);

                String text = textmodul
                    .getInhalt()
                    .stream()
                    .map(modul -> XZuFiUtil.getByLanguage(modul, locale))
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining());

                List<Link> links = XZuFiUtil.getHyperlinks(
                    textmodul.getWeiterfuehrenderLink(),
                    locale
                );

                var builder = ServiceModule.builder();
                builder
                    .text(text)
                    .title(title)
                    .links(links)
                    .validityPeriods(XZuFiUtil.getValidityPeriods(textmodul.getGueltigkeit()));
                return builder.build();
            })
            .orElse(null);
    }

    private String getTitle(Textmodul textmodul, String code, Locale locale) {
        String uri =
            textmodul.getLeikaTextmodul().getListURI() == null
                ? XZuFiUtil.DEFAULT_URI_TEXTMODUL
                : textmodul.getLeikaTextmodul().getListURI();
        CodelistEntry codelistEntryTextmodul = codelistService.getCodelistEntry(
            uri,
            textmodul.getLeikaTextmodul().getListVersionID(),
            code
        );
        String title = codelistEntryTextmodul.getText(locale);
        String differentTitle = XZuFiUtil.getFirstByLanguage(
            textmodul.getLeikaTextModulAbweichendeBezeichnung(),
            locale
        ).orElse(null);

        if (differentTitle != null) {
            title = differentTitle;
        }

        return title;
    }

    @Override
    public Set<String> getIncludedLanguages(Textmodul modul) {
        Set<String> languages = new HashSet<>();
        languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(modul.getInhalt()));
        languages.addAll(
            XZuFiUtil.getLocalesFromStringLocalized(modul.getLeikaTextModulAbweichendeBezeichnung())
        );
        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));
        return languages;
    }
}
