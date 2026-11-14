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
package suchdienst.integrationstest.validator.servicedescriptionsleikaidv4;

import static org.hamcrest.Matchers.comparesEqualTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.ResultListValidator;

public class LeikaIdResultListValidator extends ResultListValidator<LeikaIdResultListValidator> {

    public LeikaIdResultListValidator(Response response) {
        super(response);
    }

    @Override
    public LeikaIdResultListValidator validateEmptyResultList() {
        then.statusCode(HttpStatus.SC_OK).contentType(ContentType.JSON).body("page.totalElements", comparesEqualTo(0));
        return this;
    }
}
