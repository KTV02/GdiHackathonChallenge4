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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
class CodelistBackofficeNegativeTest {

    @Test
    void missingPage() {
        Response response = AdminConnector.getBackofficeElsCodelists(null, 50);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @Test
    void missingSize() {
        Response response = AdminConnector.getBackofficeElsCodelists(0, null);
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void uploadError() throws IOException {
        File codelistFile = new File(
            getClass().getClassLoader().getResource("./upload/urn__de__xzufi__codeliste__fail_error.xml").getFile()
        );

        String codelistString = Files.readString(codelistFile.toPath()).replace("$PLACEHOLDER", "error");
        Response response = AdminConnector.uploadCodelist(codelistString);
        ErrorValidator errorValidator = new ErrorValidator(response);
        errorValidator
            .validateStatusCode(HttpStatus.SC_BAD_REQUEST)
            .validateValue("success", false)
            .validateValue("msg", "codelist_upload_import_error");
    }
}
