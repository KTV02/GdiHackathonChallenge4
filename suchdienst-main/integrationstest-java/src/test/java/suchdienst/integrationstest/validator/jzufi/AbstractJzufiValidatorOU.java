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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.allOfSprachversionen;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.hasDatum;
import static suchdienst.integrationstest.validator.jzufi.helpers.JzufiMatcher.matchesDatumUndZeit;

import de.dataport.pvog.testdatengenerator.model.Anschrift;
import de.dataport.pvog.testdatengenerator.model.Bankverbindung;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;

/**
 * Abstrakte Basisklasse für JZuFi-Organisationseinheiten-Validatoren.
 * Enthält gemeinsame Validierungslogik für v2.2 und v2.3.
 *
 * @param <T> der konkrete Validator-Typ (für Fluent-API)
 */
@SuppressWarnings("unchecked")
public abstract class AbstractJzufiValidatorOU<T extends AbstractJzufiValidatorOU<T>> {

    protected final ValidatableResponse then;

    protected AbstractJzufiValidatorOU(Response response, String expectedContentType) {
        then = response.then();
        then.statusCode(HttpStatus.SC_OK).contentType(expectedContentType);
    }

    protected abstract T self();

    // --- Gemeinsame Validierungen ---

    public T validateId(String lbId, String schemeAgencyId) {
        then.body("id", allOf(hasEntry("value", lbId), hasEntry("schemeAgencyID", schemeAgencyId)));
        return self();
    }

    public T validateTitle(String title, String languageCode) {
        then.body(
            "name.findAll { it.name }.name.flatten()",
            hasItem(allOf(hasEntry("value", title), hasEntry("languageCode", languageCode)))
        );
        return self();
    }

    public T validateBeschreibung(String beschreibung, String languageCode) {
        then.body(
            "beschreibung",
            hasItem(allOf(hasEntry("value", beschreibung), hasEntry("languageCode", languageCode)))
        );
        return self();
    }

    public T validateKurzbeschreibung(String kurzbeschreibung, String languageCode) {
        then.body(
            "kurzbeschreibung",
            hasItem(allOf(hasEntry("value", kurzbeschreibung), hasEntry("languageCode", languageCode)))
        );
        return self();
    }

    public T validateInternetadressen(Link link) {
        then.body(
            "internetadresse",
            hasItems(
                hasEntry("uri", link.getUri()),
                link.getUriEnglisch() != null ? hasEntry("uri", link.getUriEnglisch()) : anything()
            )
        );
        return self();
    }

    public T validateInfoOeffnungszeitenText(List<String> expected) {
        then.body(
            "infoOeffnungszeitenText",
            hasItems(
                expected
                    .stream()
                    .map(s -> hasEntry("value", s))
                    .<Matcher<? extends Map<? extends String, ? extends String>>>toArray(Matcher[]::new)
            )
        );
        return self();
    }

    public T validateVersionsinformation(Versionsinformation versionsinformation) {
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
        return self();
    }

    public T validateBankverbindungen(List<Bankverbindung> expected) {
        List<?> actual = extractBankverbindungen();
        expected.forEach(bankverbindung ->
            assertThat(
                actual,
                hasItem(
                    allOf(
                        hasProperty("empfaenger", equalTo(bankverbindung.getEmpfaenger())),
                        hasProperty("iban", equalTo(bankverbindung.getIban()))
                    )
                )
            )
        );
        return self();
    }

    public T validateGueltigkeiten(List<Zeitraum> gueltigkeiten) {
        List<?> actual = extractGueltigkeiten();
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
        return self();
    }

    public T validateFormulare(List<Formular> formulare) {
        List<?> actual = extractFormulare();
        formulare.forEach(formular ->
            assertThat(
                actual,
                hasItem(
                    allOf(
                        hasProperty("id", hasProperty("value", equalTo(formular.getId()))),
                        hasProperty("bezeichnung", hasItems(hasProperty("value", equalTo(formular.getBezeichnung())))),
                        matchesDatei(formular),
                        hasProperty(
                            "sprachversion",
                            hasItems(
                                allOfSprachversionen("de", "deutsch"),
                                formular.getBezeichnungEnglisch() != null
                                    ? allOfSprachversionen("en", "englisch")
                                    : anything()
                            )
                        )
                    )
                )
            )
        );
        return self();
    }

    // --- Abstrakte/versionsspezifische Methoden ---

    public abstract T validateInfoSonstige(List<String> expected);

    public abstract T validateHerausgeber(Herausgeber expected);

    public abstract T validateKontaktperson(Kontaktperson expected);

    public abstract T validateAnschriften(List<Anschrift> expected);

    public abstract T validateInfoOeffnungszeitenStrukturiert(
        List<de.dataport.pvog.testdatengenerator.model.Zeitserien> expected
    );

    protected abstract List<?> extractBankverbindungen();

    protected abstract List<?> extractGueltigkeiten();

    protected abstract List<?> extractFormulare();

    protected abstract Matcher<? super Object> matchesDatei(Formular formular);
}
