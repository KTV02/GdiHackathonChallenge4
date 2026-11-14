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
package suchdienst.integrationstest.validator.servicedescriptionleikahierarchyv3;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import suchdienst.integrationstest.validator.ResultListValidator;

public class LeikaHierarchyResultListValidator extends ResultListValidator<LeikaHierarchyResultListValidator> {

    public LeikaHierarchyResultListValidator(Response response) {
        super(response, "serviceDescriptions.");
    }

    public LeikaHierarchyResultListValidator validatePage(int expectedNumber, int expectedSize, int expectedElements) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("serviceDescriptions.page.number", is(expectedNumber))
            .body("serviceDescriptions.page.size", is(expectedSize))
            .body("serviceDescriptions.page.totalElements", is(expectedElements));
        return this;
    }

    /**
     * Prüft, wie die Treffer zu den angefragten LeiKa-Schlüsseln ermittelt wurden.
     *
     * @param expectedMatchType         einer von EXACT, DESCENDANT, ANCESTOR, NONE
     * @param expectedEffectiveLeikaIds die vollständigen, 14-stelligen Schlüssel, für die die Treffer
     *                                  tatsächlich ermittelt wurden
     */
    public LeikaHierarchyResultListValidator validateLeikaMatch(
        String expectedMatchType,
        String... expectedEffectiveLeikaIds
    ) {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("leikaMatch.matchType", equalTo(expectedMatchType))
            .body("leikaMatch.effectiveLeikaIds", containsInAnyOrder(expectedEffectiveLeikaIds));
        return this;
    }

    /**
     * Prüft, dass an keiner Stelle der LeiKa-Hierarchie Treffer ermittelt werden konnten.
     */
    public LeikaHierarchyResultListValidator validateNoLeikaMatch() {
        then.statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .body("leikaMatch.matchType", equalTo("NONE"))
            .body("leikaMatch.effectiveLeikaIds", is(empty()));
        return this;
    }
}
