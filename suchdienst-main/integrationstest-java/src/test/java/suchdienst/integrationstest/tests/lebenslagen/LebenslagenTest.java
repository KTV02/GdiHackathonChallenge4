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
package suchdienst.integrationstest.tests.lebenslagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static suchdienst.integrationstest.connectors.ServiceDescriptionConnector.getPersonalMattersV1;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig.Bundesland;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.OdCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOnlinedienstCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.personalmattersv1.PersonalMattersResultListValidator;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Testziel ist die Prüfung, dass bei der Suche nach Leistungen
 * der korrekte Lebenslagenbaum ausgegeben wird.
 * Initial wird ein Leistungskatalog mit 4 Leistungen geladen
 * LeikaKey 99038009223001 - Lebenslage 2030700
 * LeikaKey 99001001000000 - Lebenslage 1050200, 1170100
 * LeikaKey 99038009223002 - Lebenslage 1180200
 * LeikaKey 99157002014000 - Lebenslage 2030400, 2160200, 1040100, 1040400
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LebenslagenTest {

    private String testcaseName;

    @BeforeAll
    static void importLeistungskatalog() {
        KatalogImportUtil.resetLeistungskatalog("leistungskatalogOK.xml");
    }

    /**
     * Test mit einer Leika aus dem Leistungskatalog - kein explzites setzen der Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void ohneKlassifizierung(TestInfo testInfo) {
        // ohne explizit gesetzte Klassifizierung
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String thirdLevel = "1180200";
        String unknownLebenslage = "0815";
        XzufiFile ohneKlassifizierung = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        ohneKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223002");
        ohneKlassifizierung.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of());

        String id = ohneKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(ohneKlassifizierung);

        String ars = ohneKlassifizierung.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = ohneKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(thirdLevel)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Rente", thirdLevel);
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            List.of(thirdLevel),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            List.of(unknownLebenslage),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test mit einer Leika aus dem Leistungskatalog - kein explzites setzen der Klassifizierung
     * Abfrage auf englisch
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void ohneKlassifizierungEN(TestInfo testInfo) {
        // Abfrage erfolgt für Sprache EN
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String thirdLevel = "1180200";
        String unknownLebenslage = "0815";
        XzufiFile ohneKlassifizierung = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        ohneKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223002");
        String id = ohneKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(ohneKlassifizierung);

        String ars = ohneKlassifizierung.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = ohneKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            0,
            10,
            null,
            true,
            Constants.LANGUAGE_EN
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(thirdLevel)
            .personalMattersFirstLevelContainsName("Life situations for citizens")
            .personalMattersSecondLevelContainsNameByCode("Retirement and Retirement Benefits", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Pension", thirdLevel);
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            0,
            10,
            List.of(thirdLevel),
            false,
            Constants.LANGUAGE_EN
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            0,
            10,
            List.of(unknownLebenslage),
            false,
            Constants.LANGUAGE_EN
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test mit einer Leika aus dem Leistungskatalog - trotzdem explizites Setzen der passenden Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitPassenderKlassifizierung(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String thirdLevel = "1180200";
        XzufiFile mitKlassifizierung = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        mitKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223002");
        String klassifizierung = "1180200";
        mitKlassifizierung.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of(klassifizierung));

        String id = mitKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(mitKlassifizierung);

        String ars = mitKlassifizierung.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = mitKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(thirdLevel)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Rente", thirdLevel);
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            List.of(thirdLevel),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);
    }

    /**
     * Test mit einer Leika aus dem Leistungskatalog - explzites setzen mit falscher Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitFalscherKlassifizierung(TestInfo testInfo) {
        // Leistungskatalog Lebenslage sollte falsche Klassifizierung überschreiben
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String thirdLevel = "1180200";
        String falscheKlassifizierung = "1180815";
        XzufiFile mitKlassifizierung = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        mitKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223002");
        mitKlassifizierung.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of(falscheKlassifizierung));

        String id = mitKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(mitKlassifizierung);

        String ars = mitKlassifizierung.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = mitKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(thirdLevel)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Rente", thirdLevel);
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),
            List.of(thirdLevel),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of(falscheKlassifizierung),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test mit einer Leika, die nicht aus Leistungskatalog kommt- kein explzites setzen der Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitUnbekannterLeikaOhneKlassifizierung(TestInfo testInfo) {
        // Leistungskatalog Lebenslage sollte falsche Klassifizierung überschreiben
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile unbekannteLeikaOhneKlassifizierung = XzufiFileCreator.newInstance()
            .name(testcaseName)
            .lb(1)
            .oe(1)
            .getMin();
        unbekannteLeikaOhneKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223099");

        String id = unbekannteLeikaOhneKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(unbekannteLeikaOhneKlassifizierung);

        String ars = unbekannteLeikaOhneKlassifizierung
            .getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .getArss()
            .getFirst();
        String queryString = unbekannteLeikaOhneKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator.personalMattersIsEmpty();
        validator.serviceDescriptionsContainsId(id);
    }

    /**
     * Test mit einer Leika, die nicht aus Leistungskatalog kommt- explizites setzen einer unbekannten Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitUnbekannterLeikaUnbekannterKlassifizierung(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String falscheKlassifizierung = "1180815";
        // Eigene, in dieser Testklasse sonst nicht verwendete LeikaId (andere Tests nutzen bewusst
        // "99038009223099" mit einer bekannten Klassifizierung). Da dieser Test über personalMattersIsEmpty()
        // eine leere Facette erwartet, darf die LeikaId nicht mit einer anderen Leistung geteilt werden,
        // die eine bekannte Klassifizierung besitzt.
        String leikaId = "99038009223098";
        XzufiFile unbekannteLeikaBekannteKlassifizierung = XzufiFileCreator.newInstance()
            .name(testcaseName)
            .lb(1)
            .oe(1)
            .getMin();
        unbekannteLeikaBekannteKlassifizierung.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa(leikaId);
        unbekannteLeikaBekannteKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .setKlassifizierung(List.of(falscheKlassifizierung));

        String id = unbekannteLeikaBekannteKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(unbekannteLeikaBekannteKlassifizierung);

        String ars = unbekannteLeikaBekannteKlassifizierung
            .getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .getArss()
            .getFirst();
        // Es wird gezielt über die (eindeutige) LeikaId gesucht (statt über den generischen
        // Textmodul-Inhalt), da die Beta-Such-API immer mit Fuzzy-Suche arbeitet. Ein Freitext-Suchbegriff
        // könnte sonst durch Tippfehlertoleranz auch andere, ähnlich benannte Testfälle in dieser Klasse
        // treffen.
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(leikaId),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator.personalMattersIsEmpty();
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(leikaId),

            List.of(falscheKlassifizierung),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test mit einer Leika, die nicht aus Leistungskatalog kommt- explizites setzen einer bekannten Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitUnbekannterLeikaBekannterKlassifizierung(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String bekannteKlassifizierung = "1180200";
        XzufiFile unbekannteLeikaUnbekannteKlassifizierung = XzufiFileCreator.newInstance()
            .name(testcaseName)
            .lb(1)
            .oe(1)
            .getMin();
        unbekannteLeikaUnbekannteKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .setReferenzLeiKa("99038009223099");
        unbekannteLeikaUnbekannteKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .setKlassifizierung(List.of(bekannteKlassifizierung));

        String id = unbekannteLeikaUnbekannteKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(unbekannteLeikaUnbekannteKlassifizierung);

        String ars = unbekannteLeikaUnbekannteKlassifizierung
            .getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .getArss()
            .getFirst();
        String queryString = unbekannteLeikaUnbekannteKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(bekannteKlassifizierung)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Rente", bekannteKlassifizierung);
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of(bekannteKlassifizierung),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);
    }

    /**
     * Test mit einer Leika, die nicht aus Leistungskatalog kommt- explizites setzen einer secondLevel Klassifizierung
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void mitUnbekannterLeikaSecondLevelKlassifizierung(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String bekannteKlassifizierung = "1180200";
        XzufiFile unbekannteLeikaSecondLevelKlassifizierung = XzufiFileCreator.newInstance()
            .name(testcaseName)
            .lb(1)
            .oe(1)
            .getMin();
        unbekannteLeikaSecondLevelKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .setReferenzLeiKa("99038009223099");
        unbekannteLeikaSecondLevelKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .setKlassifizierung(List.of(secondLevel));

        String id = unbekannteLeikaSecondLevelKlassifizierung.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(unbekannteLeikaSecondLevelKlassifizierung);

        String ars = unbekannteLeikaSecondLevelKlassifizierung
            .getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .getArss()
            .getFirst();
        String queryString = unbekannteLeikaSecondLevelKlassifizierung
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelIsEmpty(secondLevel);
        validator.serviceDescriptionsContainsId(id);

        // kein Ergebnis für untergeordnete Lebenslage
        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of(secondLevel),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        // Abfrage über untergeordnete Lage führt zu keinem Treffer
        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of(bekannteKlassifizierung),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test mit 4 Leistungen - 3 Leistungen mit Leikakeys aus Katalog - 1 Leistung mit eigener Klassifizierung
     */
    @Test
    void mehrereLeistungen(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String synonym = TestUtil.generateRandomWord() + "synonym";
        String firstLeika = "99038009223002";
        // mit lage 1180200
        String secondLeika = "99001001000000";
        // mit lage 1050200 und 1170100
        String thirdLeika = "99038009223001";
        // mit lage 2030700

        // nicht im Katalog enthalten
        String fourthLeika = "99001001001234";
        String fourthLage = "1170200";

        XzufiFile mehrereLeistungen = XzufiFileCreator.newInstance().name(testcaseName).lb(4).oe(1).getMin();
        Leistungsbeschreibung firstLB = mehrereLeistungen.getLeistungsbeschreibungen().getFirst();
        String firstId = firstLB.getFullId();
        firstLB.setReferenzLeiKa(firstLeika);
        firstLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        Leistungsbeschreibung secondLB = mehrereLeistungen.getLeistungsbeschreibungen().get(1);
        String secondId = secondLB.getFullId();
        secondLB.setReferenzLeiKa(secondLeika);
        secondLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        Leistungsbeschreibung thirdLB = mehrereLeistungen.getLeistungsbeschreibungen().get(2);
        String thirdId = thirdLB.getFullId();
        thirdLB.setReferenzLeiKa(thirdLeika);
        thirdLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        Leistungsbeschreibung fourthLB = mehrereLeistungen.getLeistungsbeschreibungen().get(3);
        String fourthId = fourthLB.getFullId();
        fourthLB.setReferenzLeiKa(fourthLeika);
        fourthLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        fourthLB.setKlassifizierung(List.of(fourthLage));

        String ars = mehrereLeistungen.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        ImportUtil.pushAndWait23(mehrereLeistungen);

        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersFirstLevelCountByCode(3, "1000000")
            .personalMattersFirstLevelCountByCode(1, "2000000")
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersSecondLevelContainsCode("1180000")
            .personalMattersSecondLevelCountByCode(1, "1180000")
            .personalMattersSecondLevelContainsCode("1170000")
            .personalMattersSecondLevelCountByCode(2, "1170000")
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersSecondLevelCountByCode(1, "2030000")
            .personalMattersThirdLevelContainsCode("1180200")
            .personalMattersThirdLevelCountByCode(1, "1180200")
            .personalMattersThirdLevelContainsCode("1170100")
            .personalMattersThirdLevelCountByCode(1, "1170100")
            .personalMattersThirdLevelContainsCode("1050200")
            .personalMattersThirdLevelCountByCode(1, "1050200")
            .personalMattersThirdLevelContainsCode("1170200")
            .personalMattersThirdLevelCountByCode(1, "1170200")
            .personalMattersThirdLevelContainsCode("2030700")
            .personalMattersThirdLevelCountByCode(1, "2030700")
            .serviceDescriptionsContainsId(firstId)
            .serviceDescriptionsContainsId(secondId)
            .serviceDescriptionsContainsId(thirdId)
            .serviceDescriptionsContainsId(fourthId);
        // PV-8448: Test der Schnittstelle /v1/personalmatters/[ars]
        final var pmResponse1 = getPersonalMattersV1(ars, Constants.LANGUAGE_DE);
        final var personalMattersValidator = new PersonalMattersResultListValidator(pmResponse1);
        // Beschreibungstext (Kommentar) zu secondLevel Lebenslagen - DE
        String expectedComment1050000 =
            "Sie suchen eine neue Wohnung oder sind gerade umgezogen? Oder möchten Sie ein Haus bauen? Die eigenen oder gemieteten vier Wände haben eine besondere Bedeutung für uns. In diesem Bereich finden Sie Informationen, Kontaktdaten und Antworten auf Fragen rund um die Themen Bauen, Immobilienkauf und Wohnen.";
        String expectedComment1170000 =
            "Umwelt ist alles was uns Menschen umgibt und auf uns einwirkt. Wie schützen wir unsere Natur sowie Tier- und Pflanzenarten, und nicht zu vergessen: das Klima?";
        String expectedComment1180000 =
            "Finanzielle Sicherheit und Versorgung im Alter, bei Krankheit oder Behinderung. Welche Angebote, Entschädigungen, Förderungen und Unterstützungen gibt es?";
        String expectedComment2030000 =
            "Sie leiten einen Betrieb mit Beschäftigten? Wie sieht es mit der Sozialversicherung aus? Wie bezahlt man die Lohnsteuer und welche Meldepflichten gibt es? Damit Sie als Arbeitgeberin oder Arbeitgeber den Überblick über Ihre Rechte und Pflichten behalten, finden Sie hier umfangreiche Informationen über arbeitsrechtliche Aspekte.";
        // Beschreibungstext (Kommentar) zu thirdLevel Lebenslagen - DE
        String expectedComment1050200 =
            "Egal ob Sie zur Miete wohnen, eine Wohnung oder ein eigenes Haus besitzen oder einen Umzug planen, hier finden Sie Informationen rund ums Thema Wohnen und Umzug.";
        String expectedComment1170100 =
            "Natur- und Artenschutz sollen helfen, gefährdete Naturgebiete sowie Tier- und Pflanzenarten zu erhalten. Energie und Rohstoffe sind begrenzte Güter und sollten deshalb sparsam verwendet werden. Sie können auch zum Klimaschutz beitragen, z. B. durch Reduzierung des Stromverbrauchs, die Art der Heizungsanlage oder eine ressourcenschonende Bauweise.";
        String expectedComment1170200 =
            "Der Zustand von Böden, Gewässer und Wasser beeinflusst viele weitere Bereiche in der Natur, z. B. das Klima, die Lebensräume für Tier- und Pflanzenarten oder den Stoffkreislauf.";
        String expectedComment1180200 =
            "In Rente zu gehen, bedeutet viel mehr, als nur den Beruf hinter sich zu lassen: etwa ein Viertel unseres Lebens liegt beim Eintritt in den Ruhestand heute noch vor uns. Darum ist das Ende des Berufslebens zugleich ein Neubeginn. Hier finden Sie Leistungen und Services, die Sie beim Übergang in die neue Lebensphase unterstützen.";
        String expectedComment2030700 =
            "Neben den gewöhnlichen Arbeitszeitmodellen gibt es einige Sonderregelungen. Die Altersteilzeit oder das Wintergeld für Beschäftigte in witterungsabhängigen Branchen sind Beispiele hierfür. Informationen und Services zu diesen Regelungen haben wir hier für Sie zusammengefasst.";

        personalMattersValidator
            .firstLevelExactly("1000000" /*Bürger*/, "2000000" /*Unternehmen*/)
            .secondLevelExactly(
                "1000000",
                "1050000" /*Bauen und Wohnen*/,
                "1170000" /*Umwelt und Klima*/,
                "1180000"
                /*Altersvorsorge und Ruhestand*/
            )
            .secondLevelComments("1000000", expectedComment1050000, expectedComment1170000, expectedComment1180000)
            .secondLevelExactly("2000000", "2030000")
            .secondLevelComments("2000000", expectedComment2030000)
            .thirdLevelExactly("1000000", "1050000", "1050200" /*Wohnen und Umzug*/)
            .thirdLevelComments("1000000", "1050000", expectedComment1050200)
            .thirdLevelExactly(
                "1000000",
                "1170000",
                "1170100" /*Klima, Natur und Arten*/,
                "1170200" /*Wasser, Gewässer und Boden*/
            )
            .thirdLevelComments("1000000", "1170000", expectedComment1170100, expectedComment1170200)
            .thirdLevelExactly("1000000", "1180000", "1180200" /*Rente und Soziale Entschädigung*/)
            .thirdLevelComments("1000000", "1180000", expectedComment1180200)
            .thirdLevelExactly("2000000", "2030000", "2030700" /*Arbeitgeber sein*/)
            .thirdLevelComments("2000000", "2030000", expectedComment2030700);
        // Für manuelle Tests des Cache machen wir den letzten Aufruf nochmal
        final var pmResponse2 = getPersonalMattersV1(ars, Constants.LANGUAGE_DE); // aus cache
        final String body1 = pmResponse1.getBody().print();
        final String body2 = pmResponse2.getBody().print();
        assertEquals(body1, body2);

        //Abfrage mit LanguageCode EN - kein separater Test für EN - prüft, dass nicht die Cache Daten für DE gezogen werden
        final var pmResponseEN = getPersonalMattersV1(ars, Constants.LANGUAGE_EN); // aus cache
        final var personalMattersValidatorEN = new PersonalMattersResultListValidator(pmResponseEN);

        // Beschreibungstext (Kommentar) zu secondLevel Lebenslagen - EN
        String expectedComment1050000EN =
            "Are you looking for a new place to live or have you just moved? Or would you like to build a house? Your own home, whether it is rented or your property, is very important to us as people. This section provides information, contact details and answers to questions relating to the topics of building, buying property, and housing.";
        String expectedComment1170000EN =
            "The environment is everything that surrounds human beings and has an effect on us. How can we protect nature as well as plant and animal species, and not least the climate?";
        String expectedComment1180000EN =
            "Financial security and provision for old age, illness and disability. What services, compensation, support and assistance exist?";
        String expectedComment2030000EN =
            "Do you run a business with employees? What is the situation for providing social security? How can you pay payroll taxes and what must you inform the authorities of? Here you will find comprehensive information on labour law issues so that you can get an overview of your rights and obligations as an employer.";

        // Beschreibungstext (Kommentar) zu thirdLevel Lebenslagen - EN
        String expectedComment1050200EN =
            "Whether you are renting, you own a flat or house or you are planning to move, you can find information on housing and moving house here.";
        String expectedComment1170100EN =
            "Protection of natural habitats and biodiversity helps preserve threatened natural areas and wild species of plants and animals. Energy and raw materials are limited resources and should be used sparingly. You can also help fight climate change by reducing your energy consumption and by choosing a resource-conserving heating system or building style.";
        String expectedComment1170200EN =
            "The state of soil and water affects many other parts of nature such as the climate, the natural habitats of animal and plant species, and the material cycle.";
        String expectedComment1180200EN =
            "Retiring means much more than concluding your professional life; at the time of retirement, the average person still has about a quarter of their life to look forward to. That is why the end of your working life also represents a new start. Here you will find the services that can support you in your transition to a new phase of life.";
        String expectedComment2030700EN =
            "Along with the usual working hours, there are special regulations for certain situations. Examples include part-time work for older employees and winter pay for employees in weather-dependent industries. Here is an overview of information and services regarding these regulations.";

        personalMattersValidatorEN
            .firstLevelExactly(
                "1000000" /*Bürger*/,
                "2000000"
                /*Unternehmen*/
            )
            .secondLevelExactly(
                "1000000",
                "1050000" /*Bauen und Wohnen*/,
                "1170000" /*Umwelt und Klima*/,
                "1180000"
                /*Altersvorsorge und Ruhestand*/
            )
            .secondLevelComments(
                "1000000",
                expectedComment1050000EN,
                expectedComment1170000EN,
                expectedComment1180000EN
            )
            .secondLevelExactly("2000000", "2030000")
            .secondLevelComments("2000000", expectedComment2030000EN)
            .thirdLevelExactly(
                "1000000",
                "1050000",
                "1050200"
                /*Wohnen und Umzug*/
            )
            .thirdLevelComments("1000000", "1050000", expectedComment1050200EN)
            .thirdLevelExactly(
                "1000000",
                "1170000",
                "1170100" /*Klima, Natur und Arten*/,
                "1170200"
                /*Wasser, Gewässer und Boden*/
            )
            .thirdLevelComments("1000000", "1170000", expectedComment1170100EN, expectedComment1170200EN)
            .thirdLevelExactly(
                "1000000",
                "1180000",
                "1180200"
                /*Rente und Soziale Entschädigun*/
            )
            .thirdLevelComments("1000000", "1180000", expectedComment1180200EN)
            .thirdLevelExactly(
                "2000000",
                "2030000",
                "2030700"
                /*Arbeitgeber sein*/
            )
            .thirdLevelComments("2000000", "2030000", expectedComment2030700EN);
    }

    /**
     * Test mit 4 LeiKa`s, die nicht im Katalog vorhanden sind
     * Besonderheit eine Klassifizierung auf SecondLevel Ebene und eine Leistung ohne Klassifizierung
     */
    @Test
    void countTree(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Setup Testdaten
        BundeslandConfig bundeslandConfig = new BundeslandConfig(
            Bundesland.BY,
            "L100042",
            "090000000000",
            List.of("094610000000")
        );

        String synonym = TestUtil.generateRandomWord() + "synonym";
        String firstLeika = "99038009221111";
        String firstLage01 = "1170100";
        String firstLage02 = "1170200";

        String secondLeika = "99038009221112";
        String secondLage01 = "1170100";
        String secondLage02 = "2030400";

        // mit secondLevelLeika
        String thirdLeika = "99038009221113";
        String thirdLage01 = "1170000";
        String thirdLage02 = "2030000";

        // nicht im Katalog enthalten - keine Klassifizierung
        String fourthLeika = "99038009221114";

        XzufiFile countTree = XzufiFileCreator.newInstance(bundeslandConfig).name(testcaseName).lb(4).oe(1).getMin();
        Leistungsbeschreibung firstLB = countTree.getLeistungsbeschreibungen().getFirst();
        String firstId = firstLB.getFullId();
        firstLB.setReferenzLeiKa(firstLeika);
        firstLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        firstLB.setKlassifizierung(List.of(firstLage01, firstLage02));
        Leistungsbeschreibung secondLB = countTree.getLeistungsbeschreibungen().get(1);
        String secondId = secondLB.getFullId();
        secondLB.setReferenzLeiKa(secondLeika);
        secondLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        secondLB.setKlassifizierung(List.of(secondLage01, secondLage02));
        Leistungsbeschreibung thirdLB = countTree.getLeistungsbeschreibungen().get(2);
        String thirdId = thirdLB.getFullId();
        thirdLB.setReferenzLeiKa(thirdLeika);
        thirdLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        thirdLB.setKlassifizierung(List.of(thirdLage01, thirdLage02));

        Leistungsbeschreibung fourthLB = countTree.getLeistungsbeschreibungen().get(3);
        String fourthId = fourthLB.getFullId();
        fourthLB.setReferenzLeiKa(fourthLeika);
        fourthLB.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));

        String ars = countTree.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        ImportUtil.pushAndWait23(countTree);

        // Test
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersFirstLevelCountByCode(3, "1000000") //firstLB, secondLB, thirdLB
            .personalMattersFirstLevelCountByCode(2, "2000000") //secondLB, thirdLB
            .personalMattersSecondLevelContainsCode("1170000")
            .personalMattersSecondLevelCountByCode(3, "1170000") //firstLB, secondLB, thirdLB
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersSecondLevelCountByCode(2, "2030000") //secondLB, thirdLB
            .personalMattersThirdLevelContainsCode("1170100")
            .personalMattersThirdLevelCountByCode(2, "1170100") //firstLB, secondLB - OFFEN: eigentlich count 3 mit thirdLB
            .personalMattersThirdLevelContainsCode("1170200")
            .personalMattersThirdLevelCountByCode(1, "1170200") //firstLB - OFFEN: eigentlich count 2 mit thirdLB
            .personalMattersThirdLevelContainsCode("2030400")
            .personalMattersThirdLevelCountByCode(1, "2030400") //secondLB - OFFEN: eigentlich count 2 mit thirdLB
            .serviceDescriptionsContainsId(firstId)
            .serviceDescriptionsContainsId(secondId)
            .serviceDescriptionsContainsId(thirdId)
            .serviceDescriptionsContainsId(fourthId)
            .validateCount(4);

        //Abfrage englisch
        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),
            0,
            10,
            null,
            true,
            Constants.LANGUAGE_EN
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validator
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersFirstLevelCountByCode(3, "1000000") //firstLB, secondLB, thirdLB
            .personalMattersFirstLevelCountByCode(2, "2000000") //secondLB, thirdLB
            .personalMattersSecondLevelContainsCode("1170000")
            .personalMattersSecondLevelCountByCode(3, "1170000") //firstLB, secondLB, thirdLB
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersSecondLevelCountByCode(2, "2030000") //secondLB, thirdLB
            .personalMattersThirdLevelContainsCode("1170100")
            .personalMattersThirdLevelCountByCode(2, "1170100") //firstLB, secondLB - OFFEN: eigentlich count 3 mit thirdLB
            .personalMattersThirdLevelContainsCode("1170200")
            .personalMattersThirdLevelCountByCode(1, "1170200") //firstLB - OFFEN: eigentlich count 2 mit thirdLB
            .personalMattersThirdLevelContainsCode("2030400")
            .personalMattersThirdLevelCountByCode(1, "2030400") //secondLB - OFFEN: eigentlich count 2 mit thirdLB
            .serviceDescriptionsContainsId(firstId)
            .serviceDescriptionsContainsId(secondId)
            .serviceDescriptionsContainsId(thirdId)
            .serviceDescriptionsContainsId(fourthId)
            .validateCount(4);
    }

    /**
     * Test mit vielen Leistungen
     * Test mit Abfrage über mehrere Seiten
     */
    @Test
    void mehrereLeistungenPaging(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String synonym = TestUtil.generateRandomWord() + "synonym";
        String firstLeika = "99038009223002";
        // mit lage 1180200
        String secondLeika = "99001001000000";
        // mit lage 1050200 und 1170100
        String thirdLeika = "99038009223001";
        // mit lage 2030700

        //nicht im Katalog enthalten
        String fourthLeika = "99001001001234";
        String fourthLage = "1170200";

        XzufiFile firstFile = XzufiFileCreator.newInstance()
            .name(testcaseName + "FIRST")
            .lb(10)
            .oe(1)
            .getMin();
        for (Leistungsbeschreibung lb : firstFile.getLeistungsbeschreibungen()) {
            lb.setReferenzLeiKa(firstLeika);
            lb.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        }

        XzufiFile secondFile = XzufiFileCreator.newInstance()
            .name(testcaseName + "SECOND")
            .lb(10)
            .oe(1)
            .getMin();
        for (Leistungsbeschreibung lb : secondFile.getLeistungsbeschreibungen()) {
            lb.setReferenzLeiKa(secondLeika);
            lb.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        }

        XzufiFile thirdFile = XzufiFileCreator.newInstance()
            .name(testcaseName + "THIRD")
            .lb(10)
            .oe(1)
            .getMin();
        for (Leistungsbeschreibung lb : thirdFile.getLeistungsbeschreibungen()) {
            lb.setReferenzLeiKa(thirdLeika);
            lb.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
        }

        XzufiFile fourthFile = XzufiFileCreator.newInstance()
            .name(testcaseName + "FOURTH")
            .lb(10)
            .oe(1)
            .getMin();
        for (Leistungsbeschreibung lb : fourthFile.getLeistungsbeschreibungen()) {
            lb.setReferenzLeiKa(fourthLeika);
            lb.getSuchbegriffe().add(new Suchbegriff(synonym, synonym + "EN"));
            lb.setKlassifizierung(List.of(fourthLage));
        }

        String ars = firstFile.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

        ImportUtil.push(firstFile);
        ImportUtil.push(secondFile);
        ImportUtil.push(thirdFile);
        ImportUtil.pushAndWait23(fourthFile);

        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );

        validate(validator);

        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),
            1,
            10,
            null,
            true
        );

        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validate(validator);

        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),
            2,
            10,
            null,
            true
        );

        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validate(validator);

        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(synonym),
            4,
            10,
            null,
            true
        );

        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validate(validator);
    }

    /**
     * Erstellen einer LB mit Lebenslage 1180200, dann ein Upate mit einer neuen Zuständigkeit einspielen.
     * Prüfen, ob die Lebenslagen wie erwartet geliefert werden und ob bei Anfrage nach der LL die LB geliefert wird.
     * Ist ein Regressions-Test gegen PV-9390.
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void lebenslagenBleibenNachUpdateAufLbErhalten(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // LB mit Lebenslagen einspielen (im Leistungskatalog Mock wird Leika 99038009223002 mit LL 1180200 verknüpft):
        String firstLevel = "1000000";
        String secondLevel = "1180000";
        String thirdLevel = "1180200";
        XzufiFile lbXzufiFile = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        Leistungsbeschreibung lb = lbXzufiFile.getLeistungsbeschreibungen().getFirst();
        lb.setReferenzLeiKa("99038009223002");
        ImportUtil.pushAndWait23(lbXzufiFile);

        // Einen OD und eine ZS zur LB einspielen, dadurch ein Update auf die LB verursachen:
        Onlinedienst od = OdCreator.getInstance().getMin(testcaseName);
        od.setBezeichnung("Neuer OD");

        XzufiFile xzufiFileOd = new XzufiFile();
        xzufiFileOd.getOnlinedienste().add(od);
        ImportUtil.pushAndWait23(xzufiFileOd);

        Zustaendigkeit zs = ZsOnlinedienstCreator.getInstance().getMin(lb.getId(), od.getId(), 1);
        XzufiFile xzufiFileRelation = new XzufiFile();
        xzufiFileRelation.getZustaendigkeitenOnlinedienst().add(zs);
        ImportUtil.pushAndWait23(xzufiFileRelation);

        // Prüfen, ob die LB noch den LL-Baum liefert:
        String lbFullId = lb.getFullId();
        String ars = lbXzufiFile.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = lb.getTextmodule().getFirst().getInhalt();
        Response serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );

        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(
            serviceDescResponse
        );
        validator
            .personalMattersFirstLevelContainsCode(firstLevel)
            .personalMattersSecondLevelContainsCode(secondLevel)
            .personalMattersThirdLevelContainsCode(thirdLevel)
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", secondLevel)
            .personalMattersThirdLevelContainsNameByCode("Rente", thirdLevel);
        validator.serviceDescriptionsContainsId(lbFullId);

        // Prüfen, ob bei einer Anfrage mit der LL die LB geliefert wird:
        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of(thirdLevel),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(lbFullId);
    }

    /**
     * Prüft PV-9511
     * Für Leikakey 99157003000000 wurden sehr viele Klassifizierungen angelegt
     * Es wurden maximal 10 Einträge pro Hierarchie zurückgegeben. Bei secondLevel Einträgen konnte das zu einem Verlust an Daten führen
     * Für firstLevel und thirdLevel sollte das realistisch aktuell ausreichen - daher wird hier nur für secondLevel getestet.
     * Wichtig für den Test ist, dass mehr als 10 Einträge ausgeliefert werden
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void vieleLebenslagen(TestInfo testInfo) {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        String usedArs = "091890139139"; // Reit im Winkel - ungewöhnlicher Ars
        XzufiFile lbXzufiFile = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).zsOe(1).getMin();
        Leistungsbeschreibung lb = lbXzufiFile.getLeistungsbeschreibungen().getFirst();
        lbXzufiFile.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().clear();
        lbXzufiFile.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(usedArs); //Reit im Winkel
        lb.setReferenzLeiKa("99157003000000");
        ImportUtil.pushAndWait23(lbXzufiFile);

        String[] expectedSecondLevelBuerger = new String[] {
            "1010000",
            "1020000",
            "1030000",
            "1040000",
            "1050000",
            "1060000",
            "1070000",
            "1080000",
            "1090000",
            "1100000",
            "1110000",
            "1120000",
            "1170000",
        };
        String[] expectedSecondLevelUnternehmen = new String[] {
            "2010000",
            "2020000",
            "2030000",
            "2040000",
            "2050000",
            "2060000",
            "2070000",
            "2080000",
            "2090000",
            "2100000",
            "2110000",
            "2120000",
            "2130000",
            "2140000",
            "2150000",
            "2160000",
        };

        final var pmResponse = getPersonalMattersV1(usedArs, Constants.LANGUAGE_DE);
        final var personalMattersValidator = new PersonalMattersResultListValidator(pmResponse);
        personalMattersValidator
            .firstLevelExactly("1000000" /*Bürger*/, "2000000" /*Unternehmen*/)
            .secondLevelExactly("1000000", expectedSecondLevelBuerger)
            .secondLevelExactly("2000000", expectedSecondLevelUnternehmen);
    }

    private void validate(ServiceDescriptionResultListValidator validator) {
        validator
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersFirstLevelCountByCode(30, "1000000")
            .personalMattersFirstLevelCountByCode(10, "2000000")
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersSecondLevelContainsCode("1180000")
            .personalMattersSecondLevelCountByCode(10, "1180000")
            .personalMattersSecondLevelContainsCode("1170000")
            .personalMattersSecondLevelCountByCode(20, "1170000")
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersSecondLevelCountByCode(10, "2030000")
            .personalMattersThirdLevelContainsCode("1180200")
            .personalMattersThirdLevelCountByCode(10, "1180200")
            .personalMattersThirdLevelContainsCode("1170100")
            .personalMattersThirdLevelCountByCode(10, "1170100")
            .personalMattersThirdLevelContainsCode("1050200")
            .personalMattersThirdLevelCountByCode(10, "1050200")
            .personalMattersThirdLevelContainsCode("1170200")
            .personalMattersThirdLevelCountByCode(10, "1170200")
            .personalMattersThirdLevelContainsCode("2030700")
            .personalMattersThirdLevelCountByCode(10, "2030700");
    }
}
