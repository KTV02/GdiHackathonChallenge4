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
package suchdienst.integrationstest.tests.sonstiges;

import static org.hamcrest.Matchers.is;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.LbCreator;
import de.dataport.pvog.testdatengenerator.creator.businessobject.ZsOrganisationseinheitCreator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;

/**
 * Testziel: Prüfung der API zum Abruf von Gebietsinformationen (ARS) nach OE-ID und LeiKa-ID.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationunitsArsControllerTest {

    @Test
    void getArsByLeikaIdAndOeIdTest(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        String expectedLeika = TestUtil.addTimestamp("leika");

        List<String> expectedArs1 = List.of("091234567890", "091000000000");
        List<String> expectedArs2 = List.of("092000000000");
        List<String> expectedArs3 = List.of("093000000000", "091000000000");
        List<String> expectedArs4 = List.of("094000000000");

        // Erzeugt eine XZuFi-File mit 2 Leistungen, 2 Organisationseinheiten und 4 Zuständigkeiten
        // Beide Leistungen referenzieren die gleiche LeiKa
        // Die ARS in den zuständigkeiten enthalten Duplikate und sind unsortiert
        XzufiFile file = createTestFile(
            testcasename,
            expectedLeika,
            expectedArs1,
            expectedArs2,
            expectedArs3,
            expectedArs4
        );
        ImportUtil.pushAndWait(file);

        var oe1 = file.getOrganisationseinheiten().get(0);
        var oe2 = file.getOrganisationseinheiten().get(1);

        // ARS werden Korrekt zur OE zurückgegeben. Keine Redundanten ARS-Einträge, Ergebnisse sind sortiert
        verifyArsResponse(expectedLeika, oe1.getFullId(), ListUtils.union(expectedArs1, expectedArs3));
        verifyArsResponse(expectedLeika, oe2.getFullId(), ListUtils.union(expectedArs2, expectedArs4));

        // Leere Antwortmenge bei nicht existierender Kombination aus LeiKa-ID und OE-ID, kein 404
        String notExistingOe = testcasename + "_phantasie";
        String notExistingLeika = TestUtil.addTimestamp("404"); // Muss zwischen 14 und 21 Zeichen lang sein
        verifyArsResponse(notExistingLeika, oe1.getFullId(), List.of());
        verifyArsResponse(expectedLeika, notExistingOe, List.of());
        verifyArsResponse(notExistingLeika, notExistingOe, List.of());
    }

    private XzufiFile createTestFile(
        String testcasename,
        String leika,
        List<String> ars1,
        List<String> ars2,
        List<String> ars3,
        List<String> ars4
    ) {
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(2).zsOe(1).getMin();
        Organisationseinheit oe1 = file.getOrganisationseinheiten().get(0);
        Organisationseinheit oe2 = file.getOrganisationseinheiten().get(1);
        Leistungsbeschreibung lb1 = file.getLeistungsbeschreibungen().get(0);
        Leistungsbeschreibung lb2 = LbCreator.getInstance().getMin(testcasename);

        Zustaendigkeit zs1 = file.getZustaendigkeitenOrganisationseinheit().get(0);
        Zustaendigkeit zs2 = file.getZustaendigkeitenOrganisationseinheit().get(1);
        Zustaendigkeit zs3 = ZsOrganisationseinheitCreator.getInstance().getMin(lb2.getId(), oe1.getId());
        Zustaendigkeit zs4 = ZsOrganisationseinheitCreator.getInstance().getMin(lb2.getId(), oe2.getId());

        zs1.setArss(ars1);
        zs2.setArss(ars2);
        zs3.setArss(ars3);
        zs4.setArss(ars4);

        lb1.setReferenzLeiKa(leika);
        lb2.setReferenzLeiKa(leika);

        file.addBusinessobjects(lb2, zs3, zs4);
        return file;
    }

    private void verifyArsResponse(String leika, String oeId, List<String> expectedArs) {
        // ARS kommen aufsteigend sortiert zurück und ohne Duplikate
        List<String> sortedArs = expectedArs.stream().distinct().sorted().toList();
        OrganisationunitsConnector.getArsByOeIdAndLeikaId(leika, oeId).then().statusCode(200).body(".", is(sortedArs));
    }
}
