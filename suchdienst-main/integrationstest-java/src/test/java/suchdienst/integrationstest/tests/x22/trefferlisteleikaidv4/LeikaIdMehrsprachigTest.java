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
package suchdienst.integrationstest.tests.x22.trefferlisteleikaidv4;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Sprache;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulursprungsportal.UrsprungsportalModul;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsleikaidv4.LeikaIdResultListValidator;

/**
 * <h1>Test der Behandlung von unvollständigen englischsprachingen Inhalten - languageComplete:false bei Suche über die LeikaId</h1>
 */

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeikaIdMehrsprachigTest {

    //Englisch unvollständig Textmodul Inhalte - EN für TM06 fehlt
    private static final String ENC_TM02_DE = "EN_notComplete02_DE";
    private static final String ENC_TM02_EN = "EN_notComplete02_EN";
    private static final String ENC_TM03_DE = "EN_notComplete03_DE";
    private static final String ENC_TM06_DE = "EN_notComplete06_DE";
    private static final String ENC_TM06_EN = "EN_notComplete06_EN";
    //Spezialisierung
    private static final String ENC_SPEZ01_TM02_DE = "EN_notComplete02SPEZ_DE";
    private static final String ENC_SPEZ01_TM03_DE = "EN_notComplete03SPEZ_DE";
    private static final String ENC_SPEZ01_TM06_DE = "EN_notComplete06SPEZ_DE";

    private static final String ENC_SPEZ02_TM02_DE = "EN_notComplete02SPEZ02_DE";
    private static final String ENC_SPEZ02_TM02_EN = "EN_notComplete02SPEZ02_EN";
    private static final String ENC_SPEZ02_TM03_DE = "EN_notComplete03SPEZ02_DE";
    private static final String ENC_SPEZ02_TM03_EN = "EN_notComplete03SPEZ02_EN";
    private static final String ENC_SPEZ02_TM06_DE = "EN_notComplete06SPEZ02_DE";
    private static final String ENC_SPEZ02_TM06_EN = "EN_notComplete06SPEZ02_EN";

    private static final BundeslandConfig BUNDESLAND_CONFIG = new BundeslandConfig(
        "L100042",
        "090000000000",
        // Neusäß, Stadtbergen
        List.of("097720184184", "097720202202")
    );
    private static final String SPEZ_ARS01 = BUNDESLAND_CONFIG.getArss().getFirst();
    private static final String SPEZ_ARS02 = BUNDESLAND_CONFIG.getArss().get(1);

    // Landkreis Augsburg, Ars wird für die Zuweisung genutzt
    private static final String ARS = "097720000000";
    // Diedorf, spezieller Ars wird für die Suche genutzt
    private static final String ARS_SEARCH = "097720130130";

    private static final String ENC_URSPRUNGSPORTAL_TITEL = "EN_notComplete_ursprungsportal_titel_DE";
    private static final String ENC_URSPRUNGSPORTAL_URI = "www.EN_notComplete_ursprungsportal_titel.de";

    //OD Data
    private static final String OD01_BESCHREIBUNG_DE = "OD01_Beschreibung_DE";
    private static final String OD01_BESCHREIBUNG_EN = "OD01_Description_EN";
    private static final String OD01_BEZEICHNUNG_DE = "OD01_Bezeichnung_DE";
    private static final String OD01_BEZEICHNUNG_EN = "OD01_Definition_EN";
    private static final String OD01_URI_DE = "http://myod01.de";
    private static final String OD01_URI_EN = "http://myod01.en";
    private static final String OD01_LINK_BESCHREIBUNG_DE = "LinkBeschreibungOD01DE";
    private static final String OD01_LINK_BESCHREIBUNG_EN = "LinkBeschreibungOD01EN";
    private static final String OD01_LINK_TITEL_DE = "LinkTitelOD01DE";

    private static final String OD02_BESCHREIBUNG_DE = "od02_Beschreibung_DE";
    private static final String OD02_BESCHREIBUNG_EN = "od02_Description_EN";
    private static final String OD02_BEZEICHNUNG_DE = "od02_Bezeichnung_DE";
    private static final String OD02_BEZEICHNUNG_EN = "od02_Definition_EN";
    private static final String OD02_URI_DE = "http://myod02.de";
    private static final String OD02_LINK_BESCHREIBUNG_DE = "LinkBeschreibungod02DE";
    private static final String OD02_LINK_TITEL_DE = "LinkTitelod02DE";
    private static final String TEASER_TM24 = "teaser";

    private static String leikaId;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        ElasticsearchConnector.removeAllData();
        /*
         * Setup Szenario
         * 1 Leistungsbeschreibung Textmodul 02DE, 02EN, 03DE, 06DE, 06EN, Ursprungsportal DE
         * OE1 - ZS 097720000000
         * OE2 - ZS 097720184184, 097720202202 (Spezialisierung1 und 2)
         * OD1 - ZS 097720000000
         * OD2 - ZS 097720184184, 097720202202 (Spezialisierung1 und 2)
         * Spezialisierung1 Textmodul 02DE, 03DE, 06DE, 25DE, 25EN - für 097720184184
         * Spezialisierung2 Textmodul 02DE, 02EN, 03DE, 03EN, 06DE, 06EN für 097720202202
         *
         */

        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmm");
        leikaId = "99001009" + dateFormat.format(timestamp);
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());

        XzufiFile fileENnotComplete = XzufiFileCreator.newInstance(BUNDESLAND_CONFIG)
            .name(testclassname)
            .lb(1)
            .oe(2)
            .od(2)
            .sp(2)
            .getMin();
        Leistungsbeschreibung lbENnotComplete = fileENnotComplete.getLeistungsbeschreibungen().getFirst();

        lbENnotComplete.setReferenzLeiKa(leikaId);

        lbENnotComplete.getTextmodule().clear();

        List<Textmodul> textmodule = new ArrayList<>();

        textmodule.add(
            Textmodul.builder()
                .leikaCode("02")
                .inhalt(ENC_TM02_DE)
                .inhaltEnglisch(ENC_TM02_EN)
                .positionDarstellung(2)
                .build()
        );
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt(ENC_TM03_DE).positionDarstellung(3).build());
        textmodule.add(
            Textmodul.builder()
                .leikaCode("06")
                .inhalt(ENC_TM06_DE)
                .inhaltEnglisch(ENC_TM06_EN)
                .positionDarstellung(6)
                .build()
        );
        textmodule.add(Textmodul.builder().inhalt(TEASER_TM24).positionDarstellung(24).leikaCode("24").build());
        lbENnotComplete.setTextmodule(textmodule);
        lbENnotComplete.getUrsprungsportale().clear();

        lbENnotComplete
            .getUrsprungsportale()
            .add(
                UrsprungsportalModul.builder()
                    .title(ENC_URSPRUNGSPORTAL_TITEL)
                    .uri(ENC_URSPRUNGSPORTAL_URI)
                    .positionDarstellung(0)
                    .build()
            );
        Spezialisierung spez01 = fileENnotComplete.getSpezialisierungen().getFirst();

        spez01.getModulSpezialisierungen().clear();
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder().leikaCode("02").inhalt(ENC_SPEZ01_TM02_DE).positionDarstellung(2).build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder().leikaCode("03").inhalt(ENC_SPEZ01_TM03_DE).positionDarstellung(3).build()
                    )
                    .build()
            );
        spez01
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder().leikaCode("06").inhalt(ENC_SPEZ01_TM06_DE).positionDarstellung(6).build()
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
                            .leikaCode("25")
                            .inhalt("encspez01TM25DE")
                            .inhaltEnglisch("encspez01TM25EN")
                            .positionDarstellung(8)
                            .build()
                    )
                    .build()
            );

        Spezialisierung spez02 = fileENnotComplete.getSpezialisierungen().get(1);

        spez02.getModulSpezialisierungen().clear();
        spez02
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("02")
                            .inhalt(ENC_SPEZ02_TM02_DE)
                            .inhaltEnglisch(ENC_SPEZ02_TM02_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        spez02
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("03")
                            .inhalt(ENC_SPEZ02_TM03_DE)
                            .inhaltEnglisch(ENC_SPEZ02_TM03_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        spez02
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .inhalt(ENC_SPEZ02_TM06_DE)
                            .inhaltEnglisch(ENC_SPEZ02_TM06_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        List<Zustaendigkeit> oeZSList = fileENnotComplete.getZustaendigkeitenOrganisationseinheit();

        oeZSList.get(0).getArss().clear();
        oeZSList.get(0).setArss(List.of(ARS));
        oeZSList.get(1).getArss().clear();

        oeZSList.get(1).setArss(List.of(SPEZ_ARS01, SPEZ_ARS02));
        List<Zustaendigkeit> odZSList = fileENnotComplete.getZustaendigkeitenOnlinedienst();

        odZSList.get(0).getArss().clear();
        odZSList.get(0).setArss(List.of(ARS));
        odZSList.get(1).getArss().clear();

        odZSList.get(1).setArss(List.of(SPEZ_ARS01, SPEZ_ARS02));
        Onlinedienst onlinedienst = fileENnotComplete.getOnlinedienste().getFirst();

        onlinedienst.setBezeichnung(OD01_BEZEICHNUNG_DE);
        onlinedienst.setBezeichnungEnglisch(OD01_BEZEICHNUNG_EN);
        onlinedienst.setKurzbezeichnung("SAMPLE Kurzbezeichnung OD01");
        onlinedienst.setBeschreibung(OD01_BESCHREIBUNG_DE);
        onlinedienst.setBeschreibungEnglisch(OD01_BESCHREIBUNG_EN);
        onlinedienst.setLinks(
            List.of(
                Link.builder()
                    .uri(OD01_URI_DE)
                    .uriEnglisch(OD01_URI_EN)
                    .titel(OD01_LINK_TITEL_DE)
                    .beschreibung(OD01_LINK_BESCHREIBUNG_DE)
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        onlinedienst.setDurchfuehrungStaatsangehoerigkeit(List.of("000", "126", "168"));
        onlinedienst.setDurchfuehrungSprachen(
            List.of(Sprache.builder().code("de").bezeichnungDeutsch("deutsch").bezeichnungNativ("Deutsch").build())
        );
        onlinedienst.setHilfeText("SAMPLE Hilfetext");
        Onlinedienst od02 = fileENnotComplete.getOnlinedienste().get(1);

        od02.setBezeichnung(OD02_BEZEICHNUNG_DE);
        od02.setBezeichnungEnglisch(OD02_BEZEICHNUNG_EN);
        od02.setKurzbezeichnung("SAMPLE Kurzbezeichnung od02");
        od02.setBeschreibung(OD02_BESCHREIBUNG_DE);
        od02.setBeschreibungEnglisch(OD02_BESCHREIBUNG_EN);
        od02.setLinks(
            List.of(
                Link.builder()
                    .uri(OD02_URI_DE)
                    .titel(OD02_LINK_TITEL_DE)
                    .beschreibung(OD02_LINK_BESCHREIBUNG_DE)
                    .beschreibungEnglisch(OD01_LINK_BESCHREIBUNG_EN)
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        od02.setDurchfuehrungStaatsangehoerigkeit(List.of("000", "126", "168"));
        od02.setDurchfuehrungSprachen(
            List.of(Sprache.builder().code("de").bezeichnungDeutsch("deutsch").bezeichnungNativ("Deutsch").build())
        );
        ImportUtil.pushAndWait(fileENnotComplete);
    }

    @DisplayName("en_notComplete_serviceLinks")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1}")
    @MethodSource("provideServiceLinkData")
    void onlineServiceLinks(
        String ars,
        String language,
        int index,
        int count,
        String linkUri,
        String linkDescription,
        String linkTitle,
        String parentOdDescription
    ) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaId, language);

        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateOnlineServiceLinksCount(0, count);

        if (linkUri.isEmpty()) {
            validator.validateContainsNoLinksUri(0, index);
        } else {
            validator.validateOnlineServiceLinksUri(0, index, linkUri);
        }
        if (linkDescription.isEmpty()) {
            validator.validateContainsNoLinkDescription(0, index);
        } else {
            validator.validateOnlineServiceLinksDescription(0, index, linkDescription);
        }
        if (!linkTitle.isBlank()) {
            validator.validateOnlineServiceLinksTitle(0, index, linkTitle);
        } else if (!parentOdDescription.isBlank()) {
            validator.validateOnlineServiceLinksTitle(0, index, parentOdDescription);
        } else {
            validator.validateOnlineServiceLinksTitle(0, index, linkUri);
        }
    }

    private static Stream<Arguments> provideServiceLinkData() {
        // Parameter 1 - 3: ars, language, index
        // Parameter 4 - 8: Erwartungswerte: Anzahl Links, Link Uri, Link Beschreibung, LinkTitel, OD Beschreibung
        return Stream.of(
            Arguments.of(ARS_SEARCH, "en", 0, 1, OD01_URI_EN, OD01_BESCHREIBUNG_EN, "", OD01_BEZEICHNUNG_EN),
            Arguments.of(
                ARS_SEARCH,
                "de",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_BESCHREIBUNG_DE,
                OD01_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            //kein Eintrag für OD02 bei ars "097770000000" und en - fehlende URI in en und fehlender Titel in en
            Arguments.of(SPEZ_ARS01, "en", 0, 1, OD01_URI_EN, OD01_BESCHREIBUNG_EN, "", OD01_BEZEICHNUNG_EN),
            Arguments.of(
                SPEZ_ARS01,
                "de",
                0,
                2,
                OD01_URI_DE,
                OD01_LINK_BESCHREIBUNG_DE,
                OD01_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            Arguments.of(
                SPEZ_ARS01,
                "de",
                1,
                2,
                OD02_URI_DE,
                OD02_LINK_BESCHREIBUNG_DE,
                OD02_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            Arguments.of(SPEZ_ARS02, "en", 0, 1, OD01_URI_EN, OD01_BESCHREIBUNG_EN, "", OD01_BEZEICHNUNG_EN),
            Arguments.of(
                SPEZ_ARS02,
                "de",
                0,
                2,
                OD01_URI_DE,
                OD01_LINK_BESCHREIBUNG_DE,
                OD01_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            Arguments.of(
                SPEZ_ARS02,
                "de",
                1,
                2,
                OD02_URI_DE,
                OD02_LINK_BESCHREIBUNG_DE,
                OD02_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            Arguments.of(
                ARS_SEARCH,
                "",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_BESCHREIBUNG_DE,
                OD01_LINK_TITEL_DE,
                OD01_BESCHREIBUNG_DE
            ),
            Arguments.of(ARS_SEARCH, "ru", 0, 1, OD01_URI_DE, OD01_LINK_BESCHREIBUNG_DE, OD01_LINK_TITEL_DE, "")
        );
    }

    @DisplayName("en_notComplete_handling")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1} param:{2}")
    @MethodSource("provideDataMatch")
    void match(
        String ars,
        String language,
        String searchParam,
        String name,
        String description,
        int ursprungsportalCount,
        String ursprungsportalTitel,
        String ursprungsportalUri,
        Boolean enCompleted
    ) {
        Response response;
        if (searchParam.isEmpty()) {
            response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaId, language);
        } else {
            response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ars, leikaId, language, searchParam);
        }

        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateName(0, name).validateLanguageComplete(0, "en", enCompleted);
    }

    private static Stream<Arguments> provideDataMatch() {
        return Stream.of(
            // Parameter 1 - 3: ars, language, (optionaler) Suchparameter
            // Parameter 4 - 9: Erwartungswerte: name, description, Ursprungsportal(Anzahl), ursprungsportalTitel, ursprungsportalURI, languageComplete
            Arguments.of(ARS_SEARCH, "en", "", ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(
                ARS_SEARCH,
                "de",
                "",
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                ARS_SEARCH,
                "ru",
                "",
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                ARS_SEARCH,
                "",
                "",
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            // ars Spezialisierung 1
            Arguments.of(SPEZ_ARS01, "en", "", ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(
                SPEZ_ARS01,
                "de",
                "",
                ENC_SPEZ01_TM03_DE,
                ENC_SPEZ01_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            // ars Spezialisierung 2
            Arguments.of(SPEZ_ARS02, "en", "", ENC_SPEZ02_TM03_EN, ENC_SPEZ02_TM06_EN, 0, "", "", false),
            Arguments.of(
                SPEZ_ARS02,
                "de",
                "",
                ENC_SPEZ02_TM03_DE,
                ENC_SPEZ02_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            // mit query Parameter
            Arguments.of(ARS_SEARCH, "en", ENC_TM02_DE, ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(ARS_SEARCH, "en", ENC_TM02_EN, ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(
                ARS_SEARCH,
                "de",
                ENC_TM02_EN,
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                ARS_SEARCH,
                "de",
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(SPEZ_ARS01, "en", ENC_SPEZ01_TM03_DE, ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(SPEZ_ARS02, "en", ENC_TM02_DE, ENC_SPEZ02_TM03_EN, ENC_SPEZ02_TM06_EN, 0, "", "", false),
            Arguments.of(ARS_SEARCH, "en", ENC_TM02_DE, ENC_TM02_EN, ENC_TM06_EN, 0, "", "", false),
            Arguments.of(
                ARS_SEARCH,
                "de",
                ENC_TM02_DE,
                ENC_TM03_DE,
                ENC_TM06_DE,
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            )
        );
    }

    @Test
    void teaser() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV4(ARS_SEARCH, leikaId);
        LeikaIdResultListValidator validator = new LeikaIdResultListValidator(response);
        validator.validateTeaser(0, TEASER_TM24);
    }
}
