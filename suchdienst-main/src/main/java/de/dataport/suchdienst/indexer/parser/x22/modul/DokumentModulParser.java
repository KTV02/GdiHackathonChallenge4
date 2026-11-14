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
import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_2_0.Dokument;
import de.xoev.xzufi.v2_2_0.Dokumentmodul;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("dokumentModulParserX22")
public class DokumentModulParser implements Parser<Dokumentmodul, ServiceModuleDetail> {

    private final MessageSource messageSource;
    private final CodelistService codelistService;

    public DokumentModulParser(CodelistService codelistService, MessageSource messageSource) {
        this.codelistService = codelistService;
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.DOKUMENT;
    }

    @Override
    public ServiceModuleDetail parse(Dokumentmodul moduleDocument, Locale locale) {
        List<ServiceModuleDetailContent> blockDocumentDetails = new ArrayList<>();

        moduleDocument.getDokumentEingehend().forEach(d -> {
            var builder = ServiceModuleDetailContent.builder();
            builder
                .title(
                    messageSource.getMessage(
                        "dokument_modul_parser.dokument_eingehend_title",
                        new String[] {
                            XZuFiUtil.getFirstByLanguage(d.getBezeichnung(), locale).orElse(null),
                        },
                        locale
                    )
                )
                .text(XZuFiUtil.getFirstByLanguage(d.getBeschreibung(), locale).orElse(""));

            builder.additionalInformation(getAdditionalInfo(d, locale));
            blockDocumentDetails.add(builder.build());
        });

        moduleDocument.getDokumentAusgehend().forEach(d -> {
            var builder = ServiceModuleDetailContent.builder();
            builder
                .title(
                    messageSource.getMessage(
                        "dokument_modul_parser.dokument_ausgehend_title",
                        new String[] {
                            XZuFiUtil.getFirstByLanguage(d.getBezeichnung(), locale).orElse(""),
                        },
                        locale
                    )
                )
                .text(XZuFiUtil.getFirstByLanguage(d.getBeschreibung(), locale).orElse(""));

            builder.additionalInformation(getAdditionalInfo(d, locale));
            blockDocumentDetails.add(builder.build());
        });

        List<Link> links = XZuFiUtil.getHyperlinks(
            moduleDocument.getWeiterfuehrenderLink(),
            locale
        );

        return ServiceModuleDetail.builder()
            .title(messageSource.getMessage("dokument_modul_parser.title", null, locale))
            .text(XZuFiUtil.getFirstByLanguage(moduleDocument.getBeschreibung(), locale).orElse(""))
            .links(links)
            .validityPeriods(XZuFiUtil.getValidityPeriods(moduleDocument.getGueltigkeit()))
            .details(blockDocumentDetails)
            .build();
    }

    private List<KeyValueObject> getAdditionalInfo(Dokument d, Locale locale) {
        List<KeyValueObject> additionalInformation = new ArrayList<>();

        String uriDokumenttyp =
            d.getTyp().getCode().getListURI() == null
                ? XZuFiUtil.DEFAULT_URI_DOKUMENTTYP
                : d.getTyp().getCode().getListURI();
        CodelistEntry codelistEntryTyp = codelistService.getCodelistEntry(
            uriDokumenttyp,
            d.getTyp().getCode().getListVersionID(),
            d.getTyp().getCode().getCode()
        );
        additionalInformation.add(
            new KeyValueObject(
                messageSource.getMessage(
                    "dokument_modul_parser.additional_information_dokumententyp_key_name",
                    null,
                    locale
                ),
                codelistEntryTyp.getText(locale)
            )
        );

        if (d.getAngabeSignatur() != null) {
            String uri =
                d.getAngabeSignatur().getListURI() == null
                    ? XZuFiUtil.DEFAULT_URI_SIGNATUR
                    : d.getAngabeSignatur().getListURI();
            String versionId =
                d.getAngabeSignatur().getListVersionID() == null
                    ? XZuFiUtil.DEFAULT_VERSION_SIGNATUR
                    : d.getAngabeSignatur().getListVersionID();

            CodelistEntry codelistEntry = codelistService.getCodelistEntry(
                uri,
                versionId,
                d.getAngabeSignatur().getCode()
            );
            String signature =
                d.getAngabeSignatur().getName() == null
                    ? codelistEntry.getText(locale)
                    : d.getAngabeSignatur().getName();
            additionalInformation.add(
                new KeyValueObject(
                    messageSource.getMessage(
                        "dokument_modul_parser.additional_information_erforderlichesignatur_key_name",
                        null,
                        locale
                    ),
                    signature
                )
            );
        }

        return additionalInformation;
    }

    @Override
    public Set<String> getIncludedLanguages(Dokumentmodul modul) {
        Set<String> languages = new HashSet<>(
            XZuFiUtil.getLocalesFromStringLocalized(modul.getBeschreibung())
        );

        modul.getDokumentAusgehend().forEach(d -> {
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(d.getBeschreibung()));
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(d.getBezeichnung()));
        });

        modul.getDokumentEingehend().forEach(d -> {
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(d.getBeschreibung()));
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(d.getBezeichnung()));
        });

        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));

        return languages;
    }
}
