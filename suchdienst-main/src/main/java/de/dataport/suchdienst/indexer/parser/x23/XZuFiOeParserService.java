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
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareOeParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOeCompleteCheckService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.LocaleUtils;
import de.xoev.xzufi.v2_3_1.CodeZustaendigkeitskriterium;
import de.xoev.xzufi.v2_3_1.CodeZustaendigkeitsrolle;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.Zustaendigkeitskriterium;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitskriteriumCodeErweiterbar;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * handles parsing of Online Services
 */
@Service("xZuFiOeParserServiceX23")
@Slf4j
public class XZuFiOeParserService {

    private final LanguageAwareOeParser localeParser;
    private final LanguageOeCompleteCheckService languageOeCompleteCheckService;
    private final XZuFiFoParserService xZuFiFoParserService;
    private final CodelistService codelistService;
    private final MessageSource messageSource;

    public XZuFiOeParserService(
        @Qualifier("languageAwareOeParserX23") LanguageAwareOeParser localeParser,
        LanguageOeCompleteCheckService languageOeCompleteCheckService,
        @Qualifier("xZuFiFoParserServiceX23") XZuFiFoParserService xZuFiFoParserService,
        CodelistService codelistService,
        MessageSource messageSource
    ) {
        this.localeParser = localeParser;
        this.languageOeCompleteCheckService = languageOeCompleteCheckService;
        this.xZuFiFoParserService = xZuFiFoParserService;
        this.codelistService = codelistService;
        this.messageSource = messageSource;
    }

    public OrganisationseinheitVL parseOrganisationseinheit(
        OrganisationseinheitErweitert xoe,
        Instant lastUpdate
    ) {
        log.debug("Parse Organisationseinheit mit der ID {}.", xoe.getId().getValue());

        OrganisationseinheitVL oe = new OrganisationseinheitVL();

        oe.setLastUpdate(lastUpdate);
        oe.setOeffentlichAnzeigen(xoe.isKennzeichenOeffentlichAnzeigen());
        oe.setValidityPeriods(XZuFiUtil.getValidityPeriods(xoe.getGueltigkeit()));
        oe.setId(XZuFiUtil.getIdentificatorString(xoe.getId(), IdentificationType.OE));

        // Setze geschachtelte Formulare, sie sind in sich Mehrsprachig und
        // werden deshalb außerhalb der OrganisationseinheitContent gespeichert
        List<FormularVL> nestesForms = xoe
            .getFormular()
            .stream()
            .map(xzufiFo -> {
                FormularVL formularVL = xZuFiFoParserService.parseFormular(xzufiFo);
                formularVL.setArs(XZuFiUtil.extractRelations(xzufiFo.getZustaendigkeit()));
                // extract relations
                return formularVL;
            })
            .toList();
        oe.setNestedForms(nestesForms);

        // Restliche Inhalte nach Sprachen parsen
        Map<Locale, OrganisationseinheitContent> languages = localeParser.parse(
            xoe,
            oe,
            lastUpdate
        );
        languages.keySet().forEach(oe.getLanguages()::add);

        // Deutsch als Standard uebernehmen
        shallowCopyContent(languages.get(Locale.GERMAN), oe);
        languages.remove(Locale.GERMAN);
        oe.setOtherLanguages(languages);

        checkLanguagesComplete(oe, languages);

        List<ZustaendigkeitOrganisationseinheit> zss = xoe.getZustaendigkeit();
        oe.setRelations(getRelations(zss));
        oe.setCriteria(getCriteria(zss));
        return oe;
    }

    private Map<String, List<ResponsibilityRole>> getRelations(
        List<? extends ZustaendigkeitOrganisationseinheit> relations
    ) {
        Map<String, List<ResponsibilityRole>> map = new HashMap<>();
        for (ZustaendigkeitOrganisationseinheit zs : relations) {
            for (CodeZustaendigkeitsrolle codeRolle : zs.getRolle()) {
                readRelationsFromCodeZustaendigkeitsrolle(zs, codeRolle, map);
            }
        }
        return map;
    }

    private void readRelationsFromCodeZustaendigkeitsrolle(
        ZustaendigkeitOrganisationseinheit zs,
        CodeZustaendigkeitsrolle codeRolle,
        Map<String, List<ResponsibilityRole>> map
    ) {
        String uri =
            codeRolle.getListURI() == null
                ? XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE
                : codeRolle.getListURI();
        String version =
            codeRolle.getListVersionID() == null
                ? XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE
                : codeRolle.getListVersionID();
        CodelistEntry entry = codelistService.getCodelistEntry(uri, version, codeRolle.getCode());

        ResponsibilityRole role = new ResponsibilityRole();
        role.setCode(codeRolle.getCode());
        role.setName(entry.getText(Locale.GERMAN));
        role.setUri(uri);
        role.setVersion(version);
        LocaleUtils.SUPPORTED_LANGUAGES.forEach(lang -> {
            if (!Locale.GERMAN.equals(lang)) {
                role.getOtherLanguages().put(lang, entry.getText(lang));
            }
        });
        zs.getGebietID().forEach(ars -> {
            if (map.containsKey(ars.getValue())) {
                map.get(ars.getValue()).add(role);
            } else {
                ArrayList<ResponsibilityRole> roles = new ArrayList<>();
                roles.add(role);
                map.put(ars.getValue(), roles);
            }
        });
        if (zs.getRolle().isEmpty()) {
            zs.getGebietID().forEach(ars -> map.put(ars.getValue(), List.of(createDefaultRole())));
        }
    }

    private Map<String, List<ResponsibilityCriterion>> getCriteria(
        List<? extends ZustaendigkeitOrganisationseinheit> relations
    ) {
        return relations
            .stream()
            .filter(Objects::nonNull)
            .collect(HashMap::new, this::groupCriteriaByArs, HashMap::putAll);
    }

    private void groupCriteriaByArs(
        Map<String, List<ResponsibilityCriterion>> result,
        ZustaendigkeitOrganisationseinheit zs
    ) {
        List<Zustaendigkeitskriterium> kriterien = zs.getKriterium();
        if (CollectionUtils.isEmpty(kriterien)) {
            return;
        }
        List<ResponsibilityCriterion> criteria = kriterien
            .stream()
            .filter(Objects::nonNull)
            .map(this::mapKriterium)
            .toList();
        zs.getGebietID().forEach(ars -> result.put(ars.getValue(), criteria));
    }

    private ResponsibilityCriterion mapKriterium(Zustaendigkeitskriterium kriterium) {
        ResponsibilityCriterion result = new ResponsibilityCriterion();
        ZustaendigkeitskriteriumCodeErweiterbar codeKriterium = kriterium.getCodeKriterium();
        CodeZustaendigkeitskriterium code = codeKriterium.getCode();
        if (code == null) {
            String nichtGelisteterWert = codeKriterium.getNichtGelisteterWert();
            result.setName(nichtGelisteterWert);
            result.setCode(nichtGelisteterWert);
        } else {
            String uri =
                code.getListURI() == null
                    ? XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_KRITERIUM
                    : code.getListURI();
            String version =
                code.getListVersionID() == null
                    ? XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_KRITERIUM
                    : code.getListVersionID();
            CodelistEntry entry = codelistService.getCodelistEntry(uri, version, code.getCode());

            result.setName(entry.getText(Locale.GERMAN));
            result.setCode(code.getCode());
            result.setUri(uri);
            result.setVersion(version);
            LocaleUtils.SUPPORTED_LANGUAGES.forEach(lang -> {
                if (!Locale.GERMAN.equals(lang)) {
                    result.getOtherLanguages().put(lang, entry.getText(lang));
                }
            });
        }
        result.setAuspraegungen(kriterium.getAuspraegung());
        return result;
    }

    private ResponsibilityRole createDefaultRole() {
        ResponsibilityRole role = new ResponsibilityRole();
        role.setCode("03");
        role.setName(
            messageSource.getMessage(
                "org_unit_controller_responsibility_contact_point",
                null,
                Locale.GERMAN
            )
        );
        role.setVersion(XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE);
        role.setUri(XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE);
        role.setOtherLanguages(
            Map.of(
                Locale.ENGLISH,
                messageSource.getMessage(
                    "org_unit_controller_responsibility_contact_point",
                    null,
                    Locale.ENGLISH
                )
            )
        );
        return role;
    }

    private void checkLanguagesComplete(
        OrganisationseinheitVL oe,
        Map<Locale, OrganisationseinheitContent> languages
    ) {
        oe.getLanguages()
            .stream()
            .filter(locale -> !Locale.GERMAN.equals(locale))
            .forEach(locale ->
                oe
                    .getLanguageComplete()
                    .put(
                        locale,
                        languageOeCompleteCheckService.checkLanguageComplete(languages, oe, locale)
                    )
            );
    }

    private void shallowCopyContent(
        OrganisationseinheitContent contentFromLanguages,
        OrganisationseinheitContent actualOrganisationUnit
    ) {
        if (contentFromLanguages != null && actualOrganisationUnit != null) {
            BusinessObjectMapper.INSTANCE.copyContent(contentFromLanguages, actualOrganisationUnit);
        }
    }
}
