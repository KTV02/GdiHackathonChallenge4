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
package suchdienst.integrationstest.tests.monitoring;

import static org.hamcrest.Matchers.is;

import com.google.common.collect.Iterables;
import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.XzufiGenerator;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import de.dataport.pvog.testdatengenerator.model.Zustaendigkeit;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.TestUtil;
import suchdienst.integrationstest.connectors.MonitoringConnector;
import suchdienst.integrationstest.validator.monitoring.MonitoringValidator;

/**
 * Testziel: Prueft die Grundfunktionalität der Monitoring-API auf das Setzen richtiger Werte und dem Abruf über mehrere Requests hinweg.
 */
class MonitoringTest {

    private static XzufiFile file;
    private static SaDMessage lbspzsMessage1;
    private static SaDMessage oeodMessage2;
    private static SaDMessage lbspzsMessage2;

    @BeforeAll
    static void setup(TestInfo testInfo) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        String testclassname = TestUtil.addTimestamp(testInfo.getTestClass().orElseThrow().getSimpleName());
        file = XzufiFileCreator.newInstance().name(testclassname).lb(2).oe(2).od(2).fo(2).sp(1).getMin();
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(file);

        // Es müssen 4 Nachrichten generiert werden, damit die Testvorrausetzung erfüllt ist.
        assert messages.size() == 4;

        // Erstes Element mit "oeodMessage1" wird ignoriert. Wird versendet aber im Suchdienst überschrieben
        lbspzsMessage1 = messages.get(1);
        oeodMessage2 = messages.get(2);
        lbspzsMessage2 = messages.get(3);

        ImportUtil.push(messages);
        ImportUtil.waitForImport(lbspzsMessage2.getNachrichtenId());
    }

    /**
     * Testet den Fall, dass für eine SchemeAgencyId kein Import läuft.
     * Um sicherzustellen, dass dieser Test nicht mit parallelen Imports kollidiert, wird eine eindeutige SchemeAgencyId verwendet.
     * Den Fall, dass ein Import aktiv ist, können wir hier leider nicht testen, da es keine Möglichkeit gibt, einen Import gezielt lange laufen zu lassen
     * und währrenddessen die Monitoring-API abzufragen.
     */
    @Test
    void monitorIsImportActive(TestInfo testInfo) {
        String schemeAgencyId = testInfo.getTestMethod().orElseThrow().getName() + "schemeAgencyId";
        MonitoringConnector.isImportActive(schemeAgencyId).then().contentType(ContentType.JSON).body(is("false"));
    }

    @Test
    void monitorLb() {
        Response response = MonitoringConnector.getBo("lb");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = file.getLeistungsbeschreibungen().get(0).getFullId();
        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, lbspzsMessage1.getTransaktionsId())
            .validateMessageId(boId1, lbspzsMessage1.getNachrichtenId())
            .validateTransactionPosition(boId1, lbspzsMessage1.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = file.getLeistungsbeschreibungen().get(1).getFullId();
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, lbspzsMessage2.getTransaktionsId())
            .validateMessageId(boId2, lbspzsMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, lbspzsMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorLbPaging() {
        String boIdLFirst = file.getLeistungsbeschreibungen().getFirst().getFullId();
        String boIdLast = Iterables.getLast(file.getLeistungsbeschreibungen()).getFullId();
        Response response = MonitoringConnector.getBo("lb", 1, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorOe() {
        Response response = MonitoringConnector.getBo("oe");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = file.getOrganisationseinheiten().get(0).getFullId();
        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId1, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId1, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = file.getOrganisationseinheiten().get(1).getFullId();
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId2, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorOePaging() {
        String boIdLFirst = file.getOrganisationseinheiten().getFirst().getFullId();
        String boIdLast = Iterables.getLast(file.getOrganisationseinheiten()).getFullId();
        Response response = MonitoringConnector.getBo("oe", 1, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorZsOe() {
        Response response = MonitoringConnector.getBo("zsoe");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = buildIdZsOe(file.getZustaendigkeitenOrganisationseinheit().get(0));

        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, lbspzsMessage1.getTransaktionsId())
            .validateMessageId(boId1, lbspzsMessage1.getNachrichtenId())
            .validateTransactionPosition(boId1, lbspzsMessage1.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = buildIdZsOe(file.getZustaendigkeitenOrganisationseinheit().get(1));
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, lbspzsMessage2.getTransaktionsId())
            .validateMessageId(boId2, lbspzsMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, lbspzsMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorZsOePaging() {
        String boIdLFirst = buildIdZsOe(file.getZustaendigkeitenOrganisationseinheit().getFirst());
        String boIdLast = buildIdZsOe(Iterables.getLast(file.getZustaendigkeitenOrganisationseinheit()));
        Response response = MonitoringConnector.getBo("zsoe", 3, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorOd() {
        Response response = MonitoringConnector.getBo("od");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = file.getOnlinedienste().get(0).getFullId();
        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId1, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId1, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = file.getOnlinedienste().get(1).getFullId();
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId2, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorOdPaging() {
        String boIdLFirst = file.getOnlinedienste().getFirst().getFullId();
        String boIdLast = Iterables.getLast(file.getOnlinedienste()).getFullId();
        Response response = MonitoringConnector.getBo("od", 1, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorZsOd() {
        Response response = MonitoringConnector.getBo("zsod");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = buildIdZsOd(file.getZustaendigkeitenOnlinedienst().get(0));

        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, lbspzsMessage1.getTransaktionsId())
            .validateMessageId(boId1, lbspzsMessage1.getNachrichtenId())
            .validateTransactionPosition(boId1, lbspzsMessage1.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = buildIdZsOd(file.getZustaendigkeitenOnlinedienst().get(1));
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, lbspzsMessage2.getTransaktionsId())
            .validateMessageId(boId2, lbspzsMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, lbspzsMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorZsOdPaging() {
        String boIdLFirst = buildIdZsOd(file.getZustaendigkeitenOnlinedienst().getFirst());
        String boIdLast = buildIdZsOd(Iterables.getLast(file.getZustaendigkeitenOnlinedienst()));
        Response response = MonitoringConnector.getBo("zsod", 3, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorFo() {
        Response response = MonitoringConnector.getBo("fo");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = file.getFormulare().get(0).getFullId();
        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId1, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId1, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = file.getFormulare().get(1).getFullId();
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, oeodMessage2.getTransaktionsId())
            .validateMessageId(boId2, oeodMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, oeodMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorFoPaging() {
        String boIdLFirst = file.getFormulare().getFirst().getFullId();
        String boIdLast = Iterables.getLast(file.getFormulare()).getFullId();
        Response response = MonitoringConnector.getBo("fo", 1, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorZsFo() {
        Response response = MonitoringConnector.getBo("zsfo");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = buildIdZsFo(file.getZustaendigkeitenFormular().getFirst());

        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, lbspzsMessage1.getTransaktionsId())
            .validateMessageId(boId1, lbspzsMessage1.getNachrichtenId())
            .validateTransactionPosition(boId1, lbspzsMessage1.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = buildIdZsFo(file.getZustaendigkeitenFormular().get(1));
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, lbspzsMessage2.getTransaktionsId())
            .validateMessageId(boId2, lbspzsMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, lbspzsMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorZsFoPaging() {
        String boIdLFirst = buildIdZsFo(file.getZustaendigkeitenFormular().getFirst());
        String boIdLast = buildIdZsFo(Iterables.getLast(file.getZustaendigkeitenFormular()));
        Response response = MonitoringConnector.getBo("zsfo", 3, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @Test
    void monitorSp() {
        Response response = MonitoringConnector.getBo("sp");
        MonitoringValidator validator = new MonitoringValidator(response);

        String boId1 = file.getSpezialisierungen().get(0).getFullId();
        validator
            .validateContainsId(boId1)
            .validateTransactionId(boId1, lbspzsMessage1.getTransaktionsId())
            .validateMessageId(boId1, lbspzsMessage1.getNachrichtenId())
            .validateTransactionPosition(boId1, lbspzsMessage1.getTransaktionsPosition())
            .validateMetadataFormat(boId1);

        String boId2 = file.getSpezialisierungen().get(1).getFullId();
        validator
            .validateContainsId(boId2)
            .validateTransactionId(boId2, lbspzsMessage2.getTransaktionsId())
            .validateMessageId(boId2, lbspzsMessage2.getNachrichtenId())
            .validateTransactionPosition(boId2, lbspzsMessage2.getTransaktionsPosition())
            .validateMetadataFormat(boId2);
    }

    @Test
    void monitorSpPaging() {
        String boIdLFirst = file.getSpezialisierungen().getFirst().getFullId();
        String boIdLast = Iterables.getLast(file.getSpezialisierungen()).getFullId();
        Response response = MonitoringConnector.getBo("sp", 1, boIdLFirst);
        new MonitoringValidator(response).validateLastId(boIdLast).validateContainsId(boIdLast);
    }

    @DisplayName("monitorOktSize100")
    @ParameterizedTest(name = "{displayName} {0}")
    @ValueSource(strings = { "lb", "oe", "od", "sp" })
    void monitorOktSize100(String bo) {
        MonitoringConnector.getBo(bo, 100, null)
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_OK)
            .and()
            .contentType(ContentType.JSON);
    }

    private String buildIdZsOe(Zustaendigkeit zs) {
        return buildIdZs("ZSOE", zs);
    }

    private String buildIdZsOd(Zustaendigkeit zs) {
        return buildIdZs("ZSOD", zs);
    }

    private String buildIdZsFo(Zustaendigkeit zs) {
        return buildIdZs("ZSFO", zs);
    }

    private String buildIdZs(String zsIdDiscriminator, Zustaendigkeit zs) {
        return (
            zs.getSchemeAgencyId() +
            "." +
            zsIdDiscriminator +
            "." +
            zs.getLeistungIDs().getFirst() +
            "." +
            zs.getParentId() +
            "." +
            zs.getArss().getFirst()
        );
    }
}
