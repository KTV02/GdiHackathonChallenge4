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
package suchdienst.integrationstest.tests.x23.trefferlistev7;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.SpCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.ModulSpezialisierung;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.Spezialisierung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

/*
In diesem Test soll geprüft werden, dass Suche über
- Inhalt Textmodul 02
- Inhalt Textmodul 03
- Inhalt Textmodul 06
- Inhalt Textmodul 24
- BegriffImKontext
- OD Bezeichnung
- OD Beschreibung
- SP Inhalt Textmodul 06
- SP Inhalt Textmodul 24
- UrsprungsportalTitel
zu einem Treffer führt

Im Test matchByMultipleItems wird geprüft, dass die und Verknüpfung mehrerer Suchbegriffe ebenfalls zu einem treffer führt
Bei 8 möglichen Feldern aber nur 5 erlaubten Suchbegriffen werden 2 Validierungen im Test durchgeführt

Im Test matchByMultipleItemsnegative wird geprüft, dass bei 4 korrekten Begriffen und einem unbekannten Begriff
kein Treffer zurückgeliefert wird
*/
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class TrefferlisteArsTest {

    private static String lbId;
    private static String ars1;
    private static String ars2;
    private static String beschreibungTM02;
    private static String titelTM03;
    private static String volltextTM06;
    private static String teaserTM24;
    private static String begriffImKontext;
    private static String ursprungsportalTitel;
    private static String odBezeichnung;
    private static String odBeschreibung;
    private static String spVolltextTM06;
    private static String spTeaserTM24;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String randomPrefix = TestUtil.generateRandomWord();
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).zsOe(2).od(1).getMin();
        //set relevant values for trefferliste
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();

        List<Textmodul> textmodule = new ArrayList<>();
        beschreibungTM02 = randomPrefix + "trefferlisteBeschreibung02tm";
        Textmodul tm02 = Textmodul.builder().inhalt(beschreibungTM02).positionDarstellung(2).leikaCode("02").build();
        titelTM03 = randomPrefix + "trefferlisteTitel03tm";
        Textmodul tm03 = Textmodul.builder().inhalt(titelTM03).positionDarstellung(3).leikaCode("03").build();
        volltextTM06 = randomPrefix + "trefferlisteVolltext06tm";
        Textmodul tm06 = Textmodul.builder().inhalt(volltextTM06).positionDarstellung(6).leikaCode("06").build();
        teaserTM24 = randomPrefix + "trefferlisteTeaser24tm";
        Textmodul tm24 = Textmodul.builder().inhalt(teaserTM24).positionDarstellung(6).leikaCode("24").build();
        textmodule.add(tm02);
        textmodule.add(tm03);
        textmodule.add(tm06);
        textmodule.add(tm24);
        lb.setTextmodule(textmodule);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        begriffImKontext = randomPrefix + "trefferlisteBegriffImKontext";
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff(begriffImKontext);
        suchbegriffe.add(suchbegriff);
        lb.setSuchbegriffe(suchbegriffe);

        ursprungsportalTitel = randomPrefix + "trefferlisteUrsprungsportalTitel";
        lb.getUrsprungsportale().getFirst().setTitle(ursprungsportalTitel);

        odBezeichnung = randomPrefix + "trefferlisteODBezeichnung";
        odBeschreibung = randomPrefix + "trefferlisteODBeschreibung";
        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.setBeschreibung(odBeschreibung);
        od.setBezeichnung(odBezeichnung);

        Spezialisierung sp = SpCreator.getInstance().getMin(testclassname, lb.getId(), 2);
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        Zustaendigkeit zsoe2 = ZsOrganisationseinheitCreator.getInstance().getMin(lb.getId(), oe.getId());
        // Volltext
        spVolltextTM06 = randomPrefix + "sptrefferlisteVolltext06tm";
        ModulSpezialisierung sptm06 = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(Textmodul.builder().leikaCode("06").inhalt(spVolltextTM06).build())
            .build();
        // Teaser
        spTeaserTM24 = randomPrefix + "sptrefferlisteTeaser24tm";
        ModulSpezialisierung sptm24 = ModulSpezialisierung.builder()
            .ersetzung(true)
            .modul(Textmodul.builder().leikaCode("24").inhalt(spTeaserTM24).build())
            .build();

        sp.setModulSpezialisierungen(List.of(sptm06, sptm24));
        file.getSpezialisierungen().add(sp);
        file.getZustaendigkeitenOrganisationseinheit().add(zsoe2);

        ImportUtil.pushAndWait23(file);

        ars1 = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        ars2 = sp.getArs().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
    }

    @Test
    void matchByDescription() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, beschreibungTM02);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByTitle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, titelTM03);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByFulltext() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, volltextTM06);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByTeaser() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, teaserTM24);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchBySynonym() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, begriffImKontext);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchBySourceportalTitle() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, ursprungsportalTitel);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    /**
     * Findet eine Leistung bei der Suche über den Volltext aus der Spezialisierung in ARS2
     * Findet die Leistung nicht in ARS1
     */
    @Test
    void matchBySpFulltext() {
        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, spVolltextTM06);
        ServiceDescriptionResultListValidator validator1 = new ServiceDescriptionResultListValidator(response1);
        validator1.validateNotContains(lbId);

        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsV7(ars2, spVolltextTM06);
        ServiceDescriptionResultListValidator validator2 = new ServiceDescriptionResultListValidator(response2);
        validator2.validateContains(lbId);
    }

    /**
     * Findet eine Leistung bei der Suche über den Teaser aus der Spezialisierung in ARS2
     * Findet die Leistung nicht in ARS1     */
    @Test
    void matchBySpTeaser() {
        Response response1 = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, spTeaserTM24);
        ServiceDescriptionResultListValidator validator1 = new ServiceDescriptionResultListValidator(response1);
        validator1.validateNotContains(lbId);

        Response response2 = ServiceDescriptionConnector.getServiceDescriptionsV7(ars2, spTeaserTM24);
        ServiceDescriptionResultListValidator validator2 = new ServiceDescriptionResultListValidator(response2);
        validator2.validateContains(lbId);
    }

    @Test
    void matchByODdescription() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, odBeschreibung);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByODdefinition() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(ars1, odBezeichnung);
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByMultipleItems() {
        List<String> multipleQueryItems1 = Arrays.asList(
            beschreibungTM02,
            titelTM03,
            ursprungsportalTitel,
            begriffImKontext,
            volltextTM06
        );
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars1,
            multipleQueryItems1,
            0,
            20,
            false,
            Collections.emptyList(),
            null
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);

        List<String> multipleQueryItems2 = Arrays.asList(
            odBeschreibung,
            odBezeichnung,
            volltextTM06,
            beschreibungTM02,
            begriffImKontext
        );
        response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars1,
            multipleQueryItems2,
            0,
            20,
            false,
            Collections.emptyList(),
            null
        );
        validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }

    @Test
    void matchByMultipleItemsNegative() {
        List<String> multipleQueryItems = Arrays.asList(
            beschreibungTM02,
            titelTM03,
            ursprungsportalTitel,
            odBeschreibung,
            "unknownItem"
        );
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ars1,
            multipleQueryItems,
            0,
            20,
            false,
            Collections.emptyList(),
            null
        );
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateNotContains(lbId);
    }
}
