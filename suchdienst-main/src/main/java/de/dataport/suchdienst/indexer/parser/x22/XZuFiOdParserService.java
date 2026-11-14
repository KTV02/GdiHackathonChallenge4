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

import de.dataport.pv_onlinegw.xzufi_konverter.mappings.x22_23.onlinedienst.OnlinedienstMapper;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.NationalityCode;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.parser.x22.locale.LanguageAwareOdParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOdCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.CodeStaatsangehoerigkeitDestatis;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.OnlinedienstParameterEfA;
import de.xoev.xzufi.v2_3_1.OnlinedienstParameterIndividuell;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * handles parsing of Online Services
 */
@Service("xZuFiOdParserServiceX22")
@Slf4j
public class XZuFiOdParserService {

    private final LanguageAwareOdParser localeParser;
    private final LanguageOdCompleteCheckService languageOdCompleteCheckService;
    private final CodelistService codelistService;

    public XZuFiOdParserService(
        CodelistService codelistService,
        @Qualifier("languageAwareOdParserX22") LanguageAwareOdParser localeParser,
        LanguageOdCompleteCheckService languageOdCompleteCheckService
    ) {
        this.codelistService = codelistService;
        this.localeParser = localeParser;
        this.languageOdCompleteCheckService = languageOdCompleteCheckService;
    }

    public OnlinedienstVL parseOnlinedienst(OnlinedienstErweitert xzufiOd) {
        log.debug("Parse Onlinedienst mit der ID {}.", xzufiOd.getId().getValue());
        OnlinedienstVL od = new OnlinedienstVL();
        od.setId(XZuFiUtil.getIdentificatorString(xzufiOd.getId(), IdentificationType.OD));
        od.setArs(XZuFiUtil.extractRelations(xzufiOd.getZustaendigkeit()));
        od.setLastUpdate(Instant.now());

        Set<Locale> executionLanguages = xzufiOd
            .getDurchfuehrungSprachen()
            .stream()
            .map(XZuFiUtil::getLocale)
            .collect(Collectors.toSet());
        od.setExecutionLanguages(executionLanguages);

        Set<String> logos = xzufiOd
            .getLogo()
            .stream()
            .map(Identifikator::getValue)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        od.setLogos(logos);

        od.setLevelOfConfidence(xzufiOd.getVertrauensniveau().getCode());

        Set<NationalityCode> executionNationalities = xzufiOd
            .getDurchfuehrungStaatsangehoerigkeit()
            .stream()
            .map(this::mapToIso2)
            .collect(Collectors.toSet());
        od.setExecutionNationalities(executionNationalities);

        od.setValidityPeriods(XZuFiUtil.getValidityPeriods(xzufiOd.getGueltigkeit()));

        Map<Locale, OnlinedienstContent> languages = localeParser.parseOnlineService(xzufiOd, od);
        OnlinedienstMapper parameterMapper = Mappers.getMapper(OnlinedienstMapper.class);
        XzufiParserUtils.addLocalizedCustomParameters(
            languages,
            parameterMapper.mapParamsToParamsIndividuell(xzufiOd.getParameter()),
            OnlinedienstParameterIndividuell::getLanguageCode,
            this::toParameter
        );
        XzufiParserUtils.addLocalizedEfaParameters(
            languages,
            parameterMapper.mapParamsToParamsEfa(xzufiOd.getParameter()),
            OnlinedienstParameterEfA::getLanguageCode,
            this::toParameter
        );
        languages.keySet().forEach(od.getLanguages()::add);
        od.setOtherLanguages(languages);
        applyGerman(languages.get(Locale.GERMAN), od);
        languages.remove(Locale.GERMAN);
        checkLanguageComplete(od, languages);

        return od;
    }

    private OnlinedienstParameter toParameter(OnlinedienstParameterIndividuell parameter) {
        OnlinedienstParameter result = new OnlinedienstParameter();
        result.setName(parameter.getParameterName());
        result.setValue(parameter.getParameterWert());
        return result;
    }

    private OnlinedienstParameter toParameter(OnlinedienstParameterEfA parameter) {
        OnlinedienstParameter result = new OnlinedienstParameter();
        result.setName(parameter.getParameterEfA().getCode());
        result.setValue(parameter.getParameterWert());
        return result;
    }

    private void checkLanguageComplete(
        OnlinedienstVL od,
        Map<Locale, OnlinedienstContent> languages
    ) {
        od.getLanguages()
            .stream()
            .filter(locale -> !Locale.GERMAN.equals(locale))
            .forEach(locale ->
                od
                    .getLanguageComplete()
                    .put(
                        locale,
                        languageOdCompleteCheckService.checkLanguageComplete(languages, od, locale)
                    )
            );
    }

    private void applyGerman(
        OnlinedienstContent contentFromLanguages,
        OnlinedienstContent actualOnlineService
    ) {
        if (contentFromLanguages != null && actualOnlineService != null) {
            BusinessObjectMapper.INSTANCE.copyContent(contentFromLanguages, actualOnlineService);
        }
    }

    private NationalityCode mapToIso2(CodeStaatsangehoerigkeitDestatis code) {
        CodelistEntry entry = codelistService.getCodelistEntry(
            code.getListURI(),
            code.getListVersionID(),
            code.getCode()
        );
        return entry
            .getValue("ISO-2")
            .map(CodelistEntry.CodeListValue::value)
            .map(iso -> "-".equals(iso) ? null : iso) // Codeliste enthaelt "-" bei nicht vorhandenen Werten
            .map(iso ->
                NationalityCode.builder().destatisCode(code.getCode()).iso2Code(iso).build()
            )
            .orElseGet(() -> {
                log.info("Konnte ISO-Code für {} nicht auflösen.", code);
                return NationalityCode.builder().destatisCode(code.getCode()).build();
            });
    }
}
