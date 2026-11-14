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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv6;

import io.restassured.response.Response;

public class FachlicheFreigabeModulValidator extends SingleDetailValidator<FachlicheFreigabeModulValidator> {

    public FachlicheFreigabeModulValidator(Response response, boolean skipPlaceholder) {
        super(response, "FACHLICHE_FREIGABE", skipPlaceholder);
    }
}
