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
package de.dataport.suchdienst.geo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.geo.service.BkgQueryBuilder;
import org.junit.jupiter.api.Test;

class BkgQueryBuilderTest implements BkgQueryBuilder {

    @Test
    void checkChars() {
        assertThat(isBkgSpecialCharacter('\\'), is(true));
        assertThat(isBkgSpecialCharacter('*'), is(true));
        assertThat(isBkgSpecialCharacter('-'), is(true));
        assertThat(isBkgSpecialCharacter('+'), is(true));
    }

    @Test
    void escape() {
        assertThat(escapeSpecialChars("Köln-Weiß"), is("Köln\\-Weiß"));
        assertThat(escapeSpecialChars("Kiel+Wik"), is("Kiel\\+Wik"));
        assertThat(escapeSpecialChars("Halle (Saale), Stadt"), is("Halle \\(Saale\\), Stadt"));
        assertThat(
            escapeSpecialChars("Falkenstein/Vogtl., Stadt"),
            is("Falkenstein/Vogtl., Stadt")
        );
        assertThat(escapeSpecialChars("1\\2\\\\3\\:4+"), is("1\\ 2\\ \\ 3\\ \\:4\\+"));
    }

    @Test
    void replace() {
        assertThat(replaceSpecialChars(""), is(""));
        assertThat(replaceSpecialChars(" "), is(""));
        assertThat(replaceSpecialChars("-"), is(""));
        assertThat(replaceSpecialChars("Köln-Weiß"), is("Köln Weiß"));
        assertThat(replaceSpecialChars("Kiel+Wik"), is("Kiel Wik"));
        assertThat(replaceSpecialChars("Kiel++Wik"), is("Kiel Wik"));
        assertThat(replaceSpecialChars("~Ge|senkirchen+"), is("Ge senkirchen"));
        assertThat(
            replaceSpecialChars("Fal+++kenstein/Vogtl.,---Stadt"),
            is("Fal kenstein/Vogtl., Stadt")
        );
    }

    @Test
    void remove() {
        assertThat(removeSpecialChars("Köln-Weiß"), is("KölnWeiß"));
        assertThat(removeSpecialChars("Kiel+Wik"), is("KielWik"));
        assertThat(removeSpecialChars("Kiel\\Wik"), is("KielWik"));
        assertThat(removeSpecialChars("Halle (Saale), Stadt"), is("Halle Saale, Stadt"));
        assertThat(
            removeSpecialChars("Falkenstein/Vogtl., Stadt"),
            is("Falkenstein/Vogtl., Stadt")
        );
    }
}
