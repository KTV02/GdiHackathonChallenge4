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
package suchdienst.integrationstest.common;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnvironmentManagerExtension implements BeforeAllCallback {

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        synchronized (EnvironmentManagerExtension.class) {
            // Einzigartiger schlüssel fuer alle Zugriffe auf diese Erweiterung
            String uniqueKey = this.getClass().getName();
            Object value = context.getRoot().getStore(GLOBAL).get(uniqueKey);
            if (value == null) {
                // Erster Test setzt den Wert um eine einmalige Ausführung zu garantieren
                context.getRoot().getStore(GLOBAL).put(uniqueKey, this);

                // Beginn des Test-Setup
                // Eine ARS-Datei ersetzt alle ARS-PLZ-Ort zuordnungen. Damit auch die PLZ-Datei.
                ImportUtil.resetArsData();
            }
        }
    }
}
