/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.model;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Repräsentiert ein Dokument im Index suchdienst_leistungskatalog, also einen Eintrag im Leistungskatalog.
 * Dieser wird mit PV-8429 vom FIM Portal heruntergeladen und mit der Lebenslagen-Codeliste verknüpft.
 * Die entstehende Struktur wird regelmäßig aktualisiert und dann beim Import von Leistungsbeschreibungen genutzt, um die
 * Zuordnung der Leistungsbeschreibung zu LL-Codes zu bestimmen.
 */
@Data
@AllArgsConstructor
public class LeikaEntry implements Identifiable {

    /**
     * Der Leika-Schlüssel, dem die Codelisten-Einträge zugeordnet sind
     */
    private String id;

    // Konvention: erste Listenelemente sind jeweils die CL-Einträge, danach folgen die hierarchisch übergeordneten.
    // Somit haben wir hier in jedem Element einen kompletten Pfad. Brauchen wir beim Import der LBs.
    private Set<List<CodelistKey>> codelistKeys;

    @Override
    public String toString() {
        var clString = codelistKeys
            .stream()
            .map(List::getFirst)
            .map(clKey -> String.format("%s(%s)", clKey.code(), clKey.version()))
            .collect(joining(", "));
        return String.format("%s -> {%s}", id, clString);
    }
}
