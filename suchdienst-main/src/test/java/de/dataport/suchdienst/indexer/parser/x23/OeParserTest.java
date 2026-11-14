/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.parser.x23;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.BkgGeoLocationResolver;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import de.dataport.suchdienst.indexer.model.mapping.AdditionalInformationType;
import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareFoParser;
import de.dataport.suchdienst.indexer.parser.x23.locale.LanguageAwareOeParser;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageFoCompleteCheckService;
import de.dataport.suchdienst.indexer.service.languagecheck.LanguageOeCompleteCheckService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OeParserTest extends AbstractX23ParserTest {

    private static final Instant LAST_UPDATE = Instant.parse("2024-07-23T07:25:50.636Z");

    private XZuFiOeParserService xZufiOeParserService;
    private BkgGeoLocationResolver geoCode;

    public OeParserTest() {
        super("x23/xzufi-sh-organisation.xml");
    }

    @BeforeEach
    void setUpService() {
        geoCode = mock(BkgGeoLocationResolver.class);
        var languageAwareOeParser = new LanguageAwareOeParser(
            codelistService,
            geoCode,
            new CoordinateTransformationService(),
            messageSource
        );
        var languageFoCheck = new LanguageFoCompleteCheckService();
        var formularParser = new XZuFiFoParserService(new LanguageAwareFoParser(), languageFoCheck);
        xZufiOeParserService = new XZuFiOeParserService(
            languageAwareOeParser,
            new LanguageOeCompleteCheckService(languageFoCheck),
            formularParser,
            codelistService,
            messageSource
        );
    }

    @Test
    void parsesOrganisationseinheitMetadataAndGermanContent() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();

        assertAll(
            () -> assertThat(parsedOe.getId(), is("L100012.OE.9137467")),
            () ->
                assertThat(
                    parsedOe.getName(),
                    is("Architekten- und Ingenieurkammer Schleswig-Holstein")
                ),
            () -> assertThat(parsedOe.isOeffentlichAnzeigen(), is(true)),
            () -> assertThat(parsedOe.getLastUpdate(), is(LAST_UPDATE)),
            () ->
                assertThat(
                    parsedOe.getValidityPeriods(),
                    contains(
                        new ValidityPeriod(LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
                    )
                ),
            () ->
                assertThat(
                    parsedOe.getLanguages(),
                    containsInAnyOrder(Locale.GERMAN, Locale.ENGLISH)
                ),
            () -> assertThat(parsedOe.getOtherLanguages().keySet(), contains(Locale.ENGLISH))
        );
    }

    @Test
    void parsesCompleteEnglishContentWithoutReplacingGermanDefault() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        var englishContent = parsedOe.getOtherLanguages().get(Locale.ENGLISH);

        assertThat(englishContent, notNullValue());
        assertAll(
            () ->
                assertThat(
                    englishContent.getName(),
                    is("Chamber of Architects and Engineers of Schleswig-Holstein")
                ),
            () ->
                assertThat(
                    parsedOe.getName(),
                    is("Architekten- und Ingenieurkammer Schleswig-Holstein")
                ),
            () -> assertThat(parsedOe.getLanguageComplete(), is(Map.of(Locale.ENGLISH, true))),
            () -> assertThat(englishContent.getLocation().getAddresses(), hasSize(1)),
            () ->
                assertThat(
                    englishContent.getLocation().getAddresses().getFirst().getStreet(),
                    is("Düsternbrooker Weg")
                ),
            () ->
                assertThat(
                    englishContent
                        .getLocation()
                        .getCommunications()
                        .stream()
                        .collect(Collectors.toMap(Communication::getCode, Communication::getValue)),
                    is(
                        Map.of(
                            "02",
                            "+49 431 57065-0",
                            "04",
                            "+49 431 57065-25",
                            "01",
                            "info@aik-sh.de"
                        )
                    )
                )
        );
    }

    @Test
    void assignsEnglishOpeningHoursToEnglishContent() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        var englishContent = parsedOe.getOtherLanguages().get(Locale.ENGLISH);

        assertThat(englishContent, notNullValue());
        assertThat(englishContent.getAdditionalInformation(), hasSize(1));
        var openingHours = englishContent.getAdditionalInformation().getFirst();
        assertAll(
            () -> assertThat(openingHours.getType(), is(AdditionalInformationType.INFO_TIMES)),
            () ->
                assertThat(
                    openingHours.getText(),
                    is(
                        "Monday - Thursday: 9:00 am - 12:00 pm and 2:00 pm - 4:00 pm\\n Friday: 9:00 am - 12:00 pm\\n\\n"
                    )
                ),
            () -> assertThat(openingHours.getAdditionalInformation(), empty())
        );
    }

    @Test
    void assignsInternetAddressesToTheirDeclaredLanguages() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        var englishContent = parsedOe.getOtherLanguages().get(Locale.ENGLISH);

        assertThat(englishContent, notNullValue());
        assertAll(
            () ->
                assertThat(
                    parsedOe.getForms(),
                    contains(new Link("http://www.aik-sh.de", "www.aik-sh.de", null))
                ),
            () ->
                assertThat(
                    englishContent.getForms(),
                    contains(
                        new Link("https://example.org/aik-sh/en", "Chamber website (English)", null)
                    )
                )
        );
    }

    @Test
    void parsesAddress() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        assertThat(parsedOe.getLocation().getAddresses(), hasSize(1));
        var address = parsedOe.getLocation().getAddresses().getFirst();

        assertAll(
            () -> assertThat(address.getCode(), is("001")),
            () -> assertThat(address.getStreet(), is("Düsternbrooker Weg")),
            () -> assertThat(address.getHouseNumber(), is("71")),
            () -> assertThat(address.getZip(), is("24105")),
            () -> assertThat(address.getCity(), is("Kiel")),
            () -> assertThat(address.getLift(), is(false))
        );
    }

    @Test
    void transformsSuppliedCoordinatesWithoutCallingGeoService() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        var geo = parsedOe.getLocation().getAddresses().getFirst().getGeo();

        assertThat(geo, notNullValue());
        // Hier wird gegen die ungefähren Geo-Koordinaten von Kiel geprüft, die in der Xml hinterlegt sind.
        // Die Koordinaten werden transformiert, ohne dass der Geo-Service aufgerufen wird.
        // Annäherung reicht hier um zu zeigen, dass die Daten aus der Xml übernommen und transformiert wurden.
        assertAll(
            () -> assertThat(geo.getSystem(), is(GeoLocation.DEFAULT_CRS)),
            () -> assertThat(Double.parseDouble(geo.getLongitude()), closeTo(10.15, 0.01)),
            () -> assertThat(Double.parseDouble(geo.getLatitude()), closeTo(54.33, 0.01))
        );
        verifyNoInteractions(geoCode);
    }

    @Test
    void parsesPhoneFaxAndEmailWithTheirChannelCodes() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        var communications = parsedOe.getLocation().getCommunications();

        assertThat(communications, hasSize(3));
        assertThat(
            communications
                .stream()
                .collect(Collectors.toMap(Communication::getCode, Communication::getValue)),
            is(Map.of("02", "+49 431 57065-0", "04", "+49 431 57065-25", "01", "info@aik-sh.de"))
        );
    }

    @Test
    void parsesOpeningHours() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();
        assertThat(parsedOe.getAdditionalInformation(), hasSize(1));
        var openingHours = parsedOe.getAdditionalInformation().getFirst();

        assertAll(
            () -> assertThat(openingHours.getType(), is(AdditionalInformationType.INFO_TIMES)),
            () ->
                assertThat(
                    openingHours.getText(),
                    is(
                        "Montag - Donnerstag: 9.00 - 12.00 Uhr und 14.00 - 16.00 Uhr\\n Freitag: 9.00 - 12.00 Uhr\\n\\n"
                    )
                ),
            () -> assertThat(openingHours.getAdditionalInformation(), empty())
        );
    }

    @Test
    void parsesInternetAddress() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();

        assertThat(
            parsedOe.getForms(),
            contains(new Link("http://www.aik-sh.de", "www.aik-sh.de", null))
        );
    }

    @Test
    void doesNotInventMissingFormsContactsOrResponsibilities() {
        OrganisationseinheitVL parsedOe = parseOrganisationseinheit();

        assertAll(
            () -> assertThat(parsedOe.getNestedForms(), empty()),
            () -> assertThat(parsedOe.getEmployees(), empty()),
            () -> assertThat(parsedOe.getBankAccounts(), empty()),
            () -> assertThat(parsedOe.getRelations().isEmpty(), is(true)),
            () -> assertThat(parsedOe.getCriteria().isEmpty(), is(true)),
            () -> assertThat(parsedOe.getCommunicationSystems().isEmpty(), is(true))
        );
    }

    private OrganisationseinheitVL parseOrganisationseinheit() {
        var organisationseinheit = provider
            .getOes()
            .findFirst()
            .orElseThrow()
            .getOrConvertXZuFi23();
        return xZufiOeParserService.parseOrganisationseinheit(organisationseinheit, LAST_UPDATE);
    }
}
