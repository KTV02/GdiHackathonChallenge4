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
package de.dataport.suchdienst.indexer.parser.x23;

import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularFileLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareFoParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageFoCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.xoev.xzufi.v2_3_1.Formular;
import de.xoev.xzufi.v2_3_1.FormulardateiLink;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Parst XZufi Formulare in die interne Datenstruktur: FormularVL.
 */
@Service("xZuFiFoParserServiceX23")
@Slf4j
public class XZuFiFoParserService {

    private final LanguageAwareFoParser localeParser;
    private final LanguageFoCompleteCheckService languageFoCompleteCheckService;

    public XZuFiFoParserService(
        @Qualifier("languageAwareFoParserX23") LanguageAwareFoParser localeParser,
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
        assignFileLinksByLanguage(xzufiFo, targetFormular, contentByLocale);
        removeLanguagesWithoutFileLinks(targetFormular, contentByLocale);
        contentByLocale.keySet().forEach(targetFormular.getLanguages()::add);
        targetFormular.setOtherLanguages(contentByLocale);
        FormularContent germanContent = contentByLocale.get(Locale.GERMAN);
        if (germanContent != null) {
            BusinessObjectMapper.INSTANCE.copyContent(germanContent, targetFormular);
        }
        contentByLocale.remove(Locale.GERMAN);
        checkLanguageComplete(targetFormular, contentByLocale);
    }

    private void assignFileLinksByLanguage(
        Formular xzufiFo,
        FormularVL targetFormular,
        Map<Locale, FormularContent> contentByLocale
    ) {
        xzufiFo
            .getDatei()
            .stream()
            .filter(FormulardateiLink.class::isInstance)
            .map(FormulardateiLink.class::cast)
            .filter(fdl -> fdl.getLink() != null)
            .forEach(fdl -> {
                String languageCode = fdl.getLink().getLanguageCode();
                if (StringUtils.isBlank(languageCode)) {
                    addFileLinkToAllLanguages(fdl, contentByLocale);
                    return;
                }

                Locale locale = XzufiParserUtils.getSupportedLocale(languageCode);
                FormularContent content = contentByLocale.get(locale);
                if (content == null) {
                    log.debug(
                        "Ignoriere Dateilink des Formulars {} fuer Sprache {}, da das Formular keine passende Sprachvariante besitzt.",
                        targetFormular.getId(),
                        locale
                    );
                    return;
                }

                content.addFileLink(fdl.getMimeCode(), fdl.getLink().getUri());
            });
    }

    private void addFileLinkToAllLanguages(
        FormulardateiLink fileLink,
        Map<Locale, FormularContent> contentByLocale
    ) {
        contentByLocale
            .values()
            .forEach(content ->
                content.addFileLink(fileLink.getMimeCode(), fileLink.getLink().getUri())
            );
    }

    private void removeLanguagesWithoutFileLinks(
        FormularVL targetFormular,
        Map<Locale, FormularContent> contentByLocale
    ) {
        contentByLocale.entrySet().removeIf(entry -> {
            boolean remove = entry
                .getValue()
                .getFileLinks()
                .stream()
                .map(FormularFileLink::getUri)
                .allMatch(Objects::isNull);
            if (remove) {
                log.debug(
                    "Formular {} wird fuer Sprache {} verworfen, da kein passender Dateilink vorliegt.",
                    targetFormular.getId(),
                    entry.getKey()
                );
            }
            return remove;
        });
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
