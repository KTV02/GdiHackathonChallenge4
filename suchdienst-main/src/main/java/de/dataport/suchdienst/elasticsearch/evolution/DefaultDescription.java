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
package de.dataport.suchdienst.elasticsearch.evolution;

import de.dataport.suchdienst.elasticsearch.api.AliasNames;

public interface DefaultDescription extends IndexUpdateDescription {
    // Relevant bei Verwaltungsleistungsindexen
    default String getAlias() {
        return null;
    }

    // Relevant bei Verwaltungsleistungsindexen
    default AliasNames getAbstractAliasName() {
        return null;
    }

    /**
     * Der Pfad zu der Datei, die die Struktur des neuen Index beschreibt. Wenn dies nicht benötigt wird,
     * weil bspw. ein Index gelöscht wird, dann einfach null zurückgeben.
     */
    String pathToMappingFile();

    /**
     * Der Pfad zu der Datei, die die Settings zu einem neuen Index erstellt. Wenn dies nicht benötigt
     * wird, dann einfach null zurückgeben.
     */
    default String pathToSettingsFile() {
        return null;
    }
}
