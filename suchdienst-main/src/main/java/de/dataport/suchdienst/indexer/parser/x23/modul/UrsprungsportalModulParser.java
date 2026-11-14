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

import de.dataport.suchdienst.indexer.model.mapping.DynamischerParameter;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.UrsprungsportalModul;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("ursprungsportalModulParserX23")
public class UrsprungsportalModulParser implements Parser<UrsprungsportalModul, ServiceModuleLink> {

    private final MessageSource messageSource;

    public UrsprungsportalModulParser(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleLink parse(UrsprungsportalModul module, Locale locale) {
        if (!getIncludedLanguages(module).contains(locale.toLanguageTag())) {
            return null;
        }
        List<DynamischerParameter> list = module
            .getDynamischeParameter()
            .stream()
            .filter(Objects::nonNull)
            .map(dp -> new DynamischerParameter(dp.getTyp().getCode(), dp.getParameterName()))
            .toList();
        LinkDynamic additionalLink = LinkDynamic.of(module.getUri(), module.getTitel(), null, list);

        return ServiceModuleLink.builder()
            .title(messageSource.getMessage("ursprungsportal_parser.title", null, locale))
            .link(additionalLink)
            .validityPeriods(XZuFiUtil.getValidityPeriods(module.getGueltigkeit()))
            .build();
    }

    @Override
    public Set<String> getIncludedLanguages(UrsprungsportalModul module) {
        return Set.of(
            module.getLanguageCode() != null
                ? module.getLanguageCode()
                : Locale.GERMAN.toLanguageTag()
        );
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.URSPRUNGSPORTAL;
    }
}
