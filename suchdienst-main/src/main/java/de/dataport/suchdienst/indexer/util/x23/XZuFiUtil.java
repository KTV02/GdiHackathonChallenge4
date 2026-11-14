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
package de.dataport.suchdienst.indexer.util.x23;

import com.google.common.collect.Lists;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapper.x23.LinkMapper;
import de.dataport.suchdienst.indexer.model.mapping.Address;
import de.dataport.suchdienst.indexer.model.mapping.Address.AddressBuilder;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Publisher;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_3_1.Amount;
import de.xoev.xzufi.v2_3_1.Anschrift;
import de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.Erreichbarkeit;
import de.xoev.xzufi.v2_3_1.Geokodierung;
import de.xoev.xzufi.v2_3_1.Herausgeber;
import de.xoev.xzufi.v2_3_1.HyperlinkErweitert;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.NameOrganisation;
import de.xoev.xzufi.v2_3_1.OnlinedienstLink;
import de.xoev.xzufi.v2_3_1.PointType;
import de.xoev.xzufi.v2_3_1.Sprache;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.StringLocalizedHTML;
import de.xoev.xzufi.v2_3_1.TextLocalizedHTMLMitGueltigkeit;
import de.xoev.xzufi.v2_3_1.Versionsinformation;
import de.xoev.xzufi.v2_3_1.Zeitraum;
import de.xoev.xzufi.v2_3_1.Zustaendigkeit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.locationtech.proj4j.ProjCoordinate;

public class XZuFiUtil {

    public static final String LEISTUNG_GEMEINSAMER_VOLLZUG = "Leistung im gemeinsamen Vollzug";

    public static final String AMOUNT_PATTERN = "#,##0.00";

    public static final String DEFAULT_URI_DOKUMENTART =
        "urn:xoev-de:fim-datenfelder:codeliste:dokumentart";
    public static final String DEFAULT_URI_ANSCHRIFT_TYP =
        "urn:xoev-de:fim:codeliste:xzufi.anschrifttyp";
    public static final String DEFAULT_VERSION_ANSCHRIFT_TYP = "4";
    public static final String DEFAULT_URI_TEXTMODUL =
        "urn:xoev-de:fim:codeliste:xzufi.leistungstextmodul";
    public static final String DEFAULT_URI_ERREICHBARKEIT = "urn:de:xoev:codeliste:erreichbarkeit";
    public static final String DEFAULT_URI_ZEITEINHEIT =
        "urn:xoev-de:fim:codeliste:xzufi.zeiteinheit";
    public static final String DEFAULT_VERSION_ZEITEINHEIT = "3";
    public static final String DEFAULT_URI_FRISTTYP = "urn:xoev-de:fim:codeliste:xzufi.fristtyp";
    public static final String DEFAULT_VERSION_FRISTTYP = "3";
    public static final String DEFAULT_URI_SIGNATUR = "urn:xoev-de:fim:codeliste:xzufi.signatur";
    public static final String DEFAULT_VERSION_SIGNATUR = "2.0";
    public static final String DEFAULT_URI_KOSTENTYP = "urn:xoev-de:fim:codeliste:xzufi.kostentyp";
    public static final String DEFAULT_VERSION_KOSTENTYP = "1.0";
    public static final String DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE =
        "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle";
    public static final String DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE = "1";
    public static final String DEFAULT_URI_ZUSTAENDIGKEITS_KRITERIUM =
        "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitskriterium";
    public static final String DEFAULT_VERSION_ZUSTAENDIGKEITS_KRITERIUM = "2";
    public static final String DEFAULT_URI_SDGINFORMATIONSBEREICH1 =
        "urn:xoev-de:fim:codeliste:xzufi.sdginformationsbereich1";
    public static final String DEFAULT_URI_SDGINFORMATIONSBEREICH2 =
        "urn:xoev-de:fim:codeliste:xzufi.sdginformationsbereich2";

    // Koordinaten für Deutschland
    private static final ProjCoordinate obenLinks = new ProjCoordinate(5.844727, 54.807017);
    private static final ProjCoordinate untenRechts = new ProjCoordinate(14.721680, 47.234490);

    private static final String AXIS_NORTHING = "northing";
    private static final String AXIS_LATITUDE = "latitude";

    private XZuFiUtil() {
        // Utility class
    }

    public static boolean isLanguageOrCountry(Locale locale, String input) {
        if (locale != null && input != null) {
            return (
                locale.getLanguage().equals(Locale.of(input).getLanguage()) ||
                locale.toLanguageTag().equals(input)
            );
        }

        return false;
    }

    public static List<String> getStringLocalizedByLanguage(
        Collection<StringLocalized> strings,
        Locale locale
    ) {
        return strings
            .stream()
            .filter(s -> isLanguageOrCountry(locale, s.getLanguageCode()))
            .map(StringLocalized::getValue)
            .toList();
    }

    public static List<String> getStringLocalizedHTMLByLanguage(
        Collection<StringLocalizedHTML> strings,
        Locale locale
    ) {
        return strings
            .stream()
            .filter(s -> isLanguageOrCountry(locale, s.getLanguageCode()))
            .map(StringLocalizedHTML::getValue)
            .toList();
    }

    public static Optional<String> getFirstStringLocalizedByLanguage(
        Collection<StringLocalized> strings,
        Locale locale
    ) {
        return strings
            .stream()
            .filter(s -> isLanguageOrCountry(locale, s.getLanguageCode()))
            .map(StringLocalized::getValue)
            .findFirst();
    }

    public static Optional<String> getFirstStringLocalizedHTMLByLanguage(
        Collection<StringLocalizedHTML> strings,
        Locale locale
    ) {
        return strings
            .stream()
            .filter(s -> isLanguageOrCountry(locale, s.getLanguageCode()))
            .map(StringLocalizedHTML::getValue)
            .findFirst();
    }

    public static Optional<String> getFirstTextLocalizedHTMLMitGueltigkeitByLanguage(
        List<TextLocalizedHTMLMitGueltigkeit> strings,
        Locale locale
    ) {
        return strings
            .stream()
            .filter(s -> isLanguageOrCountry(locale, s.getInhalt().getLanguageCode()))
            .map(t -> t.getInhalt().getValue())
            .findFirst();
    }

    public static Optional<String> getStringLocalizedHTMLByLanguage(
        StringLocalizedHTML stringLocalizedHTML,
        Locale locale
    ) {
        if (isLanguageOrCountry(locale, stringLocalizedHTML.getLanguageCode())) {
            return Optional.ofNullable(stringLocalizedHTML.getValue());
        }

        return Optional.empty();
    }

    /**
     * Wandelt ein Amount Objekt in einen String um, indem der Währungscode hinten angehängt wird
     */
    public static String mapAmount(Amount amount, Locale locale) {
        if (amount == null) {
            return "";
        }

        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        ((DecimalFormat) nf).applyPattern(AMOUNT_PATTERN);
        String value = nf.format(amount.getValue());
        if (amount.getCurrencyCode() != null) {
            value += " " + mapCurrencyCode(amount.getCurrencyCode());
        }
        return value;
    }

    /**
     * Mapped den Euro Code. Wenn mehrere Währungen dazu kommen muss die Codeliste berücksichtigt
     * werden: Code aus UN/ECE Rec.9 Codeliste. Auch veröffentlicht als ISO 4217.
     */
    public static String mapCurrencyCode(String code) {
        if (code.equals("EUR")) {
            return "Euro";
        }
        return code;
    }

    public static Location getLocation(
        List<? extends Anschrift> addresses,
        List<Erreichbarkeit> communicationsOrganisation,
        CodelistService codelistService,
        BkgGeoLocationResolver geoCode,
        CoordinateTransformationService crsTransform,
        Locale locale
    ) {
        Location location = new Location();

        var mappedAddresses = addresses
            .stream()
            .filter(a -> a.getTyp() != null)
            .map(anschrift -> {
                AddressBuilder addressBuilder = Address.builder()
                    .code(anschrift.getTyp().getCode())
                    .city(anschrift.getOrt())
                    .houseNumber(anschrift.getHausnummer())
                    .postbox(anschrift.getPostfach())
                    .street(anschrift.getStrasse())
                    .zip(anschrift.getPostleitzahl())
                    .additional(anschrift.getZusatz());

                Address address;
                if (anschrift instanceof AnschriftOrganisationseinheit anschriftoe) {
                    addressBuilder
                        .accessible(
                            getFirstStringLocalizedHTMLByLanguage(
                                anschriftoe.getInfoBarrierefreiheit(),
                                locale
                            ).orElse(null)
                        )
                        .oepnv(
                            getFirstStringLocalizedHTMLByLanguage(
                                anschriftoe.getInfoOEPNV(),
                                locale
                            ).orElse(null)
                        )
                        .parking(
                            getFirstStringLocalizedHTMLByLanguage(
                                anschriftoe.getInfoParkplatz(),
                                locale
                            ).orElse(null)
                        )
                        .lift(anschriftoe.isKennzeichenAufzug())
                        .wheelchairAccessible(anschriftoe.isKennzeichenRollstuhlgerecht());

                    address = addressBuilder.build();
                    // GeoKoordinaten werden nur für Adressen von Organisationseinheiten bestimmt.
                    // Dadurch findet keine Adressauflösung für Kontaktpersonen statt.
                    calculateGeolocation(
                        address,
                        anschriftoe.getGeokodierung(),
                        crsTransform,
                        geoCode
                    ).ifPresent(address::setGeo);
                } else {
                    address = addressBuilder.build();
                }

                String uri =
                    anschrift.getTyp().getListURI() == null
                        ? DEFAULT_URI_ANSCHRIFT_TYP
                        : anschrift.getTyp().getListURI();
                String version =
                    anschrift.getTyp().getListVersionID() == null
                        ? DEFAULT_VERSION_ANSCHRIFT_TYP
                        : anschrift.getTyp().getListVersionID();

                CodelistEntry entry = codelistService.getCodelistEntry(
                    uri,
                    version,
                    anschrift.getTyp().getCode()
                );
                address.setType(entry.getText(locale));
                return address;
            })
            .toList();

        var mappedCommunications = communicationsOrganisation
            .stream()
            .map(communication -> {
                Communication comm = new Communication();
                comm.setCode(communication.getKanal().getCode());
                String name;

                if (
                    communication.getKanal().getName() != null &&
                    !communication.getKanal().getName().isEmpty()
                ) {
                    name = communication.getKanal().getName();
                } else {
                    String uri =
                        communication.getKanal().getListURI() == null
                            ? DEFAULT_URI_ERREICHBARKEIT
                            : communication.getKanal().getListURI();
                    CodelistEntry entry = codelistService.getCodelistEntry(
                        uri,
                        communication.getKanal().getListVersionID(),
                        communication.getKanal().getCode()
                    );
                    name = entry.getText(locale);
                }
                comm.setName(name);
                comm.setValue(communication.getKennung());
                comm.setValueAdditional(communication.getKennungzusatz());
                comm.setAdditional(Optional.ofNullable(communication.getZusatz()).orElse(""));
                return comm;
            })
            .toList();

        location.setAddresses(mappedAddresses);
        location.setCommunications(mappedCommunications);
        return location;
    }

    public static Optional<GeoLocation> calculateGeolocation(
        Address address,
        List<Geokodierung> geokodierung,
        CoordinateTransformationService crsTransform,
        BkgGeoLocationResolver geoCode
    ) {
        Optional<GeoLocation> geolocation = Optional.empty();
        if (!geokodierung.isEmpty()) {
            PointType point = geokodierung.getFirst().getPoint();
            if (point.getPos() != null) {
                List<Double> pointDoubles = point.getPos().getValue();
                if (pointDoubles.size() > 1) {
                    String srsName = StringUtils.defaultIfBlank(
                        point.getPos().getSrsName(),
                        StringUtils.defaultIfBlank(point.getSrsName(), GeoLocation.DEFAULT_CRS)
                    );

                    List<String> axisLabels = getAxisLabels(point);
                    geolocation = crsTransform
                        .transform(
                            initGeolocation(pointDoubles, srsName, axisLabels, false),
                            GeoLocation.DEFAULT_CRS
                        )
                        .map(loc ->
                            checkAndCorrectGeolocation(
                                loc,
                                axisLabels,
                                pointDoubles,
                                srsName,
                                crsTransform
                            )
                        );
                }
            }
        }
        // Wenn keine Koordinaten vorhanden sind beim Geoservice nachfragen
        if (geolocation.isEmpty()) {
            return geoCode.resolve(address);
        }
        return geolocation;
    }

    /**
     * Erstellt ein Geolocation-Objekt, welches noch in das richtige Koordinatensystem
     * konvertiert werden muss
     *
     * @param pointDoubles Die Koordinaten als Double Liste
     * @param srsName      Der Name des aktuellen Koordinatensystems
     * @param axisLabels   Die Achsenbeschriftung
     * @param changeAxis   true wenn die Koordinaten gedreht werden sollen, sonst false
     */
    public static GeoLocation initGeolocation(
        List<Double> pointDoubles,
        String srsName,
        List<String> axisLabels,
        boolean changeAxis
    ) {
        double lon = pointDoubles.get(0);
        double lat = pointDoubles.get(1);
        if (changeAxis) {
            lon = pointDoubles.get(1);
            lat = pointDoubles.get(0);
        }
        if (
            ObjectUtils.isNotEmpty(axisLabels) &&
            (AXIS_LATITUDE.equals(axisLabels.getFirst()) ||
                AXIS_NORTHING.equals(axisLabels.getFirst()))
        ) {
            // Wenn die AxisLabels im Xzufi falsch gesetzt waren dann nochmal anders rum probieren
            lon = pointDoubles.get(1);
            lat = pointDoubles.get(0);
            if (changeAxis) {
                lon = pointDoubles.get(0);
                lat = pointDoubles.get(1);
            }
        }
        return new GeoLocation(lon + "", "" + lat, srsName);
    }

    public static GeoLocation checkAndCorrectGeolocation(
        GeoLocation geolocation,
        List<String> axisLabels,
        List<Double> pointDoubles,
        String srsName,
        CoordinateTransformationService crsTransform
    ) {
        if (coordinatesInGermany(geolocation.getLongitude(), geolocation.getLatitude())) {
            return geolocation;
        } else {
            Optional<GeoLocation> geoOpt = crsTransform.transform(
                initGeolocation(pointDoubles, srsName, axisLabels, true),
                GeoLocation.DEFAULT_CRS
            );
            return geoOpt
                .filter(geo -> coordinatesInGermany(geo.getLongitude(), geo.getLatitude()))
                .orElse(null);
        }
    }

    public static Optional<String> getOrganisationName(
        Collection<NameOrganisation> names,
        Locale language
    ) {
        return names
            .stream()
            .findFirst()
            .map(n -> getFirstStringLocalizedByLanguage(n.getName(), language))
            .flatMap(n -> n);
    }

    public static List<Link> getHyperlinks(Collection<HyperlinkErweitert> links, Locale locale) {
        return links
            .stream()
            .sorted(
                Comparator.comparing(
                    HyperlinkErweitert::getPositionDarstellung,
                    Comparator.comparing(h -> Optional.ofNullable(h).orElse(0))
                )
            )
            .filter(link -> isLanguageOrCountry(locale, link.getLanguageCode()))
            .map(link -> Link.of(link.getUri(), link.getTitel(), link.getBeschreibung()))
            .toList();
    }

    public static List<LinkDynamic> getOnlinedienstHyperlinks(
        Collection<OnlinedienstLink> links,
        Locale language
    ) {
        return links
            .stream()
            .filter(link -> isLanguageOrCountry(language, link.getLanguageCode()))
            .sorted(
                Comparator.comparing(
                    OnlinedienstLink::getPositionDarstellung,
                    Comparator.comparing(o -> Optional.ofNullable(o).orElse(0))
                )
            )
            .map(LinkMapper.INSTANCE::map)
            .toList();
    }

    public static Instant getVersionUpdateDate(Versionsinformation version) {
        if (version == null) {
            return null;
        }

        var xzufiCreated = version.getErstelltDatumZeit();
        var xzufiUpdated = version.getGeaendertDatumZeit();

        if (xzufiCreated != null && xzufiUpdated != null) {
            return xzufiUpdated.toInstant().toEpochMilli() > xzufiCreated.toInstant().toEpochMilli()
                ? xzufiUpdated.toInstant()
                : xzufiCreated.toInstant();
        } else if (xzufiUpdated != null) {
            return xzufiUpdated.toInstant();
        } else if (xzufiCreated != null) {
            return xzufiCreated.toInstant();
        } else {
            return null;
        }
    }

    public static List<ValidityPeriod> getValidityPeriods(Collection<Zeitraum> zeitraumCollection) {
        if (zeitraumCollection.isEmpty()) {
            return Lists.newArrayList(
                new ValidityPeriod(LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
            );
        }
        return zeitraumCollection
            .stream()
            .map(zeitraum -> {
                XMLGregorianCalendar calStart = zeitraum.getBeginn();
                LocalDate validFrom = LocalDate.of(1970, 1, 1);
                if (calStart != null) {
                    validFrom = calStart.toGregorianCalendar().toZonedDateTime().toLocalDate();
                }

                XMLGregorianCalendar calEnd = zeitraum.getEnde();
                LocalDate validTo = LocalDate.of(2999, 12, 31);
                if (calEnd != null) {
                    validTo = calEnd.toGregorianCalendar().toZonedDateTime().toLocalDate();
                }

                return new ValidityPeriod(validFrom, validTo);
            })
            .toList();
    }

    public static String getIdentificatorString(
        @NonNull Identifikator identifikator,
        @NonNull IdentificationType identificationType
    ) {
        return IdentifikatorUtil.getIdentificatorString(identifikator, identificationType);
    }

    public static Locale getLocale(Sprache sprache) {
        return Locale.of(sprache.getLanguageCode());
    }

    public static Optional<Publisher> getPublisher(Herausgeber herausgeber, Locale locale) {
        if (herausgeber == null) {
            return Optional.empty();
        }
        Publisher publisher = new Publisher();
        publisher.setName(herausgeber.getBezeichnung());
        if (!herausgeber.getKurzbezeichnung().isEmpty()) {
            publisher.setShortName(herausgeber.getKurzbezeichnung());
        }
        publisher.setDescription(
            getFirstStringLocalizedByLanguage(herausgeber.getBeschreibung(), locale).orElse(null)
        );
        return Optional.of(publisher);
    }

    public static Set<String> getLocalesFromStringLocalized(List<StringLocalized> s) {
        return s.stream().map(StringLocalized::getLanguageCode).collect(Collectors.toSet());
    }

    public static Set<String> getLocalesFromStringLocalizedHTML(List<StringLocalizedHTML> s) {
        return s.stream().map(StringLocalizedHTML::getLanguageCode).collect(Collectors.toSet());
    }

    public static Set<String> getLocalesFromHyperlinks(List<HyperlinkErweitert> links) {
        return links.stream().map(HyperlinkErweitert::getLanguageCode).collect(Collectors.toSet());
    }

    public static Set<String> extractRelations(List<? extends Zustaendigkeit> relations) {
        return relations
            .stream()
            .map(Zustaendigkeit::getGebietID)
            .flatMap(Collection::stream)
            .map(Identifikator::getValue)
            .collect(Collectors.toSet());
    }

    public static boolean zusaendigkeitHasLbId(Zustaendigkeit xzs, String lbId) {
        return xzs
            .getLeistungID()
            .stream()
            .map(ident -> XZuFiUtil.getIdentificatorString(ident, IdentificationType.LB))
            .anyMatch(lbId::equals);
    }

    public static Set<String> getArsFederalState(List<Identifikator> identifikators) {
        return identifikators
            .stream()
            .map(Identifikator::getValue)
            .map(ars -> StringUtils.substring(ars, 0, 2))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private static List<String> getAxisLabels(PointType point) {
        List<String> axisLabels = new ArrayList<>();
        if (point.getPos().getAxisLabels().size() > 1) {
            axisLabels = point.getPos().getAxisLabels();
        } else if (point.getAxisLabels().size() > 1) {
            axisLabels = point.getAxisLabels();
        }
        return axisLabels;
    }

    private static boolean coordinatesInGermany(String x, String y) {
        double roundedX = round(Double.parseDouble(x));
        double roundedY = round(Double.parseDouble(y));
        return (
            roundedX >= obenLinks.x &&
            roundedX <= untenRechts.x &&
            roundedY <= obenLinks.y &&
            roundedY >= untenRechts.y
        );
    }

    private static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(6, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
