/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.tests.jzufi;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.model.Link;
import de.dataport.pvog.testdatengenerator.model.Zeitraum;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.ImportUtil;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.OnlineserviceConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/** Testziel: Import, Konvertierung und fachliche Ausgabe von /v2/onlineservices/{id}/jzufi-2-3. */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OnlineservicesJzufiV2Test {

    private static final String PATH = "/v2/onlineservices/{id}/jzufi-2-3";

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void preservesImportedValues(boolean legacyImport, boolean rich, TestInfo info) {
        var creator = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).od(1);
        var file = rich ? creator.getMax() : creator.getMin();
        var service = file.getOnlinedienste().getFirst();
        service.setBezeichnung("Fachtest-Onlinedienst");
        service.setBezeichnungEnglisch("Functional online service");
        service.setLinks(
            List.of(
                Link.builder()
                    .titel("Antrag")
                    .titelEnglisch("Application")
                    .uri("https://example.org/antrag")
                    .uriEnglisch("https://example.org/application")
                    .build(),
                Link.builder().titel("Weiterer Antrag").uri("https://example.org/weiter").build()
            )
        );
        if (legacyImport) {
            ImportUtil.pushAndWait(file);
        } else {
            ImportUtil.pushAndWait23(file);
        }
        var response = OnlineserviceConnector.getOnlineservicesJzufi231V2(service.getFullId());
        response
            .then()
            .statusCode(200)
            .contentType("application/json")
            .header("X-PVOG-DEPRECATED", nullValue())
            .body("'@type'", equalTo("OnlinedienstErweitert"))
            .body("id.value", equalTo(service.getId()))
            .body("id.schemeAgencyID", equalTo(service.getSchemeAgencyId()))
            .body("bezeichnung.find { it.languageCode == 'de' }.value", equalTo("Fachtest-Onlinedienst"))
            .body("bezeichnung.find { it.languageCode == 'en' }.value", equalTo("Functional online service"))
            .body("link", hasSize(3))
            .body("link[0].link", equalTo("https://example.org/antrag"))
            .body("link[0].titel", equalTo("Antrag"))
            .body("link[1].link", equalTo("https://example.org/application"))
            .body("link[1].languageCode", equalTo("en"))
            .body("link[2].link", equalTo("https://example.org/weiter"));
        OpenApiSchemaValidator.validate(response, PATH, "GET");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void preservesVisibilityWithoutFiltering(boolean visible, TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).od(1).getMin();
        var service = file.getOnlinedienste().getFirst();
        service.setKennzeichenOeffentlichAnzeigen(visible);
        ImportUtil.pushAndWait23(file);
        OnlineserviceConnector.getOnlineservicesJzufi231V2(service.getFullId())
            .then()
            .statusCode(200)
            .body("id.value", equalTo(service.getId()))
            .body("kennzeichenOeffentlichAnzeigen", equalTo(visible));
    }

    @ParameterizedTest
    @ValueSource(ints = { -10, 0, 10 })
    void returnsDataRegardlessOfValidity(int yearOffset, TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).od(1).getMin();
        var service = file.getOnlinedienste().getFirst();
        var start = LocalDate.now().plusYears(yearOffset).minusMonths(1);
        var end = start.plusMonths(2);
        service.setGueltigkeiten(List.of(Zeitraum.builder().beginn(start).ende(end).build()));
        ImportUtil.pushAndWait23(file);
        OnlineserviceConnector.getOnlineservicesJzufi231V2(service.getFullId())
            .then()
            .statusCode(200)
            .body("id.value", equalTo(service.getId()))
            .body("gueltigkeit[0].beginn", equalTo(JzufiV2Fixtures.xmlDate(start)))
            .body("gueltigkeit[0].ende", equalTo(JzufiV2Fixtures.xmlDate(end)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 255 })
    void acceptsIdLengthButReportsMissingResource(int length) {
        new ErrorValidator(OnlineserviceConnector.getOnlineservicesJzufi231V2("x".repeat(length))).validateNotFound();
    }

    @Test
    void rejectsOverlongId() {
        new ErrorValidator(OnlineserviceConnector.getOnlineservicesJzufi231V2("x".repeat(256))).validateBadRequest();
    }
}
