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
package suchdienst.integrationstest.utils;

import de.dataport.pvog.testdatengenerator.model.Leistungsbeschreibung;
import de.dataport.pvog.testdatengenerator.model.Textmodul;

public class FilterUtil {

    public static Textmodul findTextmodul(String leikaCode, Leistungsbeschreibung leistung) {
        return leistung
            .getTextmodule()
            .stream()
            .filter(lb -> leikaCode.equals(lb.getLeikaCode()))
            .findAny()
            .orElseThrow();
    }
}
