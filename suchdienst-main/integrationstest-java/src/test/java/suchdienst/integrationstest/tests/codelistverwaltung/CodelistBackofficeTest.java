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
package suchdienst.integrationstest.tests.codelistverwaltung;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.validator.admin.BackofficeElsCodelistValidator;
import suchdienst.integrationstest.validator.admin.CodelistEntryValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class CodelistBackofficeTest {

    @Test
    void codelistsInitiallyLoaded() {
        Response response = AdminConnector.getBackofficeElsCodelists(0, 20);
        BackofficeElsCodelistValidator validator = new BackofficeElsCodelistValidator(response);
        validator.validateNotEmpty();
    }

    @Test
    void emptyContentGreatPageAndSize() {
        //page and size are syntactically correct but out of range for a result
        Response response = AdminConnector.getBackofficeElsCodelists(50, 50);
        BackofficeElsCodelistValidator validator = new BackofficeElsCodelistValidator(response);
        validator.validateContentCount(0);
    }

    @Test
    void pageHandling() {
        Response response = AdminConnector.getBackofficeElsCodelists(0, 20);
        int totalPages = response.jsonPath().getInt("totalPages");
        int totalElements = response.jsonPath().getInt("totalElements");
        BackofficeElsCodelistValidator validator = new BackofficeElsCodelistValidator(response);
        validator
            .validateFirst(true)
            .validateLast(false)
            .validateEmpty(false)
            .validatePageSize(20)
            .validatePageNumber(0)
            .validateContentCount(20)
            .validateNumberOfElements(20);
        //check next page - content count and numberOfElements should be equal
        response = AdminConnector.getBackofficeElsCodelists(1, 20);
        validator = new BackofficeElsCodelistValidator(response);
        validator
            .validateFirst(false)
            .validateLast(false)
            .validateEmpty(false)
            .validatePageSize(20)
            .validatePageNumber(1)
            .validateTotalElements(totalElements)
            .validateNumberOfElements(20)
            .validateContentCount(20);
        //check last page
        response = AdminConnector.getBackofficeElsCodelists(totalPages - 1, 20);
        validator = new BackofficeElsCodelistValidator(response);
        validator
            .validateFirst(false)
            .validateLast(true)
            .validateEmpty(false)
            .validatePageSize(20)
            .validatePageNumber(totalPages - 1)
            .validateNumberOfElements(totalElements % 20)
            .validateContentCount(totalElements % 20);
    }

    @Test
    void stats() {
        Response response = AdminConnector.getBackofficeElsCodelistsStats();
        CodelistEntryValidator validator = new CodelistEntryValidator(response);
        validator.validateStats();
    }
}
