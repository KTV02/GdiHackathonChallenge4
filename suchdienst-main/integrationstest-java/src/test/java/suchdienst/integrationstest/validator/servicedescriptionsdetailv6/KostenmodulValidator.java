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
public class KostenmodulValidator extends SingleDetailValidator<KostenmodulValidator> {

    public KostenmodulValidator(Response response, boolean skipPlaceholder) {
        super(response, "KOSTEN", skipPlaceholder);
    }

    public KostenmodulValidator validateDetailsVorkasse(int index, String expected) {
        validateDetailsAdditionalInformationByKey(index, "Vorkasse", expected);
        return self();
    }

    public KostenmodulValidator validateDetailsLink(int index, String expectedUri) {
        validateDetailsLinkCount(index, 1);
        validateDetailsLinkUri(index, 0, expectedUri);
        validateDetailsLinkTitle(index, 0, "Kostenbildung");
        validateDetailsLinkDescription(index, 0, "Link zur URL, wo Kostenbildung näher erläutert oder berechnet wird.");
        return self();
    }
}
