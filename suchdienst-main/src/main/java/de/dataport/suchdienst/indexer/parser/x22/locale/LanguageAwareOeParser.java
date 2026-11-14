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
package de.dataport.suchdienst.indexer.parser.x22.locale;

import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformation;
import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformationType;
import de.dataport.suchdienst.indexer.model.mapping.BankAccount;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Contact;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.KeyValueObject;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.indexer.parser.x22.locale.model.DateTimeFormatterType;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_2_0.Sprache;
import de.xoev.xzufi.v2_2_0.Zeitserien;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitPerson;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service("languageAwareOeParserX22")
public class LanguageAwareOeParser {

    private final CodelistService codelistService;
    private final BkgGeoLocationResolver geoCode;
    private final CoordinateTransformationService geoTransformService;
    private final MessageSource messageSource;

    private final Map<Locale, Map<DateTimeFormatterType, DateTimeFormatter>> formatters =
        new HashMap<>();

    public LanguageAwareOeParser(
        CodelistService codelistService,
        BkgGeoLocationResolver geoCode,
        CoordinateTransformationService geoTransformService,
        MessageSource messageSource
    ) {
        this.codelistService = codelistService;
        this.geoCode = geoCode;
        this.geoTransformService = geoTransformService;
        this.messageSource = messageSource;
        Map<DateTimeFormatterType, DateTimeFormatter> germanFormatters = new EnumMap<>(
            DateTimeFormatterType.class
        );
        germanFormatters.put(DateTimeFormatterType.TIME, DateTimeFormatter.ofPattern("HH:mm"));
        germanFormatters.put(
            DateTimeFormatterType.DATETIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        );
        germanFormatters.put(DateTimeFormatterType.DATE, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        formatters.put(Locale.GERMAN, germanFormatters);

        Map<DateTimeFormatterType, DateTimeFormatter> englishFormatters = new EnumMap<>(
            DateTimeFormatterType.class
        );
        englishFormatters.put(
            DateTimeFormatterType.TIME,
            DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH)
        );
        englishFormatters.put(
            DateTimeFormatterType.DATETIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma", Locale.ENGLISH)
        );
        englishFormatters.put(
            DateTimeFormatterType.DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
        );
        formatters.put(Locale.ENGLISH, englishFormatters);
    }

    public Map<Locale, OrganisationseinheitContent> parse(
        OrganisationseinheitErweitert xoe,
        OrganisationseinheitVL oe,
        Instant lastUpdate
    ) {
        // return language content
        return xoe
            .getSprachversion()
            .stream()
            .map(Sprache::getLanguageCode)
            .filter(languageCode -> {
                if (StringUtils.isBlank(languageCode)) {
                    log.warn("Invalide Sprache-Info in {}", oe.getId());
                    return false;
                }

                return true;
            })
            .map(XzufiParserUtils::getSupportedLocale)
            .map(locale -> {
                String language = locale.toString();
                log.debug("Parse Oe-Inhalt von {} fuer Sprache {}", oe.getId(), language);

                OrganisationseinheitContent content = parseLanguage(xoe, locale, lastUpdate);

                if (StringUtils.isBlank(content.getName())) {
                    if (XZuFiUtil.isLanguageOrCountry(locale, "de")) {
                        log.warn(
                            "Fehlerhafte Daten: OE {} enthält keinen Namen auf Deutsch! Versuche den Fehler zu ignorieren.",
                            oe.getId()
                        );
                    } else {
                        log.debug(
                            "Ignoriere unvollständigen Inhalt von OE {} für Sprache {}! Name muss vorhanden sein!",
                            oe.getId(),
                            language
                        );
                        return null;
                    }
                }

                return Map.entry(locale, content);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private OrganisationseinheitContent parseLanguage(
        OrganisationseinheitErweitert xoe,
        Locale locale,
        Instant lastUpdate
    ) {
        OrganisationseinheitContent content = new OrganisationseinheitContent();
        content.setName(XZuFiUtil.getOrganisationName(xoe.getName(), locale).orElse(null));
        content.setDescription(
            XZuFiUtil.getFirstByLanguage(xoe.getBeschreibung(), locale).orElse(null)
        );
        content.setShortDescription(
            XZuFiUtil.getFirstByLanguage(xoe.getKurzbeschreibung(), locale).orElse(null)
        );
        content.setPublisher(XZuFiUtil.getPublisher(xoe.getHerausgeber(), locale).orElse(null));

        List<Contact> employees = xoe
            .getKontaktperson()
            .stream()
            .map(person -> {
                Contact contact = new Contact();
                contact.setName(person.getVorname());
                contact.setLastname(person.getFamilienname());
                contact.setSalutation(
                    XZuFiUtil.getFirstByLanguage(person.getAnrede(), locale).orElse(null)
                );
                contact.setTitle(
                    XZuFiUtil.getFirstByLanguage(person.getTitel(), locale).orElse(null)
                );

                Location location = XZuFiUtil.getLocation(
                    person.getAnschrift(),
                    person.getErreichbarkeit(),
                    codelistService,
                    geoCode,
                    geoTransformService,
                    locale
                );
                contact.setLocation(location);
                List<Link> linksPerson = XZuFiUtil.getHyperlinks(
                    person.getInternetadresse(),
                    locale
                );
                contact.setAdditionalLinks(linksPerson);

                contact.setRelations(getRelations(person.getZustaendigkeit(), lastUpdate));

                contact.setRoom(person.getRaum());
                contact.setPosition(
                    XZuFiUtil.getFirstByLanguage(person.getPosition(), locale).orElse(null)
                );
                contact.setSpeakingTimes(
                    XZuFiUtil.getFirstByLanguage(person.getSprechzeiten(), locale).orElse(null)
                );
                contact.setPublisher(
                    XZuFiUtil.getPublisher(person.getHerausgeber(), locale).orElse(null)
                );
                return contact;
            })
            .toList();
        content.setEmployees(employees);

        Optional<String> infoSonstige = XZuFiUtil.getFirstByLanguage(xoe.getInfoSonstige(), locale);
        infoSonstige.ifPresent(s ->
            content
                .getAdditionalInformation()
                .add(
                    new AdditionalInformation(
                        messageSource.getMessage(
                            "xzufi_oe_parser_service.additional_information",
                            null,
                            locale
                        ),
                        s,
                        AdditionalInformationType.INFO_OTHER
                    )
                )
        );

        List<BankAccount> bankAccounts = xoe
            .getBankverbindung()
            .stream()
            .map(b ->
                new BankAccount(
                    b.getEmpfaenger(),
                    b.getIban(),
                    b.getBankinstitut(),
                    XZuFiUtil.getFirstByLanguage(b.getBeschreibung(), locale).orElse(null),
                    b.getBic()
                )
            )
            .toList();
        content.setBankAccounts(bankAccounts);
        parseInformationTimes(xoe, locale, content);

        List<Link> links = XZuFiUtil.getHyperlinks(xoe.getInternetadresse(), locale);
        content.setForms(links);

        content.setLocation(
            XZuFiUtil.getLocation(
                xoe.getAnschrift(),
                xoe.getErreichbarkeit(),
                codelistService,
                geoCode,
                geoTransformService,
                locale
            )
        );

        List<Communication> communicationsystems = xoe
            .getKommunikationssystem()
            .stream()
            .map(system -> {
                Communication communicationsystem = new Communication();
                communicationsystem.setValue(system.getKennung());
                if (system.getKanal().getName() != null && system.getKanal().getName().isEmpty()) {
                    communicationsystem.setName(system.getKanal().getName());
                } else {
                    CodelistEntry entry = codelistService.getCodelistEntry(
                        system.getKanal().getListURI(),
                        system.getKanal().getListVersionID(),
                        system.getKanal().getCode()
                    );
                    communicationsystem.setName(entry.getText(locale));
                }
                communicationsystem.setAdditional(system.getKennungzusatz());
                return communicationsystem;
            })
            .distinct()
            .toList();
        if (!communicationsystems.isEmpty()) {
            // füge für jede ARS die Liste hinzu
            Map<String, List<Communication>> communicationMap = new HashMap<>();
            xoe.getZustaendigkeit()
                .stream()
                .flatMap(z -> z.getGebietID().stream())
                .map(Identifikator::getValue)
                .forEach(ars -> communicationMap.put(ars, communicationsystems));
            content.setCommunicationSystems(communicationMap);
        }

        return content;
    }

    private void parseInformationTimes(
        OrganisationseinheitErweitert xoe,
        Locale locale,
        OrganisationseinheitContent oe
    ) {
        AdditionalInformation informationTimes = new AdditionalInformation(
            messageSource.getMessage("xzufi_oe_parser_service.business_hours", null, locale),
            null,
            AdditionalInformationType.INFO_TIMES
        );

        if (!xoe.getInfoOeffnungszeitenText().isEmpty()) {
            informationTimes.setText(
                XZuFiUtil.getFirstByLanguage(xoe.getInfoOeffnungszeitenText(), locale).orElse(null)
            );
        }

        xoe.getInfoOeffnungszeitenStrukturiert().forEach(time -> {
            if (time.getTyp() != null) {
                CodelistEntry type = codelistService.getCodelistEntry(
                    time.getTyp().getListURI(),
                    time.getTyp().getListVersionID(),
                    time.getTyp().getCode()
                );
                informationTimes
                    .getAdditionalInformation()
                    .add(new KeyValueObject(type.getText(locale), null));
            }

            parseInformationTimesFromRegulaereZeiten(locale, time, informationTimes);
            parseInformationTimesFromAbweichendeZeiten(locale, time, informationTimes);

            if (time.getHinweistext() != null) {
                Optional<String> hinweisText = XZuFiUtil.getByLanguage(
                    time.getHinweistext(),
                    locale
                );
                hinweisText.ifPresent(s ->
                    informationTimes
                        .getAdditionalInformation()
                        .add(
                            new KeyValueObject(
                                messageSource.getMessage(
                                    "xzufi_oe_parser_service.additional_information_hint_key_name",
                                    null,
                                    locale
                                ),
                                s
                            )
                        )
                );
            }
        });

        if (
            informationTimes.getText() != null ||
            !informationTimes.getAdditionalInformation().isEmpty()
        ) {
            oe.getAdditionalInformation().add(informationTimes);
        }
    }

    private void parseInformationTimesFromAbweichendeZeiten(
        Locale locale,
        Zeitserien time,
        AdditionalInformation informationTimes
    ) {
        time.getAbweichendeZeiten().forEach(diffTime -> {
            String diffString = "";
            diffString += LocalDateTime.ofInstant(
                diffTime.getBeginn().toGregorianCalendar().getTime().toInstant(),
                ZoneId.systemDefault()
            ).format(selectFormatter(diffTime.getBeginn(), locale));
            diffString += " - ";
            diffString += LocalDateTime.ofInstant(
                diffTime.getEnde().toGregorianCalendar().getTime().toInstant(),
                ZoneId.systemDefault()
            ).format(selectFormatter(diffTime.getBeginn(), locale));
            Optional<String> zusatz = XZuFiUtil.getFirstByLanguage(diffTime.getZusatz(), locale);
            if (zusatz.isPresent()) {
                diffString +=
                    "; " + XZuFiUtil.getFirstByLanguage(diffTime.getZusatz(), locale).orElse("");
            }

            informationTimes
                .getAdditionalInformation()
                .add(
                    new KeyValueObject(
                        messageSource.getMessage(
                            "xzufi_oe_parser_service.additional_information_divergent_hours_key_name",
                            null,
                            locale
                        ),
                        diffString
                    )
                );
        });
    }

    private void parseInformationTimesFromRegulaereZeiten(
        Locale locale,
        Zeitserien time,
        AdditionalInformation informationTimes
    ) {
        time.getRegulaereZeiten().forEach(rTime -> {
            String openingString = "";
            openingString += codelistService
                .getCodelistEntry(
                    rTime.getTagesposition().getListURI(),
                    rTime.getTagesposition().getListVersionID(),
                    rTime.getTagesposition().getCode()
                )
                .getText(locale);
            openingString += " ";

            openingString += codelistService
                .getCodelistEntry(
                    rTime.getTagestyp().getListURI(),
                    rTime.getTagestyp().getListVersionID(),
                    rTime.getTagestyp().getCode()
                )
                .getText(locale);

            openingString += " ";
            openingString += LocalDateTime.ofInstant(
                rTime.getBeginn().toGregorianCalendar().getTime().toInstant(),
                ZoneId.systemDefault()
            ).format(selectFormatter(rTime.getBeginn(), locale));
            openingString += " - ";
            openingString += LocalDateTime.ofInstant(
                rTime.getEnde().toGregorianCalendar().getTime().toInstant(),
                ZoneId.systemDefault()
            ).format(selectFormatter(rTime.getBeginn(), locale));

            if (rTime.getZusatz() != null) {
                if (XZuFiUtil.isLanguageOrCountry(locale, rTime.getZusatz().getLanguageCode())) {
                    openingString +=
                        "; " + XZuFiUtil.getByLanguage(rTime.getZusatz(), locale).orElse("");
                    informationTimes
                        .getAdditionalInformation()
                        .add(
                            new KeyValueObject(
                                messageSource.getMessage(
                                    "xzufi_oe_parser_service.additional_information_regular_hours_key_name",
                                    null,
                                    locale
                                ),
                                openingString
                            )
                        );
                }
            } else {
                informationTimes
                    .getAdditionalInformation()
                    .add(
                        new KeyValueObject(
                            messageSource.getMessage(
                                "xzufi_oe_parser_service.additional_information_regular_hours_key_name",
                                null,
                                locale
                            ),
                            openingString
                        )
                    );
            }
        });
    }

    private DateTimeFormatter selectFormatter(XMLGregorianCalendar cal, Locale locale) {
        if (cal.getDay() < 0) {
            return formatters
                .getOrDefault(locale, formatters.get(Locale.GERMAN))
                .get(DateTimeFormatterType.TIME);
        }
        if (cal.getHour() < 0) {
            return formatters
                .getOrDefault(locale, formatters.get(Locale.GERMAN))
                .get(DateTimeFormatterType.DATE);
        }
        return formatters
            .getOrDefault(locale, formatters.get(Locale.GERMAN))
            .get(DateTimeFormatterType.DATETIME);
    }

    private Set<ZustaendigkeitEntity> getRelations(
        List<ZustaendigkeitPerson> zustaendigkeiten,
        Instant lastUpdate
    ) {
        return zustaendigkeiten
            .stream()
            .flatMap(zs ->
                zs
                    .getLeistungID()
                    .stream()
                    .flatMap(lbId ->
                        zs
                            .getGebietID()
                            .stream()
                            .map(gebietId -> {
                                ZustaendigkeitEntity relation = new ZustaendigkeitEntity22(
                                    lastUpdate
                                );
                                relation.setLbId(
                                    XZuFiUtil.getIdentificatorString(lbId, IdentificationType.LB)
                                );
                                relation.setArs(StringUtils.rightPad(gebietId.getValue(), 12, '0'));
                                relation.setId(UUID.randomUUID().toString());
                                return relation;
                            })
                    )
            )
            .collect(Collectors.toSet());
    }
}
