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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import de.xoev.xzufi.v2_3_1.FachlicheFreigabeModul;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("fachlicheFreigabeModulParserX23")
public class FachlicheFreigabeModulParser
    implements Parser<FachlicheFreigabeModul, ServiceModuleDate>
{

    private final MessageSource messageSource;

    public FachlicheFreigabeModulParser(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public ServiceModuleDate parse(FachlicheFreigabeModul modul, Locale locale) {
        Optional<String> fachlichFreigegebenDurch = XZuFiUtil.getFirstStringLocalizedByLanguage(
            modul.getFachlichFreigegebenDurch(),
            locale
        );

        XMLGregorianCalendar fachlichFreigegebenAm = modul.getFachlichFreigegebenAm();

        return ServiceModuleDate.builder()
            .title(messageSource.getMessage("fachlichefreigabe_modul_parser.title", null, locale))
            .text(fachlichFreigegebenDurch.orElse(null))
            .validityPeriods(XZuFiUtil.getValidityPeriods(modul.getGueltigkeit()))
            .date(
                modul.getFachlichFreigegebenAm() != null
                    ? LocalDate.of(
                          fachlichFreigegebenAm.getYear(),
                          fachlichFreigegebenAm.getMonth(),
                          fachlichFreigegebenAm.getDay()
                      )
                    : null
            )
            .build();
    }

    @Override
    public Set<String> getIncludedLanguages(FachlicheFreigabeModul modul) {
        return new HashSet<>(
            XZuFiUtil.getLocalesFromStringLocalized(modul.getFachlichFreigegebenDurch())
        );
    }

    @Override
    public ServiceModuleCode getCode() {
        return ServiceModuleCode.FACHLICHE_FREIGABE;
    }
}
