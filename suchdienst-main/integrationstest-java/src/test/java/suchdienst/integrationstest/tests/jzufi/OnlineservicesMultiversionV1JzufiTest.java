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
package suchdienst.integrationstest.tests.jzufi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22OnlinedienstGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23OnlinedienstGenerator;
import de.dataport.pvog.testdatengenerator.model.Onlinedienst;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/**
 * Mit den Tests soll die Funktionalität der Endpunkte /v1/onlineservices/{id}/jzufi-2-2 und /v1/onlineservices/{id}/jzufi-2-3 sichergestellt werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineservicesMultiversionV1JzufiTest {

    @Test
    void checkJsonSchemaTest(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).od(1).getMin();

        ImportUtil.pushAndWait23(file);
        Response response23 = OnlineserviceConnector.getOnlineservicesJzufi231V1(
            file.getOnlinedienste().getFirst().getFullId()
        );
        Response response22 = OnlineserviceConnector.getOnlineservicesJzufi220V1(
            file.getOnlinedienste().getFirst().getFullId()
        );

        // Validate
        String endpoint22 = "/v1/onlineservices/{id}/jzufi-2-2";
        String endpoint23 = "/v1/onlineservices/{id}/jzufi-2-3";
        OpenApiSchemaValidator.validate(response22, endpoint22, "GET");
        OpenApiSchemaValidator.validate(response23, endpoint23, "GET");
    }

    @Test
    void compareOnlinedienst(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).od(1).getMin();
        CodelistConfig22 codelistConfig22 = new CodelistConfig22();
        CodelistConfig23 codelistConfig23 = new CodelistConfig23();

        OnlinedienstErweitert expected22 = new Xzufi22OnlinedienstGenerator(codelistConfig22).generateOnlinedienst(
            file.getOnlinedienste().getFirst()
        );

        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert expected23 = new Xzufi23OnlinedienstGenerator(
            codelistConfig23
        ).generateOnlinedienst(file.getOnlinedienste().getFirst());

        // Daten Einspielen
        ImportUtil.pushAndWait23(file, codelistConfig23);
        Response response23 = OnlineserviceConnector.getOnlineservicesJzufi231V1(
            file.getOnlinedienste().getFirst().getFullId()
        );
        Response response22 = OnlineserviceConnector.getOnlineservicesJzufi220V1(
            file.getOnlinedienste().getFirst().getFullId()
        );

        // Validierung
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert actual23 = response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .body()
            .as(de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class);
        assertEquals(expected23, actual23);

        OnlinedienstErweitert actual22 = response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .body()
            .as(OnlinedienstErweitert.class);
        assertEquals(expected22, actual22);
    }

    /**
     * Übermittle einen XZuFi 2.3 Datensatz und erwarte, dass er in XZuFi 2.2 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo22(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).od(1).getMax();

        // Daten als XZuFi 2.3 Senden
        ImportUtil.pushAndWait23(file);
        // Daten als XZuFi 2.2 Abrufen
        Response response = OnlineserviceConnector.getOnlineservicesJzufi220V1(
            file.getOnlinedienste().getFirst().getFullId()
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(OnlinedienstErweitert.class),
            "Übermittelter Onlinedienst passt nicht zum XZuFi 2.2 Datenmodell"
        );
    }

    /**
     * Übermittle einen XZuFi 2.2 Datensatz und erwarte, dass er in XZuFi 2.3 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo23(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        // getMax wird benötigt, da die gefüllten Felder aus getMin von XZuFi 2.3.1 abwärtskompatibel zu XZuFi 2.2 sind.
        // Nur durch getMax entsteht ein Unterschied, der einen Fehlerfall provozieren würde.
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).od(1).getMax();

        // Daten als XZuFi 2.2 Senden
        ImportUtil.pushAndWait(file);
        // Daten als XZuFi 2.3 Abrufen
        Response response = OnlineserviceConnector.getOnlineservicesJzufi231V1(
            file.getOnlinedienste().getFirst().getFullId()
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class),
            "Übermittelter Onlinedienst passt nicht zum XZuFi 2.3 Datenmodell"
        );
    }

    @Test
    void validdateFiltersOnlinedienst(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).od(1).getMin();
        Onlinedienst od = file.getOnlinedienste().getFirst();
        od.getGueltigkeiten().clear();
        od.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2080, 8, 1))
                .build()
        );

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);

        // Validierung: Gültigkeitsdatum innerhalb des Zeitraums liefert einen Treffer
        OnlineserviceConnector.getOnlineservicesJzufi231V1(od.getFullId(), LocalDate.of(2050, 1, 1))
            .then()
            .statusCode(HttpStatus.SC_OK);
        OnlineserviceConnector.getOnlineservicesJzufi220V1(od.getFullId(), LocalDate.of(2050, 1, 1))
            .then()
            .statusCode(HttpStatus.SC_OK);

        // Validierung: Gültigkeitsdatum außerhalb des Zeitraums liefert kein Ergebnis
        Response response23OutOfRange = OnlineserviceConnector.getOnlineservicesJzufi231V1(
            od.getFullId(),
            LocalDate.of(2090, 1, 1)
        );
        new ErrorValidator(response23OutOfRange).validateNotFound();

        Response response22OutOfRange = OnlineserviceConnector.getOnlineservicesJzufi220V1(
            od.getFullId(),
            LocalDate.of(2090, 1, 1)
        );
        new ErrorValidator(response22OutOfRange).validateNotFound();
    }

    // Negativ-Tests

    @Test
    void unknownId() {
        Response response = OnlineserviceConnector.getOnlineservicesJzufi231V1("unknowId");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }
}
