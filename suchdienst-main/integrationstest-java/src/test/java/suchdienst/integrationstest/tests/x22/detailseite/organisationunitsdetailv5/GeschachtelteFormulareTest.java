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
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.FoCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsFormularCreator;
import de.dataport.pvog.testdatengenerator.model.Formular;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.organisationunitsdetailv5.NestedFormsValidator;

/**
 * Die geschachtelten Formulare verfügen über eigene Zuständigkeiten.
 * 1. Mehrere vollständige Formulare werden korrekt angezeigt in deutsch
 * 2. Formulare sind auf Englisch verfügbar
 * 3. Die ARS-Hierarchie wird getestet, um sicherzustellen, dass die Formualre auf allen Verwaltungsebenen Verfügbar sind
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class GeschachtelteFormulareTest {

    /**
     * Eine Organisationseinheit hat 2 voll gefüllte geschachtelte Formulare und die Testziele sind:
     * 1. Alle Daten der Formulare werdem ausgegeben
     * 2. Eine Organisationseinheit kann mehrere Formulare haben
     */
    @Test
    void manyFoData(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String lbId = leistung.getId();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zsoe.getArss().getFirst();
        FoCreator foCreator = FoCreator.getInstance();
        Formular nestedForm1 = foCreator.getMin("geschachteltes Formular1");
        Formular nestedForm2 = foCreator.getMin("geschachteltes Formular2");
        Zustaendigkeit zsNestedForm1 = ZsFormularCreator.getInstance().getMin(lbId, nestedForm1.getId());
        Zustaendigkeit zsNestedForm2 = ZsFormularCreator.getInstance().getMin(lbId, nestedForm2.getId());
        nestedForm1.getZustaendigkeiten().add(zsNestedForm1);
        nestedForm2.getZustaendigkeiten().add(zsNestedForm2);

        nestedForm1.setBezeichnung("geschachteltes Formular1 Bezeichnung");
        nestedForm1.setBeschreibung("geschachteltes Formular1 Beschreibung");
        nestedForm1.setMimeCode("geschachteltes Formular1 mimeCode");

        nestedForm2.setBezeichnung("geschachteltes Formular2 Bezeichnung");
        nestedForm2.setBeschreibung("geschachteltes Formular2 Beschreibung");
        nestedForm2.setMimeCode("geschachteltes Formular2 mimeCode");

        oe.getFormulare().addAll(List.of(nestedForm1, nestedForm2));
        ImportUtil.pushAndWait(file);

        // Die OE soll zwei geschachtelte Formulare haben:
        Response oeResponse = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe.getFullId(),
            ars,
            leistung.getFullId()
        );
        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(oeResponse);
        nestedFormsValidator
            .validateCount(2)
            .validateId(0, nestedForm1.getFullId())
            .validateName(0, "geschachteltes Formular1 Bezeichnung")
            .validateDescription(0, "geschachteltes Formular1 Beschreibung")
            .validateUri(0, nestedForm1.getLink().getUri())
            .validateMimeCode(0, "geschachteltes Formular1 mimeCode")
            .validateId(1, nestedForm2.getFullId())
            .validateName(1, "geschachteltes Formular2 Bezeichnung")
            .validateDescription(1, "geschachteltes Formular2 Beschreibung")
            .validateUri(1, nestedForm2.getLink().getUri())
            .validateMimeCode(1, "geschachteltes Formular2 mimeCode");
    }

    /**
     * Testet, dass die gelieferten Formulare gemäß der Ars-Hierarchie reduziert werden.
     * Dazu werden Formulare mit Zuständigkeiten auf allen 5 Ars-Ebenen zur OE hinzugefügt,
     * dann über die API Anfragen auf Gemeinden gestellt, die in allen Ebenen oder nur in Teilen der Ebenen liegen.
     * Es wird geprüft, ob die Formulare auf diejenigen reduziert sind, deren Ebene zur Anfrage-Gemeinde passt.
     */
    @Test
    void foArsFilterHerachie(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // Diese GemeindeArs treten im Ars der VL in ES auf, und mit diesen Ars suchen wir auch über die API:
        // Ars z.b.in https://www.destatis.de/DE/Themen/Laender-Regionen/Regionales/Gemeindeverzeichnis/_inhalt.html#101366
        String arsGemeinde1 = "093775347141"; // Erwartung: alle FO werden gefunden
        String arsGemeinde2 = "093775347145"; // Erwartung: alle FO bis arsFOZustaendigkeitGemeinde werden gefunden
        String arsGemeinde3 = "093775348128"; // Erwartung: alle FO bis arsFOZustaendigkeitGemeindeverband werden gefunden
        String arsGemeinde4 = "093755338191"; // Erwartung: alle FO bis arsFOZustaendigkeitBezirk werden gefunden
        String arsGemeinde5 = "092750111111"; // Erwartung: alle FO bis arsBayern werden gefunden

        // Diese GemeindeArs nehmen wir für die ZS des FO:
        String arsFoZustaendigkeitGemeinde = "093775347141"; // Mitterteich Stadt
        String arsFoZustaendigkeitGemeindeverband = "093775347000"; // Mitterteich
        String arsFoZustaendigkeitKreis = "093770000000"; // Tirschenreuth
        String arsFoZustaendigkeitBezirk = "093000000000"; // Oberpfalz
        String arsBayern = "090000000000";
        List<String> arsArray = List.of(
            arsFoZustaendigkeitGemeinde,
            arsFoZustaendigkeitGemeindeverband,
            arsFoZustaendigkeitKreis,
            arsFoZustaendigkeitBezirk,
            arsBayern
        );

        BundeslandConfig bundeslandConfig = new BundeslandConfig("L100042", List.of(arsBayern));

        // Daten erzeugen, es entsteht eine VL mit allen Gemeinde-Ars aus Bayern
        XzufiFile file = XzufiFileCreator.newInstance(bundeslandConfig).name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String lbId = leistung.getId();
        Organisationseinheit organisationseinheit = file.getOrganisationseinheiten().getFirst();
        organisationseinheit.getFormulare().clear();
        FoCreator foCreator = FoCreator.getInstance();
        List<Formular> formArray = new ArrayList<>();

        for (int i = 0; i < arsArray.size(); i++) {
            formArray.add(foCreator.getMin("geschachteltes Formular" + i));
            formArray.get(i).setBezeichnung("Bezeichnung " + i);
            formArray.get(i).setBeschreibung("Beschreibung " + i);
            formArray.get(i).setLink(
                Link.builder()
                    .uri("https://gidf.de/forms" + i)
                    .build()
            );
            formArray
                .get(i)
                .getZustaendigkeiten()
                .add(
                    Zustaendigkeit.builder()
                        .parentId(formArray.get(i).getId())
                        .arss(List.of(arsArray.get(i)))
                        .leistungIDs(List.of(lbId))
                        .schemeAgencyId(file.getSchemeAgencyId())
                        .build()
                );
        }

        organisationseinheit.getFormulare().addAll(formArray);
        ImportUtil.pushAndWait(file);

        // SuD mit der Ars zur Formulare abfragen und prüfen ob alle notwendigen Formulare enthalten sind
        Response response1 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde1,
            leistung.getFullId()
        );
        NestedFormsValidator validator1 = new NestedFormsValidator(response1);
        validator1
            .validateCount(5)
            .validateName(0, "Bezeichnung 0")
            .validateName(1, "Bezeichnung 1")
            .validateName(2, "Bezeichnung 2")
            .validateName(3, "Bezeichnung 3")
            .validateName(4, "Bezeichnung 4")
            .validateDescription(0, "Beschreibung 0")
            .validateDescription(1, "Beschreibung 1")
            .validateDescription(2, "Beschreibung 2")
            .validateDescription(3, "Beschreibung 3")
            .validateDescription(4, "Beschreibung 4")
            .validateUri(0, "https://gidf.de/forms0")
            .validateUri(1, "https://gidf.de/forms1")
            .validateUri(2, "https://gidf.de/forms2")
            .validateUri(3, "https://gidf.de/forms3")
            .validateUri(4, "https://gidf.de/forms4");

        Response response2 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde2,
            leistung.getFullId()
        );
        NestedFormsValidator validator2 = new NestedFormsValidator(response2);
        validator2
            .validateCount(4)
            .validateName(0, "Bezeichnung 1")
            .validateName(1, "Bezeichnung 2")
            .validateName(2, "Bezeichnung 3")
            .validateName(3, "Bezeichnung 4")
            .validateDescription(0, "Beschreibung 1")
            .validateDescription(1, "Beschreibung 2")
            .validateDescription(2, "Beschreibung 3")
            .validateDescription(3, "Beschreibung 4")
            .validateUri(0, "https://gidf.de/forms1")
            .validateUri(1, "https://gidf.de/forms2")
            .validateUri(2, "https://gidf.de/forms3")
            .validateUri(3, "https://gidf.de/forms4");

        Response response3 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde3,
            leistung.getFullId()
        );
        NestedFormsValidator validator3 = new NestedFormsValidator(response3);
        validator3
            .validateCount(3)
            .validateName(0, "Bezeichnung 2")
            .validateName(1, "Bezeichnung 3")
            .validateName(2, "Bezeichnung 4")
            .validateDescription(0, "Beschreibung 2")
            .validateDescription(1, "Beschreibung 3")
            .validateDescription(2, "Beschreibung 4")
            .validateUri(0, "https://gidf.de/forms2")
            .validateUri(1, "https://gidf.de/forms3")
            .validateUri(2, "https://gidf.de/forms4");

        Response response4 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde4,
            leistung.getFullId()
        );
        NestedFormsValidator validator4 = new NestedFormsValidator(response4);
        validator4
            .validateCount(2)
            .validateName(0, "Bezeichnung 3")
            .validateName(1, "Bezeichnung 4")
            .validateDescription(0, "Beschreibung 3")
            .validateDescription(1, "Beschreibung 4")
            .validateUri(0, "https://gidf.de/forms3")
            .validateUri(1, "https://gidf.de/forms4");

        Response response5 = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            organisationseinheit.getFullId(),
            arsGemeinde5,
            leistung.getFullId()
        );
        NestedFormsValidator validator5 = new NestedFormsValidator(response5);
        validator5
            .validateCount(1)
            .validateName(0, "Bezeichnung 4")
            .validateDescription(0, "Beschreibung 4")
            .validateUri(0, "https://gidf.de/forms4");
    }

    @Test
    void mehrsprachigkeitFo(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        //***** Testcase *****
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        String lbId = leistung.getId();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        oe.setTitelEnglisch("Titel Englisch");
        Zustaendigkeit zsoe = file.getZustaendigkeitenOrganisationseinheit().getFirst();
        String ars = zsoe.getArss().getFirst();
        FoCreator foCreator = FoCreator.getInstance();
        Formular nestedForm = foCreator.getMin("geschachteltes Formular");
        Zustaendigkeit zsNestedForm = Zustaendigkeit.builder()
            .parentId(nestedForm.getId())
            .arss(List.of(ars))
            .leistungIDs(List.of(lbId))
            .schemeAgencyId(file.getSchemeAgencyId())
            .build();
        nestedForm.getZustaendigkeiten().add(zsNestedForm);
        nestedForm.setBezeichnung("geschachteltes Formular Bezeichnung");
        nestedForm.setBeschreibung("geschachteltes Formular Beschreibung");
        nestedForm.setBezeichnungEnglisch("Bezeichnung Englisch");
        nestedForm.setBeschreibungEnglisch("Beschreibung Englisch");
        nestedForm.setKurzbezeichnungEnglish("Kurz bezeichnung Englisch");
        nestedForm.setMimeCode("text/html");

        oe.getFormulare().add(nestedForm);
        ImportUtil.pushAndWait(file);

        // Die OE soll zwei geschachtelte Formulare haben:
        Response oeResponse = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe.getFullId(),
            ars,
            leistung.getFullId()
        );
        NestedFormsValidator nestedFormsValidator = new NestedFormsValidator(oeResponse);
        nestedFormsValidator
            .validateCount(1)
            .validateId(0, nestedForm.getFullId())
            .validateName(0, "geschachteltes Formular Bezeichnung")
            .validateDescription(0, "geschachteltes Formular Beschreibung")
            .validateUri(0, nestedForm.getLink().getUri());

        Response oeResponseEng = OrganisationunitsConnector.getOrganisationunitsDetailV5(
            oe.getFullId(),
            ars,
            leistung.getFullId(),
            null,
            Constants.LANGUAGE_EN
        );
        NestedFormsValidator nestedFormsValidatorEng = new NestedFormsValidator(oeResponseEng);
        nestedFormsValidatorEng
            .validateName(0, "Bezeichnung Englisch")
            .validateDescription(0, "Beschreibung Englisch")
            .validateMimeCode(0, "text/html");
    }
}
