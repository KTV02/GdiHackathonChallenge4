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
package de.dataport.suchdienst.indexer.util.x22;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import de.dataport.suchdienst.geo.service.CoordinateTransformationService;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.xoev.xzufi.v2_2_0.Anschrift;
import de.xoev.xzufi.v2_2_0.AnschriftOrganisationseinheit;
import de.xoev.xzufi.v2_2_0.CodeAnschrifttyp;
import de.xoev.xzufi.v2_2_0.DirectPositionType;
import de.xoev.xzufi.v2_2_0.Geokodierung;
import de.xoev.xzufi.v2_2_0.HyperlinkErweitert;
import de.xoev.xzufi.v2_2_0.PointType;
import de.xoev.xzufi.v2_2_0.Zeitraum;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class XZuFiUtilTest {

    public HyperlinkErweitert getHyperLink() {
        HyperlinkErweitert hyperlinkErweitert = new HyperlinkErweitert();
        hyperlinkErweitert.setTitel("title");
        hyperlinkErweitert.setUri("http://google.de");
        hyperlinkErweitert.setBeschreibung("beschreibung");
        hyperlinkErweitert.setLanguageCode("de");
        return hyperlinkErweitert;
    }

    public Link getAdditionalLink() {
        return Link.of("http://google.de", "title", "beschreibung");
    }

    @Test
    void testGetHyperlinks() {
        HyperlinkErweitert hyperlinkErweitert = getHyperLink();
        hyperlinkErweitert.setPositionDarstellung(0);

        Link additionalLink = getAdditionalLink();

        List<HyperlinkErweitert> hLinks = List.of(hyperlinkErweitert);
        Collection<Link> aLinks = List.of(additionalLink);

        assertThat(XZuFiUtil.getHyperlinks(hLinks, Locale.GERMAN), is(aLinks));
    }

    @Test
    void testGetHyperlinksInvalidPosition() {
        HyperlinkErweitert he1 = getHyperLink();
        he1.setPositionDarstellung(null);

        HyperlinkErweitert he2 = getHyperLink();
        he2.setPositionDarstellung(null);

        Link al1 = getAdditionalLink();
        Link al2 = getAdditionalLink();

        List<HyperlinkErweitert> hLinks = List.of(he1, he2);
        Collection<Link> aLinks = List.of(al1, al2);

        assertThat(XZuFiUtil.getHyperlinks(hLinks, Locale.GERMAN), is(aLinks));
    }

    @Test
    void testValidFrom() throws DatatypeConfigurationException {
        final GregorianCalendar now = new GregorianCalendar();
        XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(now);

        Zeitraum z1 = new Zeitraum();
        z1.setBeginn(cal);

        assertThat(
            XZuFiUtil.getValidityPeriods(List.of(z1)),
            is(
                Lists.newArrayList(
                    new ValidityPeriod(
                        cal.toGregorianCalendar().toZonedDateTime().toLocalDate(),
                        LocalDate.of(2999, 12, 31)
                    )
                )
            )
        );
    }

    @Test
    void testValidFromEmpty() {
        Zeitraum z1 = new Zeitraum();

        assertThat(
            XZuFiUtil.getValidityPeriods(List.of(z1)),
            is(
                Lists.newArrayList(
                    new ValidityPeriod(LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
                )
            )
        );
    }

    @Test
    void testGetLocationLongitudeLatitude() {
        CodelistService codelistService = mockCodelistService();

        double lon1 = 10.220130725472366;
        double lat1 = 54.40550284486676;

        DirectPositionType pos = new DirectPositionType();
        pos.getValue().add(lon1);
        pos.getValue().add(lat1);

        Geokodierung geo = new Geokodierung();
        PointType point = new PointType();
        point.getAxisLabels().add("longitude");
        point.getAxisLabels().add("latitude");
        point.setPos(pos);
        geo.setPoint(point);

        Location location = XZuFiUtil.getLocation(
            createAnschriften(geo),
            new ArrayList<>(),
            codelistService,
            null,
            new CoordinateTransformationService(),
            Locale.GERMAN
        );
        GeoLocation parsedLocation = location.getAddresses().getFirst().getGeo();
        assertEquals(parsedLocation.getLatitude(), Double.toString(lat1));
        assertEquals(parsedLocation.getLongitude(), Double.toString(lon1));
    }

    @Test
    void testGeolocationLatitudeLongitude() {
        CodelistService codelistService = mockCodelistService();
        double lon1 = 10.220130725472366;
        double lat1 = 54.40550284486676;

        DirectPositionType pos = new DirectPositionType();
        pos.getValue().add(lat1);
        pos.getValue().add(lon1);

        PointType point = new PointType();
        point.getAxisLabels().add("latitude");
        point.getAxisLabels().add("longitude");
        point.setPos(pos);

        Geokodierung geo = new Geokodierung();
        geo.setPoint(point);

        Location location = XZuFiUtil.getLocation(
            createAnschriften(geo),
            new ArrayList<>(),
            codelistService,
            null,
            new CoordinateTransformationService(),
            Locale.GERMAN
        );
        GeoLocation parsedLocation = location.getAddresses().getFirst().getGeo();
        assertEquals(parsedLocation.getLatitude(), Double.toString(lat1));
        assertEquals(parsedLocation.getLongitude(), Double.toString(lon1));
    }

    @Test
    void testGeolocationNorthingEasting() {
        CodelistService codelistService = mockCodelistService();
        double lon1 = 10.220130725472366;
        double lat1 = 54.40550284486676;
        DirectPositionType pos = new DirectPositionType();
        pos.getValue().add(lat1);
        pos.getValue().add(lon1);

        PointType point = new PointType();
        point.getAxisLabels().add("northing");
        point.getAxisLabels().add("easting");
        point.setPos(pos);

        Geokodierung geo = new Geokodierung();
        geo.setPoint(point);

        Location location = XZuFiUtil.getLocation(
            createAnschriften(geo),
            new ArrayList<>(),
            codelistService,
            null,
            new CoordinateTransformationService(),
            Locale.GERMAN
        );
        GeoLocation parsedLocation = location.getAddresses().getFirst().getGeo();
        assertEquals(parsedLocation.getLatitude(), Double.toString(lat1));
        assertEquals(parsedLocation.getLongitude(), Double.toString(lon1));
    }

    @Test
    void testGeolocationEastingNorthing() {
        CodelistService codelistService = mockCodelistService();
        double lon1 = 10.220130725472366;
        double lat1 = 54.40550284486676;

        DirectPositionType pos = new DirectPositionType();
        pos.getValue().add(lon1);
        pos.getValue().add(lat1);

        PointType point = new PointType();
        point.getAxisLabels().add("easting");
        point.getAxisLabels().add("northing");
        point.setPos(pos);

        Geokodierung geo = new Geokodierung();
        geo.setPoint(point);

        Location location = XZuFiUtil.getLocation(
            createAnschriften(geo),
            new ArrayList<>(),
            codelistService,
            null,
            new CoordinateTransformationService(),
            Locale.GERMAN
        );
        GeoLocation parsedLocation = location.getAddresses().getFirst().getGeo();
        assertEquals(parsedLocation.getLatitude(), Double.toString(lat1));
        assertEquals(parsedLocation.getLongitude(), Double.toString(lon1));
    }

    @Test
    void testGeolocationPriority() {
        CodelistService codelistService = mockCodelistService();
        double lon1 = 10.220130725472366;
        double lat1 = 54.40550284486676;

        DirectPositionType pos = new DirectPositionType();
        pos.getValue().add(lat1);
        pos.getValue().add(lon1);
        pos.getAxisLabels().add("latitude");
        pos.getAxisLabels().add("longitude");

        PointType point = new PointType();
        point.getAxisLabels().add("longitude");
        point.getAxisLabels().add("latitude");
        point.setPos(pos);

        Geokodierung geo = new Geokodierung();
        geo.setPoint(point);

        Location location = XZuFiUtil.getLocation(
            createAnschriften(geo),
            new ArrayList<>(),
            codelistService,
            null,
            new CoordinateTransformationService(),
            Locale.GERMAN
        );
        GeoLocation parsedLocation = location.getAddresses().getFirst().getGeo();
        assertEquals(parsedLocation.getLatitude(), Double.toString(lat1));
        assertEquals(parsedLocation.getLongitude(), Double.toString(lon1));
    }

    private List<Anschrift> createAnschriften(Geokodierung geo) {
        AnschriftOrganisationseinheit anschrift = new AnschriftOrganisationseinheit();
        anschrift.setTyp(new CodeAnschrifttyp());
        anschrift.getGeokodierung().add(geo);
        List<Anschrift> addresses = new ArrayList<>();
        addresses.add(anschrift);
        return addresses;
    }

    private CodelistService mockCodelistService() {
        CodelistService codelistService = Mockito.mock(CodelistService.class);
        Mockito.when(
            codelistService.getCodelistEntry(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.isNull()
            )
        ).then(e -> CodelistEntry.empty("", "", "", ErrorTypeCodelist.CODE));
        return codelistService;
    }
}
