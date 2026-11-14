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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.mocks.LeistungskatalogMock;
import suchdienst.integrationstest.utils.ArtifactWriter;
import suchdienst.integrationstest.utils.WaitUtil;
import suchdienst.integrationstest.validator.servicedescriptionv8.ServiceDescriptionResultListValidator;

/**
 * Damit ein Leistungskatalog importiert werden kann ist in der application.yaml konfiguriert werden
 * <pre>
 * {@code
 *     pvog:
 *       pvexternaldownloader:
 *         config:
 *           leistungskatalog:
 *             url: "http://localhost:8001/leistungskatalog
 * }
 * </pre>
 * Der Katalog wird über einen Mockserver ausgespielt, der auf Port 8001 lauscht.
 * Initialisiert wird er mit dem auszuliefernden Content - dem Leistungskatalog und einem zurückzugebenden Statuscode
 * <p>
 * Damit nicht unnötig bzw. fehlerhaft versucht wird den Leistungskatalog zu importieren, wurde der Import Scheduler
 * konfiguriert.
 * <pre>
 * {@code
 *   dataport:
 *     suchdienst:
 *       importer:
 *         initial-leika-import-delay: "P1D"
 *         delay-between-leika-imports: "P2D"
 * }
 * </pre>
 * Der Import startet erstmals nach 24 Stunden und danach alle 2 Tage
 */
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class LeistungskatalogTest {

    private ArtifactWriter writer;
    private String testcaseName;
    private LeistungskatalogMock leistungskatalogMock;

    @BeforeEach
    void setup() {
        leistungskatalogMock = new LeistungskatalogMock();
    }

    @AfterEach
    void teardown() {
        writer.writeServerLog();
        leistungskatalogMock.stop();
    }

    @AfterAll
    static void importLeistungskatalog() {
        KatalogImportUtil.resetLeistungskatalog("leistungskatalogOK.xml");
    }

    @Test
    void importOK(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogOK.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    @Test
    void importRetryOK(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogOK.xml").toURI()));

        leistungskatalogMock.registerMock("", 500);
        // Trigger Import
        AdminConnector.updateLeistungskatalog();

        // Lasse Backend in Retry laufen
        WaitUtil.waitFor(500, TimeUnit.MILLISECONDS);
        leistungskatalogMock.clear();

        // Stelle einen Erfolgreichen Mock zur Verfügung
        leistungskatalogMock.registerMock(content, 200);

        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogImportRetryOK(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    @Test
    void importFailsWith404(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcasename);
        String content = "";

        leistungskatalogMock.registerMock(content, 404);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogImportFails404(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    @Test
    void importFailsWith500(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcasename);
        String content = "";

        leistungskatalogMock.registerMock(content, 500);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogImportFails500(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    @Test
    void importFailsWithNoContent(TestInfo testInfo) {
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcasename);
        String content = "";

        leistungskatalogMock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogImportFails(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    @Test
    void importFileCorrupt(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(
            Paths.get(ClassLoader.getSystemResource("leistungskatalogERROR.xml").toURI())
        );

        leistungskatalogMock.registerMock(content, 200);

        //Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogImportFails(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );
    }

    /**
     * Test, dass beim Update eines Leistungskatalogs eine vorher importierte Leistung angepasst wird
     * LeikaId 99038009223001
     * initial 2030700 zugeordnet
     * nach Update 2030500 und 1040400
     */
    @Test
    void updateCatalogUpdateLage(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // initialer Import
        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogOK.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        leistungskatalogMock.clear();

        // Import einer Leistung
        XzufiFile fileUpdateLagen = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        fileUpdateLagen.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223001");
        ImportUtil.pushAndWait23(fileUpdateLagen);

        String ars = fileUpdateLagen.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = fileUpdateLagen
            .getLeistungsbeschreibungen()
            .getFirst()
            .getTextmodule()
            .getFirst()
            .getInhalt();
        String id = fileUpdateLagen.getLeistungsbeschreibungen().getFirst().getFullId();

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
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersThirdLevelContainsCode("2030700")
            .personalMattersFirstLevelContainsName("Geschäftslagen für Unternehmen")
            .personalMattersSecondLevelContainsNameByCode("Arbeitgeber sein", "2030000")
            .personalMattersThirdLevelContainsNameByCode("Sonderregelungen der Arbeitszeit", "2030700");
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("2030700"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        writer = new ArtifactWriter(testcaseName);
        content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogUPDATE.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);
        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        // warten auf Indexupdate
        WaitUtil.waitFor(1, TimeUnit.SECONDS);
        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validator
            .personalMattersFirstLevelContainsCode("2000000")
            .personalMattersSecondLevelContainsCode("2030000")
            .personalMattersThirdLevelContainsCode("2030500")
            .personalMattersFirstLevelContainsName("Geschäftslagen für Unternehmen")
            .personalMattersSecondLevelContainsNameByCode("Arbeitgeber sein", "2030000")
            .personalMattersThirdLevelContainsNameByCode("Arbeitssicherheit", "2030500")
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersSecondLevelContainsCode("1040000")
            .personalMattersThirdLevelContainsCode("1040400")
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Arbeit", "1040000")
            .personalMattersThirdLevelContainsNameByCode("Anerkennung ausländischer Berufsqualifikationen", "1040400");
        validator.serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("2030500", "1040400"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);
        // vorherige lage ist nicht mehr zugeordnet
        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("2030700"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test, dass beim Update eines Leistungskatalogs eine vorher importierte Leistung nach dem Import keine Lebenslage
     * mehr zugeordnet ist
     * LeikaId 99038009223002
     * initial 1180200 zugeordnet
     * nach Update leer
     */
    @Test
    void updateCatalogDeleteLage(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // initialer Import
        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogOK.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        leistungskatalogMock.clear();

        // Import einer Leistung
        XzufiFile fileUpdateLagen = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        fileUpdateLagen.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99038009223002");
        String id = fileUpdateLagen.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(fileUpdateLagen);

        String ars = fileUpdateLagen.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = fileUpdateLagen
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
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersSecondLevelContainsCode("1180000")
            .personalMattersThirdLevelContainsCode("1180200")
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Altersvorsorge und Ruhestand", "1180000")
            .personalMattersThirdLevelContainsNameByCode("Rente", "1180200");
        validator.serviceDescriptionsContainsId(id);

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("1180200"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        writer = new ArtifactWriter(testcaseName);
        content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogUPDATE.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);
        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        // warten auf Indexupdate
        WaitUtil.waitFor(1, TimeUnit.SECONDS);
        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("1180200"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);
    }

    /**
     * Test, dass beim Update eines Leistungskatalogs eine vorher importierte Leistung (ohne Lebenslagenzuordnung)
     * nach dem Import eine Lebenslage zugeordnet bekommt
     * LeikaId 99046048000000
     * initial leert
     * nach Update 1150200
     */
    @Test
    void updateCatalogAddLage(TestInfo testInfo) throws Exception {
        testcaseName = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());

        // initialer Import
        writer = new ArtifactWriter(testcaseName);
        String content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogOK.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);

        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        leistungskatalogMock.clear();

        // Import einer Leistung
        XzufiFile fileAddLage = XzufiFileCreator.newInstance().name(testcaseName).lb(1).oe(1).getMin();
        fileAddLage.getLeistungsbeschreibungen().getFirst().setReferenzLeiKa("99046048000000");
        String id = fileAddLage.getLeistungsbeschreibungen().getFirst().getFullId();
        ImportUtil.pushAndWait23(fileAddLage);

        String ars = fileAddLage.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();
        String queryString = fileAddLage.getLeistungsbeschreibungen().getFirst().getTextmodule().getFirst().getInhalt();
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

        Response serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("1150200"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsNotContainsId(id);

        writer = new ArtifactWriter(testcaseName);
        content = Files.readString(Paths.get(ClassLoader.getSystemResource("leistungskatalogUPDATE.xml").toURI()));

        leistungskatalogMock.registerMock(content, 200);
        // Trigger Import
        AdminConnector.updateLeistungskatalog();
        assertDoesNotThrow(
            () -> WaitUtil.waitForLeistungskatalogSuccessfulImported(writer),
            "erwartete Meldungen sind nicht im Server log aufgetaucht"
        );

        // warten auf Indexupdate
        WaitUtil.waitFor(1, TimeUnit.SECONDS);
        serviceDescResponse = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            null,
            true
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponse);
        validator
            .personalMattersFirstLevelContainsCode("1000000")
            .personalMattersSecondLevelContainsCode("1150000")
            .personalMattersThirdLevelContainsCode("1150200")
            .personalMattersFirstLevelContainsName("Lebenslagen für Bürgerinnen und Bürger")
            .personalMattersSecondLevelContainsNameByCode("Recht und Verbraucherschutz", "1150000")
            .personalMattersThirdLevelContainsNameByCode("Gerichtliche Verfahren, Anzeige und Klage", "1150200");
        validator.serviceDescriptionsContainsId(id);

        serviceDescResponseByLage = ServiceDescriptionConnector.getServiceDescriptionsV8(
            ars,
            List.of(queryString),

            List.of("1150200"),
            false
        );
        validator = new ServiceDescriptionResultListValidator(serviceDescResponseByLage);
        validator.personalMattersIsEmpty().serviceDescriptionsContainsId(id);
    }
}
