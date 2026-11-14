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
package suchdienst.integrationstest.tests.trefferlistev8;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.LeiKaTypisierung;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.ArrayList;
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
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Die Such-API (V8) kennt keinen "ignoreLeikaTypes"-Parameter mehr: Es wird immer die volle, in den
 * SuchdienstProperties konfigurierte Leika-Typ-Whitelist verwendet. Dieser Test ersetzt den
 * parametrisierten v7-Test {@code IgnoreLeikaTypesTest} und prüft nur noch das fest einprogrammierte Verhalten:
 * Eine Leistung mit beliebiger (in der Whitelist enthaltener) Leika-Typisierung wird immer gefunden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeikaTypesTest {

    private static String lbId;
    private static String ars;
    private static String queryString;

    @BeforeAll
    static void setupTestclass(TestInfo testInfo) {
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testclassname).lb(1).oe(1).getMin();
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        lb.getTypisierung().clear();
        LeiKaTypisierung typisierung = new LeiKaTypisierung();
        typisierung.setCode("4");
        typisierung.setName("Regelungskompetenz auf Landesebene");
        lb.getTypisierung().add(typisierung);
        LeiKaTypisierung typisierung5 = new LeiKaTypisierung();
        typisierung5.setCode("5");
        typisierung5.setName("Regelungskompetenz auf kommunaler Ebene");
        lb.getTypisierung().add(typisierung5);

        List<Suchbegriff> suchbegriffe = new ArrayList<>();
        Suchbegriff suchbegriff = new Suchbegriff();
        suchbegriff.setBegriff("LeikaTypesTestEindeutigerSuchbegriff");
        suchbegriffe.add(suchbegriff);
        lb.setSuchbegriffe(suchbegriffe);

        ImportUtil.pushAndWait23(file);

        ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        lbId = file.getLeistungsbeschreibungen().getFirst().getFullId();
        queryString = "LeikaTypesTestEindeutigerSuchbegriff";
    }

    @Test
    void findsLeistungRegardlessOfLeikaType() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsV8(ars, queryString);

        // Validierung: Es wird immer die volle Whitelist verwendet, die Leistung wird also unabhängig von ihrer
        // Leika-Typisierung gefunden.
        ServiceDescriptionResultListValidator validator = new ServiceDescriptionResultListValidator(response);
        validator.validateContains(lbId);
    }
}
