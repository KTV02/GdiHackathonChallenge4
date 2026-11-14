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
package suchdienst.integrationstest.validator.servicedescriptionsdetailv5;

import io.restassured.response.Response;
import suchdienst.integrationstest.validator.LinklistValidator;

public class OnlineServiceLinkHelpLinkValidator extends LinklistValidator<OnlineServiceLinkHelpLinkValidator> {

    public OnlineServiceLinkHelpLinkValidator(Response response, int onlineserviceIndex) {
        super(response, "onlineServiceLinks" + "[" + onlineserviceIndex + "].helpLinks");
    }
}
