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

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
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
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.PersonalMattersValidator;

/**
 * Testziel ist die Prüfung, dass bei Abfrage der Leistungs Details
 * die korrekten Lebenslagen angezeigt werden
 * Initial wird ein Leistungskatalog mit 4 Leistungen geladen
 * LeikaKey 99038009223001 - Lebenslage 2030700
 * LeikaKey 99001001000000 - Lebenslage 1050200, 1170100
 * LeikaKey 99038009223002 - Lebenslage 1180200
 * LeikaKey 99157002014000 - Lebenslage 2030400, 2160200, 1040100, 1040400
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
@SuppressWarnings("java:S1192")
class LebenslagenDetailseiteTest {

    @BeforeAll
    static void importLeistungskatalog() {
        KatalogImportUtil.resetLeistungskatalog("leistungskatalogOK.xml");
    }

    /**
     * Testet, dass bei Verwendung eines LeikakKeys, der eine komplexerer Struktur mit mehrerern Lebenslagen aufweist
     * alle Daten auch ausgegeben werden
     * Abfrage erfolgt auf deutsch und englisch
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void multiLagen(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile multiLagenXzufi = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMax();
        multiLagenXzufi.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99157002014000");
        multiLagenXzufi.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of());
        String id = multiLagenXzufi.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(multiLagenXzufi);

        String ars = multiLagenXzufi.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, id);

        PersonalMattersValidator personalMattersValidator = new PersonalMattersValidator(response);
        personalMattersValidator
            .firstLevelExactly("1000000", "2000000")
            .secondLevelExactly("1000000", "1040000")
            .secondLevelExactly("2000000", "2030000", "2160000")
            .thirdLevelExactly("1000000", "1040000", "1040100", "1040400")
            .thirdLevelExactly("2000000", "2030000", "2030400")
            .thirdLevelExactly("2000000", "2160000", "2160200")
            .firstLevelNameByCode("1000000", "Lebenslagen für Bürgerinnen und Bürger")
            .firstLevelNameByCode("2000000", "Geschäftslagen für Unternehmen")
            .secondLevelNameByCode("1040000", "Arbeit")
            .secondLevelNameByCode("2030000", "Arbeitgeber sein")
            .secondLevelNameByCode("2160000", "Geschäftsauflösung und Unternehmensübergang")
            .thirdLevelNameByCode("1040100", "Weiterbildung")
            .thirdLevelNameByCode("1040400", "Anerkennung ausländischer Berufsqualifikationen")
            .thirdLevelNameByCode("2030400", "Mitarbeiterbezogene Meldepflichten")
            .thirdLevelNameByCode("2160200", "Betriebsübernahme");

        Response responseEN = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            id,
            Constants.LANGUAGE_EN
        );
        personalMattersValidator = new PersonalMattersValidator(responseEN);
        personalMattersValidator
            .firstLevelExactly("1000000", "2000000")
            .secondLevelExactly("1000000", "1040000")
            .secondLevelExactly("2000000", "2030000", "2160000")
            .thirdLevelExactly("1000000", "1040000", "1040100", "1040400")
            .thirdLevelExactly("2000000", "2030000", "2030400")
            .thirdLevelExactly("2000000", "2160000", "2160200")
            .firstLevelNameByCode("1000000", "Life situations for citizens")
            .firstLevelNameByCode("2000000", "Business situations for companies")
            .secondLevelNameByCode("1040000", "Work")
            .secondLevelNameByCode("2030000", "Being an Employer")
            .secondLevelNameByCode("2160000", "Business Liquidation and Transfers of Undertakings")
            .thirdLevelNameByCode("1040100", "Further education")
            .thirdLevelNameByCode("1040400", "Recognition of foreign professional qualifications")
            .thirdLevelNameByCode("2030400", "Employee-related registration requirements")
            .thirdLevelNameByCode("2160200", "Taking over a business");
    }

    /**
     * Getestet wird für eine Leistung, die nicht im Katalog auftaucht aber 2 eigene Definitionen mitbringt
     * 1170100 - hierfür gibt es einen Codelisteneintrag
     * 1170900 - hierfür existiert kein Codelisteneintrag - darf nicht berücksichtigt werden
     * Abfrage erfolgt auf deutsch und englisch
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void lebenslageNichtImKatalog(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile multiLagenXzufi = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMax();
        multiLagenXzufi.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99157002014111");
        multiLagenXzufi.getLeistungsbeschreibungen().getFirst().setKlassifizierung(
            List.of(
                "1170100", //codelisten Eintrag vorhanden
                "1170900" //codelisten Eintrag fehlt, darf nicht ausgegeben werden
            )
        );

        String id = multiLagenXzufi.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(multiLagenXzufi);

        String ars = multiLagenXzufi.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, id);

        PersonalMattersValidator personalMattersValidator = new PersonalMattersValidator(response);
        personalMattersValidator
            .firstLevelExactly("1000000")
            .secondLevelExactly("1000000", "1170000")
            .thirdLevelExactly("1000000", "1170000", "1170100")
            .firstLevelNameByCode("1000000", "Lebenslagen für Bürgerinnen und Bürger")
            .secondLevelNameByCode("1170000", "Umwelt und Klima")
            .thirdLevelNameByCode("1170100", "Klima, Natur und Arten");

        Response responseEN = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            id,
            Constants.LANGUAGE_EN
        );
        personalMattersValidator = new PersonalMattersValidator(responseEN);
        personalMattersValidator
            .firstLevelExactly("1000000")
            .secondLevelExactly("1000000", "1170000")
            .thirdLevelExactly("1000000", "1170000", "1170100")
            .firstLevelNameByCode("1000000", "Life situations for citizens")
            .secondLevelNameByCode("1170000", "Climate and the Environment")
            .thirdLevelNameByCode("1170100", "Nature, climate and biodiversity");
    }

    /**
     * Getestet wird für eine Leistung, die nicht im Katalog auftaucht
     * es wurden keine Lebenslagen gesetzt
     * Abfrage erfolgt auf deutsch und englisch
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void ohneLebensLage(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile ohneLebensLage = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMax();
        ohneLebensLage.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99157002014111");
        ohneLebensLage.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of());
        String id = ohneLebensLage.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(ohneLebensLage);

        String ars = ohneLebensLage.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, id);

        PersonalMattersValidator personalMattersValidator = new PersonalMattersValidator(response);
        personalMattersValidator.isEmpty();

        Response responseEN = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            id,
            Constants.LANGUAGE_EN
        );
        personalMattersValidator = new PersonalMattersValidator(responseEN);
        personalMattersValidator.isEmpty();
    }

    /**
     * Getestet wird für eine Leistung, die nicht im Katalog auftaucht
     * als Lebenslage ist eine Lebenslage auf secondLevel (endet mit 4 nullen) gesetzt
     * Abfrage erfolgt auf deutsch und englisch
     */
    @Test
    @SuppressWarnings("java:S2699") // Die Assertions werden vom Validator durchgeführt.
    void secondLevelLebensLage(TestInfo testInfo) {
        String testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile secondLevelLebensLage = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMax();
        secondLevelLebensLage.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99157002014111");
        secondLevelLebensLage.getLeistungsbeschreibungen().getFirst().setKlassifizierung(List.of("1170000")); //codelisten Eintrag vorhanden
        String id = secondLevelLebensLage.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(secondLevelLebensLage);

        String ars = secondLevelLebensLage.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        Response response = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, id);
        PersonalMattersValidator personalMattersValidator = new PersonalMattersValidator(response);
        personalMattersValidator
            .firstLevelExactly("1000000")
            .secondLevelExactly("1000000", "1170000")
            .firstLevelNameByCode("1000000", "Lebenslagen für Bürgerinnen und Bürger")
            .secondLevelNameByCode("1170000", "Umwelt und Klima");

        Response responseEN = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(
            ars,
            id,
            Constants.LANGUAGE_EN
        );
        personalMattersValidator = new PersonalMattersValidator(responseEN);
        personalMattersValidator
            .firstLevelExactly("1000000")
            .secondLevelExactly("1000000", "1170000")
            .firstLevelNameByCode("1000000", "Life situations for citizens")
            .secondLevelNameByCode("1170000", "Climate and the Environment");
    }
}
