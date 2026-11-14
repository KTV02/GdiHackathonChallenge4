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
package de.dataport.suchdienst.indexer.parser.x22;

import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularFileLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareFoParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageFoCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.Formular;
import de.xoev.xzufi.v2_2_0.FormulardateiLink;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Parst XZufi Formulare in die interne Datenstruktur: FormularVL.
 */
@Service("xZuFiFoParserServiceX22")
@Slf4j
public class XZuFiFoParserService {

    private final LanguageAwareFoParser localeParser;
    private final LanguageFoCompleteCheckService languageFoCompleteCheckService;

    public XZuFiFoParserService(
        @Qualifier("languageAwareFoParserX22") LanguageAwareFoParser localeParser,
        LanguageFoCompleteCheckService languageFoCompleteCheckService
    ) {
        this.localeParser = localeParser;
        this.languageFoCompleteCheckService = languageFoCompleteCheckService;
    }

    public FormularVL parseFormular(Formular xzufiFo) {
        log.debug("Parse Formular mit der ID {}.", xzufiFo.getId().getValue());
        FormularVL fo = new FormularVL();
        fo.setId(XZuFiUtil.getIdentificatorString(xzufiFo.getId(), IdentificationType.FO));
        fo.setArs(XZuFiUtil.extractRelations(xzufiFo.getZustaendigkeit()));
        fo.setLastUpdate(Instant.now());
        fo.setValidityPeriods(XZuFiUtil.getValidityPeriods(xzufiFo.getGueltigkeit()));

        parseLanguages(xzufiFo, fo);
        return fo;
    }

    /**
     * Parst die sprachabhängigen Felder des Formulars, setzt die deutsche Variante davon als
     * Member in das Formular (Superklasse FormularContent).
     * Setzt "otherLanguages" und "languageComplete".
     */
    private void parseLanguages(Formular xzufiFo, FormularVL targetFormular) {
        Map<Locale, FormularContent> contentByLocale = localeParser.parseFormular(
            xzufiFo,
            targetFormular
        );
        List<FormularFileLink> fileLinks = parseFileLinks(xzufiFo);
        contentByLocale.values().forEach(content -> content.setFileLinks(fileLinks));
        contentByLocale.keySet().forEach(targetFormular.getLanguages()::add);
        targetFormular.setOtherLanguages(contentByLocale);
        FormularContent germanContent = contentByLocale.get(Locale.GERMAN);
        if (germanContent != null) {
            BusinessObjectMapper.INSTANCE.copyContent(germanContent, targetFormular);
        }
        contentByLocale.remove(Locale.GERMAN);
        checkLanguageComplete(targetFormular, contentByLocale);
    }

    private List<FormularFileLink> parseFileLinks(Formular xzufiFo) {
        return xzufiFo
            .getDatei()
            .stream()
            .filter(FormulardateiLink.class::isInstance)
            .map(FormulardateiLink.class::cast)
            .map(fdl -> new FormularFileLink(fdl.getMimeCode(), fdl.getUri()))
            .toList();
    }

    private void checkLanguageComplete(FormularVL fo, Map<Locale, FormularContent> languages) {
        fo.getLanguages()
            .stream()
            .filter(locale -> !Locale.GERMAN.equals(locale))
            .forEach(locale ->
                fo
                    .getLanguageComplete()
                    .put(
                        locale,
                        languageFoCompleteCheckService.checkLanguageComplete(languages, fo, locale)
                    )
            );
    }
}
