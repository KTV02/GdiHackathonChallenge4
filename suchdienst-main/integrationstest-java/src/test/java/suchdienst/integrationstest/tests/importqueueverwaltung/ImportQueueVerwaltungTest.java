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
package suchdienst.integrationstest.tests.importqueueverwaltung;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ImportQueueConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.pushxzufi22queue.QueueValidator;

/**
 * Testziel:
 * Es gilt die Import Queue mit ihren Endpunkten zu testen.
 * Folgende endpunkte existieren:
 * <p>POST /queue/enqueue gibt als Rückgabe eine id zurück. Diese id kann man z.B. für die Endpunkte</p>
 * <p>DELETE /queue,</p>
 * <p>GET /queue/detail oder</p>
 * <p>nutzen.</p>
 *
 * <p>Der Endpunkt GET /queue hat zudem eine menge optionaler Parameter, mit der man die Queue filtern kann.</p>
 *
 * <p>Details dazu in suchdienst.core\src\main\java\de\dataport\suchdienst\importer\controller\PushImportResource.java</p>
 *
 * <p>Interne Logik:</p>
 * <p>Ein 'ImportRequest' (eine Element in der Queue) landet im Status 'OPEN' in der Queue. Ein Java Process pullt alle offenen Requests und setzt sie darauf hin auf 'DONE' und setzt noch den Zeitstempel 'requestProcessedAt'. Fehlerhafte Importe werden auf den Zustand 'ERROR' gesetzt.</p>
 */
@ExtendWith({ EnvironmentManagerExtension.class })
class ImportQueueVerwaltungTest {

    private static String importId;
    private static String transactionId;
    private static String messageId;

    @BeforeAll
    // Je nachdem, wann der Test ausgeführt wird, sind schon einige Einträge in der Queue. Wir fügen einen speziellen hinzu und arbeiten nur mit diesem, um unabhängig zu sein.
    static void setUp() throws URISyntaxException, IOException {
        //Testvoraussetzung
        Path xmlFile = Paths.get(ClassLoader.getSystemResource("tests/importqueueverwaltung/emptyContent.xml").toURI());
        String xmlString = Files.readString(xmlFile, StandardCharsets.UTF_8);

        transactionId = UUID.randomUUID().toString();
        messageId = UUID.randomUUID().toString();
        importId = ImportQueueConnector.pushXzufi(xmlString, messageId, transactionId, "3");

        await("Warten auf Warteschlange")
            .atMost(Duration.ofSeconds(180))
            .with()
            .pollInterval(Duration.ofMillis(1000))
            .until(
                () ->
                    ImportQueueConnector.getQueueStatus(Objects.requireNonNull(importId), "DONE").getInt(
                        "totalElements"
                    ),
                is(1)
            );
    }

    @Test
    void findByFilterMessageId() {
        Response response = ImportQueueConnector.getQueueStatus(null, messageId, null, null, null, null);
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterId() {
        Response response = ImportQueueConnector.getQueueStatus(importId, null, null, null, null, null);
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterTransactionId() {
        Response response = ImportQueueConnector.getQueueStatus(null, null, transactionId, null, null, null);
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterAcceptedAfter() {
        String requestAcceptedAt = ImportQueueConnector.getQueueStatus(importId, null, null, null, null, null)
            .jsonPath()
            .getString("content[0].requestAcceptedAt");
        LocalDateTime acceptedAfter = LocalDateTime.ofInstant(Instant.parse(requestAcceptedAt), ZoneId.of("UTC")).minus(
            1,
            ChronoUnit.MILLIS
        );
        String acceptedAfterString = acceptedAfter.toInstant(ZoneOffset.UTC).toString();

        Response response = ImportQueueConnector.getQueueStatus(importId, null, null, acceptedAfterString, null, null);
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterAcceptedBefore() {
        String requestAcceptedAt = ImportQueueConnector.getQueueStatus(importId, null, null, null, null, null)
            .jsonPath()
            .getString("content[0].requestAcceptedAt");
        LocalDateTime acceptedBefore = LocalDateTime.ofInstant(Instant.parse(requestAcceptedAt), ZoneId.of("UTC")).plus(
            1,
            ChronoUnit.MILLIS
        );
        String acceptedBeforeString = acceptedBefore.toInstant(ZoneOffset.UTC).toString();

        //Fuer den AcceptedBefore Fall muess die ImportID als zusätzliches Filter-Kriterium übergeben werden, da ansonsten die Antwortmenge nicht kontrollierbar ist.
        Response response = ImportQueueConnector.getQueueStatus(importId, null, null, null, acceptedBeforeString, null);
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterDone() {
        Response response = ImportQueueConnector.getQueueStatus(importId, null, null, null, null, "DONE");
        new QueueValidator(response).validateTotalElements(1).validateContains(importId);
    }

    @Test
    void findByFilterUnknownStatus() {
        Response response = ImportQueueConnector.getQueueStatus(null, null, null, null, null, "UNKNOWN");
        new ErrorValidator(response).validateBadRequest();
    }

    @Test
    void changeEntryStatus(TestInfo testInfo) {
        //Testvoraussetzung
        XzufiFile xzufiFile = XzufiFileCreator.newInstance()
            .name(TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName()))
            .lb(1)
            .oe(1)
            .getMin();
        String currentImportId = ImportUtil.push(xzufiFile);
        ImportUtil.waitForImport(currentImportId);

        // Aendern des Status
        ImportQueueConnector.updateStatus(currentImportId, "ERROR").then().statusCode(HttpStatus.SC_ACCEPTED);
        ImportUtil.waitForImport(currentImportId, "ERROR");

        // Validieren des Status
        Response response = ImportQueueConnector.getQueueStatus(currentImportId, null, null, null, null, "ERROR");
        new QueueValidator(response).validateTotalElements(1).validateContains(currentImportId);
    }

    @Test
    void downloadXzufiByToken() {
        Response requestTokenResponse = ImportQueueConnector.requestToken(importId);

        requestTokenResponse.then().statusCode(HttpStatus.SC_OK).contentType(ContentType.TEXT);

        String downloadToken = requestTokenResponse.body().print();
        assertThat(downloadToken, not(is(emptyOrNullString())));

        Response downLoadByTokenResponse = ImportQueueConnector.downLoadByToken(downloadToken);
        downLoadByTokenResponse
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.BINARY)
            .header("Content-Disposition", is("attachment; filename=\"xzufi_" + importId + ".xml\""));

        String start =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<xzufi:transfer.operation.040502 xmlns:xzufi=\"http://xoev.de/schemata/xzufi/2_2_0\" xmlns:ns3=\"http://www.opengis.net/gml/3.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" produktbezeichnung=\"\" produkthersteller=\"\" xzufiVersion=\"2.2.0\">";
        String end = "</xzufi:transfer.operation.040502>\n";

        String importRequest = new String(downLoadByTokenResponse.asByteArray());
        assertThat(importRequest, startsWith(start));
        assertThat(importRequest, endsWith(end));
    }

    @Test
    void deleteEntry(TestInfo testInfo) throws InterruptedException {
        //Testvoraussetzung
        XzufiFile xzufiFile = XzufiFileCreator.newInstance()
            .name(TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName()))
            .lb(1)
            .oe(1)
            .getMin();
        String currentImportId = ImportUtil.push(xzufiFile);
        ImportUtil.waitForImport(currentImportId);
        Response responseBeforeDelete = ImportQueueConnector.getQueueStatus(
            currentImportId,
            null,
            null,
            null,
            null,
            null
        );
        new QueueValidator(responseBeforeDelete).validateTotalElements(1).validateContains(currentImportId);

        //Löschen
        ImportQueueConnector.delete(currentImportId).then().statusCode(202);
        TimeUnit.SECONDS.sleep(5); // NOSONAR - das Lesen des queueStatus wird hier bewusst verzögert

        //Validieren
        Response responseAfterDelete = ImportQueueConnector.getQueueStatus(
            currentImportId,
            null,
            null,
            null,
            null,
            null
        );
        new QueueValidator(responseAfterDelete).validateTotalElements(0);
    }

    @Test
    void negativeImport() throws IOException, URISyntaxException {
        // Import für Negativ-Testfall
        Path xmlFileError = Paths.get(
            ClassLoader.getSystemResource("tests/importqueueverwaltung/badContent.xml").toURI()
        );
        String xmlErrorString = Files.readString(xmlFileError, StandardCharsets.UTF_8);
        String importIdError = ImportQueueConnector.pushXzufi(
            xmlErrorString,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "3"
        );

        ImportUtil.waitForImport(importIdError, "ERROR");
        Response response = ImportQueueConnector.getQueueStatus(importIdError, null, null, null, null, "ERROR");
        new QueueValidator(response).validateTotalElements(1).validateContains(importIdError);
    }
}
