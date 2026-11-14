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
import de.dataport.pvog.testdatengenerator.model.Bankverbindung;
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
import suchdienst.integrationstest.connectors.OrganisationunitsConnector;
import suchdienst.integrationstest.validator.ErrorValidator;
import suchdienst.integrationstest.validator.jzufi.OpenApiSchemaValidator;

/** Testziel: Import, Konvertierung und fachliche Ausgabe von /v2/organisationunits/{id}/jzufi-2-3. */
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class OrganisationunitsJzufiV2Test {

    private static final String PATH = "/v2/organisationunits/{id}/jzufi-2-3";

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void preservesImportedValues(boolean legacyImport, boolean rich, TestInfo info) {
        var creator = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1);
        var file = rich ? creator.getMax() : creator.getMin();
        var organisation = file.getOrganisationseinheiten().getFirst();
        organisation.setTitel("Fachtest-Behoerde");
        organisation.setTitelEnglisch("Functional test authority");
        organisation.setBankverbindungen(
            List.of(Bankverbindung.builder().empfaenger("Fachtest-Kasse").iban("DE81700500000008393621").build())
        );
        if (legacyImport) {
            ImportUtil.pushAndWait(file);
        } else {
            ImportUtil.pushAndWait23(file);
        }

        var response = OrganisationunitsConnector.getOrganisationunitsJzufi231V2(organisation.getFullId());
        response
            .then()
            .statusCode(200)
            .contentType("application/json")
            .header("X-PVOG-DEPRECATED", nullValue())
            .body("id.value", equalTo(organisation.getId()))
            .body("id.schemeAgencyID", equalTo(organisation.getSchemeAgencyId()))
            .body("name[0].name.find { it.languageCode == 'de' }.value", equalTo("Fachtest-Behoerde"))
            .body("name[0].name.find { it.languageCode == 'en' }.value", equalTo("Functional test authority"))
            .body("bankverbindung[0].empfaenger", equalTo("Fachtest-Kasse"))
            .body("bankverbindung[0].iban", equalTo("DE81700500000008393621"))
            .body("bankverbindung[0]", not(hasKey("bic")));
        if (organisation.getAnschriften().isEmpty()) {
            response.then().body("$", not(hasKey("anschrift")));
        } else {
            response.then().body("anschrift", hasSize(organisation.getAnschriften().size()));
            for (int i = 0; i < organisation.getAnschriften().size(); i++) {
                var address = organisation.getAnschriften().get(i);
                response
                    .then()
                    .body("anschrift[" + i + "].strasse", equalTo(address.getStrasse()))
                    .body("anschrift[" + i + "].hausnummer", equalTo(address.getHausnummer()))
                    .body("anschrift[" + i + "].postleitzahl", equalTo(address.getPostleitzahl()))
                    .body("anschrift[" + i + "].ort", equalTo(address.getOrt()));
            }
        }
        OpenApiSchemaValidator.validate(response, PATH, "GET");
    }

    @ParameterizedTest
    @ValueSource(ints = { -10, 0, 10 })
    void returnsDataRegardlessOfValidity(int yearOffset, TestInfo info) {
        var file = XzufiFileCreator.newInstance().name(JzufiV2Fixtures.name(info)).lb(1).oe(1).getMin();
        var organisation = file.getOrganisationseinheiten().getFirst();
        var start = LocalDate.now().plusYears(yearOffset).minusMonths(1);
        var end = start.plusMonths(2);
        organisation.setGueltigkeiten(List.of(Zeitraum.builder().beginn(start).ende(end).build()));
        ImportUtil.pushAndWait23(file);
        OrganisationunitsConnector.getOrganisationunitsJzufi231V2(organisation.getFullId())
            .then()
            .statusCode(200)
            .body("id.value", equalTo(organisation.getId()))
            .body("gueltigkeit[0].beginn", equalTo(JzufiV2Fixtures.xmlDate(start)))
            .body("gueltigkeit[0].ende", equalTo(JzufiV2Fixtures.xmlDate(end)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 255 })
    void acceptsIdLengthButReportsMissingResource(int length) {
        new ErrorValidator(
            OrganisationunitsConnector.getOrganisationunitsJzufi231V2("x".repeat(length))
        ).validateNotFound();
    }

    @Test
    void rejectsOverlongId() {
        new ErrorValidator(
            OrganisationunitsConnector.getOrganisationunitsJzufi231V2("x".repeat(256))
        ).validateBadRequest();
    }
}
