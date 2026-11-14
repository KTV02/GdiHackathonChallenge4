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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static suchdienst.integrationstest.common.Constants.XzufiVersion.V2_2_0;
import static suchdienst.integrationstest.common.Constants.XzufiVersion.V2_3_1;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22LeistungsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23LeistungsGenerator;
import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.xoev.xzufi.v2_2_0.Leistung;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/**
 * Mit diesen Tests soll die Funktionaliaet des Endpunkts: /v3/servicedescriptions/jzufi sichergestellt werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ServicedescriptionsV3JzufiTest {

    private final CodelistConfig22 codelistConfig22 = new CodelistConfig22();
    private final CodelistConfig23 codelistConfig23 = new CodelistConfig23();

    @Test
    void compareLeistungen(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();

        Leistung expected22 = new Xzufi22LeistungsGenerator(codelistConfig22).generateLeistung(
            file.getLeistungsbeschreibungen().getFirst()
        );
        de.xoev.xzufi.v2_3_1.Leistung expected23 = new Xzufi23LeistungsGenerator(codelistConfig23).generateLeistung(
            file.getLeistungsbeschreibungen().getFirst()
        );

        // Daten Einspielen
        ImportUtil.pushAndWait23(file, codelistConfig23);
        Response response22 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_2_0
        );
        Response response23 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_3_1
        );

        // Validierung
        // Mappen und Vergleichen der Leistungen
        de.xoev.xzufi.v2_3_1.Leistung actual23 = response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.3.1")
            .extract()
            .body()
            .as(de.xoev.xzufi.v2_3_1.Leistung.class);
        assertEquals(expected23, actual23);
        // Vorsicht hier soll nicht der Konverter geprüft werden, für den Vergleich wird sich auf Id beschränkt.
        Leistung actual22 = response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.2.0")
            .extract()
            .body()
            .as(Leistung.class);
        assertEquals(expected22.getId(), actual22.getId());
    }

    @Test
    void checkJsonSchemaTest(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);
        Response response22 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_2_0
        );
        Response response23 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_3_1
        );

        // Validierung
        String endpoint = "/v3/servicedescriptions/jzufi";
        OpenApiSchemaValidator.validate(response22, endpoint, "GET");
        OpenApiSchemaValidator.validate(response23, endpoint, "GET");
    }

    @Test
    void checkSpecificJsonPaths(TestInfo testInfo) {
        // Setup
        String testcasename = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testcasename).lb(1).oe(1).getMin();
        // Suchbegriff setzten
        Leistungsbeschreibung lb = file.getLeistungsbeschreibungen().getFirst();
        Suchbegriff suchbegriff = Suchbegriff.builder().begriff("Ausweisdokument").build();
        lb.setSuchbegriffe(List.of(suchbegriff));
        lb.setVersionsinformation(
            Versionsinformation.builder()
                // Setze nur zwei Properties in der Versionsinformation, um das Verhalten der Ausgabe von Null-Werte zu testen
                .version("latest")
                .bearbeiter("john doe")
                .build()
        );

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);
        Response response23 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(lb.getFullId(), V2_3_1);
        Response response22 = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(lb.getFullId(), V2_2_0);

        // Validierung
        response23
            .then()
            .body("id", allOf(hasEntry("value", lb.getId()), hasEntry("schemeAgencyID", file.getSchemeAgencyId())))
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.3.1")
            .body("modulBegriffImKontext[0].begriffImKontext[0].begriff.value", equalTo(suchbegriff.getBegriff()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("versionsinformation.size()", is(2))
            .body("versionsinformation.version ", is("latest"))
            .body("versionsinformation.geaendertDurch ", is("john doe"))
            .body("versionsinformation", not(hasKey("erstelltDurch")));

        response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json;xzufi-version=2.2.0")
            .body("id", allOf(hasEntry("value", lb.getId()), hasEntry("schemeAgencyID", file.getSchemeAgencyId())))
            .body("modulBegriffImKontext[0].begriffImKontext[0].begriff.value", equalTo(suchbegriff.getBegriff()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("versionsinformation.size()", is(2))
            .body("versionsinformation.version ", is("latest"))
            .body("versionsinformation.geaendertDurch ", is("john doe"))
            .body("versionsinformation", not(hasKey("erstelltDurch")));
    }

    /**
     * Übermittle einen XZuFi 2.3 Datensatz und erwarte, dass er in XZuFi 2.2 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo22(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMax();

        // Daten als XZuFi 2.3 Senden
        ImportUtil.pushAndWait23(file);
        // Daten als XZuFi 2.2 Abrufen
        Response response = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_2_0
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_2_0.Leistung.class),
            "Übermittelte Leistung passt nicht zum XZuFi 2.2 Datenmodell"
        );
    }

    /**
     * Übermittle einen XZuFi 2.2 Datensatz und erwarte, dass er in XZuFi 2.3 Datensatz umgewandelt wird
     */
    @Test
    void testConverterTo23(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMax();

        // Daten als XZuFi 2.2 Senden
        ImportUtil.pushAndWait(file);
        // Daten als XZuFi 2.3 Abrufen
        Response response = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            file.getLeistungsbeschreibungen().getFirst().getFullId(),
            V2_3_1
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_3_1.Leistung.class),
            "Übermittelte Leistung passt nicht zum XZuFi 2.3 Datenmodell"
        );
    }

    // Negativ-Tests
    @Test
    void blankParameter() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3("", V2_3_1);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void unknownId() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3("unknowId", V2_3_1);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void maxLengthV2() {
        Response responseValidParam = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            "a".repeat(255),
            V2_3_1
        );
        ErrorValidator validator = new ErrorValidator(responseValidParam);
        validator.validateNotFound("Die angegebene Resource konnte nicht gefunden werden.");

        Response responseInvalidParam = ServiceDescriptionConnector.getServiceDescriptionsArsJzufiV3(
            "a".repeat(256),
            V2_3_1
        );
        ErrorValidator validatorInvalidParam = new ErrorValidator(responseInvalidParam);
        validatorInvalidParam.validateBadRequest();
    }
}
