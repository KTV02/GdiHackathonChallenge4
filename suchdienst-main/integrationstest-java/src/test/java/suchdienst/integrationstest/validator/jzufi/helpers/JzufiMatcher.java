/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.validator.jzufi.helpers;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;

import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Erreichbarkeit;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.modultypen.Frist;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristDauer;
import de.dataport.pvog.testdatengenerator.model.modultypen.FristStichtag;
import de.dataport.pvog.testdatengenerator.model.modultypen.moduldokument.Dokument;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class JzufiMatcher {

    public static Matcher<? super Object> hasCode(String code, String listURI, String listVersionID) {
        return allOf(
            hasProperty("code", equalTo(code)),
            hasProperty("listURI", equalTo(listURI)),
            hasProperty("listVersionID", equalTo(listVersionID))
        );
    }

    public static Matcher<? super Object> hasTime(String propertyName, LocalTime dateTime) {
        return hasProperty(propertyName, hasToString(dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
    }

    public static Matcher<? super Object> hasDatum(String propertyName, LocalDate date) {
        LocalDateTime localDateTime = date.atStartOfDay();
        return hasProperty(
            propertyName,
            hasToString(
                localDateTime.atOffset(determineOffset(localDateTime)).format(DateTimeFormatter.ISO_OFFSET_DATE)
            )
        );
    }

    public static Matcher<Map<? extends String, ? extends String>> matchesDatumUndZeit(
        String propertyName,
        LocalDateTime date
    ) {
        return hasEntry(
            propertyName,
            date.atOffset(determineOffset(date)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

    public static Matcher<? super Object> matchesCode(String propertyName, List<String> codes) {
        return hasProperty(
            propertyName,
            containsInAnyOrder(
                codes
                    .stream()
                    .map(code -> hasProperty("code", equalTo(code)))
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object> matchesBeschreibung(String expectedBeschreibungen) {
        if (expectedBeschreibungen != null) {
            return hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expectedBeschreibungen))));
        } else {
            return Matchers.anything();
        }
    }

    public static Matcher<? super Object> matchesBezeichnungen(List<String> bezeichnungen) {
        return hasProperty(
            "bezeichnung",
            containsInAnyOrder(
                bezeichnungen
                    .stream()
                    .map(bezeichnung -> hasProperty("value", equalTo(bezeichnung)))
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object> hasBeschreibungen(String... expected) {
        return hasProperty(
            "beschreibung",
            hasItems(
                Stream.of(expected)
                    .filter(Objects::nonNull)
                    .map(beschreibung -> hasProperty("value", equalTo(beschreibung)))
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object> dateStartsWith(int year, int month, int day) {
        return hasToString(startsWith(String.format("%04d-%02d-%02d", year, month, day)));
    }

    public static Matcher<? super Object> matchesWeiterfuehrenderLink(List<Link> expectedLinks) {
        return hasProperty(
            "weiterfuehrenderLink",
            containsInAnyOrder(
                expectedLinks
                    .stream()
                    .flatMap(link ->
                        Stream.of(
                            link.getUri() == null
                                ? null
                                : allOf(
                                      hasProperty("uri", equalTo(link.getUri())),
                                      hasProperty("beschreibung", equalTo(link.getBeschreibung())),
                                      hasProperty("positionDarstellung", equalTo(link.getPositionDarstellung())),
                                      hasProperty("titel", equalTo(link.getTitel()))
                                  ),
                            link.getUriEnglisch() == null
                                ? null
                                : allOf(
                                      hasProperty("uri", equalTo(link.getUriEnglisch())),
                                      hasProperty("beschreibung", equalTo(link.getBeschreibungEnglisch())),
                                      hasProperty(
                                          "positionDarstellung",
                                          equalTo(link.getPositionDarstellungEnglisch())
                                      ),
                                      hasProperty("titel", equalTo(link.getTitelEnglisch()))
                                  )
                        )
                    )
                    .filter(Objects::nonNull)
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object> matchesDokument(String propertyName, List<Dokument> expected) {
        return hasProperty(
            propertyName,
            containsInAnyOrder(
                expected
                    .stream()
                    .map(dokument ->
                        allOf(
                            hasProperty(
                                "typ",
                                hasProperty("code", hasProperty("code", equalTo(dokument.getTypCode())))
                            ),
                            matchesBezeichnungen(List.of(dokument.getBezeichnung(), dokument.getBezeichnungEnglisch())),
                            matchesBeschreibung(dokument.getBeschreibung())
                        )
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object> matchesFristauswahl(Frist frist) {
        if (frist instanceof FristDauer dauer) {
            return hasProperty(
                "fristauswahl",
                hasProperty(
                    "fristDauer",
                    allOf(
                        hasProperty("dauer", equalTo(dauer.getDauer())),
                        hasProperty("einheit", hasProperty("code", equalTo(dauer.getEinheit()))),
                        hasProperty("dauerBis", equalTo(dauer.getDauerBis())),
                        hasProperty("einheitBis", hasProperty("code", equalTo(dauer.getEinheitBis())))
                    )
                )
            );
        } else if (frist instanceof FristStichtag stichtag) {
            return hasProperty(
                "fristauswahl",
                hasProperty(
                    "fristStichtag",
                    allOf(
                        hasProperty(
                            "von",
                            hasProperty(
                                "datum",
                                dateStartsWith(stichtag.getVonJahr(), stichtag.getVonMonat(), stichtag.getVonTag())
                            )
                        ),
                        hasProperty(
                            "bis",
                            hasProperty(
                                "datum",
                                dateStartsWith(stichtag.getBisJahr(), stichtag.getBisMonat(), stichtag.getBisTag())
                            )
                        )
                    )
                )
            );
        }
        return anything();
    }

    public static Matcher<? super Object> hasLink(String property, Link expected) {
        return hasProperty(
            property,
            hasItems(
                expected.getUri() != null ? hasProperty("uri", equalTo(expected.getUri())) : anything(),
                expected.getUriEnglisch() != null ? hasProperty("uri", equalTo(expected.getUriEnglisch())) : anything()
            )
        );
    }

    public static Matcher<? super Object> hasErreichbarkeiten(
        List<Erreichbarkeit> expected,
        CodelistConfig22 config22
    ) {
        return hasProperty(
            "erreichbarkeit",
            hasItems(
                expected
                    .stream()
                    .map(erreichbarkeit ->
                        allOf(
                            hasProperty(
                                "kanal",
                                hasCode(
                                    erreichbarkeit.getCode(),
                                    CodelistConfig22.ERREICHBARKEIT,
                                    config22.getVersion(CodelistConfig22.ERREICHBARKEIT)
                                )
                            ),
                            hasProperty("kennung", equalTo(erreichbarkeit.getKennung()))
                        )
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    public static Matcher<? super Object>[] allOfAnschriften(List<Anschrift> expected, CodelistConfig22 config22) {
        return expected
            .stream()
            .map(anschrift ->
                allOf(
                    hasProperty(
                        "typ",
                        hasCode(
                            anschrift.getTyp(),
                            CodelistConfig22.ANSCHRIFTTYP,
                            config22.getVersion(CodelistConfig22.ANSCHRIFTTYP)
                        )
                    ),
                    hasProperty("postleitzahl", equalTo(anschrift.getPostleitzahl())),
                    hasProperty("ort", equalTo(anschrift.getOrt()))
                )
            )
            .<Matcher<? super Object>>toArray(Matcher[]::new);
    }

    public static Matcher<? super Object> allOfSprachversionen(String code, String bezeichnung) {
        return allOf(
            hasProperty("languageCode", equalTo(code)),
            hasProperty("sprachbezeichnungDeutsch", equalTo(bezeichnung))
        );
    }

    private static ZoneOffset determineOffset(LocalDateTime dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("Europe/Berlin"));
        return zonedDateTime.getOffset();
    }
}
