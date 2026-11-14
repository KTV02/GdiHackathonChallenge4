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
package suchdienst.integrationstest.tests.x22.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.ServiceDescriptionValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class TextmoduleTest {

    private static final String[] LEIKA_CODES_SORTIERT_AUFSTEIGEND = new String[] {
        "02",
        "03",
        "05",
        "06",
        "07",
        "08",
        "09",
        "11",
        "14",
        "15",
        "16",
        "18",
        "22",
        "23",
        "24",
        "25",
    };

    private static final String[] LEIKA_CODES_UNSORTIERT = new String[] {
        "25",
        "24",
        "02",
        "23",
        "07",
        "15",
        "11",
        "08",
        "18",
        "09",
        "05",
        "22",
        "06",
        "16",
        "03",
        "14",
    };

    private static final int[] POSITION_DARSTELLUNG_UNSORTIERT = new int[] {
        854,
        319,
        693,
        349,
        946,
        458,
        831,
        934,
        645,
        438,
        168,
        348,
        369,
        147,
        456,
        145,
    };

    private static final LinkedHashMap<String, String> CODE_ZU_MODUL_AUSGABESORTIERUNG = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG = new LinkedHashMap<>();

    static {
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("24", "Kurzinfo");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("23", "Ansprechpunkt");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("06", "Beschreibung");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("09", "Voraussetzungen");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("08", "Erforderliche Unterlagen");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("14", "Formulare");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("16", "Hinweise (Besonderheiten)");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("15", "weiterführende Informationen");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("07", "Rechtsgrundlage(n)");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("18", "Urheber");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("22", "Zuständige Stelle");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("02", "Leistungsbezeichnung");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("03", "Leistungsbezeichnung II (Leistungsname)");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("11", "Verfahrensablauf");
        CODE_ZU_MODUL_AUSGABESORTIERUNG.put("25", "Rechtsbehelf");

        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("24", "Quick info");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("23", "Contact point");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("06", "Description");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("09", "Prerequisites");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("08", "Required documents");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("14", "Forms");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("16", "Notes (specifics)");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("15", "Further Information");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("07", "Legal basis(s)");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("18", "Copyright");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("22", "Competent office");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("02", "Service description");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("03", "Service description II (service name)");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("11", "Procedure");
        CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.put("25", "Legal remedy");
    }

    static final String LEIKA_CODE_FOR_MODULE_WITH_LINK = "18";

    @Test
    void dataInLb(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        for (String leikaCode : LEIKA_CODES_SORTIERT_AUFSTEIGEND) {
            String inhalt = String.format("Textmodul%s", leikaCode);
            textmodule.add(
                Textmodul.builder()
                    .leikaCode(leikaCode)
                    .inhalt(inhalt)
                    .positionDarstellung(Integer.valueOf(leikaCode))
                    .build()
            );
        }

        leistung.setTextmodule(textmodule);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator.validateName("Textmodul03");

        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%s", entry.getKey()))
                .validateType(i, "_module")
                .validateLinkCount(i, 0)
                .validateAdditionalInformationCount(i, 0);
            i++;
        }
    }

    @Test
    void dataInLbEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        for (String leikaCode : LEIKA_CODES_SORTIERT_AUFSTEIGEND) {
            String inhalt = String.format("Textmodul%s", leikaCode);
            String inhaltEnglisch = inhalt.concat(" Englisch");
            textmodule.add(
                Textmodul.builder()
                    .leikaCode(leikaCode)
                    .inhalt(inhalt)
                    .inhaltEnglisch(inhaltEnglisch)
                    .positionDarstellung(Integer.valueOf(leikaCode))
                    .build()
            );
        }

        leistung.setTextmodule(textmodule);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator.validateName("Textmodul03 Englisch");

        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%s Englisch", entry.getKey()))
                .validateType(i, "_module")
                .validateLinkCount(i, 0)
                .validateAdditionalInformationCount(i, 0);
            i++;
        }
    }

    @Test
    void dataInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String linkUri = String.format("http://example.com/%s", testcasename);

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        List<ModulSpezialisierung> module = new ArrayList<>();

        for (String leikaCode : LEIKA_CODES_SORTIERT_AUFSTEIGEND) {
            String inhalt = String.format("Textmodul%sSpez", leikaCode);
            Link link = LEIKA_CODE_FOR_MODULE_WITH_LINK.equals(leikaCode) ? Link.builder().uri(linkUri).build() : null;

            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode(leikaCode)
                            .inhalt(inhalt)
                            .positionDarstellung(Integer.valueOf(leikaCode))
                            .link(link)
                            .build()
                    )
                    .build()
            );
        }

        spezialisierung.setModulSpezialisierungen(module);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator.validateName("Textmodul03Spez");

        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%sSpez", entry.getKey()))
                .validateType(i, "_module")
                .validateAdditionalInformationCount(i, 0);
            if (LEIKA_CODE_FOR_MODULE_WITH_LINK.equals(entry.getKey())) {
                validator.validateLinkCount(i, 1).validateLinkUri(i, 0, linkUri);
            } else {
                validator.validateLinkCount(i, 0);
            }
            i++;
        }
    }

    @Test
    void dataInSpEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String linkUri = String.format("http://example.com/%s", testcasename);

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMax();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        List<ModulSpezialisierung> module = new ArrayList<>();
        for (String leikaCode : LEIKA_CODES_SORTIERT_AUFSTEIGEND) {
            String inhalt = String.format("Textmodul%sSpez", leikaCode);
            String inhaltEnglisch = inhalt.concat(" Englisch");
            Link link = LEIKA_CODE_FOR_MODULE_WITH_LINK.equals(leikaCode)
                ? Link.builder().uriEnglisch(linkUri).build()
                : null;

            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode(leikaCode)
                            .inhalt(inhalt)
                            .inhaltEnglisch(inhaltEnglisch)
                            .positionDarstellung(Integer.valueOf(leikaCode))
                            .linkEnglisch(link)
                            .build()
                    )
                    .build()
            );
        }

        spezialisierung.setModulSpezialisierungen(module);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );

        // Validierung
        ServiceDescriptionValidator serviceDescriptionValidator = new ServiceDescriptionValidator(response);
        serviceDescriptionValidator.validateName("Textmodul03Spez Englisch");

        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_ENGLISCH_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%sSpez Englisch", entry.getKey()))
                .validateType(i, "_module")
                .validateAdditionalInformationCount(i, 0);
            if (LEIKA_CODE_FOR_MODULE_WITH_LINK.equals(entry.getKey())) {
                validator.validateLinkCount(i, 1).validateLinkUri(i, 0, linkUri);
            } else {
                validator.validateLinkCount(i, 0);
            }
            i++;
        }
    }

    @Test
    void reihenfolgeInLb(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        for (int i = 0; i < LEIKA_CODES_UNSORTIERT.length; i++) {
            String inhalt = String.format("Textmodul%s", LEIKA_CODES_UNSORTIERT[i]);
            textmodule.add(
                Textmodul.builder()
                    .leikaCode(LEIKA_CODES_UNSORTIERT[i])
                    .inhalt(inhalt)
                    .positionDarstellung(POSITION_DARSTELLUNG_UNSORTIERT[i])
                    .build()
            );
        }

        leistung.setTextmodule(textmodule);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%s", entry.getKey()));
            i++;
        }
    }

    @Test
    void reihenfolgeInSp(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        List<ModulSpezialisierung> module = new ArrayList<>();
        for (int i = 0; i < LEIKA_CODES_UNSORTIERT.length; i++) {
            String inhalt = String.format("Textmodul%s", LEIKA_CODES_UNSORTIERT[i]);

            module.add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode(LEIKA_CODES_UNSORTIERT[i])
                            .inhalt(inhalt)
                            .positionDarstellung(POSITION_DARSTELLUNG_UNSORTIERT[i])
                            .build()
                    )
                    .build()
            );
        }

        spezialisierung.setModulSpezialisierungen(module);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = spezialisierung.getArs().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        TextmodulValidator validator = new TextmodulValidator(response);
        validator.validateCount(15);

        int i = 0;
        for (var entry : CODE_ZU_MODUL_AUSGABESORTIERUNG.entrySet()) {
            validator
                .validateCode(i, entry.getKey())
                .validateTitle(i, entry.getValue())
                .validateText(i, String.format("Textmodul%s", entry.getKey()));
            i++;
        }
    }

    @Test
    void emptyInhalt(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(Textmodul.builder().leikaCode("02").inhalt("Textmodul02").positionDarstellung(1).build());
        textmodule.add(Textmodul.builder().leikaCode("03").inhalt("Textmodul03").positionDarstellung(2).build());
        textmodule.add(Textmodul.builder().leikaCode("06").inhalt("Textmodul06").positionDarstellung(3).build());
        textmodule.add(Textmodul.builder().leikaCode("08").inhalt("").positionDarstellung(4).build());
        textmodule.add(Textmodul.builder().leikaCode("22").inhalt("").positionDarstellung(5).build());
        textmodule.add(Textmodul.builder().leikaCode("23").inhalt("").positionDarstellung(6).build());
        textmodule.add(Textmodul.builder().leikaCode("77").inhalt("").positionDarstellung(7).build());
        leistung.setTextmodule(textmodule);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );
        // Validierung
        TextmodulValidator validator = new TextmodulValidator(response);
        validator
            .validateCount(3) // Nur drei da Textmodule mit leerem Inhalt nicht ausgegeben werden sollen (08, 22, 23)
            .validateCode(0, "06")
            .validateTitle(0, "Beschreibung")
            .validateText(0, "Textmodul06")
            .validateCode(1, "02")
            .validateTitle(1, "Leistungsbezeichnung")
            .validateText(1, "Textmodul02")
            .validateCode(2, "03")
            .validateTitle(2, "Leistungsbezeichnung II (Leistungsname)")
            .validateText(2, "Textmodul03");

        Response responseEn = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );
        // Validierung
        TextmodulValidator validatorEn = new TextmodulValidator(responseEn);
        validatorEn
            .validateCount(3) // Nur drei da Textmodule mit leerem Inhalt nicht ausgegeben werden sollen (08, 22, 23)
            .validateCode(0, "06")
            .validateTitle(0, "Beschreibung")
            .validateText(0, "Textmodul06")
            .validateCode(1, "02")
            .validateTitle(1, "Leistungsbezeichnung")
            .validateText(1, "Textmodul02")
            .validateCode(2, "03")
            .validateTitle(2, "Leistungsbezeichnung II (Leistungsname)")
            .validateText(2, "Textmodul03");
    }

    @Test
    void emptyInhaltEnglisch(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Daten erzeugen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        List<Textmodul> textmodule = new ArrayList<>();
        textmodule.add(
            Textmodul.builder()
                .leikaCode("02")
                .inhalt("Textmodul02")
                .inhaltEnglisch("Textmodul02Englisch")
                .positionDarstellung(1)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("03")
                .inhalt("Textmodul03")
                .inhaltEnglisch("Textmodul03Englisch")
                .positionDarstellung(2)
                .build()
        );
        textmodule.add(
            Textmodul.builder()
                .leikaCode("06")
                .inhalt("Textmodul06")
                .inhaltEnglisch("Textmodul06Englisch")
                .positionDarstellung(3)
                .build()
        );
        textmodule.add(
            Textmodul.builder().leikaCode("08").inhalt("").inhaltEnglisch("").positionDarstellung(4).build()
        );
        textmodule.add(
            Textmodul.builder().leikaCode("22").inhalt("").inhaltEnglisch("").positionDarstellung(5).build()
        );
        textmodule.add(
            Textmodul.builder().leikaCode("23").inhalt("").inhaltEnglisch("").positionDarstellung(6).build()
        );
        leistung.setTextmodule(textmodule);

        // Daten einspielen und an der SuD API abfragen
        ImportUtil.pushAndWait(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_DE
        );
        // Validierung
        TextmodulValidator validator = new TextmodulValidator(response);
        validator
            .validateCount(3) // Nur drei da Textmodule mit leerem Inhalt nicht ausgegeben werden sollen (08, 22, 23)
            .validateCode(0, "06")
            .validateTitle(0, "Beschreibung")
            .validateText(0, "Textmodul06")
            .validateCode(1, "02")
            .validateTitle(1, "Leistungsbezeichnung")
            .validateText(1, "Textmodul02")
            .validateCode(2, "03")
            .validateTitle(2, "Leistungsbezeichnung II (Leistungsname)")
            .validateText(2, "Textmodul03");

        Response responseEn = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(
            ars,
            leistung.getFullId(),
            Constants.LANGUAGE_EN
        );
        // Validierung
        TextmodulValidator validatorEn = new TextmodulValidator(responseEn);
        validatorEn
            .validateCount(3) // Nur drei da Textmodule mit leerem Inhalt nicht ausgegeben werden sollen (08, 22, 23)
            .validateCode(0, "06")
            .validateTitle(0, "Description")
            .validateText(0, "Textmodul06Englisch")
            .validateCode(1, "02")
            .validateTitle(1, "Service description")
            .validateText(1, "Textmodul02Englisch")
            .validateCode(2, "03")
            .validateTitle(2, "Service description II (service name)")
            .validateText(2, "Textmodul03Englisch");
    }

    /**
     * Testet das Verhalten bei leerem Textmodul mit Code 06. Der SuD akzeptiert das leere Volltextmodul und gibt es entsprechend aus.
     */
    @Test
    void emptyInhalt06(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul06";
        //Setup mit leeren Textmodul 06
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("06"))
            .findFirst()
            .ifPresent(t -> t.setInhalt(null));
        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(fileSetup);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateDetailsCount("06", 0);

        //push Spezialisierung mit gefülltem Textmodul 06 -> Description wird gefüllt
        XzufiFile file = new XzufiFile();
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("06").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateDetailsCount("06", 1).validateVolltext(spezTextmodulInhalt);
    }

    /**
     * Testet das Verhalten bei fehlendem Textmodul mit Code 06. Der SuD akzeptiert das leere Volltextmodul und gibt die Leistung entsprechend aus.
     */
    @Test
    void missingTextmodul06(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String spezTextmodulInhalt = testcasename + "SpezTextModul06";
        //Setup mit leeren Textmodul 06
        XzufiFile fileSetup = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = fileSetup.getLeistungsbeschreibungen().getFirst();
        leistung
            .getTextmodule()
            .stream()
            .filter(t -> t.getLeikaCode().equals("06"))
            .findFirst()
            .ifPresent(t -> leistung.getTextmodule().remove(t));
        String ars = fileSetup.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ImportUtil.pushAndWait(fileSetup);

        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        ServiceDescriptionValidator validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateDetailsCount("06", 0);

        //push Spezialisierung mit gefülltem Textmodul 06 -> Description wird gefüllt
        XzufiFile file = new XzufiFile();
        Spezialisierung spezialisierung = SpCreator.getInstance().getMin(testcasename, leistung.getId());
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung
            .getModulSpezialisierungen()
            .add(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(Textmodul.builder().leikaCode("06").inhalt(spezTextmodulInhalt).build())
                    .build()
            );
        file.getSpezialisierungen().add(spezialisierung);
        ImportUtil.pushAndWait(file);

        response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());
        validator = new ServiceDescriptionValidator(response);
        validator.validateId(leistung.getFullId()).validateDetailsCount("06", 1).validateVolltext(spezTextmodulInhalt);
    }
}
