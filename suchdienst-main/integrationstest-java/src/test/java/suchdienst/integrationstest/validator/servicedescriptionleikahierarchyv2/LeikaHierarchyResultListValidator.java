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
package suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv2;

import io.restassured.response.Response;
import suchdienst.integrationstest.validator.ResultListValidator;

public class LeikaHierarchyResultListValidator extends ResultListValidator<LeikaHierarchyResultListValidator> {

    public LeikaHierarchyResultListValidator(Response response) {
        super(response);
    }
}
