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

import de.dataport.pvog.testdatengenerator.creator.CreatorConstants;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionssuggestionv1.SuggestionResultListValidator;

/**
 * <h1>Test der Vorschlagswerte bei einer Leistung mit mehrsprachigem Inhalt</h1>
 * Vorschlagswerte werden aus folgenden Leistungsdaten gewonnen:
 * BegriffImKontext
 * Inhalt Textmodul 02
 * Inhalt Textmodul 03
 * Titel Ursprungsportal
 * Für die Voschlagsliste irrelevant aber bedeutsam für die Trefferliste:
 * Inhalt Textmodul 06
 * Onlinedienst Beschreibung
 * Onlinedienst Bezeichnung
 * <p>
 * Inhalte aus Spezialisierungen werden für die Vorschlagsliste nicht herangezogen
 */

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MehrsprachigkeitLeistungTest {

    private static final String RANDOM_PREFIX_DE = TestUtil.generateRandomWord();
    private static final String RANDOM_PREFIX_EN = TestUtil.generateRandomWord();
    private static final String RANDOM_PREFIX_SPEZ = TestUtil.generateRandomWord();

    private static final String ARS = CreatorConstants.DEFAULT_BUNDESLANDCONFIG.getBasicArs();
    private static String spezArs01;

    //fields used for suggestions
    private static final String BESCHREIBUNG_TM02_DE = RANDOM_PREFIX_DE + "Inhalt02tm";
    private static final String TITEL_TM03_DE = RANDOM_PREFIX_DE + "Inhalt03tm";
    private static final String BEGRIFF_IM_KONTEXT_A_DE = RANDOM_PREFIX_DE + "KontextA";
    private static final String BEGRIFF_IM_KONTEXT_B_DE = RANDOM_PREFIX_DE + "KontextB";
    private static final String URSPRUNGSPORTAL_TITEL_DE = RANDOM_PREFIX_DE + "Ursprung";

    private static final String BESCHREIBUNG_TM02_EN = RANDOM_PREFIX_EN + "Content02tm";
    private static final String TITEL_TM03_EN = RANDOM_PREFIX_EN + "Content03tm";
    private static final String BEGRIFF_IM_KONTEXT_A_EN = RANDOM_PREFIX_EN + "ContextA";
    private static final String BEGRIFF_IM_KONTEXT_B_EN = RANDOM_PREFIX_EN + "ContextB";
    private static final String URSPRUNGSPORTAL_TITEL_EN = RANDOM_PREFIX_EN + "Source";
    //fields not used for suggestion but used for matching resultlist
    private static final String OD_BEZEICHNUNG_DE = RANDOM_PREFIX_DE + "OdBezeichnung";
    private static final String OD_BESCHREIBUNG_DE = RANDOM_PREFIX_DE + "OdBeschreibung";
    private static final String VOLLTEXT_TM06_DE = RANDOM_PREFIX_DE + "Inhalt06tm";

    private static final String OD_BEZEICHNUNG_EN = RANDOM_PREFIX_EN + "OdDefinition";
    private static final String OD_BESCHREIBUNG_EN = RANDOM_PREFIX_EN + "OdDescription";
    private static final String VOLLTEXT_TM06_EN = RANDOM_PREFIX_EN + "Content06tm";

    //Spezialisierung
    private static final String SPEZ_BESCHREIBUNG_TM02_DE = RANDOM_PREFIX_SPEZ + "Inhalt02tm";
    private static final String SPEZ_TITEL_TM03_DE = RANDOM_PREFIX_SPEZ + "Inhalt03tm";
    private static final String SPEZ_BEGRIFF_IM_KONTEXT_A_DE = RANDOM_PREFIX_SPEZ + "KontextA";
    private static final String SPEZ_URSPRUNGSPORTAL_TITEL_DE = RANDOM_PREFIX_SPEZ + "Ursprung";
    private static final String SPEZ_VOLLTEXT_TM06_DE = RANDOM_PREFIX_SPEZ + "InhaltTM06";

    private static final String SPEZ_BESCHREIBUNG_TM02_EN = RANDOM_PREFIX_SPEZ + "Content02tm";
    private static final String SPEZ_TITEL_TM03_EN = RANDOM_PREFIX_SPEZ + "Content03tm";
    private static final String SPEZ_BEGRIFF_IM_KONTEXT_A_EN = RANDOM_PREFIX_SPEZ + "ContextA";
    private static final String SPEZ_URSPRUNGSPORTAL_TITEL_EN = RANDOM_PREFIX_SPEZ + "Source";
    private static final String SPEZ_VOLLTEXT_TM06_EN = RANDOM_PREFIX_SPEZ + "ContentTM06";

    /**
     * Initial wird ein XZUFI mit mehrsprachigem Inhalten für die Vorschlagswerte
     * Textmodul 02, Textmodul 03, BegriffImKontext und Ursprungsportal Titel angelegt
     * Die de, en und Spezialisierungsinhalte haben einen eigenen Präfix, der als Eingabewert für die Vorschlagsliste verwendet werden soll
     * Daneben werden auch die für die Suche relevanten Inhalte mehrsprachig angelegt:
     * Textmodul 06, Bezeichnung und Beschreibung des Onlinedienstes
     * Zusätzlich wird ein Spezialisierung erzeugt, die mehrsprachig Textmodul 02,03,06,BegriffImKontext und Usprungsportal Titel spezialisiert
     */
    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).od(1).sp(1).getMin();

        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.getTextmodule().clear();

        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(
            Textmodul.builder()
                .leikaCode("02")
                .inhalt(BESCHREIBUNG_TM02_DE)
                .inhaltEnglisch(BESCHREIBUNG_TM02_EN)
                .positionDarstellung(2)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("03")
                .inhalt(TITEL_TM03_DE)
                .inhaltEnglisch(TITEL_TM03_EN)
                .positionDarstellung(3)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("06")
                .inhalt(VOLLTEXT_TM06_DE)
                .inhaltEnglisch(VOLLTEXT_TM06_EN)
                .positionDarstellung(6)
                .build()
        );
        lb.setTextmodule(textmodule);

        lb.getUrsprungsportale().clear();
        lb.setUrsprungsportale(
            List.of(
                UrsprungsportalModul.builder()
                    .title(URSPRUNGSPORTAL_TITEL_DE)
                    .titleEnglisch(URSPRUNGSPORTAL_TITEL_EN)
                    .uri("http://source.de")
                    .uriEnglisch("http://source.en")
                    .positionDarstellung(0)
                    .build()
            )
        );

        lb.getSuchbegriffe().clear();
        lb.setSuchbegriffe(
            List.of(
                Suchbegriff.builder().begriff(BEGRIFF_IM_KONTEXT_A_DE).begriffEnglisch(BEGRIFF_IM_KONTEXT_A_EN).build(),
                Suchbegriff.builder().begriff(BEGRIFF_IM_KONTEXT_B_DE).begriffEnglisch(BEGRIFF_IM_KONTEXT_B_EN).build()
            )
        );

        Spezialisierung spez01 = file.getSpezialisierungen().getFirst();
        spez01.getModulSpezialisierungen().clear();
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("02")
                            .inhalt(SPEZ_BESCHREIBUNG_TM02_DE)
                            .inhaltEnglisch(SPEZ_BESCHREIBUNG_TM02_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("03")
                            .inhalt(SPEZ_TITEL_TM03_DE)
                            .inhaltEnglisch(SPEZ_TITEL_TM03_EN)
                            .positionDarstellung(3)
                            .build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .inhalt(SPEZ_VOLLTEXT_TM06_DE)
                            .inhaltEnglisch(SPEZ_VOLLTEXT_TM06_EN)
                            .positionDarstellung(6)
                            .build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        UrsprungsportalModul.builder()
                            .title(SPEZ_URSPRUNGSPORTAL_TITEL_DE)
                            .uri("www.spez.de")
                            .titleEnglisch(SPEZ_URSPRUNGSPORTAL_TITEL_EN)
                            .uriEnglisch("www.spez.en")
                            .positionDarstellung(0)
                            .build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Suchbegriff.builder()
                            .begriff(SPEZ_BEGRIFF_IM_KONTEXT_A_DE)
                            .begriffEnglisch(SPEZ_BEGRIFF_IM_KONTEXT_A_EN)
                            .build()
                    )
                    .build()
            );
        spezArs01 = spez01.getArs().getFirst();
        List<Zustaendigkeit> odZSList = file.getZustaendigkeitenOnlinedienst();
        odZSList.getFirst().getArss().clear();
        odZSList.getFirst().setArss(List.of(ARS));

        Onlinedienst onlinedienst = file.getOnlinedienste().getFirst();
        onlinedienst.setBezeichnung(OD_BEZEICHNUNG_DE);
        onlinedienst.setBezeichnungEnglisch(OD_BEZEICHNUNG_EN);
        onlinedienst.setBeschreibung(OD_BESCHREIBUNG_DE);
        onlinedienst.setBeschreibungEnglisch(OD_BESCHREIBUNG_EN);

        ImportUtil.pushAndWait23(file);
    }

    /**
     * In diesem Test wird geprüft, dass bei Abfrage mit en auch die englischsprachigen Inhalte für:
     * Textmodul 02 und 03, BegriffImKontext und Titel Ursprungsportal ausgegeben werden
     * Bei Abfrage mit de sollen die deutschsprachigen Inhalte ausgegeben werden
     */
    @DisplayName("mehrsprachig suggestions match")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1} q:{2}")
    @MethodSource("provideDataMatch")
    void match(String ars, String language, String q, List<String> expected) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(ars, q, true, language);
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateListContains(expected);
        validator.validateCount(expected.size());
    }

    /**
     * In diesem Test wird geprüft, dass für folge Konstellationen keine Vorschlagswerte auftauchen:
     * Abfrage mit en und de Präfix
     * Abfrage mit de und en Präfix
     * Abfrage mit de oder en mit spezialisierungs Präfix
     * kein Vorschlag für Textmodul 06 und Onlinedienst Bezeichnung und Beschreibung
     */
    @DisplayName("mehrsprachig suggestions nomatch")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1} q:{2} unexpected:{3}")
    @MethodSource("provideDataNoMatch")
    void noMatch(String ars, String language, String q, List<String> unexpected) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsSuggestionsV1(ars, q, true, language);
        SuggestionResultListValidator validator = new SuggestionResultListValidator(response);
        validator.validateContainsNoneOf(unexpected);
    }

    private static Stream<Arguments> provideDataMatch() {
        return Stream.of(
            //die ersten 3 Argumente sind Abfrageparameter ars, language, q die folgenden sind die Erwartungswerte expectedResultList, count
            //abhängig von der Sprache Vorschlagswerte aus BegriffImKontext, Textmodul 02, Textmodul 03 und Ursprungsportal Titel
            Arguments.of(
                ARS,
                "en",
                RANDOM_PREFIX_EN,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_EN,
                    BEGRIFF_IM_KONTEXT_B_EN,
                    BESCHREIBUNG_TM02_EN,
                    TITEL_TM03_EN,
                    URSPRUNGSPORTAL_TITEL_EN
                )
            ),
            Arguments.of(
                ARS,
                "de",
                RANDOM_PREFIX_DE,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_DE,
                    BEGRIFF_IM_KONTEXT_B_DE,
                    BESCHREIBUNG_TM02_DE,
                    TITEL_TM03_DE,
                    URSPRUNGSPORTAL_TITEL_DE
                )
            ),
            Arguments.of(
                spezArs01,
                "en",
                RANDOM_PREFIX_EN,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_EN,
                    BEGRIFF_IM_KONTEXT_B_EN,
                    BESCHREIBUNG_TM02_EN,
                    TITEL_TM03_EN,
                    URSPRUNGSPORTAL_TITEL_EN
                )
            ),
            Arguments.of(
                spezArs01,
                "de",
                RANDOM_PREFIX_DE,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_DE,
                    BEGRIFF_IM_KONTEXT_B_DE,
                    BESCHREIBUNG_TM02_DE,
                    TITEL_TM03_DE,
                    URSPRUNGSPORTAL_TITEL_DE
                )
            )
        );
    }

    private static Stream<Arguments> provideDataNoMatch() {
        return Stream.of(
            //die ersten 3 Argumente sind Abfrageparameter ars, language, q - kein Begriff aus der Liste darf im Response auftauchen
            // bei Abfrage mit en darf kein de Inhalt gefunden werden
            Arguments.of(
                ARS,
                "en",
                RANDOM_PREFIX_DE,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_DE,
                    BEGRIFF_IM_KONTEXT_B_DE,
                    BESCHREIBUNG_TM02_DE,
                    TITEL_TM03_DE,
                    URSPRUNGSPORTAL_TITEL_DE
                )
            ),
            // Textmodul 06, Onlinedienst Beschreibung und Bezeichnung liefern zwar Treffer aber sollen nicht in Vorschlagsliste auftauchen
            Arguments.of(
                ARS,
                "en",
                RANDOM_PREFIX_DE,
                List.of(
                    OD_BESCHREIBUNG_DE,
                    OD_BEZEICHNUNG_DE,
                    VOLLTEXT_TM06_DE,
                    OD_BESCHREIBUNG_EN,
                    OD_BEZEICHNUNG_EN,
                    VOLLTEXT_TM06_EN
                )
            ),
            //Werte aus Spezialisierungen sollen nicht gefunden werden
            Arguments.of(
                spezArs01,
                "en",
                RANDOM_PREFIX_SPEZ,
                List.of(
                    SPEZ_BEGRIFF_IM_KONTEXT_A_EN,
                    SPEZ_BESCHREIBUNG_TM02_EN,
                    SPEZ_TITEL_TM03_EN,
                    SPEZ_URSPRUNGSPORTAL_TITEL_EN,
                    SPEZ_VOLLTEXT_TM06_EN
                )
            ),
            // bei Abfrage mit de darf kein en Inhalt gefunden werden
            Arguments.of(
                ARS,
                "de",
                RANDOM_PREFIX_EN,
                List.of(
                    BEGRIFF_IM_KONTEXT_A_EN,
                    BEGRIFF_IM_KONTEXT_B_EN,
                    BESCHREIBUNG_TM02_EN,
                    TITEL_TM03_EN,
                    URSPRUNGSPORTAL_TITEL_EN
                )
            ),
            // Textmodul 06, Onlinedienst Beschreibung und Bezeichnung liefern zwar Treffer aber sollen nicht in Vorschlagsliste auftauchen
            Arguments.of(ARS, "de", RANDOM_PREFIX_EN, List.of(OD_BESCHREIBUNG_EN, OD_BEZEICHNUNG_EN, VOLLTEXT_TM06_EN)),
            Arguments.of(ARS, "de", RANDOM_PREFIX_DE, List.of(OD_BESCHREIBUNG_DE, OD_BEZEICHNUNG_DE, VOLLTEXT_TM06_DE)),
            //Werte aus Spezialisierungen sollen nicht gefunden werden
            Arguments.of(
                spezArs01,
                "de",
                RANDOM_PREFIX_SPEZ,
                List.of(
                    SPEZ_BEGRIFF_IM_KONTEXT_A_DE,
                    SPEZ_BESCHREIBUNG_TM02_DE,
                    SPEZ_TITEL_TM03_DE,
                    SPEZ_URSPRUNGSPORTAL_TITEL_DE,
                    SPEZ_VOLLTEXT_TM06_DE
                )
            )
        );
    }
}
