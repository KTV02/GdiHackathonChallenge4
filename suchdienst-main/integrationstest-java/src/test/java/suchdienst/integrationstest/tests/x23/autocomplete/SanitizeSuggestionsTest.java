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
package suchdienst.integrationstest.tests.x23.autocomplete;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class SanitizeSuggestionsTest {

    private static final String RANDOM_PREFIX_TITEL = TestUtil.generateRandomWord();
    private static final String RANDOM_PREFIX_NAME = TestUtil.generateRandomWord();
    private static final String RANDOM_PREFIX_BEGRIFF_IM_KONTEXT = TestUtil.generateRandomWord();
    private static final String RANDOM_PREFIX_URSPRUNGS_PORTAL = TestUtil.generateRandomWord();

    private static String ars;

    private static final List<Pair<String, String>> HTML_FIELD_ELEMENT_LIST = List.of(
        Pair.of("<p>", "Paragraph</p>"),
        Pair.of("<br>", "Umbruch</br>"),
        Pair.of("<h1>", "Header</h1>"),
        Pair.of("<b>", "Bold</b>"),
        Pair.of("<i>", "Kursiv</i>"),
        Pair.of("<em>", "EMKursiv</em>"),
        Pair.of("<sup>", "Hoch</sup>"),
        Pair.of("<sub>", "Tief</sub>"),
        Pair.of("<ul>", "UnsortedList</ul>"),
        Pair.of("<ol>", "SortedList</ol>"),
        Pair.of("<li>", "List</li>"),
        Pair.of("<a>", "Anker</a>")
    );

    private static final List<String> TAG_LIST = Arrays.asList(
        "<p>",
        "<br>",
        "<h1>",
        "<b>",
        "<i>",
        "<em>",
        "<sup>",
        "<sub>",
        "<ul>",
        "<ol>",
        "<li>",
        "<a>",
        "&lt;p&gt;",
        "&lt;br&gt;",
        "&lt;h1&gt;",
        "&lt;b&gt;",
        "&lt;i&gt;",
        "&lt;em&gt;",
        "&lt;sup&gt;",
        "&lt;sub&gt;",
        "&lt;ul&gt;",
        "&lt;ol&gt;",
        "&lt;li&gt;",
        "&lt;a&gt;"
    );

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BB);

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testclassname).lb(1).oe(1).od(1).getMin();

        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        String beschreibungTM02 = buildHtmlString(RANDOM_PREFIX_NAME + "sanitizedName");
        Textmodul tm02 = Textmodul.builder().inhalt(beschreibungTM02).positionDarstellung(2).leikaCode("02").build();
        String titelTM03 = buildHtmlString(RANDOM_PREFIX_TITEL + "sanitizedTitel");
        Textmodul tm03 = Textmodul.builder().inhalt(titelTM03).positionDarstellung(3).leikaCode("03").build();
        Textmodul tm06 = Textmodul.builder().inhalt("DummyTM06").positionDarstellung(6).leikaCode("06").build();
        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        String begriffImKontext = buildHtmlString(RANDOM_PREFIX_BEGRIFF_IM_KONTEXT + "sanitizedBegriff");
        Suchbegriff suchbegriffA = new Suchbegriff();
        suchbegriffA.setBegriff(begriffImKontext);
        suchbegriffe.add(suchbegriffA);
        lb.setSuchbegriffe(suchbegriffe);

        String ursprungsportalTitel = buildHtmlString(RANDOM_PREFIX_URSPRUNGS_PORTAL + "sanitizedUrsprung");
        lb.getUrsprungsportale().getFirst().setTitle(ursprungsportalTitel);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait23(file);
    }

    @Test
    void sanitizedName() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            RANDOM_PREFIX_NAME,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        for (String tagName : TAG_LIST) {
            validator.validateNotContainsHtmlTag(tagName);
        }
    }

    @Test
    void sanitizedTitle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            RANDOM_PREFIX_TITEL,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        for (String tagName : TAG_LIST) {
            validator.validateNotContainsHtmlTag(tagName);
        }
    }

    @Test
    void sanitizedBegriff() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            RANDOM_PREFIX_BEGRIFF_IM_KONTEXT,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        for (String tagName : TAG_LIST) {
            validator.validateNotContainsHtmlTag(tagName);
        }
    }

    @Test
    void sanitizedUrsprungsportalTitel() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(
            ars,
            RANDOM_PREFIX_URSPRUNGS_PORTAL,
            true
        );
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        for (String tagName : TAG_LIST) {
            validator.validateNotContainsHtmlTag(tagName);
        }
    }

    private static String buildHtmlString(String element) {
        StringBuilder builder = new StringBuilder();
        for (Pair<String, String> pairEntry : HTML_FIELD_ELEMENT_LIST) {
            builder.append(" ").append(pairEntry.getLeft()).append(element).append(pairEntry.getRight());
        }
        return builder.toString().trim();
    }
}
