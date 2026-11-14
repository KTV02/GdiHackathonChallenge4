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
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.parser.CompositeParser;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.Dokument;
import de.xoev.xzufi.v2_3_1.Dokumentmodul;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("dokumentModulParserX23")
public class DokumentModulParser
    extends ModulParser
    implements CompositeParser<Dokumentmodul, ServiceModuleDetail>
{

    private final MessageSource messageSource;

    public DokumentModulParser(CodelistService codelistService, MessageSource messageSource) {
        super(codelistService);
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
            Dokument dokument = d.getDokument();
            builder
                .title(
                    messageSource.getMessage(
                        "dokument_modul_parser.dokument_eingehend_title",
                        new String[] {
                            XZuFiUtil.getFirstStringLocalizedByLanguage(
                                dokument.getBezeichnung(),
                                locale
                            ).orElse(null),
                        },
                        locale
                    )
                )
                .text(
                    XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
                        dokument.getBeschreibung(),
                        locale
                    ).orElse("")
                );

            builder.additionalInformation(getAdditionalInfo(dokument, locale));
            blockDocumentDetails.add(builder.build());
        });

        moduleDocument.getDokumentAusgehend().forEach(d -> {
            var builder = ServiceModuleDetailContent.builder();
            builder
                .title(
                    messageSource.getMessage(
                        "dokument_modul_parser.dokument_ausgehend_title",
                        new String[] {
                            XZuFiUtil.getFirstStringLocalizedByLanguage(
                                d.getBezeichnung(),
                                locale
                            ).orElse(""),
                        },
                        locale
                    )
                )
                .text(
                    XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
                        d.getBeschreibung(),
                        locale
                    ).orElse("")
                );

            builder.additionalInformation(getAdditionalInfo(d, locale));
            blockDocumentDetails.add(builder.build());
        });

        List<Link> links = new ArrayList<>();
        //Falls beschreibungEingehend gefüllt ist, werden die Links in das erzeugte 08-Modul (erforderliche Unterlagen) übernommen.Falls nicht, werden die Links ins Dokumentmodul übernommen.
        if (moduleDocument.getBeschreibungEingehend().isEmpty()) {
            links = XZuFiUtil.getHyperlinks(moduleDocument.getWeiterfuehrenderLink(), locale);
        }

        return ServiceModuleDetail.builder()
            .title(messageSource.getMessage("dokument_modul_parser.title", null, locale))
            .links(links)
            .validityPeriods(XZuFiUtil.getValidityPeriods(moduleDocument.getGueltigkeit()))
            .details(blockDocumentDetails)
            .accessChannel(getAccessChannel(moduleDocument))
            .build();
    }

    @Override
    public List<Map.Entry<ServiceModuleCode, AbstractServiceModule>> parseComposite(
        Dokumentmodul moduleDocument,
        Locale locale
    ) {
        List<Map.Entry<ServiceModuleCode, AbstractServiceModule>> results = new ArrayList<>();

        ServiceModuleDetail detail = parse(moduleDocument, locale);
        if (detail != null) {
            results.add(Map.entry(ServiceModuleCode.DOKUMENT, detail));
        }

        ServiceModule textModule = parseTextModule(moduleDocument, locale);
        if (textModule != null && textModule.hasContent()) {
            results.add(Map.entry(ServiceModuleCode.ERFORDERLICHE_UNTERLAGEN, textModule));
        }

        return results;
    }

    private ServiceModule parseTextModule(Dokumentmodul moduleDocument, Locale locale) {
        if (moduleDocument.getBeschreibungEingehend().isEmpty()) {
            return null;
        }

        String text = XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
            moduleDocument.getBeschreibungEingehend(),
            locale
        ).orElse("");

        List<Link> links = XZuFiUtil.getHyperlinks(
            moduleDocument.getWeiterfuehrenderLink(),
            locale
        );

        return ServiceModule.builder()
            .title(messageSource.getMessage("dokument_modul_parser.title", null, locale))
            .text(text)
            .links(links)
            .validityPeriods(XZuFiUtil.getValidityPeriods(moduleDocument.getGueltigkeit()))
            .accessChannel(getAccessChannel(moduleDocument))
            .build();
    }

    private List<KeyValueObject> getAdditionalInfo(Dokument d, Locale locale) {
        List<KeyValueObject> additionalInformation = new ArrayList<>();
        //TODO: laut Spezifikation kann hier statt Code auch ein nichtGelisteterWert geliefert werden, wird das ausgeschlossen oder führt das zu einer Exception? Gleiches gilt für den 22-Parser.
        //außerdem enthält die Liste teilweise vollkommen andere Codes als die 2.3 Liste. Soll hier noch konvertiert werden?
        String uriDokumenttyp =
            d.getArt().getCode().getListURI() == null
                ? XZuFiUtil.DEFAULT_URI_DOKUMENTART
                : d.getArt().getCode().getListURI();
        CodelistEntry codelistEntryTyp = codelistService.getCodelistEntry(
            uriDokumenttyp,
            d.getArt().getCode().getListVersionID(),
            d.getArt().getCode().getCode()
        );
        //TODO: es ist zu entscheiden, ob hier auf die neue Bezeichnung "Dokumentart" zu wechseln ist.
        // Dies könnte in SuD-Clients zu Problemen führen, sofern dort entsprechende Logik implementiert ist.
        // Im SuD-Frontend wäre eine Umbenennung aber unproblematisch - wie eine schnelle Code-Analyse gezeigt hat.
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
            XZuFiUtil.getLocalesFromStringLocalizedHTML(modul.getBeschreibungEingehend())
        );
        languages.addAll(
            XZuFiUtil.getLocalesFromStringLocalizedHTML(modul.getBeschreibungAusgehend())
        );

        modul.getDokumentAusgehend().forEach(d -> {
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalizedHTML(d.getBeschreibung()));
            languages.addAll(XZuFiUtil.getLocalesFromStringLocalized(d.getBezeichnung()));
        });

        modul.getDokumentEingehend().forEach(d -> {
            languages.addAll(
                XZuFiUtil.getLocalesFromStringLocalizedHTML(d.getDokument().getBeschreibung())
            );
            languages.addAll(
                XZuFiUtil.getLocalesFromStringLocalized(d.getDokument().getBezeichnung())
            );
        });

        languages.addAll(XZuFiUtil.getLocalesFromHyperlinks(modul.getWeiterfuehrenderLink()));

        return languages;
    }
}
