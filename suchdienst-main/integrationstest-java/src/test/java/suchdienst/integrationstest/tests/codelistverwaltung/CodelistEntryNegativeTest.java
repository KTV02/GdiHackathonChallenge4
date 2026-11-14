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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.mocks.CodelistMock;
import suchdienst.integrationstest.validator.ErrorValidator;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class CodelistEntryNegativeTest {

    private static final String KNOWN_URI = "urn:de:xzufi:codeliste:leistungstextmodulleika";
    private static final String KNOWN_VERSION = "20201201";

    private static CodelistMock codelistMock;

    @BeforeAll
    static void setup() {
        codelistMock = new CodelistMock();
    }

    @AfterEach
    void reset() {
        codelistMock.reset();
    }

    @Test
    void missingUri() {
        Response response = AdminConnector.getCodelistEntry(null, KNOWN_VERSION);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void wrongUriFormat() {
        Response response = AdminConnector.getCodelistEntry("https://[::1", KNOWN_VERSION);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void missingVersion() {
        Response response = AdminConnector.getCodelistEntry(KNOWN_URI, null);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void entryById() {
        String unknownId = "unknown";
        Response response = AdminConnector.getCodelistEntryById(unknownId);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound();
    }

    @Test
    void downloadEntry() {
        codelistMock.createUnknownCodelistEntryResponse();

        Response response = AdminConnector.downloadEntry("unknownuri", KNOWN_VERSION);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateNotFound(
            "Die Codeliste http://localhost:8003/api/xrepository/unknownuri_20201201/genericode wurde in xRepository nicht gefunden."
        );
    }
}
