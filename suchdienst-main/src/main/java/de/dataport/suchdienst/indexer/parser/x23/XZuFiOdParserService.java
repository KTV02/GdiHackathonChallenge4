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

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.OnlinedienstStatus;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.NationalityCode;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareOdParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOdCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.xoev.xzufi.v2_3_1.Bild;
import de.xoev.xzufi.v2_3_1.CodeStaatsangehoerigkeitDestatis;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.OnlinedienstParameterEfA;
import de.xoev.xzufi.v2_3_1.OnlinedienstParameterIndividuell;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * handles parsing of Online Services
 */
@Service("xZuFiOdParserServiceX23")
@Slf4j
public class XZuFiOdParserService {

    private final LanguageAwareOdParser localeParser;
    private final LanguageOdCompleteCheckService languageOdCompleteCheckService;
    private final CodelistService codelistService;

    public XZuFiOdParserService(
        CodelistService codelistService,
        @Qualifier("languageAwareOdParserX23") LanguageAwareOdParser localeParser,
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
        od.setOeffentlichAnzeigen(xzufiOd.isKennzeichenOeffentlichAnzeigen());

        Set<Locale> executionLanguages = xzufiOd
            .getDurchfuehrungWeitereSprachen()
            .stream()
            .map(XZuFiUtil::getLocale)
            .collect(Collectors.toSet());

        od.setExecutionLanguages(executionLanguages);
        od.setLogos(getLogos(xzufiOd.getLogo()));
        od.setLevelOfConfidence(xzufiOd.getVertrauensniveau().getCode());

        if (xzufiOd.getStatus() == null) {
            log.warn("Onlinedienst {} enthält keinen Onlinedienststatus.", od.getId());
        } else {
            CodelistKey status = new CodelistKey(
                xzufiOd.getStatus().getListURI(),
                xzufiOd.getStatus().getListVersionID(),
                xzufiOd.getStatus().getCode()
            );
            od.setOnlinedienstStatus(status);
            if (!OnlinedienstStatus.isKnown(status)) {
                log.warn(
                    "Onlinedienst '{}' mit unbekannter URI/Version/Code '{}' Kombination im Feld Onlinedienststatus entdeckt, Prüfung auf neue Codelisten-Version erforderlich",
                    od.getId(),
                    status
                );
            }
        }

        Set<NationalityCode> executionNationalities = xzufiOd
            .getDurchfuehrungStaatsangehoerigkeit()
            .stream()
            .map(this::mapToIso2)
            .collect(Collectors.toSet());
        od.setExecutionNationalities(executionNationalities);

        od.setValidityPeriods(XZuFiUtil.getValidityPeriods(xzufiOd.getGueltigkeit()));

        Map<Locale, OnlinedienstContent> languages = localeParser.parseOnlineService(xzufiOd, od);
        XzufiParserUtils.addLocalizedCustomParameters(
            languages,
            xzufiOd.getParameterIndividuell(),
            OnlinedienstParameterIndividuell::getLanguageCode,
            this::toParameter
        );
        XzufiParserUtils.addLocalizedEfaParameters(
            languages,
            xzufiOd.getParameterEfA(),
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

    private Set<String> getLogos(Bild bild) {
        var stringRef = Optional.ofNullable(bild).flatMap(b ->
            Optional.ofNullable(b.getDateiReferenz()).flatMap(ref ->
                Optional.ofNullable(ref.getDateiID())
                    .flatMap(id -> Optional.ofNullable(id.getValue()))
                    .or(() -> Optional.ofNullable(ref.getDateiURL()))
            )
        );

        return stringRef.map(Set::of).orElse(Collections.emptySet());
    }
}
