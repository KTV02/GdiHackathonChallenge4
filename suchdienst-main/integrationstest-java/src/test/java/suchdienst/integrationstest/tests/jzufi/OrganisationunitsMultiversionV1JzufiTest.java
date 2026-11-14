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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22FormularGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22OrganisationseinheitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi22.Xzufi22ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23FormularGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23OrganisationseinheitsGenerator;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23ZustaendigkeitsGenerator;
import de.dataport.pvog.testdatengenerator.model.Bankverbindung;
import de.dataport.pvog.testdatengenerator.model.Organisationseinheit;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/**
 * Mit den Tests soll die Funktionalität der Endpunkte /v1/organisationunits/{id}/jzufi-2-2 und /v1/organisationunits/{id}/jzufi-2-3 sichergestellt werden.
 */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationunitsMultiversionV1JzufiTest {

    @Test
    void checkJsonSchemaTest(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();

        ImportUtil.pushAndWait23(file);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufi231V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufi220V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );

        // Validate
        String endpoint22 = "/v1/organisationunits/{id}/jzufi-2-2";
        String endpoint23 = "/v1/organisationunits/{id}/jzufi-2-3";
        OpenApiSchemaValidator.validate(response22, endpoint22, "GET");
        OpenApiSchemaValidator.validate(response23, endpoint23, "GET");
    }

    @Test
    void compareOrganisationseinheit(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();
        CodelistConfig22 codelistConfig22 = new CodelistConfig22();
        CodelistConfig23 codelistConfig23 = new CodelistConfig23();

        OrganisationseinheitErweitert expected22 = new Xzufi22OrganisationseinheitsGenerator(
            codelistConfig22,
            new Xzufi22FormularGenerator(codelistConfig22, new Xzufi22ZustaendigkeitsGenerator(codelistConfig22))
        ).generateOrganisationseinheit(file.getOrganisationseinheiten().getFirst());

        de.xoev.xzufi.v2_3_1.Organisationseinheit expected23 = new Xzufi23OrganisationseinheitsGenerator(
            codelistConfig23,
            new Xzufi23FormularGenerator(codelistConfig23, new Xzufi23ZustaendigkeitsGenerator(codelistConfig23))
        ).generateOrganisationseinheit(file.getOrganisationseinheiten().getFirst());

        // Daten Einspielen
        ImportUtil.pushAndWait23(file, codelistConfig23);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufi231V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufi220V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );

        // Validierung
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert actual23 = response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .body()
            .as(de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class);
        assertEquals(expected23, actual23);

        OrganisationseinheitErweitert actuall22 = response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .extract()
            .body()
            .as(OrganisationseinheitErweitert.class);
        assertEquals(expected22, actuall22);
    }

    @Test
    void checkSpecificJsonPaths(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();

        // Bankverbindungen
        Bankverbindung verbindugn1 = Bankverbindung.builder()
            // Setze nur zwei Properties in der Bankverbindung, um das Verhalten der Ausgabe von Null-Werte zu testen
            .empfaenger("Timon Testreich")
            .iban("DE81700500000008393621")
            .build();
        file.getOrganisationseinheiten().getFirst().setBankverbindungen(List.of(verbindugn1));

        ImportUtil.pushAndWait23(file);
        Response response23 = OrganisationunitsConnector.getOrganisationunitsJzufi231V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );
        Response response22 = OrganisationunitsConnector.getOrganisationunitsJzufi220V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );

        // Validate
        response22
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .body("id.value", equalTo(file.getOrganisationseinheiten().getFirst().getId()))
            .body("id.schemeAgencyID", equalTo(file.getOrganisationseinheiten().getFirst().getSchemeAgencyId()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("bankverbindung[0]", not(hasKey("bic")))
            .body("bankverbindung[0]", not(hasKey("bankinstitut")))
            .body("bankverbindung[0]", not(hasKey("verwendungszweck")))
            .body("bankverbindung[0].size()", is(2))
            .body("bankverbindung[0].empfaenger", equalTo(verbindugn1.getEmpfaenger()))
            .body("bankverbindung[0].iban", equalTo(verbindugn1.getIban()));

        response23
            .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType("application/json")
            .body("id.value", equalTo(file.getOrganisationseinheiten().getFirst().getId()))
            .body("id.schemeAgencyID", equalTo(file.getOrganisationseinheiten().getFirst().getSchemeAgencyId()))
            .body("bankverbindung[0].size()", is(2))
            .body("bankverbindung[0].empfaenger", equalTo(verbindugn1.getEmpfaenger()))
            .body("bankverbindung[0].iban", equalTo(verbindugn1.getIban()))
            // Stellt sicher, dass Werte, die im PV_Schema nicht definiert (null) sind, nicht im JSON ausgeliefert werden
            .body("bankverbindung[0]", not(hasKey("bic")))
            .body("bankverbindung[0]", not(hasKey("bankinstitut")))
            .body("bankverbindung[0]", not(hasKey("verwendungszweck")));
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
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufi220V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(OrganisationseinheitErweitert.class),
            "Übermittelte Organisationseinheit passt nicht zum XZuFi 2.2 Datenmodell"
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
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMax();

        // Daten als XZuFi 2.2 Senden
        ImportUtil.pushAndWait(file);
        // Daten als XZuFi 2.3 Abrufen
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufi231V1(
            file.getOrganisationseinheiten().getFirst().getFullId()
        );

        // Validate
        assertDoesNotThrow(
            () ->
                response
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON)
                    .extract()
                    .as(de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert.class),
            "Übermittelte Organisationseinheit passt nicht zum XZuFi 2.3 Datenmodell"
        );
    }

    @Test
    void validdateFiltersOrganisationseinheit(TestInfo testInfo) {
        // Setup
        String testname = TestUtil.addTimestamp(testInfo.getTestMethod().orElseThrow().getName());
        XzufiFile file = XzufiFileCreator.newInstance().name(testname).lb(1).oe(1).getMin();
        Organisationseinheit oe = file.getOrganisationseinheiten().getFirst();
        oe.getGueltigkeiten().clear();
        oe.getGueltigkeiten().add(
            Zeitraum.builder()
                .beginn(LocalDate.of(2020, 8, 1))
                .ende(LocalDate.of(2080, 8, 1))
                .build()
        );

        // Daten Einspielen
        ImportUtil.pushAndWait23(file);

        // Validierung: Gültigkeitsdatum innerhalb des Zeitraums liefert einen Treffer
        OrganisationunitsConnector.getOrganisationunitsJzufi231V1(oe.getFullId(), LocalDate.of(2050, 1, 1))
            .then()
            .statusCode(HttpStatus.SC_OK);
        OrganisationunitsConnector.getOrganisationunitsJzufi220V1(oe.getFullId(), LocalDate.of(2050, 1, 1))
            .then()
            .statusCode(HttpStatus.SC_OK);

        // Validierung: Gültigkeitsdatum außerhalb des Zeitraums liefert kein Ergebnis
        Response response23OutOfRange = OrganisationunitsConnector.getOrganisationunitsJzufi231V1(
            oe.getFullId(),
            LocalDate.of(2090, 1, 1)
        );
        new ErrorValidator(response23OutOfRange).validateNotFound();

        Response response22OutOfRange = OrganisationunitsConnector.getOrganisationunitsJzufi220V1(
            oe.getFullId(),
            LocalDate.of(2090, 1, 1)
        );
        new ErrorValidator(response22OutOfRange).validateNotFound();
    }

    // Negativ-Tests

    @Test
    void unknownId() {
        Response response = OrganisationunitsConnector.getOrganisationunitsJzufi231V1("unknowId");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }
}
