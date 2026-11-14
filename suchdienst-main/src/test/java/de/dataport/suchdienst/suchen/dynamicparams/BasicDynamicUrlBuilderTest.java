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
package de.dataport.suchdienst.suchen.dynamicparams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BasicDynamicUrlBuilderTest {

    @Test
    void testDynamicUrlBuild_mitParams() {
        var url = "https://www.dataport.de/api";

        var ars = new DynamicParamPair("001", "ars", "000100010001");
        var oeId = new DynamicParamPair("002", "oeId", "oeoeoeoe");

        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(ars);
        basicDynamicUrl.addDynamicParamPair(oeId);

        var completeUrl = basicDynamicUrl.build();

        assertEquals("https://www.dataport.de/api?ars=000100010001&oeId=oeoeoeoe", completeUrl);
    }

    @Test
    void testDynamicUrlBuild_ohneParams() {
        var url = "https://www.dataport.de/api";

        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        var completeUrl = basicDynamicUrl.build();

        assertEquals("https://www.dataport.de/api", completeUrl);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "https://digitalerführerscheinantrag.de/01.RZ/Umtausch",
            "https://例え.テスト/antrag",
            "https://xn--digitalerfhrerscheinantrag-7zc.de/01.RZ/Umtausch",
            "https://www.dataport.de/antrag%20stellen?return=%2Fstart#abschnitt",
        }
    )
    void testDynamicUrlBuild_ohneParams_unveraendert(String url) {
        assertEquals(url, new BasicDynamicUrlBuilder(url).build());
    }

    @ParameterizedTest
    @CsvSource({
        "https://digitalerführerscheinantrag.de/01.RZ/Umtausch, https://xn--digitalerfhrerscheinantrag-7zc.de/01.RZ/Umtausch?ars=000100010001",
        "https://例え.テスト/antrag, https://xn--r8jz45g.xn--zckzah/antrag?ars=000100010001",
        "https://xn--digitalerfhrerscheinantrag-7zc.de/antrag, https://xn--digitalerfhrerscheinantrag-7zc.de/antrag?ars=000100010001",
        "https://[::1]:8443/antrag, https://[::1]:8443/antrag?ars=000100010001",
    })
    void testDynamicUrlBuild_mitParams_internationalisierteDomains(String url, String expectedUrl) {
        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(new DynamicParamPair("001", "ars", "000100010001"));

        assertEquals(expectedUrl, basicDynamicUrl.build());
    }

    @Test
    void testDynamicUrlBuild_mitParams_encoding() {
        var basicDynamicUrl = new BasicDynamicUrlBuilder("https://führerschein.de/antrag");
        basicDynamicUrl.addDynamicParamPair(
            new DynamicParamPair("001", "rück weg", "Köln & Kiel=100%")
        );

        assertEquals(
            "https://xn--fhrerschein-thb.de/antrag?r%C3%BCck%20weg=K%C3%B6ln%20%26%20Kiel%3D100%25",
            basicDynamicUrl.build()
        );
    }

    @Test
    void testDynamicUrlBuild_mitParams_ungueltigeUrl() {
        var url = "https://www.dataport.de/ungueltiger pfad";
        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(new DynamicParamPair("001", "ars", "000100010001"));

        assertEquals(url, basicDynamicUrl.build());
    }

    @Test
    void testDynamicUrlBuild_mitVorhandenemQueryParam() {
        var url = "https://www.dataport.de/api?vogel=rabe";

        var ars = new DynamicParamPair("001", "ars", "000100010001");
        var oeId = new DynamicParamPair("002", "oeId", "oeoeoeoe");

        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(ars);
        basicDynamicUrl.addDynamicParamPair(oeId);

        var completeUrl = basicDynamicUrl.build();

        assertEquals(
            "https://www.dataport.de/api?vogel=rabe&ars=000100010001&oeId=oeoeoeoe",
            completeUrl
        );
    }

    /**
     * Da wir Onlinedienst URLs mit dem String 'null' als value erhalten, wird explizit getestet, dass
     * die Methode mit diesem bereits vorhandenen Wert umgehen kann.
     * Erwartet wird die Darstellung als Array in der URL.
     * Ein Array in der URL ist nicht einheitlich durch ein RFC spezifiziert und könnte auch andere Ausprägungen haben.
     * Wir behalten die durch die Redaktionssysteme gelieferten Daten bei und ergänzen zusätzliche
     * Parameter.
     * Siehe: <a href="https://en.wikipedia.org/wiki/Query_string">Beschreibung von Query Params</a>
     */
    @Test
    void testDynamicUrlBuild_mitArray() {
        var url = "https://www.dataport.de/api?ars=null";

        var ars = new DynamicParamPair("001", "ars", "000100010001");
        var oeId = new DynamicParamPair("002", "oeId", "oeoeoeoe");

        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(ars);
        basicDynamicUrl.addDynamicParamPair(oeId);

        var completeUrl = basicDynamicUrl.build();

        assertEquals(
            "https://www.dataport.de/api?ars=null&ars=000100010001&oeId=oeoeoeoe",
            completeUrl
        );
    }

    @Test
    void testDynamicUrlBuild_identischeNamen() {
        var url = "https://www.dataport.de/api";

        var ars = new DynamicParamPair("001", "name", "000100010001");
        var oeId = new DynamicParamPair("002", "name", "oeoeoeoe");

        var basicDynamicUrl = new BasicDynamicUrlBuilder(url);
        basicDynamicUrl.addDynamicParamPair(ars);
        basicDynamicUrl.addDynamicParamPair(oeId);

        var completeUrl = basicDynamicUrl.build();

        assertEquals("https://www.dataport.de/api?name=000100010001&name=oeoeoeoe", completeUrl);
    }
}
