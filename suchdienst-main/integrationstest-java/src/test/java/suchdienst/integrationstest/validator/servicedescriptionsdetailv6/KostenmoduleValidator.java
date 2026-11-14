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
public class KostenmoduleValidator extends DetailValidator<KostenmoduleValidator> {

    public KostenmoduleValidator(Response response, boolean skipPlaceholder) {
        super(response, "KOSTEN", skipPlaceholder);
    }

    public KostenmoduleValidator validateDetailsVorkasse(int indexModul, int index, String expected) {
        validateDetailsAdditionalInformationByKey(indexModul, index, "Vorkasse", expected);
        return self();
    }

    public KostenmoduleValidator validateDetailsLink(int indexModul, int index, String expectedUri) {
        validateDetailsLinkCount(indexModul, index, 1);
        validateDetailsLinkUri(indexModul, index, 0, expectedUri);
        validateDetailsLinkTitle(indexModul, index, 0, "Kostenbildung");
        validateDetailsLinkDescription(
            indexModul,
            index,
            0,
            "Link zur URL, wo Kostenbildung näher erläutert oder berechnet wird."
        );
        return self();
    }
}
