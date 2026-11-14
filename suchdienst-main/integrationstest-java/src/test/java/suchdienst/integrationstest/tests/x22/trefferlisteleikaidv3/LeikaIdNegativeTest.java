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
package suchdienst.integrationstest.tests.x22.trefferlisteleikaidv3;

import io.restassured.response.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import suchdienst.integrationstest.common.Constants;
import suchdienst.integrationstest.common.EnvironmentManagerExtension;
import suchdienst.integrationstest.common.LoggingExtension;
import suchdienst.integrationstest.connectors.ServiceDescriptionConnector;
import suchdienst.integrationstest.validator.ErrorValidator;

@ExtendWith({ EnvironmentManagerExtension.class, LoggingExtension.class })
@SuppressWarnings("java:S1192")
class LeikaIdNegativeTest {

    @Test
    void arsIsString() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3("P", "99001009000555");
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "muss mit \"[0-9]*\" übereinstimmen");
    }

    @Test
    void toLongArs() {
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "0977201841849",
            "99001009000555"
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("ars", "Größe muss zwischen 0 und 12 sein");
    }

    @Test
    void toBigSize() {
        List<String> leikaIds = Collections.singletonList("99001009000555");
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            0,
            10000,
            null,
            null,
            Constants.LANGUAGE_DE
        );

        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("size", "muss kleiner-gleich 9999 sein");
    }

    @Test
    void toSmallSize() {
        List<String> leikaIds = Collections.singletonList("99001009000555");
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            0,
            4,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("size", "muss größer-gleich 5 sein");
    }

    @Test
    void negativePage() {
        List<String> leikaIds = Collections.singletonList("99001009000555");
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            -1,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("page", "muss größer-gleich 0 sein");
    }

    @Test
    void toBigPage() {
        List<String> leikaIds = Collections.singletonList("99001009000555");
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");
        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            101,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("page", "muss kleiner-gleich 100 sein");
    }

    @Test
    void emptyQuery() {
        List<String> leikaIds = Collections.singletonList("99001009000555");
        List<String> singleQueryItem = Collections.singletonList("");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            0,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("q[0]", "Größe muss zwischen 3 und 2147483647 sein");
    }

    @Test
    void emptyLeikas() {
        List<String> leikaIds = Collections.singletonList("");
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            leikaIds,
            null,
            0,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest().validateField("leikaIds[0]", "Größe muss zwischen 14 und 21 sein");
    }

    @Test
    void missingRequiredParameter() {
        List<String> singleQueryItem = Collections.singletonList("ersterBegriffImKontext");
        List<String> leikaIds = Collections.singletonList("99001009000555");

        Response response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            "097720184184",
            singleQueryItem,
            null,
            null,
            0,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        ErrorValidator validator = new ErrorValidator(response);
        validator.validateBadRequest();
        response = ServiceDescriptionConnector.getServiceDescriptionsLeikaIdV3(
            null,
            singleQueryItem,
            leikaIds,
            null,
            0,
            10,
            null,
            null,
            Constants.LANGUAGE_DE
        );
        validator = new ErrorValidator(response);
        validator.validateBadRequest();
    }
}
