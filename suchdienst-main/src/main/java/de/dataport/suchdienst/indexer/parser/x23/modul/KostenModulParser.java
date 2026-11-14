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

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.Kosten;
import de.xoev.xzufi.v2_3_1.Kostenmodul;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("kostenModulParserX23")
public class KostenModulParser
    extends ModulParser
    implements Parser<Kostenmodul, ServiceModuleDetail>
{

    private final MessageSource messageSource;

    public KostenModulParser(CodelistService codelistService, MessageSource messageSource) {
        super(codelistService);
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.KOSTEN;
    }

    @Override
    public ServiceModuleDetail parse(Kostenmodul modul, Locale locale) {
        List<ServiceModuleDetailContent> detailsCosts = modul
            .getKosten()
            .stream()
            .sorted(
                Comparator.comparing(
                    Kosten::getPositionDarstellung,
                    Comparator.comparing(h -> Optional.ofNullable(h).orElse(0))
                )
            )
            .map(costList -> mapCostsToModule(costList, locale))
            .toList();

        ServiceModuleDetail module = ServiceModuleDetail.builder()
            .title(messageSource.getMessage("kosten_modul_parser.title", null, locale))
            .text(
                XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
                    modul.getBeschreibung(),
                    locale
                ).orElse("")
            )
            .details(detailsCosts)
            .validityPeriods(XZuFiUtil.getValidityPeriods(modul.getGueltigkeit()))
            .links(XZuFiUtil.getHyperlinks(modul.getWeiterfuehrenderLink(), locale))
            .accessChannel(getAccessChannel(modul))
            .build();

        // Es wurde ein leeres Kostenmodul übertragen
        if (
            StringUtils.isEmpty(module.getText()) &&
            detailsCosts.isEmpty() &&
            module.getLinks().isEmpty()
        ) {
            return null;
        }

        return module;
    }

    @Override
    public Set<String> getIncludedLanguages(Kostenmodul modul) {
        Set<String> languages = new HashSet<>();
        languages.addAll(XZuFiUtil.getLocalesFromStringLocalizedHTML(modul.getBeschreibung()));
        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));
        modul.getKosten().forEach(k -> {
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalizedHTML(k.getBeschreibung()));
            if (k.getKostenauswahl() != null && k.getKostenauswahl().getKostenFrei() != null) {
                languages.addAll(
                    XZuFiUtil.getLocalesFromStringLocalized(
                        k.getKostenauswahl().getKostenFrei().getBeschreibungKostenfreiheit()
                    )
                );
            }
            if (k.getKostenauswahl() != null && k.getKostenauswahl().getKostenVariabel() != null) {
                languages.addAll(
                    XZuFiUtil.getLocalesFromStringLocalized(
                        k.getKostenauswahl().getKostenVariabel().getBeschreibungVariabilitaet()
                    )
                );
            }
        });
        return languages;
    }

    private ServiceModuleDetailContent mapCostsToModule(Kosten costList, Locale locale) {
        List<Link> links = new ArrayList<>();
        List<KeyValueObject> additionalInformation = new ArrayList<>();
        String description = XZuFiUtil.getStringLocalizedHTMLByLanguage(
            costList.getBeschreibung(),
            locale
        )
            .stream()
            .findFirst()
            .orElse(null);

        if (costList.getLinkKostenbildung() != null) {
            costList
                .getLinkKostenbildung()
                .stream()
                .map(hl ->
                    Link.of(
                        hl.getUri(),
                        messageSource.getMessage(
                            "kosten_modul_parser.additionalLink_title",
                            null,
                            locale
                        ),
                        messageSource.getMessage(
                            "kosten_modul_parser.additionalLink_description",
                            null,
                            locale
                        )
                    )
                )
                .forEach(links::add);
        }

        String title = getTitle(costList, locale);

        if (costList.getKostenauswahl().getKostenVariabel() != null) {
            XZuFiUtil.getFirstStringLocalizedByLanguage(
                costList.getKostenauswahl().getKostenVariabel().getBeschreibungVariabilitaet(),
                locale
            ).ifPresent(descriptionVariable ->
                additionalInformation.add(new KeyValueObject(null, descriptionVariable))
            );
        }

        var builderDetailType = ServiceModuleDetailContent.builder()
            .title(title)
            .text(description)
            .links(links);

        if (costList.isVorkasse() != null && costList.isVorkasse()) {
            additionalInformation.add(
                new KeyValueObject(
                    messageSource.getMessage(
                        "kosten_modul_parser.additional_information_key_name",
                        null,
                        locale
                    ),
                    messageSource.getMessage(
                        "kosten_modul_parser.additional_information_value_positive",
                        null,
                        locale
                    )
                )
            );
        } else if (costList.isVorkasse() != null && !costList.isVorkasse()) {
            additionalInformation.add(
                new KeyValueObject(
                    messageSource.getMessage(
                        "kosten_modul_parser.additional_information_key_name",
                        null,
                        locale
                    ),
                    messageSource.getMessage(
                        "kosten_modul_parser.additional_information_value_negative",
                        null,
                        locale
                    )
                )
            );
        }

        builderDetailType.additionalInformation(additionalInformation);

        return builderDetailType.build();
    }

    private String getTitle(Kosten costList, Locale locale) {
        StringBuilder title = new StringBuilder();
        addKostenTypToTitle(costList, locale, title);
        addKostenauswahlToTitle(costList, locale, title);
        return title.toString();
    }

    private void addKostenauswahlToTitle(Kosten costList, Locale locale, StringBuilder title) {
        if (costList.getKostenauswahl().getKostenFix() != null) {
            title.append(
                XZuFiUtil.mapAmount(costList.getKostenauswahl().getKostenFix().getBetrag(), locale)
            );
        } else if (costList.getKostenauswahl().getKostenFrei() != null) {
            List<String> titles = XZuFiUtil.getStringLocalizedByLanguage(
                costList.getKostenauswahl().getKostenFrei().getBeschreibungKostenfreiheit(),
                locale
            );
            if (!titles.isEmpty()) {
                title.append(titles.getFirst());
            } else {
                title.append(
                    messageSource.getMessage("kosten_modul_parser.no_charge", null, locale)
                );
            }
        } else if (costList.getKostenauswahl().getKostenVariabel() != null) {
            title.append(getCostsVariable(costList, locale));
        }
    }

    private void addKostenTypToTitle(Kosten costList, Locale locale, StringBuilder title) {
        if (
            costList.getTyp().getCode() == null || costList.getTyp().getCode().getCode().isEmpty()
        ) {
            title.append(costList.getTyp().getNichtGelisteterWert()).append(": ");
        } else {
            String uri =
                costList.getTyp().getCode().getListURI() == null
                    ? XZuFiUtil.DEFAULT_URI_KOSTENTYP
                    : costList.getTyp().getCode().getListURI();
            String version =
                costList.getTyp().getCode().getListVersionID() == null
                    ? XZuFiUtil.DEFAULT_VERSION_KOSTENTYP
                    : costList.getTyp().getCode().getListVersionID();
            CodelistEntry codelistEntry = codelistService.getCodelistEntry(
                uri,
                version,
                costList.getTyp().getCode().getCode()
            );
            if (codelistEntry.getCode() == null) {
                title.append(costList.getTyp().getNichtGelisteterWert()).append(": ");
            } else {
                title.append(codelistEntry.getText(locale)).append(": ");
            }
        }
    }

    private String getCostsVariable(Kosten costs, Locale locale) {
        String value = "";
        if (
            costs.getKostenauswahl().getKostenVariabel().getBetragUntergrenze() == null &&
            costs.getKostenauswahl().getKostenVariabel().getBetragObergrenze() != null
        ) {
            value = messageSource.getMessage(
                "kosten_modul_parser.upper_limit",
                new String[] {
                    XZuFiUtil.mapAmount(
                        costs.getKostenauswahl().getKostenVariabel().getBetragObergrenze(),
                        locale
                    ),
                },
                locale
            );
        } else if (
            costs.getKostenauswahl().getKostenVariabel().getBetragUntergrenze() != null &&
            costs.getKostenauswahl().getKostenVariabel().getBetragObergrenze() == null
        ) {
            value = messageSource.getMessage(
                "kosten_modul_parser.lower_limit",
                new String[] {
                    XZuFiUtil.mapAmount(
                        costs.getKostenauswahl().getKostenVariabel().getBetragUntergrenze(),
                        locale
                    ),
                },
                locale
            );
        } else if (
            costs.getKostenauswahl().getKostenVariabel().getBetragUntergrenze() != null &&
            costs.getKostenauswahl().getKostenVariabel().getBetragObergrenze() != null
        ) {
            value =
                XZuFiUtil.mapAmount(
                    costs.getKostenauswahl().getKostenVariabel().getBetragUntergrenze(),
                    locale
                ) +
                " - " +
                XZuFiUtil.mapAmount(
                    costs.getKostenauswahl().getKostenVariabel().getBetragObergrenze(),
                    locale
                );
        }
        return value;
    }
}
