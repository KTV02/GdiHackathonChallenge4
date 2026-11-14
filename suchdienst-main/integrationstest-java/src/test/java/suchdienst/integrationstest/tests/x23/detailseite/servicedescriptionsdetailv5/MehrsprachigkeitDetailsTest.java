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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.DynamischerParameter;
import de.dataport.pvog.testdatengenerator.model.Formular;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.ObjectUtils;
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
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.FormsValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.TextmodulValidator;

/**
 * <h1>Test der Behandlung von unvollständigen englischsprachingen Inhalten - languageComplete:false</h1>
 * Bei unvollständigen englischsprachigen Inhalten soll für Abfrage in en auch nur die englischsprachigen Inhalte
 * zurückgegeben werden.
 * Ein besonderes Augenmerk wird auf die Spezialisierungen gerichtet
 */

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class MehrsprachigkeitDetailsTest {

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
    private static final String ENC_SPEZ01_TM25_DE = "EN_notCompleteRechtsbehelf25SPEZ_DE";
    private static final String ENC_SPEZ01_TM25_EN = "EN_notCompleteRechtsbehelf25SPEZ_EN";
    private static final String ENC_SPEC01_URSPRUNGSPORTAL_TITEL_DE = "EN_notComplete_spez01_ursprungsportal_titel_DE";
    private static final String ENC_SPEC01_URSPRUNGSPORTAL_URI_DE =
        "www.EN_notComplete_spez01_ursprungsportal_titel.DE";
    private static final String ENC_SPEC01_URSPRUNGSPORTAL_TITEL_EN = "EN_notComplete_spez01_ursprungsportal_titel_EN";
    private static final String ENC_SPEC01_URSPRUNGSPORTAL_URI_EN =
        "www.EN_notComplete_spez01_ursprungsportal_titel.en";

    private static final String ENC_SPEZ02_TM02_DE = "EN_notComplete02SPEZ02_DE";
    private static final String ENC_SPEZ02_TM02_EN = "EN_notComplete02SPEZ02_EN";
    private static final String ENC_SPEZ02_TM03_DE = "EN_notComplete03SPEZ02_DE";
    private static final String ENC_SPEZ02_TM03_EN = "EN_notComplete03SPEZ02_EN";
    private static final String ENC_SPEZ02_TM06_DE = "EN_notComplete06SPEZ02_DE";
    private static final String ENC_SPEZ02_TM06_EN = "EN_notComplete06SPEZ02_EN";

    //Textmodul 24 - Teaser wird als description ausgegeben, wenn es verfügbar ist
    private static final String ENC_SPEZ02_TM24_DE = "EN_notCompleteTM24SPEZ02_DE";
    private static final String ENC_SPEZ02_TM24_EN = "EN_notCompleteTM24SPEZ02_EN";

    private static final String ENC_SPEZ03_TM02_DE = "EN_notComplete02SPEZ03_DE";

    private static final String ENC_SPEZ04_TM02_DE = "Additional spez04 EN_notComplete02_DE";
    private static final String ENC_SPEZ04_TM02_EN = "Additional spez04 EN_notComplete02_EN";
    private static final String ENC_SPEZ04_TM03_DE = "Additional spez04EN_notComplete03_DE";
    private static final String ENC_SPEZ04_TM03_EN = "Additional spez04 EN_notComplete03_EN";
    private static final String ENC_SPEZ04_TM06_DE = "Additional spez04 EN_notComplete06_DE";
    private static final String ENC_SPEZ04_TM06_EN = "Additional spez04 EN_notComplete06_EN";

    private static final BundeslandConfig bundeslandConfig = new BundeslandConfig(
        Bundesland.BY,
        "L100042",
        "090000000000",
        // München, Nürnberg, Moos, Egg an der Günz
        List.of("091620000000", "095640000000", "092715205135", "097785758130")
    );
    private static String spezArs01;
    private static String spezArs02;
    private static String spezArs03;
    private static String spezArs04;
    // Augsburg
    private static final String ARS = "097610000000";
    private static String lbId;

    private static final String ENC_URSPRUNGSPORTAL_TITEL = "EN_notComplete_ursprungsportal_titel_DE";
    private static final String ENC_URSPRUNGSPORTAL_URI = "www.EN_notComplete_ursprungsportal_titel.de";

    //OD Data
    private static final String OD01_BESCHREIBUNG_DE = "OD01_Beschreibung_DE";
    private static final String OD01_BESCHREIBUNG_EN = "OD01_Description_EN";
    private static final String OD01_BEZEICHNUNG_DE = "OD01_Bezeichnung_DE";
    private static final String OD01_BEZEICHNUNG_EN = "OD01_Definition_EN";
    private static final String OD01_URI_DE = "http://myod01.de";
    private static final String OD01_URI_EN = "http://myod01.en";
    private static final String OD01_PARAM_DE = "arsod01DE";
    private static final String OD01_PARAM_EN = "arsod01EN";
    private static final String OD01_LINK_DESCRIPTION_DE = "LinkBeschreibungOD01DE";
    private static final String OD01_LINK_DESCRIPTION_EN = "LinkBeschreibungOD01EN";
    private static final String OD01_LINK_TITEL_DE = "LinkTitelOD01DE";
    private static final String OD01_LINK_TITEL_EN = "LinkTitelOD01EN"; // in xzufi 2.3 ist OD Link Pflicht!

    private static final String OD02_BESCHREIBUNG_DE = "od02_Beschreibung_DE";
    private static final String OD02_BESCHREIBUNG_EN = "od02_Description_EN";
    private static final String OD02_BEZEICHNUNG_DE = "od02_Bezeichnung_DE";
    private static final String OD02_BEZEICHNUNG_EN = "od02_Definition_EN";
    private static final String OD02_URI_DE = "http://myod02.de";
    private static final String OD02_PARAM_DE = "arsod02DE";
    private static final String OD02_PARAM_EN = "arsod02EN";
    private static final String OD02_LINK_DESCRIPTION_DE = "LinkBeschreibungod02DE";
    private static final String OD02_LINK_TITEL_DE = "LinkTitelod02DE";
    private static final String OD02_LINK_TITEL_EN = "LinkTitelod02EN";

    /**
     * Für die Tests wird folgendes Szenario bereitgestellt
     * <p>
     * Default Leistung
     * Textmodul 02 -> de und en
     * Textmodul 03 -> de
     * Textmodul 06 -> de und en
     * Ursprungsportal Titel -> de
     * </p>
     * <p>
     * Spezialisierung 1
     * Textmodul 02 -> de
     * Textmodul 03 -> de
     * Textmodul 06 -> de
     * Textmodul 25 -> de und en
     * Ursprungsportal Titel -> de und en
     * </p>
     * <p>
     * Spezialisierung 2
     * Textmodul 02 -> de und en
     * Textmodul 03 -> de und en
     * Textmodul 06 -> de und en
     * Textmodul 24 -> de und en
     * Ursprungsportal Titel -> de und en
     * </p>
     * <p>
     * Spezialisierung 3
     * Textmodul 03 -> de
     * </p>
     * <p>
     * Spezialisierung 4
     * <b>Besonderheit - Ersetzung = false</b>
     * Textmodul 02 -> de und en
     * Textmodul 03 -> de und en
     * Textmodul 06 -> de und en
     * </p>
     */

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile fileENnotComplete = XzufiFileCreator.newInstance(bundeslandConfig)
            .name(testclassname)
            .lb(1)
            .oe(2)
            .zsOe(1)
            .od(2)
            .zsOd(1)
            .sp(4)
            .fo(1)
            .zsFo(1)
            .getMin();

        Leistungsbeschreibung lbENnotComplete = fileENnotComplete.getLeistungsbeschreibungen().getFirst();
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
        lbENnotComplete.setTextmodule(textmodule);

        lbENnotComplete.getUrsprungsportale().clear();
        lbENnotComplete.setUrsprungsportale(
            List.of(
                UrsprungsportalModul.builder()
                    .title(ENC_URSPRUNGSPORTAL_TITEL)
                    .uri(ENC_URSPRUNGSPORTAL_URI)
                    .positionDarstellung(0)
                    .build()
            )
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
                        UrsprungsportalModul.builder()
                            .title(ENC_SPEC01_URSPRUNGSPORTAL_TITEL_DE)
                            .uri(ENC_SPEC01_URSPRUNGSPORTAL_URI_DE)
                            .titleEnglisch(ENC_SPEC01_URSPRUNGSPORTAL_TITEL_EN)
                            .uriEnglisch(ENC_SPEC01_URSPRUNGSPORTAL_URI_EN)
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
                        Textmodul.builder()
                            .leikaCode("25")
                            .inhalt(ENC_SPEZ01_TM25_DE)
                            .inhaltEnglisch(ENC_SPEZ01_TM25_EN)
                            .positionDarstellung(8)
                            .build()
                    )
                    .build()
            );
        spezArs01 = spez01.getArs().getFirst();

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
        spez02
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("24")
                            .inhalt(ENC_SPEZ02_TM24_DE)
                            .inhaltEnglisch(ENC_SPEZ02_TM24_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        spezArs02 = spez02.getArs().getFirst();

        Spezialisierung spez03 = fileENnotComplete.getSpezialisierungen().get(2);
        spez03.getModulSpezialisierungen().clear();
        spez03
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder().leikaCode("02").inhalt(ENC_SPEZ03_TM02_DE).positionDarstellung(2).build()
                    )
                    .build()
            );
        spezArs03 = spez03.getArs().getFirst();

        Spezialisierung spez04 = fileENnotComplete.getSpezialisierungen().get(3);
        spez04.getModulSpezialisierungen().clear();
        spez04
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("02")
                            .inhalt(ENC_SPEZ04_TM02_DE)
                            .inhaltEnglisch(ENC_SPEZ04_TM02_EN)
                            .positionDarstellung(2)
                            .build()
                    )
                    .build()
            );
        spez04
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("03")
                            .inhalt(ENC_SPEZ04_TM03_DE)
                            .inhaltEnglisch(ENC_SPEZ04_TM03_EN)
                            .positionDarstellung(3)
                            .build()
                    )
                    .build()
            );
        spez04
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .inhalt(ENC_SPEZ04_TM06_DE)
                            .inhaltEnglisch(ENC_SPEZ04_TM06_EN)
                            .positionDarstellung(6)
                            .build()
                    )
                    .build()
            );
        spezArs04 = spez04.getArs().getFirst();

        List<Zustaendigkeit> odZSList = fileENnotComplete.getZustaendigkeitenOnlinedienst();
        odZSList.get(0).getArss().clear();
        odZSList.get(0).setArss(List.of(bundeslandConfig.getBasicArs()));

        odZSList.get(1).getArss().clear();
        odZSList.get(1).setArss(List.of(spezArs01, spezArs02));

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
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName(OD01_PARAM_DE)
                                .parameterNameEnglisch(OD01_PARAM_EN)
                                .build()
                        )
                    )
                    .titel(OD01_LINK_TITEL_DE)
                    .titelEnglisch(OD01_LINK_TITEL_EN)
                    .beschreibung(OD01_LINK_DESCRIPTION_DE)
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
                    .dynamischeParameter(
                        List.of(
                            DynamischerParameter.builder()
                                .typ("001")
                                .parameterName(OD02_PARAM_DE)
                                .parameterNameEnglisch(OD02_PARAM_EN)
                                .build()
                        )
                    )
                    .titel(OD02_LINK_TITEL_DE)
                    .titelEnglisch(OD02_LINK_TITEL_EN)
                    .beschreibung(OD02_LINK_DESCRIPTION_DE)
                    .beschreibungEnglisch(OD01_LINK_DESCRIPTION_EN)
                    .positionDarstellung(1010)
                    .positionDarstellungEnglisch(1)
                    .build()
            )
        );
        od02.setDurchfuehrungStaatsangehoerigkeit(List.of("000", "126", "168"));
        od02.setDurchfuehrungSprachen(
            List.of(Sprache.builder().code("de").bezeichnungDeutsch("deutsch").bezeichnungNativ("Deutsch").build())
        );

        // Das Setup beim Formular sorgt hier - implizit - dafür, dass
        // es sowohl eine deutsche als auch eine unvollständige englische Sprachversion in dem Formular VL gibt.
        // Die englische enthält nur als description den Wert "Beschreibung Englisch", das Feld "name" bleibt leer.
        // Erwartet wird dann beim Auslesen von englischen Details über die v5/servicedescription/details, dass.... ??? siehe Methode "forms"
        Formular fo1 = fileENnotComplete.getFormulare().getFirst();
        fo1.setBezeichnung("fo1-bezeichnung");
        fo1.setKurzbezeichnungEnglish("fo1-kurzbezeichnung-englisch");
        fo1.getLink().setUri("https://gidf.de/fo1");
        fo1.getLink().setUriEnglisch("https://gidf.de/fo1");
        Zustaendigkeit zsfo = fileENnotComplete.getZustaendigkeitenFormular().getFirst();
        zsfo.setArss(List.of(ARS));
        ImportUtil.pushAndWait23(fileENnotComplete);

        lbId = fileENnotComplete.getLeistungsbeschreibungen().getFirst().getFullId();
    }

    @DisplayName("en_notComplete_handling")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1}")
    @MethodSource("provideDetailsData")
    void details(
        String ars,
        String language,
        String name,
        String description,
        String volltext,
        String leistung,
        String leistungII,
        String teaser,
        String rechtsbehelf,
        int ursprungsportalCount,
        String ursprungsportalTitel,
        String ursprungsportalUri,
        Boolean enCompleted
    ) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId, language);

        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator
            .validateName(name)
            .validateDescription(description)
            .validateLanguageCompleteForEN(enCompleted)
            .validateSourcePortalsCount(ursprungsportalCount);

        TextmodulValidator textmodulValidator = new TextmodulValidator(response);
        var baseIndex = ObjectUtils.isEmpty(teaser) ? 0 : 1;
        textmodulValidator.validateText(baseIndex, volltext).validateText(baseIndex + 1, leistung);
        if (leistung.isEmpty()) {
            textmodulValidator.validateContainsNotCode("02");
        } else {
            textmodulValidator.validateTextByCode("02", leistung);
        }
        if (leistungII.isEmpty()) {
            textmodulValidator.validateContainsNotCode("03");
        } else {
            textmodulValidator.validateTextByCode("03", leistungII);
        }
        if (teaser.isEmpty()) {
            textmodulValidator.validateContainsNotCode("24");
        } else {
            textmodulValidator.validateTextByCode("24", teaser);
        }
        if (rechtsbehelf.isEmpty()) {
            textmodulValidator.validateContainsNotCode("25");
        } else {
            textmodulValidator.validateTextByCode("25", rechtsbehelf);
        }
        if (ursprungsportalCount > 0) {
            validator.validateSourcePortalTitle(0, ursprungsportalTitel).validateSourcePortalUri(0, ursprungsportalUri);
        }
    }

    @DisplayName("en_notComplete_serviceLinks")
    @ParameterizedTest(name = "{displayName} ars:{0} language:{1}")
    @MethodSource("provideServiceLinkData")
    void onlineServiceLinks(
        String ars,
        String language,
        int index,
        int count,
        String uri,
        String description,
        String titel,
        int paramIndex,
        String param,
        String parentOdDescription
    ) {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId, language);
        OnlineServiceLinkValidator validator = new OnlineServiceLinkValidator(response);
        validator.validateCount(count);
        if (uri.isEmpty()) {
            validator.validateContainsNoUri(index);
        } else {
            validator.validateUri(index, uri);
        }
        if (description.isEmpty()) {
            validator.validateContainsNoDescription(index);
        } else {
            validator.validateDescription(index, description);
        }
        validator.validateTitle(index, titel);
        if (param.isEmpty()) {
            validator.validateContainsNoParameterName(index, paramIndex);
        } else {
            validator.validateDynamicParametersParameterName(index, paramIndex, param);
        }
    }

    /**
     * Das Test-Setup hat ein Formular generiert, dass für englisch nicht vollständig gefüllt ist und dessen Name nicht gesetzt ist.
     * Erwartet wird hier, analog zu den anderen englisch-unvollständigen Modellen, dass es keine Ersatzbehandlung dafür gibt,
     * stattdessen soll der Name im Response einfach fehlen.
     */
    @Test
    @DisplayName("en_notComplete_forms")
    void forms() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ARS, lbId, "en");
        FormsValidator validator = new FormsValidator(response);
        validator
            .validateCount(1)
            .validateName(0, null)
            .validateDescription(0, null)
            .validateUri(0, "https://gidf.de/fo1");
    }

    private static Stream<Arguments> provideServiceLinkData() {
        //die ersten 3 Argumente sind Abfrageparameter ars, language und index die folgenden sind die Erwartungswerte Anzahl Links, Link_uri,
        // Link_Beschreibung, Link_Titel, Param Index, Dynamischer Parameter name, OD Beschreibung
        return Stream.of(
            Arguments.of(ARS, "en", 0, 1, OD01_URI_EN, "", OD01_LINK_TITEL_EN, 0, OD01_PARAM_EN, OD01_BEZEICHNUNG_EN),
            Arguments.of(
                ARS,
                "de",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            //kein Eintrag für OD02 bei ars "097770000000" und en - fehlende URI in en
            Arguments.of(
                spezArs01,
                "en",
                0,
                1,
                OD01_URI_EN,
                "",
                OD01_LINK_TITEL_EN,
                0,
                OD01_PARAM_EN,
                OD01_BEZEICHNUNG_EN
            ),
            Arguments.of(
                spezArs01,
                "de",
                0,
                2,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                spezArs01,
                "de",
                1,
                2,
                OD02_URI_DE,
                OD02_LINK_DESCRIPTION_DE,
                OD02_LINK_TITEL_DE,
                0,
                OD02_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                spezArs02,
                "en",
                0,
                1,
                OD01_URI_EN,
                "",
                OD01_LINK_TITEL_EN,
                0,
                OD01_PARAM_EN,
                OD01_BEZEICHNUNG_EN
            ),
            Arguments.of(
                spezArs02,
                "de",
                0,
                2,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                spezArs02,
                "de",
                1,
                2,
                OD02_URI_DE,
                OD02_LINK_DESCRIPTION_DE,
                OD02_LINK_TITEL_DE,
                0,
                OD02_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                ARS,
                "",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                ARS,
                "ru",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                ""
            ),
            Arguments.of(
                spezArs03,
                "en",
                0,
                1,
                OD01_URI_EN,
                "",
                OD01_LINK_TITEL_EN,
                0,
                OD01_PARAM_EN,
                OD01_BEZEICHNUNG_EN
            ),
            Arguments.of(
                spezArs03,
                "de",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            ),
            Arguments.of(
                spezArs04,
                "en",
                0,
                1,
                OD01_URI_EN,
                "",
                OD01_LINK_TITEL_EN,
                0,
                OD01_PARAM_EN,
                OD01_BEZEICHNUNG_EN
            ),
            Arguments.of(
                spezArs04,
                "de",
                0,
                1,
                OD01_URI_DE,
                OD01_LINK_DESCRIPTION_DE,
                OD01_LINK_TITEL_DE,
                0,
                OD01_PARAM_DE,
                OD01_BEZEICHNUNG_DE
            )
        );
    }

    private static Stream<Arguments> provideDetailsData() {
        return Stream.of(
            //die ersten 3 Argumente sind Abfrageparameter ars, language die folgenden sind die Erwartungswerte name, description, Volltext, Leistungsbezeichnung, Leistungsbezeichnung II, Teaser, Rechtsbehelf,
            // Ursprungsportal(Anzahl), ursprungsportalTitel, ursprungsportalURI, languageComplete
            //
            // default ars
            Arguments.of(ARS, "en", ENC_TM02_EN, ENC_TM06_EN, ENC_TM06_EN, ENC_TM02_EN, "", "", "", 0, "", "", false),
            Arguments.of(
                ARS,
                "de",
                ENC_TM03_DE,
                ENC_TM06_DE,
                ENC_TM06_DE,
                ENC_TM02_DE,
                ENC_TM03_DE,
                "",
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                ARS,
                "ru",
                ENC_TM03_DE,
                ENC_TM06_DE,
                ENC_TM06_DE,
                ENC_TM02_DE,
                ENC_TM03_DE,
                "",
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                ARS,
                "",
                ENC_TM03_DE,
                ENC_TM06_DE,
                ENC_TM06_DE,
                ENC_TM02_DE,
                ENC_TM03_DE,
                "",
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            // ars Spezialisierung 1
            Arguments.of(
                spezArs01,
                "en",
                ENC_TM02_EN,
                ENC_TM06_EN,
                ENC_TM06_EN,
                ENC_TM02_EN,
                "",
                "",
                ENC_SPEZ01_TM25_EN,
                1,
                ENC_SPEC01_URSPRUNGSPORTAL_TITEL_EN,
                ENC_SPEC01_URSPRUNGSPORTAL_URI_EN,
                false
            ),
            Arguments.of(
                spezArs01,
                "de",
                ENC_SPEZ01_TM03_DE,
                ENC_SPEZ01_TM06_DE,
                ENC_SPEZ01_TM06_DE,
                ENC_SPEZ01_TM02_DE,
                ENC_SPEZ01_TM03_DE,
                "",
                ENC_SPEZ01_TM25_DE,
                1,
                ENC_SPEC01_URSPRUNGSPORTAL_TITEL_DE,
                ENC_SPEC01_URSPRUNGSPORTAL_URI_DE,
                false
            ),
            // ars Spezialisierung 2
            Arguments.of(
                spezArs02,
                "en",
                ENC_SPEZ02_TM03_EN,
                //PV-8781 - Teaser fuer Description
                ENC_SPEZ02_TM24_EN,
                ENC_SPEZ02_TM06_EN,
                ENC_SPEZ02_TM02_EN,
                ENC_SPEZ02_TM03_EN,
                ENC_SPEZ02_TM24_EN,
                "",
                0,
                "",
                "",
                false
            ),
            Arguments.of(
                spezArs02,
                "de",
                ENC_SPEZ02_TM03_DE,
                //PV-8781 - Teaser fuer Description
                ENC_SPEZ02_TM24_DE,
                ENC_SPEZ02_TM06_DE,
                ENC_SPEZ02_TM02_DE,
                ENC_SPEZ02_TM03_DE,
                ENC_SPEZ02_TM24_DE,
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                spezArs03,
                "en",
                ENC_TM02_EN,
                ENC_TM06_EN,
                ENC_TM06_EN,
                ENC_TM02_EN,
                "",
                "",
                "",
                0,
                "",
                "",
                false
            ),
            Arguments.of(
                spezArs03,
                "de",
                ENC_TM03_DE,
                ENC_TM06_DE,
                ENC_TM06_DE,
                ENC_SPEZ03_TM02_DE,
                ENC_TM03_DE,
                "",
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            ),
            Arguments.of(
                spezArs04,
                "en",
                ENC_SPEZ04_TM03_EN,
                ENC_TM06_EN + ENC_SPEZ04_TM06_EN,
                ENC_TM06_EN + ENC_SPEZ04_TM06_EN,
                ENC_TM02_EN + ENC_SPEZ04_TM02_EN,
                ENC_SPEZ04_TM03_EN,
                "",
                "",
                0,
                "",
                "",
                false
            ),
            Arguments.of(
                spezArs04,
                "de",
                ENC_TM03_DE + ENC_SPEZ04_TM03_DE,
                ENC_TM06_DE + ENC_SPEZ04_TM06_DE,
                ENC_TM06_DE + ENC_SPEZ04_TM06_DE,
                ENC_TM02_DE + ENC_SPEZ04_TM02_DE,
                ENC_TM03_DE + ENC_SPEZ04_TM03_DE,
                "",
                "",
                1,
                ENC_URSPRUNGSPORTAL_TITEL,
                ENC_URSPRUNGSPORTAL_URI,
                false
            )
        );
    }
}
