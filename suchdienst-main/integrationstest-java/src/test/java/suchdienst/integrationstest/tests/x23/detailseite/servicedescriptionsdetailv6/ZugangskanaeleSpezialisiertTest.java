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
package suchdienst.integrationstest.tests.x23.detailseite.servicedescriptionsdetailv6;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.modul.KostenmodulCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.modultypen.Modul;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Amount;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kosten;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.KostenFix;
import de.dataport.pvog.testdatengenerator.model.modultypen.modulkosten.Kostenmodul;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.KostenmodulValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.KostenmoduleValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv6.TextmodulValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ZugangskanaeleSpezialisiertTest {

    /* ⌈¯¯¯¯¯¯¯¯¯⌉
      | Ergänzen |
      ⌊_________⌋*/

    @Test
    void textOnOffErgaenzenOnOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOnOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .positionDarstellung(1)
            .inhalt("Das ist der Inhalt von Textmodul1 mit Zugangskanal: online & offline")
            .build();
        leistung.setTextmodule(List.of(textOnOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .positionDarstellung(2)
                            .inhalt(" +Zusätzliche Informationen zu: online & offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(1)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE_OFFLINE.name())
            .validateText(
                0,
                "Das ist der Inhalt von Textmodul1 mit Zugangskanal: online & offline +Zusätzliche Informationen zu: online & offline"
            );
    }

    @Test
    void textOnOffErgaenzenOnUndOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOnOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online & offline")
            .build();
        leistung.setTextmodule(List.of(textOnOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                            .positionDarstellung(2)
                            .inhalt(" +Zusätzliche Informationen: online")
                            .build()
                    )
                    .build(),
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                            .positionDarstellung(3)
                            .inhalt(" +Zusätzliche Informationen: offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, "Inhalt Textmodul mit Zugangskanal: online & offline +Zusätzliche Informationen: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, "Inhalt Textmodul mit Zugangskanal: online & offline +Zusätzliche Informationen: offline");
    }

    @Test
    void textOnUndOffErgaenzenOnUndOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online")
            .build();
        Textmodul textOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
            .positionDarstellung(2)
            .inhalt("Inhalt Textmodul mit Zugangskanal: offline")
            .build();
        leistung.setTextmodule(List.of(textOn, textOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                            .positionDarstellung(1)
                            .inhalt(" +Zusätzliche Informationen: online")
                            .build()
                    )
                    .build(),
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                            .positionDarstellung(2)
                            .inhalt(" +Zusätzliche Informationen: offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, "Inhalt Textmodul mit Zugangskanal: online +Zusätzliche Informationen: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, "Inhalt Textmodul mit Zugangskanal: offline +Zusätzliche Informationen: offline");
    }

    @Test
    void textOnErgaenzenOnOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online")
            .build();
        leistung.setTextmodule(List.of(textOn));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" +Zusätzliche Informationen: online & offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, "Inhalt Textmodul mit Zugangskanal: online +Zusätzliche Informationen: online & offline")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, " +Zusätzliche Informationen: online & offline");
    }

    @Test
    void textOffErgaenzenOnOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: offline")
            .build();
        leistung.setTextmodule(List.of(textOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" +Zusätzliche Informationen: online & offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, "Inhalt Textmodul mit Zugangskanal: offline +Zusätzliche Informationen: online & offline")
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, " +Zusätzliche Informationen: online & offline");
    }

    @Test
    void textOnErgaenzenOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online")
            .build();
        leistung.setTextmodule(List.of(textOn));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(false)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" +Zusätzliche Informationen: offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, "Inhalt Textmodul mit Zugangskanal: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, " +Zusätzliche Informationen: offline");
    }

    /* ⌈¯¯¯¯¯¯¯¯¯⌉
      | Ersetzen |
      ⌊_________⌋*/

    @Test
    void textOnOffErsetzenOnOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online & offline")
            .build();
        leistung.setTextmodule(List.of(textOn));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" =Ersetze Informationen: online & offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(1)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE_OFFLINE.name())
            .validateText(0, " =Ersetze Informationen: online & offline");
    }

    @Test
    void textOnOffErsetzenOn(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online & offline")
            .build();
        leistung.setTextmodule(List.of(textOn));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                            .positionDarstellung(1)
                            .inhalt(" =Ersetze Informationen: online")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, " =Ersetze Informationen: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, "Inhalt Textmodul mit Zugangskanal: online & offline");
    }

    @Test
    void textOnUndOffErsetzenOnOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online")
            .build();
        Textmodul textOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
            .positionDarstellung(2)
            .inhalt("Inhalt Textmodul mit Zugangskanal: offline")
            .build();
        leistung.setTextmodule(List.of(textOn, textOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" =Ersetze Informationen: online & offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(1)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE_OFFLINE.name())
            .validateText(0, " =Ersetze Informationen: online & offline");
    }

    @Test
    void textOnUndOffErsetzenOnUndOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOn = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: online")
            .build();
        Textmodul textOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
            .positionDarstellung(2)
            .inhalt("Inhalt Textmodul mit Zugangskanal: offline")
            .build();
        leistung.setTextmodule(List.of(textOn, textOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                            .positionDarstellung(2)
                            .inhalt(" =Ersetze Informationen: online")
                            .build()
                    )
                    .build(),
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                            .positionDarstellung(1)
                            .inhalt(" =Ersetze Informationen: offline")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, " =Ersetze Informationen: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, " =Ersetze Informationen: offline");
    }

    @Test
    void textOffErsetzenOn(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();

        Textmodul textOff = Textmodul.builder()
            .leikaCode("06")
            .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
            .positionDarstellung(1)
            .inhalt("Inhalt Textmodul mit Zugangskanal: offline")
            .build();
        leistung.setTextmodule(List.of(textOff));

        // Spezialisierung - Textmodul
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.setModulSpezialisierungen(
            List.of(
                ModulSpezialisierung.builder()
                    .ersetzung(true)
                    .modul(
                        Textmodul.builder()
                            .leikaCode("06")
                            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                            .positionDarstellung(1)
                            .inhalt(" =Ersetze Informationen: online")
                            .build()
                    )
                    .build()
            )
        );

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        TextmodulValidator validator = new TextmodulValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateText(0, " =Ersetze Informationen: online")
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateText(1, "Inhalt Textmodul mit Zugangskanal: offline");
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Test
    void kostenOnOffErgaenzenOff(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi - Steup und Einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.setKostenmodule(List.of(KostenmodulCreator.getInstance().getMax()));

        Kosten fixKosten40E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(40)).currencyCode("EUR").build())
            .build();
        fixKosten40E.setCodeTyp("001");
        Kostenmodul kostenmodul = leistung.getKostenmodule().getFirst();
        kostenmodul.setZugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE);
        kostenmodul.setKosten(List.of(fixKosten40E));
        kostenmodul.setBeschreibung("Beschreibung Kostenmodul: fix Kosten: 40");

        // Spezialisierung
        Kosten fixKosten80E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(80)).currencyCode("EUR").build())
            .build();
        fixKosten80E.setCodeTyp("001");
        Kostenmodul kostenmodulErgaenzung = KostenmodulCreator.getInstance().getMin();
        kostenmodulErgaenzung.setZugangskanalCode(Modul.ZugangskanalCode.OFFLINE);
        kostenmodulErgaenzung.setKosten(List.of(fixKosten80E));
        kostenmodulErgaenzung.setBeschreibung("Beschreibung Kostenmodul: fix Kosten: 80");
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        ModulSpezialisierung spModul = ModulSpezialisierung.builder()
            .ersetzung(false)
            .modul(kostenmodulErgaenzung)
            .build();
        spezialisierung.getModulSpezialisierungen().clear();
        spezialisierung.getModulSpezialisierungen().add(spModul);

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmoduleValidator validator = new KostenmoduleValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateType(0, "_moduleDetail")
            .validateTitle(0, "Kosten & Gebühren")
            .validateType(1, "_moduleDetail")
            .validateTitle(1, "Kosten & Gebühren")
            // Online bleibt unverändert
            .validateText(0, "Beschreibung Kostenmodul: fix Kosten: 40")
            .validateDetailsCount(0, 1)
            .validateDetailsTitle(0, 0, "Abgabe: 40,00 Euro")
            // Offline wird ergänzt
            .validateText(1, "Beschreibung Kostenmodul: fix Kosten: 40Beschreibung Kostenmodul: fix Kosten: 80")
            .validateDetailsCount(1, 2)
            .validateDetailsTitle(1, 0, "Abgabe: 40,00 Euro")
            .validateDetailsTitle(1, 1, "Abgabe: 80,00 Euro");
    }

    @Test
    void kostenOnOffErsetzenOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup und Einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).od(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Kosten fixKosten40E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(40)).currencyCode("EUR").build())
            .build();
        fixKosten40E.setCodeTyp("001");
        Kostenmodul kostenmodelOnOff = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE_OFFLINE)
            .beschreibung("Kostenmodul - Beschreibung: fix Kosten 40")
            .kosten(List.of(fixKosten40E))
            .build();
        leistung.setKostenmodule(List.of(kostenmodelOnOff));

        // Spezialisierung
        Kosten fixKosten80E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(80)).currencyCode("EUR").build())
            .build();
        fixKosten80E.setCodeTyp("001");
        ModulSpezialisierung modulSpezialisierung = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Kostenmodul.builder()
                    .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                    .beschreibung("Kostenmodul - Beschreibung: fix Kosten 80")
                    .kosten(List.of(fixKosten80E))
                    .build()
            )
            .build();
        file.getSpezialisierungen().getFirst().setModulSpezialisierungen(List.of(modulSpezialisierung));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        //Validieren
        KostenmoduleValidator validator = new KostenmoduleValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateType(0, "_moduleDetail")
            .validateTitle(0, "Kosten & Gebühren")
            .validateType(1, "_moduleDetail")
            .validateTitle(1, "Kosten & Gebühren")
            .validateText(0, "Kostenmodul - Beschreibung: fix Kosten 40")
            .validateText(1, "Kostenmodul - Beschreibung: fix Kosten 80")
            .validateDetailsCount(0, 1)
            .validateDetailsCount(1, 1)
            .validateDetailsTitle(0, 0, "Abgabe: 40,00 Euro")
            .validateDetailsTitle(1, 0, "Abgabe: 80,00 Euro");
    }

    @Test
    void kostenOnErsetzenOn(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup und Einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).od(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.getKostenmodule().clear();

        Kosten kostenFix20E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(20)).currencyCode("EUR").build())
            .build();
        kostenFix20E.setCodeTyp("001");
        Kostenmodul kostenmodulOn = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .beschreibung("Kostenmodul - Beschreibung: fix Kosten 20")
            .kosten(List.of(kostenFix20E))
            .build();
        leistung.getKostenmodule().add(kostenmodulOn);

        //Spezialisierung
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.getModulSpezialisierungen().clear();
        Kosten kostenFix10E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(10)).currencyCode("EUR").build())
            .build();
        kostenFix10E.setCodeTyp("001");
        Kostenmodul kostenSpezOn = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .beschreibung("Kostenmodul - Beschreibung: fix Kosten 10")
            .kosten(List.of(kostenFix10E))
            .build();
        ModulSpezialisierung kostenErsetzenOn = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(kostenSpezOn)
            .build();
        spezialisierung.getModulSpezialisierungen().add(kostenErsetzenOn);

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validieren
        // Kostenmodul mit selben ZK wird ersetzt
        KostenmodulValidator validator = new KostenmodulValidator(response, true);
        validator
            .validateCount(1)
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE.name())
            .validateType("_moduleDetail")
            .validateTitle("Kosten & Gebühren")
            .validateText("Kostenmodul - Beschreibung: fix Kosten 10")
            .validateDetailsTitle(0, "Abgabe: 10,00 Euro");
    }

    @Test
    void kostenOnErgaenzenOn(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup und Einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).od(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.getKostenmodule().clear();

        Kosten kostenFix20E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(20)).currencyCode("EUR").build())
            .build();
        kostenFix20E.setCodeTyp("001");
        Kostenmodul kostenmodulOn = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .beschreibung("Beschreibung: fix Kosten 20")
            .kosten(List.of(kostenFix20E))
            .build();
        leistung.getKostenmodule().add(kostenmodulOn);

        // Spezialisierung
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        spezialisierung.getModulSpezialisierungen().clear();
        Kosten kostenFix10E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(10)).currencyCode("EUR").build())
            .build();
        kostenFix10E.setCodeTyp("001");
        Kostenmodul kostenSpezOn = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .beschreibung("Beschreibung: fix Kosten 10")
            .kosten(List.of(kostenFix10E))
            .build();
        ModulSpezialisierung modulSpezOn = ModulSpezialisierung.builder().ersetzung(false).modul(kostenSpezOn).build();
        spezialisierung.getModulSpezialisierungen().add(modulSpezOn);

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmodulValidator validator = new KostenmodulValidator(response, true);
        validator
            .validateCount(1)
            .validateAccessChannel(Modul.ZugangskanalCode.ONLINE.name())
            .validateType("_moduleDetail")
            .validateTitle("Kosten & Gebühren")
            .validateText("Beschreibung: fix Kosten 20Beschreibung: fix Kosten 10")
            .validateDetailsTitle(0, "Abgabe: 20,00 Euro")
            .validateDetailsTitle(1, "Abgabe: 10,00 Euro");
    }

    @Test
    void kostenOnErsetzenOnUndOff(TestInfo testInfo) {
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // XZuFi-Setup und Einspielen
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).od(1).sp(1).getMin();

        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        leistung.getKostenmodule().clear();

        Kosten kostenFix20E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(20)).currencyCode("EUR").build())
            .build();
        kostenFix20E.setCodeTyp("001");
        Kostenmodul kostenmodulOn = Kostenmodul.builder()
            .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
            .beschreibung("Beschreibung: fix Kosten 20E")
            .kosten(List.of(kostenFix20E))
            .build();
        leistung.getKostenmodule().add(kostenmodulOn);

        // Spezialisierung
        Spezialisierung spezialisierung = file.getSpezialisierungen().getFirst();
        Kosten kostenFix10E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(10)).currencyCode("EUR").build())
            .build();
        kostenFix10E.setCodeTyp("001");
        Kosten kostenFix50E = KostenFix.builder()
            .betrag(Amount.builder().value(BigDecimal.valueOf(50)).currencyCode("EUR").build())
            .build();
        kostenFix50E.setCodeTyp("001");
        ModulSpezialisierung modulSpezOn10 = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Kostenmodul.builder()
                    .zugangskanalCode(Modul.ZugangskanalCode.ONLINE)
                    .beschreibung("Beschreibung: fix Kosten 10")
                    .kosten(List.of(kostenFix10E))
                    .build()
            )
            .build();
        ModulSpezialisierung modulSpezOn50 = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(
                Kostenmodul.builder()
                    .zugangskanalCode(Modul.ZugangskanalCode.OFFLINE)
                    .beschreibung("Beschreibung: fix Kosten 50")
                    .kosten(List.of(kostenFix50E))
                    .build()
            )
            .build();
        spezialisierung.setModulSpezialisierungen(List.of(modulSpezOn10, modulSpezOn50));

        ImportUtil.pushAndWait23(file);
        String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV6(ars, leistung.getFullId());

        // Validierung
        KostenmoduleValidator validator = new KostenmoduleValidator(response, true);
        validator
            .validateCount(2)
            .validateAccessChannel(0, Modul.ZugangskanalCode.ONLINE.name())
            .validateAccessChannel(1, Modul.ZugangskanalCode.OFFLINE.name())
            .validateType(0, "_moduleDetail")
            .validateTitle(0, "Kosten & Gebühren")
            .validateType(1, "_moduleDetail")
            .validateTitle(1, "Kosten & Gebühren")
            .validateText(0, "Beschreibung: fix Kosten 10")
            .validateText(1, "Beschreibung: fix Kosten 50")
            .validateDetailsTitle(0, 0, "Abgabe: 10,00 Euro")
            .validateDetailsTitle(1, 0, "Abgabe: 50,00 Euro");
    }
}
