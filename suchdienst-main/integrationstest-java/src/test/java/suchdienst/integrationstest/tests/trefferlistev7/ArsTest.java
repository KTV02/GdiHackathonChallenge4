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
package suchdienst.integrationstest.tests.trefferlistev7;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ElasticsearchConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionv7.ServiceDescriptionResultListValidator;

/*
 * Leistungen darf nur ausgegeben werden, wenn sie einer OE oder OD zugeordnet sind.
 * Weder ARS in SP noch FO haben darauf einfluss
 */
class ArsTest {

    @BeforeAll
    static void setup() {
        ElasticsearchConnector.removeAllData();
    }

    @AfterAll
    static void teardown() {
        ElasticsearchConnector.removeAllData();
    }

    @Test
    void arsSPTest(TestInfo testinfo) {
        String testname = TestUtil.addTimestamp(testinfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bundesland = new BundeslandConfig(BundeslandConfig.Bundesland.BY);
        String begriff = "Schaumkrone";
        String arsOE = "091755114121"; // Glonn
        String arsSP = "091750122122"; // Grafing b München

        XzufiFile file = XzufiFileCreator.newInstance(bundesland).name(testname).lb(1).oe(1).sp(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        // OE nur für Glonn gültig
        file.getZustaendigkeitenOrganisationseinheit()
            .getFirst()
            .setArss(new ArrayList<>(List.of(arsOE)));
        // Spezialisierung für beide ARS gültig
        file.getSpezialisierungen()
            .getFirst()
            .setArs(new ArrayList<>(List.of(arsOE, arsSP)));

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul textmodul = Textmodul.builder().inhalt(begriff).positionDarstellung(1).leikaCode("03").build();
        textmodule.add(textmodul);
        leistung.setTextmodule(textmodule);

        ImportUtil.pushAndWait23(file);

        // Validierung: Leistung wird nur mit gültiger OE angezeigt.
        Response responseOE = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsOE).q(begriff).build()
        );
        Response responseSP = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsSP).q(begriff).build()
        );

        ServiceDescriptionResultListValidator resValidatorOE = new ServiceDescriptionResultListValidator(responseOE);
        resValidatorOE.validateContains(file.getLeistungsbeschreibungen().getFirst().getFullId());
        ServiceDescriptionResultListValidator resValidatorSP = new ServiceDescriptionResultListValidator(responseSP);
        resValidatorSP.validateEmptyResultList();

        // Füge Zuständigkeit für OE hinzu
        XzufiFile updateOE = new XzufiFile();
        updateOE.setOrganisationseinheiten(file.getOrganisationseinheiten());
        updateOE.setZustaendigkeitenOrganisationseinheit(file.getZustaendigkeitenOrganisationseinheit());
        updateOE.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().add(arsSP);

        ImportUtil.pushAndWait23(updateOE);

        // Validierung: Leistung wird auch für Grafing b München angezeigt.
        responseSP = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsSP).q(begriff).build()
        );
        resValidatorSP = new ServiceDescriptionResultListValidator(responseSP);
        resValidatorSP.validateContains(file.getLeistungsbeschreibungen().getFirst().getFullId());
    }

    @Test
    void arsFOTest(TestInfo testinfo) {
        String testname = TestUtil.addTimestamp(testinfo.getTestClass().orElseThrow().getName());
        BundeslandConfig bundesland = new BundeslandConfig(BundeslandConfig.Bundesland.BY);
        String begriff = "Laubrechen";
        String arsOD = "094725416127"; // Creußen
        String arsFO = "094720190190"; // Speicherdorf

        XzufiFile file = XzufiFileCreator.newInstance(bundesland).name(testname).lb(1).od(1).fo(1).getMin();
        Leistungsbeschreibung leistung = file.getLeistungsbeschreibungen().getFirst();
        // OE nur für Creußen gültig
        file.getZustaendigkeitenOnlinedienst()
            .getFirst()
            .setArss(new ArrayList<>(List.of(arsOD)));
        // FO für Creußen und Speicherdorf
        file.getZustaendigkeitenFormular()
            .getFirst()
            .setArss(new ArrayList<>(List.of(arsOD, arsFO)));

        List<Textmodul> textmodule = new ArrayList<>();
        Textmodul textmodul = Textmodul.builder().inhalt(begriff).positionDarstellung(1).leikaCode("03").build();
        textmodule.add(textmodul);
        leistung.setTextmodule(textmodule);

        ImportUtil.pushAndWait23(file);

        // Validierung: Leistung wird nur mit gültiger OD angezeigt.
        Response responseOD = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsOD).q(begriff).build()
        );
        Response responseFO = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsFO).q(begriff).build()
        );

        ServiceDescriptionResultListValidator resValidatorOD = new ServiceDescriptionResultListValidator(responseOD);
        resValidatorOD.validateContains(file.getLeistungsbeschreibungen().getFirst().getFullId());
        ServiceDescriptionResultListValidator resValidatorFO = new ServiceDescriptionResultListValidator(responseFO);
        resValidatorFO.validateEmptyResultList();

        // Füge Zuständigkeit für OD hinzu
        XzufiFile updateOD = new XzufiFile();
        updateOD.setOnlinedienste(file.getOnlinedienste());
        updateOD.setZustaendigkeitenOnlinedienst(file.getZustaendigkeitenOnlinedienst());
        updateOD.getZustaendigkeitenOnlinedienst().getFirst().getArss().add(arsFO);

        ImportUtil.pushAndWait23(updateOD);

        // Validierung: Leistung wird auch für Speicherdorf angezeigt.
        responseFO = ServiceDescriptionConnector.getServiceDescriptionsV7(
            ServiceDescriptionConnector.V7ConnectorProperties.builder().ars(arsFO).q(begriff).build()
        );
        resValidatorFO = new ServiceDescriptionResultListValidator(responseFO);
        resValidatorFO.validateContains(file.getLeistungsbeschreibungen().getFirst().getFullId());
    }
}
