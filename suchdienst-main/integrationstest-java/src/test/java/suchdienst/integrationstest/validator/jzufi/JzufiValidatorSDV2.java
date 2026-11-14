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
package suchdienst.integrationstest.validator.jzufi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.allOfSprachversionen;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasCode;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasDatum;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesBeschreibung;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesCode;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesDatumUndZeit;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesDokument;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesFristauswahl;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesWeiterfuehrenderLink;

import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.model.LeiKaTypisierung;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulbearbeitungsdauer.Bearbeitungsdauermodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.moduldokument.Dokumentmodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenFix;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenFrei;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenVariabel;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import de.xoev.xzufi.v2_2_0.CodeSDGInformationsbereich;
import de.xoev.xzufi.v2_2_0.Fristmodul;
import de.xoev.xzufi.v2_2_0.Kosten;
import de.xoev.xzufi.v2_2_0.Sprachversion;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class JzufiValidatorSDV2 {

    private final ValidatableResponse then;
    private final CodelistConfig22 codelistConfig = new CodelistConfig22();

    public JzufiValidatorSDV2(Response response) {
        then = response.then();
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON);
    }

    public void validate(Leistungsbeschreibung expected) {
        if (expected.getId() != null && expected.getSchemeAgencyId() != null) {
            validateId(expected.getId(), expected.getSchemeAgencyId());
        }
        if (expected.getReferenzLeiKa() != null) {
            validateReferenzLeiKa(expected.getReferenzLeiKa());
        }
        if (expected.getTypisierung() != null) {
            expected.getTypisierung().stream().map(LeiKaTypisierung::getCode).forEach(this::validateTypisierung);
        }
        if (expected.getTextmodule() != null) {
            validateTextmodule(expected.getTextmodule());
        }
        if (expected.getUrsprungsportale() != null) {
            expected.getUrsprungsportale().forEach(this::validateModulUrsprungsportal);
        }
        if (expected.getKostenmodule() != null) {
            validateModulKosten(expected.getKostenmodule());
        }
        if (expected.getBearbeitungsdauermodule() != null) {
            expected.getBearbeitungsdauermodule().forEach(this::validateModulBearbeitungsdauer);
        }
        if (expected.getDokumentmodule() != null) {
            expected.getDokumentmodule().forEach(this::validateModulDokument);
        }
        if (expected.getFristmodule() != null) {
            expected.getFristmodule().forEach(this::validateModulFrist);
        }
        if (expected.getSdgInformationen() != null) {
            validateInformationsbereichSDG(expected.getSdgInformationen());
        }
        if (expected.getGueltigkeiten() != null) {
            validateGueltigkeiten(expected.getGueltigkeiten());
        }
        if (expected.getGueltigkeitGebietIDs() != null) {
            expected
                .getGueltigkeitGebietIDs()
                .forEach(id -> validateGueltigkeitGebietID(id, expected.getSchemeAgencyId()));
        }
        if (expected.getVersionsinformation() != null) {
            validateVersionsinformation(expected.getVersionsinformation());
        }
        if (!expected.getKlassifizierung().isEmpty()) {
            validateKlassifizierung(expected.getKlassifizierung());
        }
    }

    public JzufiValidatorSDV2 validateReferenzLeiKa(String referenzLeiKa) {
        then.body("referenzLeiKa", hasItem(hasEntry("code", referenzLeiKa)));
        return this;
    }

    public JzufiValidatorSDV2 validateTypisierung(String code) {
        then.body("typisierung", hasItem(hasEntry("code", code)));
        return this;
    }

    public JzufiValidatorSDV2 validateGueltigkeitGebietID(String ars, String schemeAgencyId) {
        then.body(
            "gueltigkeitGebietID",
            containsInAnyOrder(allOf(hasEntry("value", ars), hasEntry("schemeAgencyID", schemeAgencyId)))
        );

        return this;
    }

    public JzufiValidatorSDV2 validateSprachversion(String languageCode, String sprachbezeichungDeutsch) {
        List<Sprachversion> actual = then.extract().jsonPath().getList("sprachversion", Sprachversion.class);
        assertThat(actual, hasItem(allOfSprachversionen(languageCode, sprachbezeichungDeutsch)));

        return this;
    }

    public JzufiValidatorSDV2 validateModulUrsprungsportal(UrsprungsportalModul modulUrsprungsportal) {
        if (modulUrsprungsportal.getUri() != null) {
            validateModulUrspungsportal(modulUrsprungsportal.getUri(), modulUrsprungsportal.getTitle(), "de");
        }
        if (modulUrsprungsportal.getUriEnglisch() != null) {
            validateModulUrspungsportal(
                modulUrsprungsportal.getUriEnglisch(),
                modulUrsprungsportal.getTitleEnglisch(),
                "en"
            );
        }
        return this;
    }

    public JzufiValidatorSDV2 validateTextmodule(List<Textmodul> textmodule) {
        List<de.xoev.xzufi.v2_2_0.Textmodul> actual = then
            .extract()
            .jsonPath()
            .getList("modulText", de.xoev.xzufi.v2_2_0.Textmodul.class);
        textmodule.forEach(textmodul -> validateTextmodul(actual, textmodul));
        return this;
    }

    public JzufiValidatorSDV2 validateTextmodul(List<de.xoev.xzufi.v2_2_0.Textmodul> actual, Textmodul expected) {
        assertThat(
            actual,
            hasItem(
                allOf(
                    hasProperty("positionDarstellung", equalTo(expected.getPositionDarstellung())),
                    hasProperty("leikaTextmodul", hasProperty("code", equalTo(expected.getLeikaCode()))),
                    hasProperty("inhalt", hasItem(hasProperty("value", equalTo(expected.getInhalt()))))
                )
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateModulKosten(List<Kostenmodul> kostenmodul) {
        de.xoev.xzufi.v2_2_0.Kostenmodul actualModulKosten = then
            .extract()
            .jsonPath()
            .getObject("modulKosten", de.xoev.xzufi.v2_2_0.Kostenmodul.class);

        kostenmodul
            .stream()
            .flatMap(modul -> modul.getKosten().stream())
            .forEach(kosten -> {
                if (kosten instanceof KostenFrei kostenFrei) {
                    validateKostenFrei(actualModulKosten.getKosten(), kostenFrei);
                } else if (kosten instanceof KostenFix kostenFix) {
                    validateKostenFix(actualModulKosten.getKosten(), kostenFix);
                } else if (kosten instanceof KostenVariabel kostenVariabel) {
                    validateKostenVariabel(actualModulKosten.getKosten(), kostenVariabel);
                }
            });
        return this;
    }

    private JzufiValidatorSDV2 validateKostenFix(List<Kosten> actual, KostenFix expected) {
        assertThat(
            "KostenFix abgleichen",
            actual,
            hasItem(
                allOf(
                    hasProperty("typ", hasProperty("code", hasProperty("code", equalTo(expected.getCodeTyp())))),
                    hasProperty(
                        "kostenauswahl",
                        hasProperty(
                            "kostenFix",
                            hasProperty("betrag", hasProperty("value", equalTo(expected.getBetrag().getValue())))
                        )
                    ),
                    hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expected.getBeschreibungen()))))
                )
            )
        );
        return this;
    }

    private JzufiValidatorSDV2 validateKostenFrei(List<Kosten> actual, KostenFrei expected) {
        assertThat(
            "KostenFrei abgleichen",
            actual,
            hasItem(
                allOf(
                    hasProperty("typ", hasProperty("code", hasProperty("code", equalTo(expected.getCodeTyp())))),
                    hasProperty(
                        "kostenauswahl",
                        hasProperty(
                            "kostenFrei",
                            hasProperty(
                                "beschreibungKostenfreiheit",
                                hasItem(hasProperty("value", equalTo(expected.getBeschreibungenKostenfreiheit())))
                            )
                        )
                    ),
                    hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expected.getBeschreibungen()))))
                )
            )
        );
        return this;
    }

    private JzufiValidatorSDV2 validateKostenVariabel(List<Kosten> actual, KostenVariabel expected) {
        assertThat(
            "KostenVariabel abgleichen",
            actual,
            hasItem(
                allOf(
                    hasProperty("typ", hasProperty("code", hasProperty("code", equalTo(expected.getCodeTyp())))),
                    hasProperty(
                        "kostenauswahl",
                        hasProperty(
                            "kostenVariabel",
                            allOf(
                                hasProperty(
                                    "betragUntergrenze",
                                    hasProperty("value", equalTo(expected.getBetragUntergrenze().getValue()))
                                ),
                                hasProperty(
                                    "betragObergrenze",
                                    hasProperty("value", equalTo(expected.getBetragObergrenze().getValue()))
                                ),
                                hasProperty(
                                    "beschreibungVariabilitaet",
                                    hasItem(hasProperty("value", equalTo(expected.getBeschreibungenVariabilitaet())))
                                )
                            )
                        )
                    ),
                    hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expected.getBeschreibungen())))),
                    hasProperty("linkKostenbildung", equalTo(expected.getLinkKostenbildung().getUri())),
                    hasProperty("positionDarstellung", equalTo(expected.getPositionDarstellung()))
                )
            )
        );
        return this;
    }

    private void validateModulUrspungsportal(String uri, String titel, String languageCode) {
        then.body(
            "modulUrsprungsportal",
            hasItem(
                Matchers.<Map<String, String>>allOf(
                    hasEntry("uri", uri),
                    titel != null ? hasEntry("titel", titel) : anything(),
                    hasEntry("languageCode", languageCode)
                )
            )
        );
    }

    public JzufiValidatorSDV2 validateModulBearbeitungsdauer(Bearbeitungsdauermodul expected) {
        de.xoev.xzufi.v2_2_0.Bearbeitungsdauermodul actual = then
            .extract()
            .jsonPath()
            .getObject("modulBearbeitungsdauer", de.xoev.xzufi.v2_2_0.Bearbeitungsdauermodul.class);

        assertThat(
            "Bearbeitungsdauermodul abgleichen",
            actual,
            allOf(
                hasProperty("positionDarstellung", equalTo(expected.getPositionDarstellung())),
                hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expected.getBeschreibung())))),
                matchesWeiterfuehrenderLink(expected.getWeiterfuehrendeLink())
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateModulFrist(
        de.dataport.pvog.testdatengenerator.model.modultypen.modulfrist.Fristmodul expected
    ) {
        Fristmodul actual = then.extract().jsonPath().getObject("modulFrist", Fristmodul.class);

        assertThat(
            "Fristmodul abgleichen",
            actual,
            allOf(
                hasProperty("positionDarstellung", equalTo(expected.getPositionDarstellung())),
                hasProperty("beschreibung", hasItem(hasProperty("value", equalTo(expected.getBeschreibung())))),
                matchesWeiterfuehrenderLink(expected.getLinks()),
                hasProperty(
                    "frist",
                    containsInAnyOrder(
                        expected
                            .getFristen()
                            .stream()
                            .map(frist ->
                                allOf(
                                    hasProperty(
                                        "typ",
                                        hasProperty("code", hasProperty("code", equalTo(frist.getTyp())))
                                    ),
                                    matchesBeschreibung(frist.getBeschreibungen()),
                                    matchesBeschreibung(frist.getBeschreibungenEnglisch()),
                                    hasProperty("positionDarstellung", equalTo(frist.getPositionDarstellung())),
                                    matchesFristauswahl(frist)
                                )
                            )
                            .<Matcher<? super Object>>toArray(Matcher[]::new)
                    )
                )
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateModulDokument(Dokumentmodul expected) {
        de.xoev.xzufi.v2_2_0.Dokumentmodul actual = then
            .extract()
            .jsonPath()
            .getObject("modulDokument", de.xoev.xzufi.v2_2_0.Dokumentmodul.class);

        assertThat(
            "Dokumentmodul abgleichen",
            actual,
            allOf(
                expected.getPositionDarstellung() != null
                    ? hasProperty("positionDarstellung", equalTo(expected.getPositionDarstellung()))
                    : anything(),
                matchesBeschreibung(expected.getBeschreibungEingehend()),
                matchesWeiterfuehrenderLink(expected.getLinks()),
                matchesDokument("dokumentEingehend", expected.getEingehend()),
                matchesDokument("dokumentAusgehend", expected.getAusgehend())
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateVertrauensniveau(String code) {
        then.body("vertrauensniveau.code", equalTo(code));
        return this;
    }

    public JzufiValidatorSDV2 validateLeistungsadressat(List<String> codes) {
        then.body(
            "leistungsadressat",
            containsInAnyOrder(
                codes
                    .stream()
                    .map(code -> hasEntry("code", code))
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateKlassifizierung(List<String> codes) {
        then.body(
            "klassifizierung",
            containsInAnyOrder(
                codes
                    .stream()
                    .map(code ->
                        allOf(
                            hasEntry("code", code),
                            hasEntry("listURI", CodelistConfig22.PVLAGEN),
                            hasEntry("listVersionID", codelistConfig.getVersion(CodelistConfig22.PVLAGEN))
                        )
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateInformationsbereichSDG(List<String> codes) {
        List<CodeSDGInformationsbereich> actual = then
            .extract()
            .jsonPath()
            .getList("informationsbereichSDG", CodeSDGInformationsbereich.class);

        assertThat(
            actual,
            containsInAnyOrder(
                codes
                    .stream()
                    .map(code ->
                        hasCode(
                            code,
                            CodelistConfig22.SDGINFORMATIONSBEREICH,
                            codelistConfig.getVersion(CodelistConfig22.SDGINFORMATIONSBEREICH)
                        )
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateRelevantFuerWirtschaftszweig(List<String> codes) {
        then.body(matchesCode("relevantFuerWirtschaftszweig", codes));
        return this;
    }

    public JzufiValidatorSDV2 validateRelevantFuerRechtsform(List<String> codes) {
        then.body(matchesCode("relevantFuerRechtsform", codes));
        return this;
    }

    public JzufiValidatorSDV2 validateRelevantFuerStaatsangehoerigkeit(List<String> codes) {
        then.body(matchesCode("relevantFuerStaatsangehoerigkeit", codes));
        return this;
    }

    public JzufiValidatorSDV2 validateVersionsinformation(Versionsinformation versionsinformation) {
        then.body(
            "versionsinformation",
            allOf(
                hasEntry("version", versionsinformation.getVersion()),
                hasEntry("geaendertDurch", versionsinformation.getBearbeiter()),
                hasEntry("erstelltDurch", versionsinformation.getErsteller()),
                matchesDatumUndZeit("erstelltDatumZeit", versionsinformation.getErstelldatum()),
                matchesDatumUndZeit("geaendertDatumZeit", versionsinformation.getBearbeitungsdatum())
            )
        );
        return this;
    }

    public JzufiValidatorSDV2 validateId(String lbId, String schemeAgencyId) {
        then.body("id", allOf(hasEntry("value", lbId), hasEntry("schemeAgencyID", schemeAgencyId)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public JzufiValidatorSDV2 validateGueltigkeiten(List<Zeitraum> gueltigkeiten) {
        List<de.xoev.xzufi.v2_2_0.Zeitraum> actual = then
            .extract()
            .jsonPath()
            .getList("gueltigkeit", de.xoev.xzufi.v2_2_0.Zeitraum.class);

        assertThat(
            actual,
            containsInAnyOrder(
                gueltigkeiten
                    .stream()
                    .map(gueltigkeit ->
                        allOf(hasDatum("beginn", gueltigkeit.getBeginn()), hasDatum("ende", gueltigkeit.getEnde()))
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );

        return this;
    }
}
