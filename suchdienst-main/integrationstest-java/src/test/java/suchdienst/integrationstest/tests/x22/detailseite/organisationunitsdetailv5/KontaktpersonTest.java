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
package suchdienst.integrationstest.tests.x22.detailseite.organisationunitsdetailv5;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsKontaktpersonCreator;
import de.dataport.pvog.testdatengenerator.model.Erreichbarkeit;
import de.dataport.pvog.testdatengenerator.model.Herausgeber;
import de.dataport.pvog.testdatengenerator.model.Kontaktperson;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.EmployeeValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
@SuppressWarnings("java:S1192")
class KontaktpersonTest {

    @Test
    void kpDaten(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);
        String ars1 = bundeslandConfig.getArss().getFirst();

        String id = "KP_" + testcasename;

        // Daten erzeuegn
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getKontaktpersonen().clear();
        organisationseinheit.getKontaktpersonen().add(
            Kontaktperson.builder()
                .id(id)
                .vorname("Name")
                .familienname("Familie")
                .raum("001")
                .erreichbarkeiten(
                    List.of(
                        Erreichbarkeit.builder().code("01").kennung("kontaktperson@person.de").build(),
                        Erreichbarkeit.builder().code("02").kennung("04913374201").build()
                    )
                )
                .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                .herausgeber(
                    Herausgeber.builder()
                        .beschreibung("KPHerausgeberBeschreibung")
                        .bezeichnung("KPHerausgeberBezeichnung")
                        .kurzbezeichnung("KPHerausgeberKurzbezeichnung")
                        .build()
                )
                .build()
        );
        Zustaendigkeit zustaendigkeit = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            id
        );
        zustaendigkeit.getArss().clear();
        zustaendigkeit.getArss().add(ars1);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit);

        // Daten einspielen und vom SuD abrufen
        ImportUtil.pushAndWait(file);
        Response response = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars1,
            leistung.getFullId()
        );

        // Validieren
        EmployeeValidator validator = new EmployeeValidator(response);
        validator
            .validateCount(1)
            .validateName(0, "Name")
            .validateLastname(0, "Familie")
            .validateRoom(0, "001")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, ars1)
            .validateLocationUrlCount(0, 0)
            .validateLocationInformationCount(0, 0)
            .validateLocationAddressCount(0, 0)
            .validateLocationCommunicationCount(0, 2)
            .validateLocationCommunicationCode(0, 0, "01")
            .validateLocationCommunicationName(0, 0, "E-Mail")
            .validateLocationCommunicationValue(0, 0, "kontaktperson@person.de")
            .validateLocationCommunicationCode(0, 1, "02")
            .validateLocationCommunicationName(0, 1, "Telefon Festnetz")
            .validateLocationCommunicationValue(0, 1, "04913374201")
            .validateHerausgeberBezeichnung(0, "KPHerausgeberBezeichnung")
            .validateHerausgeberKurzbezeichnung(0, "KPHerausgeberKurzbezeichnung")
            .validateHerausgeberBeschreibung(0, "KPHerausgeberBeschreibung");
    }

    /**
     * Testet, dass die gelieferten Kontaktpersonen gemäß der Ars-Hierarchie reduziert werden.
     * Dazu werden Kontaktpersonen mit Zuständigkeiten auf allen 5 Ars-Ebenen zur OE hinzugefügt,
     * dann über die API Anfragen auf Gemeinden gestellt, die in allen Ebenen oder nur in Teilen der Ebenen liegen.
     * Es wird geprüft, ob die Kontaktpersonen auf diejenigen reduziert sind, deren Ebene zur Anfrage-Gemeinde passt.
     */
    @Test
    void arsFilterHerachie(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        String kpId1 = "KP_" + testcasename + "_01";
        String kpId2 = "KP_" + testcasename + "_02";
        String kpId3 = "KP_" + testcasename + "_03";
        String kpId4 = "KP_" + testcasename + "_04";
        String kpId5 = "KP_" + testcasename + "_05";

        // Diese GemeindeArs treten im Ars der VL in ES auf, und mit diesen Ars suchen wir auch über die API:
        // Ars z.b.in https://www.destatis.de/DE/Themen/Laender-Regionen/Regionales/Gemeindeverzeichnis/_inhalt.html#101366
        String arsGemeinde1 = "093775347141"; // Erwartung: alle KP werden gefunden
        String arsGemeinde2 = "093775347145"; // Erwartung: alle KP bis arsKpZustaendigkeitGemeinde werden gefunden
        String arsGemeinde3 = "093775348128"; // Erwartung: alle KP bis arsKpZustaendigkeitGemeindeverband werden gefunden
        String arsGemeinde4 = "093755338191"; // Erwartung: alle KP bis arsKpZustaendigkeitBezirk werden gefunden
        String arsGemeinde5 = "092750111111"; // Erwartung: alle KP bis arsBayern werden gefunden

        // Diese GemeindeArs nehmen wir für die ZS der KP:
        String arsKpZustaendigkeitGemeinde = "093775347141"; // Mitterteich Stadt
        String arsKpZustaendigkeitGemeindeverband = "093775347000"; // Mitterteich
        String arsKpZustaendigkeitKreis = "093770000000"; // Tirschenreuth
        String arsKpZustaendigkeitBezirk = "093000000000"; // Oberpfalz
        String arsBayern = "090000000000";

        BundeslandConfig bundeslandConfig = new BundeslandConfig("L100042", List.of(arsBayern));

        // Daten erzeugen, es entsteht eine VL mit allen Gemeinde-Ars aus Bayern
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();

        organisationseinheit.getKontaktpersonen().clear();
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId1)
                    .vorname("Name1")
                    .familienname("Familie1")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit1 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId1
        );
        zustaendigkeit1.getArss().clear();
        zustaendigkeit1.getArss().add(arsKpZustaendigkeitGemeinde);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit1);
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId2)
                    .vorname("Name2")
                    .familienname("Familie2")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit2 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId2
        );
        zustaendigkeit2.getArss().clear();
        zustaendigkeit2.getArss().add(arsKpZustaendigkeitGemeindeverband);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit2);
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId3)
                    .vorname("Name3")
                    .familienname("Familie3")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit3 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId3
        );
        zustaendigkeit3.getArss().clear();
        zustaendigkeit3.getArss().add(arsKpZustaendigkeitKreis);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit3);
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId4)
                    .vorname("Name4")
                    .familienname("Familie4")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit4 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId4
        );
        zustaendigkeit4.getArss().clear();
        zustaendigkeit4.getArss().add(arsKpZustaendigkeitBezirk);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit4);
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId5)
                    .vorname("Name5")
                    .familienname("Familie5")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit5 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId5
        );
        zustaendigkeit5.getArss().clear();
        zustaendigkeit5.getArss().add(arsBayern);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit5);

        // Daten einspielen
        ImportUtil.pushAndWait(file);

        // SuD mit der Ars zur Kontaktperson abfragen und prüfen ob alle notwendigen Kontaktpersonen enthalten sind
        Response response1 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde1,
            leistung.getFullId()
        );
        EmployeeValidator validator1 = new EmployeeValidator(response1);
        validator1
            .validateCount(5)
            .validateName(0, "Name1")
            .validateLastname(0, "Familie1")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, arsKpZustaendigkeitGemeinde)
            .validateName(1, "Name2")
            .validateLastname(1, "Familie2")
            .validateRelationLbId(1, 0, leistung.getFullId())
            .validateRelationArs(1, 0, arsKpZustaendigkeitGemeindeverband)
            .validateName(2, "Name3")
            .validateLastname(2, "Familie3")
            .validateRelationLbId(2, 0, leistung.getFullId())
            .validateRelationArs(2, 0, arsKpZustaendigkeitKreis)
            .validateName(3, "Name4")
            .validateLastname(3, "Familie4")
            .validateRelationLbId(3, 0, leistung.getFullId())
            .validateRelationArs(3, 0, arsKpZustaendigkeitBezirk)
            .validateName(4, "Name5")
            .validateLastname(4, "Familie5")
            .validateRelationLbId(4, 0, leistung.getFullId())
            .validateRelationArs(4, 0, arsBayern);

        Response response2 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde2,
            leistung.getFullId()
        );
        EmployeeValidator validator2 = new EmployeeValidator(response2);
        validator2
            .validateCount(4)
            .validateName(0, "Name2")
            .validateLastname(0, "Familie2")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, arsKpZustaendigkeitGemeindeverband)
            .validateName(1, "Name3")
            .validateLastname(1, "Familie3")
            .validateRelationLbId(1, 0, leistung.getFullId())
            .validateRelationArs(1, 0, arsKpZustaendigkeitKreis)
            .validateName(2, "Name4")
            .validateLastname(2, "Familie4")
            .validateRelationLbId(2, 0, leistung.getFullId())
            .validateRelationArs(2, 0, arsKpZustaendigkeitBezirk)
            .validateName(3, "Name5")
            .validateLastname(3, "Familie5")
            .validateRelationLbId(3, 0, leistung.getFullId())
            .validateRelationArs(3, 0, arsBayern);

        Response response3 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde3,
            leistung.getFullId()
        );
        EmployeeValidator validator3 = new EmployeeValidator(response3);
        validator3
            .validateCount(3)
            .validateName(0, "Name3")
            .validateLastname(0, "Familie3")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, arsKpZustaendigkeitKreis)
            .validateName(1, "Name4")
            .validateLastname(1, "Familie4")
            .validateRelationLbId(1, 0, leistung.getFullId())
            .validateRelationArs(1, 0, arsKpZustaendigkeitBezirk)
            .validateName(2, "Name5")
            .validateLastname(2, "Familie5")
            .validateRelationLbId(2, 0, leistung.getFullId())
            .validateRelationArs(2, 0, arsBayern);

        Response response4 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde4,
            leistung.getFullId()
        );
        EmployeeValidator validator4 = new EmployeeValidator(response4);
        validator4
            .validateCount(2)
            .validateName(0, "Name4")
            .validateLastname(0, "Familie4")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, arsKpZustaendigkeitBezirk)
            .validateName(1, "Name5")
            .validateLastname(1, "Familie5")
            .validateRelationLbId(1, 0, leistung.getFullId())
            .validateRelationArs(1, 0, arsBayern);

        Response response5 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde5,
            leistung.getFullId()
        );
        EmployeeValidator validator5 = new EmployeeValidator(response5);
        validator5
            .validateCount(1)
            .validateName(0, "Name5")
            .validateLastname(0, "Familie5")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, arsBayern);
    }

    @Test
    void manyKontaktpersonen(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        BundeslandConfig bundeslandConfig = new BundeslandConfig(Bundesland.BY);

        String kpId1 = "KP_" + testcasename + "_01";
        String kpId2 = "KP_" + testcasename + "_02";

        // Daten erzeuegn
        String ars = bundeslandConfig.getArss().getFirst();
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getKontaktpersonen().clear();
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId1)
                    .vorname("Name1")
                    .familienname("Familie1")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit1 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId1
        );
        zustaendigkeit1.getArss().clear();
        zustaendigkeit1.getArss().add(ars);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit1);
        organisationseinheit
            .getKontaktpersonen()
            .add(
                Kontaktperson.builder()
                    .id(kpId2)
                    .vorname("Name2")
                    .familienname("Familie2")
                    .schemeAgencyId(bundeslandConfig.getSchemeAgencyId())
                    .build()
            );
        Zustaendigkeit zustaendigkeit2 = ZsKontaktpersonCreator.getInstance(bundeslandConfig).getMin(
            leistung.getId(),
            kpId2
        );
        zustaendigkeit2.getArss().clear();
        zustaendigkeit2.getArss().add(ars);
        file.getZustaendigkeitenKontaktperson().add(zustaendigkeit2);

        // Daten einspielen
        ImportUtil.pushAndWait(file);

        // SuD mit der Ars zur Kontaktperson abfragen und prüfen ob alle notwendigen Kontaktpersonen enthalten sind
        Response response1 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            ars,
            leistung.getFullId()
        );
        EmployeeValidator validator1 = new EmployeeValidator(response1);
        validator1
            .validateCount(2)
            .validateName(0, "Name1")
            .validateLastname(0, "Familie1")
            .validateRelationLbId(0, 0, leistung.getFullId())
            .validateRelationArs(0, 0, ars)
            .validateName(1, "Name2")
            .validateLastname(1, "Familie2")
            .validateRelationLbId(1, 0, leistung.getFullId())
            .validateRelationArs(1, 0, ars);
    }
}
