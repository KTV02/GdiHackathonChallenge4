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
package suchdienst.integrationstest.validator.organisationunitsdetailv5;

import io.restassured.response.Response;

public class OeffnungszeitValidator extends AdditionalInformationValidator<OeffnungszeitValidator> {

    public OeffnungszeitValidator(Response response) {
        super(response, "INFO_TIMES");
    }

    @Override
    protected OeffnungszeitValidator self() {
        return this;
    }
}
