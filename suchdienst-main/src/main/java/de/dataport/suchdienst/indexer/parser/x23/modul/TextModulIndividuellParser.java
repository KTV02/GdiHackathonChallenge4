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
package de.dataport.suchdienst.indexer.parser.x23.modul;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.CodeTextmodultypIndividuell;
import de.xoev.xzufi.v2_3_1.TextmodulIndividuell;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("textModulIndividuellParserX23")
public class TextModulIndividuellParser
    extends ModulParser
    implements Parser<TextmodulIndividuell, ServiceModule>
{

    private final MessageSource messageSource;
    private final Set<String> ignorableDummyCodelists;

    public TextModulIndividuellParser(
        CodelistService codelistService,
        MessageSource messageSource,
        SuchdienstProperties props
    ) {
        super(codelistService);
        this.messageSource = messageSource;
        this.ignorableDummyCodelists = props.getDummyCodeLists();
    }

    @Override
    public ServiceModuleCode getCode() {
        return null;
    }

    @Override
    public ServiceModule parse(TextmodulIndividuell txtModul, Locale locale) {
        String content = XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
            txtModul.getInhalt(),
            locale
        ).orElse(null);

        CodelistEntry entry = getEntry(txtModul);

        var fallback = Optional.ofNullable(txtModul.getIndividuellesTextmodultyp())
            .flatMap(typ -> Optional.ofNullable(typ.getName()))
            .orElseGet(() ->
                messageSource.getMessage("individuell_modul_parser.fallback_title", null, locale)
            );

        List<Link> links = XZuFiUtil.getHyperlinks(txtModul.getWeiterfuehrenderLink(), locale);

        return ServiceModule.builder()
            .title(entry.isNonExistent() ? fallback : entry.getText(locale))
            .text(content)
            .links(links)
            .validityPeriods(XZuFiUtil.getValidityPeriods(txtModul.getGueltigkeit()))
            .accessChannel(getAccessChannel(txtModul))
            .build();
    }

    private CodelistEntry getEntry(TextmodulIndividuell txtModul) {
        CodeTextmodultypIndividuell individuell = txtModul.getIndividuellesTextmodultyp();
        if (
            this.ignorableDummyCodelists.contains(
                individuell.getListURI() + "_" + individuell.getListVersionID()
            )
        ) {
            // dummy codes nicht aufloesen
            return CodelistEntry.empty(
                individuell.getCode(),
                individuell.getListURI(),
                individuell.getListVersionID(),
                ErrorTypeCodelist.LIST
            );
        }
        return codelistService.getCodelistEntry(
            individuell.getListURI(),
            individuell.getListVersionID(),
            individuell.getCode()
        );
    }

    @Override
    public Set<String> getIncludedLanguages(TextmodulIndividuell modul) {
        Set<String> languages = new HashSet<>();
        languages.addAll(XZuFiUtil.getLocalesFromStringLocalizedHTML(modul.getInhalt()));
        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));
        return languages;
    }
}
