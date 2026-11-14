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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.xoev.xzufi.v2_2_0.CodeZeiteinheit;
import de.xoev.xzufi.v2_2_0.FristAuswahl;
import de.xoev.xzufi.v2_2_0.FristDauer;
import de.xoev.xzufi.v2_2_0.FristStichtag;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

/**
 * Für die Abfrage der Codelisteinträge wird ein Mock verwendet.
 * In der FristAuswahl wird ein Code definiert. Normalerweise so etwas wie SEKUNDE.
 * Bei Abfrage des Codes wird dann Sekunde oder second zurückgeliefert.
 * Für den Test wird nicht WERKTAG verwendet, sondern Werktag bzw. working day und
 * der Mock service gibt dann den gleichen Code zurück
 */
class FristHelperTest {

    private final CodelistService codelistService = Mockito.mock(CodelistService.class);

    private static final String URI_CODELIST_ZEITEINHEIT =
        "urn:xoev-de:fim:codeliste:xzufi.zeiteinheit";

    @BeforeEach
    void setup() {
        Mockito.when(
            codelistService.getCodelistEntry(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any()
            )
        ).then(a -> {
            var entry = new CodelistEntry();
            entry.setUri(a.getArgument(0));
            entry.setText(Locale.GERMAN, a.getArgument(2));
            entry.setText(Locale.ENGLISH, a.getArgument(2));
            if (a.getArgument(2) != null && !a.getArgument(2).equals("-")) {
                entry.setCode(a.getArgument(2));
            }
            return entry;
        });
    }

    @Nested
    class FristDauerTests {

        private static class TestDataBuilder {

            private boolean isCodeVonSet;
            private String codeVon;

            private boolean isCodeBisSet;
            private String codeBis;

            private Integer dauerVon;

            private Integer dauerBis;

            public TestDataBuilder codeVon(String code) {
                codeVon = code;
                isCodeVonSet = true;
                return this;
            }

            public TestDataBuilder codeBis(String code) {
                codeBis = code;
                isCodeBisSet = true;
                return this;
            }

            public TestDataBuilder dauerVon(Integer dauer) {
                dauerVon = dauer;
                return this;
            }

            public TestDataBuilder dauerBis(Integer dauer) {
                dauerBis = dauer;
                return this;
            }

            public FristAuswahl build() {
                var fristDauer = new FristDauer();

                if (dauerVon != null) {
                    fristDauer.setDauer(dauerVon);
                }
                if (isCodeVonSet) {
                    var einheit = new CodeZeiteinheit();
                    einheit.setCode(codeVon);
                    einheit.setListURI(URI_CODELIST_ZEITEINHEIT);
                    einheit.setListVersionID("1.1");
                    fristDauer.setEinheit(einheit);
                }

                if (dauerBis != null) {
                    fristDauer.setDauerBis(dauerBis);
                }
                if (isCodeBisSet) {
                    var einheit = new CodeZeiteinheit();
                    einheit.setCode(codeBis);
                    einheit.setListURI(URI_CODELIST_ZEITEINHEIT);
                    einheit.setListVersionID("1.1");
                    fristDauer.setEinheitBis(einheit);
                }

                var fristAuswahl = new FristAuswahl();
                fristAuswahl.setFristDauer(fristDauer);

                return fristAuswahl;
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Sekunde,1 - 5 Sekunden",
            "Minute,1 - 5 Minuten",
            "Stunde,1 - 5 Stunden",
            "Werktag,1 - 5 Werktage",
            "Tag,1 - 5 Tage",
            "Woche,1 - 5 Wochen",
            "Monat,1 - 5 Monate",
            "Jahr,1 - 5 Jahre",
        })
        void testSimpleDauerVonBis(String codeListValue, String expectedResult) throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .dauerVon(1)
                .codeVon(codeListValue)
                .dauerBis(5)
                .codeBis(codeListValue)
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.GERMAN);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "Sekunde,1 - 5 Sekunden",
            "Minute,1 - 5 Minuten",
            "Stunde,1 - 5 Stunden",
            "Werktag,1 - 5 Werktage",
            "Tag,1 - 5 Tage",
            "Woche,1 - 5 Wochen",
            "Monat,1 - 5 Monate",
            "Jahr,1 - 5 Jahre",
        })
        void testSimpleDauerVonBisOhneBisEinheit(String codeListValue, String expectedResult)
            throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .dauerVon(1)
                .codeVon(codeListValue)
                .dauerBis(5)
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.GERMAN);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "second,1 - 5 seconds",
            "minute,1 - 5 minutes",
            "hour,1 - 5 hours",
            "working day,1 - 5 working days",
            "day,1 - 5 days",
            "week,1 - 5 weeks",
            "month,1 - 5 months",
            "year,1 - 5 years",
        })
        void testSimpleDauerVonBisEnglisch(String codeListValue, String expectedResult)
            throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .dauerVon(1)
                .codeVon(codeListValue)
                .dauerBis(5)
                .codeBis(codeListValue)
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.ENGLISH);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "1,Sekunde,1 Sekunde",
            "5,Sekunde,5 Sekunden",
            "1,Minute,1 Minute",
            "5,Minute,5 Minuten",
            "1,Stunde,1 Stunde",
            "5,Stunde,5 Stunden",
            "1,Werktag,1 Werktag",
            "5,Werktag,5 Werktage",
            "1,Tag,1 Tag",
            "5,Tag,5 Tage",
            "1,Woche,1 Woche",
            "5,Woche,5 Wochen",
            "1,Monat,1 Monat",
            "5,Monat,5 Monate",
            "1,Jahr,1 Jahr",
            "5,Jahr,5 Jahre",
        })
        void testSimpleDauerVon(String dauer, String codeListValue, String expectedResult)
            throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .codeVon(codeListValue)
                .dauerVon(Integer.parseInt(dauer))
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.GERMAN);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "1,second,1 second",
            "5,second,5 seconds",
            "1,minute,1 minute",
            "5,minute,5 minutes",
            "1,hour,1 hour",
            "5,hour,5 hours",
            "1,working day,1 working day",
            "5,working day,5 working days",
            "1,day,1 day",
            "5,day,5 days",
            "1,week,1 week",
            "5,week,5 weeks",
            "1,month,1 month",
            "5,month,5 months",
            "1,year,1 year",
            "5,year,5 years",
        })
        void testSimpleDauerVonEnglisch(String dauer, String codeListValue, String expectedResult)
            throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .codeVon(codeListValue)
                .dauerVon(Integer.parseInt(dauer))
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.ENGLISH);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "Sekunde, Minute, 1, 1, 1 Sekunde bis 1 Minute",
            "Sekunde, Minute,1, 5, 1 Sekunde bis 5 Minuten",
            "Sekunde, Minute,2, 5, 2 Sekunden bis 5 Minuten",
            "Sekunde, Minute,2, 1, 2 Sekunden bis 1 Minute",
            "Stunde, Tag, 1, 1, 1 Stunde bis 1 Tag",
            "Stunde, Tag,1, 5, 1 Stunde bis 5 Tage",
            "Stunde, Tag,2, 5, 2 Stunden bis 5 Tage",
            "Stunde, Tag,2, 1, 2 Stunden bis 1 Tag",
            "Werktag, Woche, 1, 1, 1 Werktag bis 1 Woche",
            "Werktag, Woche,1, 5, 1 Werktag bis 5 Wochen",
            "Werktag, Woche,2, 5, 2 Werktage bis 5 Wochen",
            "Werktag, Woche,2, 1, 2 Werktage bis 1 Woche",
            "Monat, Jahr, 1, 1, 1 Monat bis 1 Jahr",
            "Monat, Jahr,1, 5, 1 Monat bis 5 Jahre",
            "Monat, Jahr,2, 5, 2 Monate bis 5 Jahre",
            "Monat, Jahr,2, 1, 2 Monate bis 1 Jahr",
        })
        void testMixedDauerVonBis(
            String codeVon,
            String codeBis,
            String dauerVon,
            String dauerBis,
            String expectedResult
        ) throws IOException {
            FristHelper helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .codeVon(codeVon)
                .dauerVon(Integer.parseInt(dauerVon))
                .codeBis(codeBis)
                .dauerBis(Integer.parseInt(dauerBis))
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.GERMAN);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @ParameterizedTest
        @CsvSource({
            "second, minute, 1, 1, 1 second to 1 minute",
            "second, minute,1, 5, 1 second to 5 minutes",
            "second, minute,2, 5, 2 seconds to 5 minutes",
            "second, minute,2, 1, 2 seconds to 1 minute",
            "hour, day, 1, 1, 1 hour to 1 day",
            "hour, day,1, 5, 1 hour to 5 days",
            "hour, day,2, 5, 2 hours to 5 days",
            "hour, day,2, 1, 2 hours to 1 day",
            "working day, week, 1, 1, 1 working day to 1 week",
            "working day, week,1, 5, 1 working day to 5 weeks",
            "working day, week,2, 5, 2 working days to 5 weeks",
            "working day, week,2, 1, 2 working days to 1 week",
            "month, year, 1, 1, 1 month to 1 year",
            "month, year,1, 5, 1 month to 5 years",
            "month, year,2, 5, 2 months to 5 years",
            "month, year,2, 1, 2 months to 1 year",
        })
        void testMixedDauerVonBisEnglisch(
            String codeVon,
            String codeBis,
            String dauerVon,
            String dauerBis,
            String expectedResult
        ) throws IOException {
            var helper = new FristHelper(codelistService);
            var fristAuswahl = new TestDataBuilder()
                .codeVon(codeVon)
                .dauerVon(Integer.parseInt(dauerVon))
                .codeBis(codeBis)
                .dauerBis(Integer.parseInt(dauerBis))
                .build();
            var result = helper.calculateDauer(fristAuswahl, Locale.ENGLISH);
            assertThat(result, equalTo(Optional.of(expectedResult)));
        }

        @Test
        void testSimpleDauerVonNegative() throws IOException {
            /*
        Fehlt die Einheit in der Fristauswahl, kann eine vernünftige Ausgabe nicht erfolgen.
        Es wird ein leeres Ergebnis Optional.empty() zurückggeben
         */
            var fristAuswahlMissingEinheit = new TestDataBuilder().dauerVon(1).build();
            var helper = new FristHelper(codelistService);
            var result = helper.calculateDauer(fristAuswahlMissingEinheit, Locale.GERMAN);
            assertThat(result, equalTo(Optional.empty()));

            /*
        Fehlt an der Einheit der Code in der Fristauswahl, kann eine vernünftige Ausgabe nicht erfolgen.
        Es wird ein leeres Ergebnis Optional.empty() zurückggeben
         */
            var fristAuswahlMissingCode = new TestDataBuilder().dauerVon(1).codeVon(null).build();
            helper = new FristHelper(codelistService);
            result = helper.calculateDauer(fristAuswahlMissingCode, Locale.GERMAN);
            assertThat(result, equalTo(Optional.empty()));
        }
    }

    @Nested
    class FristStichtagTests {

        private static class TestDataBuilder {

            private String von;
            private String bis;

            public TestDataBuilder von(String von) {
                this.von = von;
                return this;
            }

            public TestDataBuilder bis(String bis) {
                this.bis = bis;
                return this;
            }

            public FristAuswahl build() {
                var fristStichtag = new FristStichtag();

                if (this.von != null) {
                    FristStichtag.Von fristVon = new FristStichtag.Von();
                    fristVon.setDatum(toXmlCalendar(this.von));
                    fristStichtag.setVon(fristVon);
                }

                if (this.bis != null) {
                    FristStichtag.Bis fristBis = new FristStichtag.Bis();
                    fristBis.setDatum(toXmlCalendar(this.bis));
                    fristStichtag.setBis(fristBis);
                }

                var fristAuswahl = new FristAuswahl();
                fristAuswahl.setFristStichtag(fristStichtag);

                return fristAuswahl;
            }

            private XMLGregorianCalendar toXmlCalendar(String date) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date))));
                try {
                    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
                } catch (DatatypeConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @ParameterizedTest
        @CsvSource({
            "'2000-01-01T00:00:00Z', '2000-02-01T00:00:00Z', '01.01.2000 - 01.02.2000'",
            "'2000-01-01T00:00:00Z',                       , '01.01.2000'",
            "                     , '2000-01-01T00:00:00Z', '01.01.2000'",
            "                     ,                       ,",
        })
        void testStichtagVonBis(String von, String bis, String expectedResult) throws IOException {
            var fristAuswahl = new TestDataBuilder().von(von).bis(bis).build();
            var helper = new FristHelper(codelistService);
            Optional<String> result = helper.calculateDauer(fristAuswahl, Locale.GERMAN);
            if (expectedResult == null) {
                assertTrue(result.isEmpty());
            } else {
                assertTrue(result.isPresent());
                assertThat(result.get(), equalTo(expectedResult));
            }
        }
    }
}
