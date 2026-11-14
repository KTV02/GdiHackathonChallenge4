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
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import de.dataport.pvog.testdatengenerator.creator.XzufiFileCreator;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.xzufi23.Xzufi23LeistungsGenerator;
import de.dataport.pvog.testdatengenerator.model.Suchbegriff;
import de.dataport.pvog.testdatengenerator.model.Versionsinformation;
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
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/** Testziel: Import, Konvertierung und fachliche Ausgabe von /v2/servicedescriptions/{id}/jzufi-2-3. */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class ServicedescriptionsJzufiV2Test {

    private static final String PATH = "/v2/servicedescriptions/{id}/jzufi-2-3";

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void preservesImportedValues(boolean legacyImport, boolean rich, TestInfo info) {
        var creator = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1);
        var file = rich ? creator.getMax() : creator.getMin();
        var service = file.getLeistungsbeschreibungen().getFirst();
        service.setSuchbegriffe(List.of(Suchbegriff.builder().begriff("Ausweisdokument").build()));
        service.setVersionsinformation(
            Versionsinformation.builder().version("fachtest-v2").bearbeiter("Fachredaktion").build()
        );
        var expected = new Xzufi23LeistungsGenerator(new CodelistConfig23()).generateLeistung(service);
        if (legacyImport) {
            ImportUtil.pushAndWait(file);
        } else {
            ImportUtil.pushAndWait23(file);
        }

        var response = ServiceDescriptionConnector.getServiceDescriptionsJzufi231V2(service.getFullId());
        response
            .then()
            .statusCode(200)
            .contentType("application/json")
            .header("X-PVOG-DEPRECATED", nullValue())
            .body("id.value", equalTo(service.getId()))
            .body("id.schemeAgencyID", equalTo(service.getSchemeAgencyId()))
            .body("referenzLeistungsschluessel[0].value", equalTo(service.getReferenzLeiKa()))
            .body("modulBegriffImKontext[0].begriffImKontext[0].begriff.value", equalTo("Ausweisdokument"))
            .body("modulText", hasSize(expected.getModulText().size()))
            .body("versionsinformation.version", equalTo("fachtest-v2"))
            .body("versionsinformation.geaendertDurch", equalTo("Fachredaktion"))
            .body("versionsinformation", not(hasKey("erstelltDurch")));
        for (var module : expected.getModulText()) {
            String modulePath =
                "modulText.find { it.leikaTextmodul.code == '" + module.getLeikaTextmodul().getCode() + "' }";
            response.then().body(modulePath + ".leikaTextmodul.code", equalTo(module.getLeikaTextmodul().getCode()));
            if (module.getInhalt().isEmpty()) {
                response.then().body(modulePath + ".inhalt", nullValue());
            }
            for (int index = 0; index < module.getInhalt().size(); index++) {
                var content = module.getInhalt().get(index);
                response
                    .then()
                    .body(modulePath + ".inhalt[" + index + "].value", equalTo(content.getValue()))
                    .body(modulePath + ".inhalt[" + index + "].languageCode", equalTo(content.getLanguageCode()));
            }
        }
        if (!rich) {
            for (var absentModule : List.of(
                "modulFrist",
                "modulBearbeitungsdauer",
                "modulKosten",
                "modulFachlicheFreigabe",
                "modulAuskunftshinweis"
            )) {
                response.then().body("$", not(hasKey(absentModule)));
            }
        }
        OpenApiSchemaValidator.validate(response, PATH, "GET");
    }

    @ParameterizedTest
    @ValueSource(ints = { -10, 0, 10 })
    void returnsDataRegardlessOfValidity(int yearOffset, TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1).getMin();
        var service = file.getLeistungsbeschreibungen().getFirst();
        var start = LocalDate.now().plusYears(yearOffset).minusMonths(1);
        var end = start.plusMonths(2);
        service.setGueltigkeiten(List.of(Zeitraum.builder().beginn(start).ende(end).build()));
        ImportUtil.pushAndWait23(file);
        ServiceDescriptionConnector.getServiceDescriptionsJzufi231V2(service.getFullId())
            .then()
            .statusCode(200)
            .body("id.value", equalTo(service.getId()))
            .body("gueltigkeit[0].beginn", equalTo(JzufiV2Fixtures.xmlDate(start)))
            .body("gueltigkeit[0].ende", equalTo(JzufiV2Fixtures.xmlDate(end)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 255 })
    void acceptsIdLengthButReportsMissingResource(int length) {
        new ErrorValidator(
            ServiceDescriptionConnector.getServiceDescriptionsJzufi231V2("x".repeat(length))
        ).validateNotFound();
    }

    @Test
    void rejectsOverlongId() {
        new ErrorValidator(
            ServiceDescriptionConnector.getServiceDescriptionsJzufi231V2("x".repeat(256))
        ).validateBadRequest();
    }
}
