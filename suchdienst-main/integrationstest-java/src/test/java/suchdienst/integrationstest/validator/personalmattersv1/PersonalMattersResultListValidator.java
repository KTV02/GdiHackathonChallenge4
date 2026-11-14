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
package suchdienst.integrationstest.validator.personalmattersv1;

import static org.hamcrest.Matchers.contains;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.ResultListValidator;

/**
 * PV-8448: Validierungen für Rückgabe von /v1/personalmatters/{ars}.
 * PV-9379: Validierung von Geschwisterreihenfolge mit <code>Matchers.contains</code> statt <code>Matchers.hasItems</code>.
 */
public final class PersonalMattersResultListValidator extends ResultListValidator<PersonalMattersResultListValidator> {

    public PersonalMattersResultListValidator(Response response) {
        super(response);
    }

    public PersonalMattersResultListValidator firstLevelExactly(String... expectedCodes) {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("code", contains(expectedCodes));
        return self();
    }

    public PersonalMattersResultListValidator secondLevelExactly(String level0Name, String... expectedCodes) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("find{it.code=='" + level0Name + "'}.children.code", contains(expectedCodes));
        return self();
    }

    public PersonalMattersResultListValidator secondLevelComments(String level0Code, String... expectedComments) {
        then.body("find{it.code=='" + level0Code + "'}.children.comment", contains(expectedComments));
        return self();
    }

    public PersonalMattersResultListValidator thirdLevelExactly(
        String level0Code,
        String level1Code,
        String... expectedCodes
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(
                "find{it.code=='" + level0Code + "'}.children.find{it.code=='" + level1Code + "'}.children.code",
                contains(expectedCodes)
            );
        return self();
    }

    public PersonalMattersResultListValidator thirdLevelComments(
        String level0Code,
        String level1Code,
        String... expectedComments
    ) {
        then.body(
            "find{it.code=='" + level0Code + "'}.children.find{it.code=='" + level1Code + "'}.children.comment",
            contains(expectedComments)
        );
        return self();
    }
}
