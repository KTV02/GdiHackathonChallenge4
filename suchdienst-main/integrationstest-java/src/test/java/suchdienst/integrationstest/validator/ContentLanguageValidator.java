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
package suchdienst.integrationstest.validator;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.lang.reflect.Proxy;
import org.apache.http.HttpStatus;

public class ContentLanguageValidator {

    /**
     * Diese Klasse enthält nur statischen Code
     */
    private ContentLanguageValidator() {}

    /**
     * Verifiziert, dass response den Header "content-language" mit dem Wert lang enthält.
     */
    public static ValidatableResponse validateContentLanguage(ValidatableResponse response, String lang) {
        return response.header("content-language", lang);
    }

    private static boolean isSuccessful(int statusCode) {
        return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES;
    }

    /**
     * Erzeugt ein neues {@link Response}-Objekt, dessen {@link Response#then()}-Methode ein {@link ValidatableResponse} liefert, bei dem
     * bereits geprüft wird, ob der erwartete "content-language"-Header enthalten ist.
     * <p>
     * <b>ACHTUNG</b>: Damit die Verifizierung erfolgt, muss die Methode "then" aufgerufen werden.
     * <p>
     * Hinweise:
     * 1. Diese Methode bietet sich an, um an zentraler Stelle sicherzustellen, dass der "content-language"-Header
     *    geprüft wird, also z. B. in (Test-)Connectors.
     * 2. Es wird angenommen, dass der Suchdienst immer "de" und "en" unterstützt und außerdem de liefert, falls
     *    eine nicht unterstützte Sprache angefordert wurde
     */
    public static Response validateContentLanguage(Response response, String lang) {
        final String expectedLang = "en".equalsIgnoreCase(lang) ? "en" : "de";
        return (Response) Proxy.newProxyInstance(
            Response.class.getClassLoader(),
            new Class[] { Response.class },
            (proxy, method, args) -> {
                if (method.getName().equals("then") && isSuccessful(response.statusCode())) {
                    return validateContentLanguage(response.then(), expectedLang);
                }
                return method.invoke(response, args);
            }
        );
    }
}
