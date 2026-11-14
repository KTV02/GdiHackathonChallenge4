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

@SuppressWarnings("UnusedReturnValue")
public class BearbeitungsdauermodulValidator extends SingleDetailValidator<BearbeitungsdauermodulValidator> {

    public BearbeitungsdauermodulValidator(Response response) {
        super(response, "BEARBEITUNGSDAUER");
    }
}
