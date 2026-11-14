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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class OrganisationseinheitVLTest {

    @Test
    void applyLanguageOverrideFiltersNestedFormsByLanguageAndFileLink() {
        OrganisationseinheitVL organisationseinheit = new OrganisationseinheitVL();
        FormularVL multilingualForm = formular(
            "formular-mehrsprachig",
            "Deutsches Formular",
            Locale.GERMAN
        );
        multilingualForm.addFileLink("application/pdf", "https://example.org/de.pdf");
        FormularContent englishContent = new FormularContent();
        englishContent.setName("English form");
        englishContent.addFileLink("application/pdf", "https://example.org/en.pdf");
        multilingualForm.getLanguages().add(Locale.ENGLISH);
        multilingualForm.getOtherLanguages().put(Locale.ENGLISH, englishContent);

        FormularVL germanOnlyForm = formular("formular-deutsch", "Nur deutsch", Locale.GERMAN);
        germanOnlyForm.addFileLink("application/pdf", "https://example.org/nur-de.pdf");

        FormularVL englishFormWithoutFileLink = formular(
            "formular-englisch-ohne-link",
            "Deutsch mit englischer Variante ohne Link",
            Locale.GERMAN
        );
        englishFormWithoutFileLink.addFileLink(
            "application/pdf",
            "https://example.org/de-ohne-en.pdf"
        );
        FormularContent englishContentWithoutFileLink = new FormularContent();
        englishContentWithoutFileLink.setName("English form without link");
        englishFormWithoutFileLink.getLanguages().add(Locale.ENGLISH);
        englishFormWithoutFileLink
            .getOtherLanguages()
            .put(Locale.ENGLISH, englishContentWithoutFileLink);

        organisationseinheit.setNestedForms(
            List.of(multilingualForm, germanOnlyForm, englishFormWithoutFileLink)
        );

        organisationseinheit.applyLanguageOverride(Locale.ENGLISH);

        assertThat(organisationseinheit.getNestedForms(), hasSize(1));
        FormularVL nestedForm = organisationseinheit.getNestedForms().getFirst();
        assertThat(nestedForm.getId(), is("formular-mehrsprachig"));
        assertThat(nestedForm.getName(), is("English form"));
        assertThat(
            nestedForm.getFileLinks().stream().map(FormularFileLink::getUri).toList(),
            contains("https://example.org/en.pdf")
        );
    }

    private FormularVL formular(String id, String name, Locale language) {
        FormularVL formular = new FormularVL();
        formular.setId(id);
        formular.setName(name);
        formular.getLanguages().add(language);
        return formular;
    }
}
