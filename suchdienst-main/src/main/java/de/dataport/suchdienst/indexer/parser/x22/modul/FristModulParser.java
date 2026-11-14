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
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_2_0.FristMitTyp;
import de.xoev.xzufi.v2_2_0.Fristmodul;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("fristModulParserX22")
public class FristModulParser implements Parser<Fristmodul, ServiceModuleDetail> {

    private final MessageSource messageSource;
    private final CodelistService codelistService;
    private final FristHelper fristHelper;

    public FristModulParser(
        CodelistService codelistService,
        @Qualifier("fristHelperX22") FristHelper fristHelper,
        MessageSource messageSource
    ) {
        this.codelistService = codelistService;
        this.fristHelper = fristHelper;
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.FRIST;
    }

    @Override
    public ServiceModuleDetail parse(Fristmodul moduleFrist, Locale locale) {
        List<ServiceModuleDetailContent> blockDetailsFrist = new ArrayList<>();

        String description = XZuFiUtil.getFirstByLanguage(
            moduleFrist.getBeschreibung(),
            locale
        ).orElse("");
        List<Link> links = moduleFrist
            .getWeiterfuehrenderLink()
            .stream()
            .filter(l -> XZuFiUtil.isLanguageOrCountry(locale, l.getLanguageCode()))
            .map(l -> Link.of(l.getUri(), l.getTitel(), l.getBeschreibung()))
            .toList();

        for (FristMitTyp frist : moduleFrist.getFrist()) {
            ServiceModuleDetailContent serviceModuleDetailContent = createServiceModulContent(
                locale,
                frist
            );
            blockDetailsFrist.add(serviceModuleDetailContent);
        }

        return ServiceModuleDetail.builder()
            .title(messageSource.getMessage("frist_modul_parser.title", null, locale))
            .text(description)
            .links(links)
            .validityPeriods(XZuFiUtil.getValidityPeriods(moduleFrist.getGueltigkeit()))
            .details(blockDetailsFrist)
            .build();
    }

    private ServiceModuleDetailContent createServiceModulContent(Locale locale, FristMitTyp frist) {
        var builder = ServiceModuleDetailContent.builder();

        String descriptionDetail = XZuFiUtil.getFirstByLanguage(
            frist.getBeschreibung(),
            locale
        ).orElse("");
        descriptionDetail += descriptionDetail.isEmpty() ? "" : ": ";

        if (frist.getFristauswahl() != null) {
            Optional<String> dauer = fristHelper.calculateDauer(frist.getFristauswahl(), locale);
            if (dauer.isPresent()) {
                descriptionDetail += dauer.get();
            }
        }
        builder.text(descriptionDetail);

        if (frist.getTyp().getCode() == null) {
            String nichtGelisteterWert = frist.getTyp().getNichtGelisteterWert();
            builder.title(nichtGelisteterWert);
        } else {
            String uri =
                frist.getTyp().getCode().getListURI() == null
                    ? XZuFiUtil.DEFAULT_URI_FRISTTYP
                    : frist.getTyp().getCode().getListURI();
            String version =
                frist.getTyp().getCode().getListVersionID() == null
                    ? XZuFiUtil.DEFAULT_VERSION_FRISTTYP
                    : frist.getTyp().getCode().getListVersionID();

            CodelistEntry codelistEntry = codelistService.getCodelistEntry(
                uri,
                version,
                frist.getTyp().getCode().getCode()
            );

            builder.title(codelistEntry.getText(locale));
        }

        return builder.build();
    }

    @Override
    public Set<String> getIncludedLanguages(Fristmodul modul) {
        Set<String> languages = new HashSet<>(
            XZuFiUtil.getLocalesFromStringLocalized(modul.getBeschreibung())
        );
        modul
            .getFrist()
            .forEach(f ->
                languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(f.getBeschreibung()))
            );
        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));
        return languages;
    }
}
