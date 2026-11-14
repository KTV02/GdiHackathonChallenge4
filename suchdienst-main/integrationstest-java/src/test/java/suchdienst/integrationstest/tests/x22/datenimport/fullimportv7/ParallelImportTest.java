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
package suchdienst.integrationstest.tests.x22.datenimport.fullimportv7;

import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.distributor.NoDistributor;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.OnlineServiceLinkValidator;
import suchdienst.integrationstest.validator.servicedescriptionsdetailv5.ServiceDescriptionValidator;

/**
 * Test-Ziel: Prüft die Verarbeitung von vielen parallelen Nachrichten aus unterschiedlichen Redaktionssystemen.
 * Die Nachrichten Typen (LB/ZS/SP und OE/OD) aus den Redaktionssystemen werden dabei gemischt übertragen.
 * Dies bedeutet, dass erst alle Bundesländer Daten zu LB/SP/ZS bekommen, bevor alle OE/OD versendet werden.
 * Der Suchdienst muss diese Nachrichten in seinem Task-Manager korrekt trennen und parallel Verarbeiten.
 * Der Suchdienst muss die relevanten Informationen zu einer Verwaltungsleistung "sammeln", da beide Nachrichten benötigt werden.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ParallelImportTest {

    private static ExecutorService executor;

    @BeforeAll
    static void setup() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterAll
    static void teardown() {
        executor.shutdown();
    }

    @Test
    void parallelImportMixPush() {
        NoDistributor distributor = new NoDistributor();

        // BundeslandConfigs initialisieren, keine Daten für den Bund
        //List.of(BundeslandConfig.Bundesland.BUND, BundeslandConfig.Bundesland.BE, BundeslandConfig.)
        List<BundeslandConfig> configs = Arrays.stream(BundeslandConfig.Bundesland.values())
            .filter(bl -> !BundeslandConfig.Bundesland.BUND.equals(bl))
            .map(BundeslandConfig::new)
            .toList();

        // Erzeugen eines XZUFI-Datensatzes pro Bundesland
        List<XzufiFile> files = configs
            .stream()
            .map(config ->
                XzufiFileCreator.newInstance(config).name(config.getKuerzel()).lb(2).oe(2).od(2).sp(1).getMax()
            )
            .toList();

        // Importieren der Daten
        List<CompletableFuture<Void>> futureImports = files
            .stream()
            .map(f ->
                CompletableFuture.runAsync(() -> {
                    // Wir importieren zuerst die LB/ZS/SP Nachricht in den Suchdienst
                    XzufiFile f1 = new XzufiFile();
                    f1.getLeistungsbeschreibungen().addAll(f.getLeistungsbeschreibungen());
                    f1.getZustaendigkeitenOrganisationseinheit().addAll(f.getZustaendigkeitenOrganisationseinheit());
                    f1.getZustaendigkeitenOnlinedienst().addAll(f.getZustaendigkeitenOnlinedienst());
                    f1.getSpezialisierungen().addAll(f.getSpezialisierungen());
                    ImportUtil.pushAndWait(f1, distributor);
                }, executor).thenRunAsync(() -> {
                    // Anschließend die OE/OD Nachricht, so garantieren wir eine Mischung der Nachrichten
                    // in der Queue
                    XzufiFile f2 = new XzufiFile();
                    f2.getOrganisationseinheiten().addAll(f.getOrganisationseinheiten());
                    f2.getOnlinedienste().addAll(f.getOnlinedienste());
                    ImportUtil.pushAndWait(f2, distributor);
                }, executor)
            )
            .toList();

        // Warten auf Import
        futureImports.forEach(CompletableFuture::join);

        // Validieren der Daten
        for (XzufiFile file : files) {
            String ars = file.getZustaendigkeitenOrganisationseinheit().getFirst().getArss().getFirst();

            String lbId1 = file.getLeistungsbeschreibungen().get(0).getFullId();
            String lbId2 = file.getLeistungsbeschreibungen().get(1).getFullId();

            String oeId1 = file.getOrganisationseinheiten().get(0).getFullId();
            String oeId2 = file.getOrganisationseinheiten().get(1).getFullId();

            String odId1 = file.getOnlinedienste().getFirst().getFullId();
            String odId2 = file.getOnlinedienste().getFirst().getFullId();

            // Test LB1
            Response response1 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId1);

            ServiceDescriptionValidator sdValidator = new ServiceDescriptionValidator(response1);
            sdValidator.validateId(lbId1).validateOrganisationIdsContains(oeId1).validateOrganisationIdsContains(oeId2);

            OnlineServiceLinkValidator odValidator = new OnlineServiceLinkValidator(response1);
            odValidator.validateHasId(odId1).validateHasId(odId2);

            // Test LB2
            Response response2 = ServiceDescriptionConnector.getServiceDescriptionsDetailV5(ars, lbId2);

            ServiceDescriptionValidator sdValidator2 = new ServiceDescriptionValidator(response2);
            sdValidator2
                .validateId(lbId2)
                .validateOrganisationIdsContains(oeId1)
                .validateOrganisationIdsContains(oeId2);

            OnlineServiceLinkValidator odValidator2 = new OnlineServiceLinkValidator(response2);
            odValidator2.validateHasId(odId1).validateHasId(odId2);
        }
    }
}
