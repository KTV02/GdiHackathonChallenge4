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

import de.dataport.suchdienst.common.model.LeikaKey;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.LeikaTyping;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareLbParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageLbCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.xoev.xzufi.v2_3_1.Code;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.InformationsbereichSDG;
import de.xoev.xzufi.v2_3_1.Leistung;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * handles parsing of ServicesDescriptions
 */
@Service("xZuFiLbParserServiceX23")
@Slf4j
public class XZuFiLbParserService {

    private final LanguageAwareLbParser localeParser;
    private final LanguageLbCompleteCheckService languageLbCompleteCheckService;
    private final CodelistService codelistService;

    public XZuFiLbParserService(
        CodelistService codelistService,
        @Qualifier("languageAwareLbParserX23") LanguageAwareLbParser localeParser,
        LanguageLbCompleteCheckService languageLbCompleteCheckService
    ) {
        this.codelistService = codelistService;
        this.localeParser = localeParser;
        this.languageLbCompleteCheckService = languageLbCompleteCheckService;
    }

    public LeistungsbeschreibungVL parseLeistungen(Leistung xlb) {
        LeistungsbeschreibungVL lb = parseLeistung(xlb);
        lb.setLastUpdate(Instant.now());

        lb.setArs(
            xlb
                .getGueltigkeitGebietID()
                .stream()
                .map(Identifikator::getValue)
                .collect(Collectors.toSet())
        );
        lb.getArs()
            .stream()
            .map(ars -> StringUtils.substring(ars, 0, 2))
            .filter(StringUtils::isNotBlank)
            .forEach(ars -> lb.getArsFederalstate().add(ars));

        if (lb.getArsFederalstate().isEmpty()) {
            // FIXME Die LB hat keine Organisationseinheit und damit auch keine
            // ArsFederlstate.
            // Wenn kein ArsFederalstate vorhanden ist sagt Elasticsearch: "The ability to
            // index a suggestion with no context on a context enabled completion field is
            // deprecated and will be removed in the next major release."
            lb.getArsFederalstate().add("-");
        }
        return lb;
    }

    protected LeistungsbeschreibungVL parseLeistung(Leistung xlb) {
        log.debug("Parse Leistungsbeschreibung mit der ID {}.", xlb.getId().getValue());
        LeistungsbeschreibungVL lb = new LeistungsbeschreibungVL();
        lb.setExternalLastUpdate(XZuFiUtil.getVersionUpdateDate(xlb.getVersionsinformation()));
        lb.setId(XZuFiUtil.getIdentificatorString(xlb.getId(), IdentificationType.LB));
        lb.setLanguageCode(Locale.GERMAN.toLanguageTag());
        lb.setTyping(
            xlb
                .getTypisierung()
                .stream()
                .filter(Objects::nonNull)
                .map(t ->
                    new LeikaTyping(t.getCode(), t.getName(), t.getListURI(), t.getListVersionID())
                )
                .toList()
        );
        lb.setGemeinsamerVollzug(
            xlb
                .getKategorie()
                .stream()
                .anyMatch(
                    s ->
                        XZuFiUtil.getFirstStringLocalizedByLanguage(
                            s.getBezeichnung(),
                            Locale.GERMAN
                        ).isPresent() &&
                        XZuFiUtil.getFirstStringLocalizedByLanguage(
                            s.getBezeichnung(),
                            Locale.GERMAN
                        )
                            .get()
                            .equals(XZuFiUtil.LEISTUNG_GEMEINSAMER_VOLLZUG)
                )
        );

        lb.getLeikakeys().addAll(
            xlb
                .getReferenzLeistungsschluessel()
                .stream()
                .map(Identifikator::getValue)
                .collect(Collectors.toSet())
        );

        lb.getLeikaHierarchyKeys().addAll(
            lb
                .getLeikakeys()
                .stream()
                .filter(leikaId -> {
                    if (Strings.CS.startsWith(leikaId, "TMP")) {
                        log.debug(
                            "Ignoriere TMP-LeikaId {} in Leistung {} bei der Bildung der Leika-Hierarchie",
                            leikaId,
                            lb.getId()
                        );
                        return false;
                    } else {
                        return true;
                    }
                })
                .flatMap(leika ->
                    new LeikaKey(leika).getHierarchy().stream().map(LeikaKey::getSearchKey)
                )
                .collect(Collectors.toSet())
        );

        parseSDGInformationToLb(xlb, lb);

        lb.setAddressees(getAddressees(xlb, Locale.GERMAN));

        // Gültigkeit der Leistung
        lb.setValidityPeriods(XZuFiUtil.getValidityPeriods(xlb.getGueltigkeit()));

        // Inhalte nach Sprachen parsen
        Map<Locale, LeistungsbeschreibungContent> languages = localeParser.parse(xlb, lb);
        languages.keySet().forEach(lb.getAvailableLanguages()::add);

        // Deutsch als Standard uebernehmen
        applyGerman(languages.get(Locale.GERMAN), lb);
        languages.remove(Locale.GERMAN);
        lb.setOtherLanguages(languages);
        checkLanguagesComplete(lb, languages);

        return lb;
    }

    private void checkLanguagesComplete(
        LeistungsbeschreibungVL lb,
        Map<Locale, LeistungsbeschreibungContent> languages
    ) {
        lb.getAvailableLanguages()
            .stream()
            .filter(locale -> !Locale.GERMAN.equals(locale))
            .forEach(locale ->
                lb
                    .getLanguageComplete()
                    .put(
                        locale,
                        languageLbCompleteCheckService.checkLanguageComplete(languages, lb, locale)
                    )
            );
    }

    private void parseSDGInformationToLb(Leistung xlb, LeistungsbeschreibungVL lb) {
        InformationsbereichSDG informationsbereicheSDG = xlb.getInformationsbereicheSDG();
        if (informationsbereicheSDG == null) {
            return;
        }
        informationsbereicheSDG.getInformationsbereichSDG1().forEach(sdg -> {
            CodelistEntry entry = getEntryForSet(
                sdg,
                XZuFiUtil.DEFAULT_URI_SDGINFORMATIONSBEREICH1
            );
            lb.getSdgInformation().add(entry);
        });

        informationsbereicheSDG.getInformationsbereichSDG2().forEach(sdg -> {
            CodelistEntry entry = getEntryForSet(
                sdg,
                XZuFiUtil.DEFAULT_URI_SDGINFORMATIONSBEREICH2
            );
            lb.getSdgInformation2().add(entry);
        });
    }

    private CodelistEntry getEntryForSet(Code code, String defaultUri) {
        if (code.getName() != null && !code.getName().isEmpty()) {
            CodelistEntry entry = new CodelistEntry();
            entry.setCode(code.getCode());
            entry.setVersion(code.getListVersionID());
            entry.setText(Locale.GERMAN, code.getName()); // FIXME kann der ganze Block hier weg?
            return entry;
        } else {
            String uri = code.getListURI();
            if (uri == null || uri.isEmpty()) {
                uri = defaultUri;
            }
            return codelistService.getCodelistEntry(uri, code.getListVersionID(), code.getCode());
        }
    }

    private void applyGerman(
        LeistungsbeschreibungContent contentFromLanguages,
        LeistungsbeschreibungContent actualServiceDescription
    ) {
        if (contentFromLanguages != null && actualServiceDescription != null) {
            BusinessObjectMapper.INSTANCE.copyContent(
                contentFromLanguages,
                actualServiceDescription
            );
        }
    }

    private Set<String> getAddressees(Leistung xlb, Locale locale) {
        return xlb
            .getLeistungsadressat()
            .stream()
            .map(addressee -> {
                CodelistEntry entry = codelistService.getCodelistEntry(
                    addressee.getListURI(),
                    addressee.getListVersionID(),
                    addressee.getCode()
                );
                return entry.getText(locale);
            })
            .collect(Collectors.toSet());
    }
}
