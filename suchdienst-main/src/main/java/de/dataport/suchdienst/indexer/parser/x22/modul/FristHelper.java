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

import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.CodeZeiteinheit;
import de.xoev.xzufi.v2_2_0.FristAuswahl;
import de.xoev.xzufi.v2_2_0.FristDauer;
import de.xoev.xzufi.v2_2_0.FristStichtag;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component("fristHelperX22")
public class FristHelper {

    private final CodelistService codelistService;
    private final DateTimeFormatter dateFormatMonatTag = DateTimeFormatter.ofPattern("dd.MM.");
    private final DateTimeFormatter dateFormatTag = DateTimeFormatter.ofPattern("dd.");
    private final DateTimeFormatter dateFormatDatum = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JsonNode singularPluralMapping;

    public FristHelper(CodelistService codelistService) throws IOException {
        this.codelistService = codelistService;
        this.singularPluralMapping = new ObjectMapper().readTree(
            new ClassPathResource("static-text/units_singular_plural_mapping.json").getInputStream()
        );
    }

    Optional<String> calculateDauer(FristAuswahl frist, Locale locale) {
        try {
            return calculateFristDauer(frist.getFristDauer(), locale).or(() ->
                calculateFristStichtag(frist.getFristStichtag())
            );
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Parsen einer Frist", e);
            return Optional.empty();
        }
    }

    private Optional<String> calculateFristStichtag(FristStichtag fristStichtag) {
        if (fristStichtag == null) {
            return Optional.empty();
        }

        var von = Optional.ofNullable(fristStichtag.getVon()).map(fristStichtagVon ->
            formatFristStichtag(
                fristStichtagVon.getDatum(),
                fristStichtagVon.getMonatTag(),
                fristStichtagVon.getTag()
            )
        );
        var bis = Optional.ofNullable(fristStichtag.getBis()).map(fristStichtagBis ->
            formatFristStichtag(
                fristStichtagBis.getDatum(),
                fristStichtagBis.getMonatTag(),
                fristStichtagBis.getTag()
            )
        );

        if (von.isPresent() && bis.isPresent()) {
            return Optional.of(von.get() + " - " + bis.get());
        }
        return bis.or(() -> von);
    }

    @NonNull
    private String formatFristStichtag(
        XMLGregorianCalendar datum,
        XMLGregorianCalendar monatTag,
        XMLGregorianCalendar tag
    ) {
        if (datum != null) {
            return formatDateTime(datum, dateFormatDatum);
        }
        if (monatTag != null) {
            return formatDateTime(monatTag, dateFormatMonatTag);
        }
        if (tag != null) {
            return formatDateTime(tag, dateFormatTag);
        }
        return "";
    }

    private Optional<String> calculateFristDauer(FristDauer fristDauer, Locale locale) {
        if (fristDauer == null) {
            return Optional.empty();
        }

        var einheitVon = fristDauer.getEinheit();
        if (einheitVon == null || einheitVon.getCode() == null) {
            return Optional.empty();
        }

        if (fristDauer.getDauerBis() == null) {
            return getDurationWithoutBis(fristDauer, locale).describeConstable();
        }

        var einheitBis = fristDauer.getEinheitBis();
        if (einheitBis == null || einheitBis.getCode() == null) {
            einheitBis = einheitVon;
        }
        return (
            isEqualUnits(einheitVon, einheitBis)
                ? getDurationSameUnit(fristDauer, locale)
                : getDurationMixedUnit(fristDauer, locale)
        ).describeConstable();
    }

    private boolean isEqualUnits(@NonNull CodeZeiteinheit a, @NonNull CodeZeiteinheit b) {
        return (
            Objects.equals(a.getListURI(), b.getListURI()) &&
            Objects.equals(a.getListVersionID(), b.getListVersionID()) &&
            Objects.equals(a.getCode(), b.getCode())
        );
    }

    private String getDurationWithoutBis(FristDauer dauer, Locale locale) {
        return (
            dauer.getDauer() +
            " " +
            getLocalizedUnit(dauer.getEinheit(), dauer.getDauer() > 1, locale)
        );
    }

    private String getDurationSameUnit(FristDauer dauer, Locale locale) {
        return (
            dauer.getDauer() +
            " - " +
            dauer.getDauerBis() +
            " " +
            getLocalizedUnit(dauer.getEinheit(), true, locale)
        );
    }

    private String getDurationMixedUnit(FristDauer dauer, Locale locale) {
        String dauerVon =
            dauer.getDauer() +
            " " +
            getLocalizedUnit(dauer.getEinheit(), dauer.getDauer() > 1, locale);
        String dauerBis =
            dauer.getDauerBis() +
            " " +
            getLocalizedUnit(dauer.getEinheitBis(), dauer.getDauerBis() > 1, locale);
        return locale.equals(Locale.GERMAN)
            ? dauerVon + " bis " + dauerBis
            : dauerVon + " to " + dauerBis;
    }

    private String getLocalizedUnit(CodeZeiteinheit einheit, boolean plural, Locale locale) {
        var uri = ObjectUtils.getIfNull(einheit.getListURI(), XZuFiUtil.DEFAULT_URI_ZEITEINHEIT);
        var version = ObjectUtils.getIfNull(
            einheit.getListVersionID(),
            XZuFiUtil.DEFAULT_VERSION_ZEITEINHEIT
        );
        var code = einheit.getCode();
        var localizedEinheit = codelistService.getCodelistEntry(uri, version, code).getText(locale);
        return plural ? getDurationUnitAsPlural(localizedEinheit, locale) : localizedEinheit;
    }

    /**
     * Versucht eine Zeiteinheit mithilfe einer Mapping-Datei von Singular in Plural zu wandeln.
     *
     * @param unitSingular Zeiteinheit im Singular
     * @param locale       Sprache
     * @return Plural-Form des übergebenen Strings oder den ursprünglichen String, wenn kein Mapping vorhanden ist.
     */
    private String getDurationUnitAsPlural(String unitSingular, Locale locale) {
        JsonNode languageNode = singularPluralMapping.get(locale.toString());
        if (languageNode != null) {
            JsonNode unitPluralNode = languageNode.get(unitSingular.toLowerCase());
            if (unitPluralNode != null) {
                return unitPluralNode.asString();
            } else {
                log.warn(
                    "Begriff '{}' konnte nicht in seine Plural-Form übersetzt werden. " +
                        "Es wurde kein Eintrag in der Mappings-Datei definiert.",
                    unitSingular
                );
            }
        } else {
            log.warn(
                "Begriff '{}' konnte nicht in seine Plural-Form übersetzt werden. Sprache '{}' ist nicht definiert.",
                unitSingular,
                locale
            );
        }

        return unitSingular;
    }

    private String formatDateTime(XMLGregorianCalendar dateTime, DateTimeFormatter formatter) {
        return LocalDateTime.ofInstant(
            dateTime.toGregorianCalendar().getTime().toInstant(),
            ZoneId.systemDefault()
        ).format(formatter);
    }
}
