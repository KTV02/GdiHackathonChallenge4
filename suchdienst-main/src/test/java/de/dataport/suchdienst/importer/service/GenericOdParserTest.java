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
package de.dataport.suchdienst.importer.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;

class GenericOdParserTest {

    private static final TransactionMetadata META_DATA = new TransactionMetadata(
        "1",
        "1",
        "1",
        "1",
        "1"
    );

    private final GenericOdParser parser = new GenericOdParser();

    @Test
    void parseOdsSetsValidityPeriodsFromGueltigkeitX22() throws DatatypeConfigurationException {
        de.xoev.xzufi.v2_2_0.OnlinedienstErweitert xod =
            new de.xoev.xzufi.v2_2_0.OnlinedienstErweitert();
        xod.setId(identifikator22("od-1"));
        de.xoev.xzufi.v2_2_0.Zeitraum zeitraum = new de.xoev.xzufi.v2_2_0.Zeitraum();
        zeitraum.setBeginn(xmlDate(2020, 8, 1));
        zeitraum.setEnde(xmlDate(2080, 8, 1));
        xod.getGueltigkeit().add(zeitraum);

        List<OnlinedienstEntity> result = parser.parseOnlinediensteElastic(
            Stream.of(new GenericOnlinedienst(xod)),
            META_DATA,
            Instant.now()
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValidityPeriods()).containsExactly(
            new ValidityPeriod(LocalDate.of(2020, 8, 1), LocalDate.of(2080, 8, 1))
        );
    }

    @Test
    void parseOdsSetsValidityPeriodsFromGueltigkeitX23() throws DatatypeConfigurationException {
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert xod =
            new de.xoev.xzufi.v2_3_1.OnlinedienstErweitert();
        xod.setId(identifikator23("od-1"));
        de.xoev.xzufi.v2_3_1.Zeitraum zeitraum = new de.xoev.xzufi.v2_3_1.Zeitraum();
        zeitraum.setBeginn(xmlDate(2020, 8, 1));
        zeitraum.setEnde(xmlDate(2080, 8, 1));
        xod.getGueltigkeit().add(zeitraum);

        List<OnlinedienstEntity> result = parser.parseOnlinediensteElastic(
            Stream.of(new GenericOnlinedienst(xod)),
            META_DATA,
            Instant.now()
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getValidityPeriods()).containsExactly(
            new ValidityPeriod(LocalDate.of(2020, 8, 1), LocalDate.of(2080, 8, 1))
        );
    }

    @Test
    void parseOdsDefaultsValidityPeriodWhenGueltigkeitIsEmptyX22() {
        de.xoev.xzufi.v2_2_0.OnlinedienstErweitert xod =
            new de.xoev.xzufi.v2_2_0.OnlinedienstErweitert();
        xod.setId(identifikator22("od-1"));

        List<OnlinedienstEntity> result = parser.parseOnlinediensteElastic(
            Stream.of(new GenericOnlinedienst(xod)),
            META_DATA,
            Instant.now()
        );

        assertThat(result.getFirst().getValidityPeriods()).containsExactly(
            new ValidityPeriod(LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))
        );
    }

    private static de.xoev.xzufi.v2_2_0.Identifikator identifikator22(String value) {
        de.xoev.xzufi.v2_2_0.Identifikator id = new de.xoev.xzufi.v2_2_0.Identifikator();
        id.setValue(value);
        id.setSchemeAgencyID("de.bund");
        return id;
    }

    private static de.xoev.xzufi.v2_3_1.Identifikator identifikator23(String value) {
        de.xoev.xzufi.v2_3_1.Identifikator id = new de.xoev.xzufi.v2_3_1.Identifikator();
        id.setValue(value);
        id.setSchemeAgencyID("de.bund");
        return id;
    }

    private static XMLGregorianCalendar xmlDate(int year, int month, int day)
        throws DatatypeConfigurationException {
        GregorianCalendar calendar = GregorianCalendar.from(
            LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC)
        );
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    }
}
