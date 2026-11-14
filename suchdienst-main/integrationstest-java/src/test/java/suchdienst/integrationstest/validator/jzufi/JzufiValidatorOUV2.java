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
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.allOfAnschriften;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasBeschreibungen;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasCode;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasErreichbarkeiten;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasLink;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasTime;

import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.xoev.xzufi.v2_2_0.FormulardateiLink;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.Sprachversion;
import de.xoev.xzufi.v2_2_0.StringLocalized;
import de.xoev.xzufi.v2_2_0.Zeitserien;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;

public class JzufiValidatorOUV2 extends AbstractJzufiValidatorOU<JzufiValidatorOUV2> {

    private final CodelistConfig22 codelistConfig = new CodelistConfig22();

    public JzufiValidatorOUV2(Response response) {
        super(response, ContentType.JSON.toString());
    }

    @Override
    protected JzufiValidatorOUV2 self() {
        return this;
    }

    @Override
    public JzufiValidatorOUV2 validateInfoSonstige(List<String> expected) {
        then.body(
            "infoSonstige",
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
    public JzufiValidatorOUV2 validateHerausgeber(Herausgeber expected) {
        de.xoev.xzufi.v2_2_0.Herausgeber actual = then
            .extract()
            .jsonPath()
            .getObject("herausgeber", de.xoev.xzufi.v2_2_0.Herausgeber.class);

        assertThat(
            "Herausgeber abgleich",
            actual,
            allOf(
                hasProperty("id", hasProperty("value", equalTo(expected.getId()))),
                hasProperty("bezeichnung", hasItem(expected.getBezeichnung())),
                hasProperty("kurzbezeichnung", hasItem(expected.getKurzbezeichnung())),
                hasBeschreibungen(expected.getBeschreibung(), expected.getBeschreibungEnglisch())
            )
        );
        return this;
    }

    @Override
    public JzufiValidatorOUV2 validateKontaktperson(Kontaktperson expected) {
        List<de.xoev.xzufi.v2_2_0.Kontaktperson> actual = then
            .extract()
            .jsonPath()
            .getList("kontaktperson", de.xoev.xzufi.v2_2_0.Kontaktperson.class);

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
                    hasProperty("anschrift", hasItems(allOfAnschriften(expected.getAnschriften(), codelistConfig))),
                    hasErreichbarkeiten(expected.getErreichbarkeiten(), codelistConfig),
                    hasLink("internetadresse", expected.getInternetadressen())
                )
            )
        );

        return this;
    }

    @Override
    public JzufiValidatorOUV2 validateAnschriften(List<Anschrift> expected) {
        List<de.xoev.xzufi.v2_2_0.Anschrift> actual = then
            .extract()
            .jsonPath()
            .getList("anschrift", de.xoev.xzufi.v2_2_0.Anschrift.class);

        assertThat(actual, hasItems(allOfAnschriften(expected, codelistConfig)));
        return this;
    }

    @Override
    public JzufiValidatorOUV2 validateInfoOeffnungszeitenStrukturiert(
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
                        hasProperty(
                            "typ",
                            hasCode(
                                oeffnungszeit.getType(),
                                CodelistConfig22.ZEITSERIENTYP,
                                codelistConfig.getVersion(CodelistConfig22.ZEITSERIENTYP)
                            )
                        ),
                        hasProperty(
                            "hinweistext",
                            hasProperty(
                                "value",
                                anyOf(
                                    equalTo(oeffnungszeit.getHinweistextEnglisch()),
                                    equalTo(oeffnungszeit.getHinweistext())
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
                                                hasCode(
                                                    zeitraum.getTagestyp(),
                                                    CodelistConfig22.TAGESTYP,
                                                    codelistConfig.getVersion(CodelistConfig22.TAGESTYP)
                                                )
                                            ),
                                            hasProperty(
                                                "tagesposition",
                                                hasCode(
                                                    zeitraum.getTagesposition(),
                                                    CodelistConfig22.TAGESPOSITION,
                                                    codelistConfig.getVersion(CodelistConfig22.TAGESPOSITION)
                                                )
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
        return then.extract().jsonPath().getList("bankverbindung", de.xoev.xzufi.v2_2_0.Bankverbindung.class);
    }

    @Override
    protected List<?> extractGueltigkeiten() {
        return then.extract().jsonPath().getList("gueltigkeit", de.xoev.xzufi.v2_2_0.Zeitraum.class);
    }

    @Override
    protected List<?> extractFormulare() {
        return getFormulare();
    }

    @Override
    protected Matcher<? super Object> matchesDatei(Formular formular) {
        return hasProperty(
            "datei",
            hasItems(
                allOf(
                    hasProperty("uri", equalTo(formular.getLink().getUri())),
                    hasProperty("mimeCode", equalTo(formular.getMimeCode()))
                ),
                formular.getLink().getUriEnglisch() != null
                    ? allOf(
                          hasProperty("uri", equalTo(formular.getLink().getUriEnglisch())),
                          hasProperty("mimeCode", equalTo(formular.getMimeCode()))
                      )
                    : anything()
            )
        );
    }

    private List<de.xoev.xzufi.v2_2_0.Formular> getFormulare() {
        // Formular enthält abstrakte Klassen, weshalb kein einfaches Mapping möglich ist
        JsonPath jsonPath = then.extract().jsonPath();
        int count = jsonPath.getList("formular").size();
        List<de.xoev.xzufi.v2_2_0.Formular> forms = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            de.xoev.xzufi.v2_2_0.Formular formular = new de.xoev.xzufi.v2_2_0.Formular();

            formular.setId(jsonPath.getObject("formular[" + i + "].id", Identifikator.class));
            formular
                .getBezeichnung()
                .addAll(jsonPath.getList("formular[" + i + "].bezeichnung", StringLocalized.class));
            formular.getDatei().addAll(jsonPath.getList("formular[" + i + "].datei", FormulardateiLink.class));
            formular
                .getSprachversion()
                .addAll(jsonPath.getList("formular[" + i + "].sprachversion", Sprachversion.class));

            // Weitere Felder könnten hier hinzugefügt werden, falls benötigt
            forms.add(formular);
        }

        return forms;
    }
}
