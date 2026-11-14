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
import suchdienst.integrationstest.validator.admin.CodelistEntryByIdValidator;
import suchdienst.integrationstest.validator.admin.CodelistEntryValidator;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class CodelistEntryTest {

    private static final String KNOWN_URI = "urn:de:xzufi:codeliste:leistungstextmodulleika";
    private static final String KNOWN_VERSION = "20201201";

    private static final String KNOWN_ID = "urn:de:xzufi:codeliste:leistungstextmodulleika_20201201_02";

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
    void entryByUriAndVersion() {
        Response response = AdminConnector.getCodelistEntry(KNOWN_URI, KNOWN_VERSION);
        CodelistEntryValidator validator = new CodelistEntryValidator(response);
        validator.validateCount(16).validateId(0, KNOWN_ID).validateCode(0, "02");

        String existingUriButNotFittingToVersion = "urn:xoev-de:fim:codeliste:xzufi.begriffimkontexttyp";
        response = AdminConnector.getCodelistEntry(existingUriButNotFittingToVersion, KNOWN_VERSION);
        validator = new CodelistEntryValidator(response);
        validator.validateCount(0);

        String existingVersionButNotFittingToUri = "1.0";
        response = AdminConnector.getCodelistEntry(KNOWN_URI, existingVersionButNotFittingToUri);
        validator = new CodelistEntryValidator(response);
        validator.validateCount(0);
    }

    @Test
    void entryById() {
        Response response = AdminConnector.getCodelistEntryById(KNOWN_ID);
        CodelistEntryByIdValidator validator = new CodelistEntryByIdValidator(response);
        //only 1 entry will be returned
        validator.validateUri(KNOWN_URI).validateVersion(KNOWN_VERSION);
    }

    @Test
    void downloadEntry() throws Exception {
        codelistMock.createLeistungstextmodulleikaEntryResponse();

        Response response = AdminConnector.downloadEntry(KNOWN_URI, KNOWN_VERSION);
        CodelistEntryValidator validator = new CodelistEntryValidator(response);
        validator.validateDownloadOk();
    }
}
