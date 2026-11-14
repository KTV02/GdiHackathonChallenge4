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
package suchdienst.integrationstest.validator.servicedescriptionsleikaidv3;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.ResultListValidator;

public class LeikaIdResultListValidator extends ResultListValidator<LeikaIdResultListValidator> {

    public LeikaIdResultListValidator(Response response) {
        super(response);
    }

    public LeikaIdResultListValidator validateOrganisationsCount(int index, int expectedCount) {
        String path = "content[" + index + "].organisationIds.size()";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(path, is(expectedCount));
        return this;
    }

    public LeikaIdResultListValidator validateOrganisationsContains(int index, String oeId) {
        String path = "content[" + index + "].organisationIds";
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body(path, hasItem(oeId));
        return this;
    }

    public LeikaIdResultListValidator validateOrganisationsNotEmpty(int index) {
        String path = "content[" + index + "].organisationIds";
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body(path, is(not(empty())));
        return this;
    }
}
