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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasTime;

import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.xoev.xzufi.v2_3_1.FormulardateiLink;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.Sprachversion;
import de.xoev.xzufi.v2_3_1.StringLocalized;
import de.xoev.xzufi.v2_3_1.Zeitserien;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;

/**
 * Hamcrest-basierter Validator für den Endpunkt {@code /v2/organisationunits/jzufi}
 * mit XZuFi-Version 2.3.1.
 */
public class JzufiValidatorOUV23 extends AbstractJzufiValidatorOU<JzufiValidatorOUV23> {

    private final CodelistConfig23 codelistConfig = new CodelistConfig23();

    public JzufiValidatorOUV23(Response response) {
        super(response, "application/json;xzufi-version=2.3.1");
    }

    @Override
    protected JzufiValidatorOUV23 self() {
        return this;
    }

    @Override
    public JzufiValidatorOUV23 validateInfoSonstige(List<String> expected) {
        // In v2.3: infoSonstige[].inhalt.value
        then.body(
            "infoSonstige.inhalt",
            hasItems(
                expected
                    .stream()
                    .map(s -> hasEntry("value", s))
                    .<Matcher<? extends Map<? extends String, ? extends String>>>toArray(Matcher[]::new)
            )
        );
        return this;
    }

    @Override
    public JzufiValidatorOUV23 validateHerausgeber(Herausgeber expected) {
        de.xoev.xzufi.v2_3_1.Herausgeber actual = then
            .extract()
            .jsonPath()
            .getObject("herausgeber", de.xoev.xzufi.v2_3_1.Herausgeber.class);

        assertThat(
            "Herausgeber abgleich",
            actual,
            allOf(
                hasProperty("id", hasProperty("value", equalTo(expected.getId()))),
                hasProperty("bezeichnung", hasItem(expected.getBezeichnung())),
                hasProperty("kurzbezeichnung", hasItem(expected.getKurzbezeichnung())),
                hasProperty(
                    "beschreibung",
                    hasItems(
                        hasProperty("value", equalTo(expected.getBeschreibung())),
                        expected.getBeschreibungEnglisch() != null
                            ? hasProperty("value", equalTo(expected.getBeschreibungEnglisch()))
                            : anything()
                    )
                )
            )
        );
        return this;
    }

    @Override
    public JzufiValidatorOUV23 validateKontaktperson(Kontaktperson expected) {
        List<de.xoev.xzufi.v2_3_1.Kontaktperson> actual = then
            .extract()
            .jsonPath()
            .getList("kontaktperson", de.xoev.xzufi.v2_3_1.Kontaktperson.class);

        assertThat(
            "Kontaktperson abgleich",
            actual,
            hasItem(
                allOf(
                    hasProperty("id", hasProperty("value", equalTo(expected.getId()))),
                    hasProperty("anrede", hasItem(hasProperty("value", equalTo(expected.getAnrede())))),
                    hasProperty("titel", hasItem(hasProperty("value", equalTo(expected.getTitel())))),
                    hasProperty("vorname", equalTo(expected.getVorname())),
                    hasProperty("familienname", equalTo(expected.getFamilienname())),
                    hasProperty("raum", equalTo(expected.getRaum())),
                    hasProperty("anschrift", hasItems(allOfAnschriften23(expected.getAnschriften()))),
                    hasErreichbarkeiten23(expected.getErreichbarkeiten()),
                    hasLink23("internetadresse", expected.getInternetadressen())
                )
            )
        );

        return this;
    }

    @Override
    public JzufiValidatorOUV23 validateAnschriften(List<Anschrift> expected) {
        List<de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit> actual = then
            .extract()
            .jsonPath()
            .getList("anschrift", de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit.class);

        assertThat(actual, hasItems(allOfAnschriften23(expected)));
        return this;
    }

    @Override
    public JzufiValidatorOUV23 validateInfoOeffnungszeitenStrukturiert(
        List<de.dataport.pvog.testdatengenerator.model.Zeitserien> expected
    ) {
        List<Zeitserien> actual = then
            .extract()
            .jsonPath()
            .getList("infoOeffnungszeitenStrukturiert", Zeitserien.class);

        expected.forEach(oeffnungszeit ->
            assertThat(
                actual,
                hasItem(
                    allOf(
                        hasProperty("typ", hasCode23(oeffnungszeit.getType(), CodelistConfig23.ZEITSERIENTYP)),
                        // In v2.3: hinweistext ist eine Liste von StringLocalized
                        hasProperty(
                            "hinweistext",
                            hasItem(
                                anyOf(
                                    hasProperty("value", equalTo(oeffnungszeit.getHinweistextEnglisch())),
                                    hasProperty("value", equalTo(oeffnungszeit.getHinweistext()))
                                )
                            )
                        ),
                        hasProperty(
                            "regulaereZeiten",
                            hasItems(
                                oeffnungszeit
                                    .getRegulaereZeiten()
                                    .stream()
                                    .map(zeitraum ->
                                        allOf(
                                            hasTime("beginn", zeitraum.getBeginn()),
                                            hasTime("ende", zeitraum.getEnde()),
                                            hasProperty(
                                                "tagestyp",
                                                hasCode23(zeitraum.getTagestyp(), CodelistConfig23.TAGESTYP)
                                            ),
                                            hasProperty(
                                                "tagesposition",
                                                hasCode23(zeitraum.getTagesposition(), CodelistConfig23.TAGESPOSITION)
                                            )
                                        )
                                    )
                                    .<Matcher<? super Object>>toArray(Matcher[]::new)
                            )
                        )
                    )
                )
            )
        );

        return this;
    }

    @Override
    protected List<?> extractBankverbindungen() {
        return then.extract().jsonPath().getList("bankverbindung", de.xoev.xzufi.v2_3_1.Bankverbindung.class);
    }

    @Override
    protected List<?> extractGueltigkeiten() {
        return then.extract().jsonPath().getList("gueltigkeit", de.xoev.xzufi.v2_3_1.Zeitraum.class);
    }

    @Override
    protected List<?> extractFormulare() {
        return getFormulare();
    }

    @Override
    protected Matcher<? super Object> matchesDatei(Formular formular) {
        // In v2.3: datei[].link.uri statt datei[].uri
        return hasProperty(
            "datei",
            hasItems(
                allOf(
                    hasProperty("link", hasProperty("uri", equalTo(formular.getLink().getUri()))),
                    hasProperty("mimeCode", equalTo(formular.getMimeCode()))
                ),
                formular.getLink().getUriEnglisch() != null
                    ? allOf(
                          hasProperty("link", hasProperty("uri", equalTo(formular.getLink().getUriEnglisch()))),
                          hasProperty("mimeCode", equalTo(formular.getMimeCode()))
                      )
                    : anything()
            )
        );
    }

    private List<de.xoev.xzufi.v2_3_1.Formular> getFormulare() {
        // Formular enthält abstrakte Klassen, weshalb kein einfaches Mapping möglich ist
        JsonPath jsonPath = then.extract().jsonPath();
        int count = jsonPath.getList("formular").size();
        List<de.xoev.xzufi.v2_3_1.Formular> forms = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            de.xoev.xzufi.v2_3_1.Formular formular = new de.xoev.xzufi.v2_3_1.Formular();

            formular.setId(jsonPath.getObject("formular[" + i + "].id", Identifikator.class));
            formular
                .getBezeichnung()
                .addAll(jsonPath.getList("formular[" + i + "].bezeichnung", StringLocalized.class));
            formular.getDatei().addAll(jsonPath.getList("formular[" + i + "].datei", FormulardateiLink.class));
            formular
                .getSprachversion()
                .addAll(jsonPath.getList("formular[" + i + "].sprachversion", Sprachversion.class));

            forms.add(formular);
        }

        return forms;
    }

    // --- v2.3-spezifische Hilfsmethoden ---

    private Matcher<? super Object> hasCode23(String code, String listURI) {
        return allOf(
            hasProperty("code", equalTo(code)),
            hasProperty("listURI", equalTo(listURI)),
            hasProperty("listVersionID", equalTo(codelistConfig.getVersion(listURI)))
        );
    }

    @SuppressWarnings("unchecked")
    private Matcher<? super Object>[] allOfAnschriften23(List<Anschrift> expected) {
        return expected
            .stream()
            .map(anschrift ->
                allOf(
                    hasProperty("typ", hasCode23(anschrift.getTyp(), CodelistConfig23.ANSCHRIFTTYP)),
                    hasProperty("postleitzahl", equalTo(anschrift.getPostleitzahl())),
                    hasProperty("ort", equalTo(anschrift.getOrt()))
                )
            )
            .<Matcher<? super Object>>toArray(Matcher[]::new);
    }

    private Matcher<? super Object> hasErreichbarkeiten23(
        List<de.dataport.pvog.testdatengenerator.model.Erreichbarkeit> expected
    ) {
        return hasProperty(
            "erreichbarkeit",
            hasItems(
                expected
                    .stream()
                    .map(erreichbarkeit ->
                        allOf(
                            hasProperty("kanal", hasCode23(erreichbarkeit.getCode(), CodelistConfig23.ERREICHBARKEIT)),
                            hasProperty("kennung", equalTo(erreichbarkeit.getKennung()))
                        )
                    )
                    .<Matcher<? super Object>>toArray(Matcher[]::new)
            )
        );
    }

    private Matcher<? super Object> hasLink23(String property, Link expected) {
        return hasProperty(
            property,
            hasItems(
                expected.getUri() != null ? hasProperty("uri", equalTo(expected.getUri())) : anything(),
                expected.getUriEnglisch() != null ? hasProperty("uri", equalTo(expected.getUriEnglisch())) : anything()
            )
        );
    }
}
