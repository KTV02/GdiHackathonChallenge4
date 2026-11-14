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
public class TextmodulValidator extends DetailValidator<TextmodulValidator> {

    public TextmodulValidator(Response response) {
        super(response, "TEXT_MODUL");
    }

    public TextmodulValidator(Response response, boolean skipPlaceholder) {
        super(response, "TEXT_MODUL", skipPlaceholder);
    }
}
