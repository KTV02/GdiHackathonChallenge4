/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

/**
 * Gebündelter Lookup-Kontext für die Verarbeitung eines OD-Import-Batches.
 *
 * <p>Enthält alle vorab aus Elasticsearch geladenen Zuständigkeits-, Leika- und
 * OE-Daten, die für den Aufbau der Onlinedienst-Relationen benötigt werden.
 * Wird einmalig pro Batch-Seite in {@code OdRelationImportService}
 * aufgebaut und als kompakter Parameter an den {@link OdRelationAssembler} übergeben.</p>
 *
 * @param oeStammdaten       OE-Stammdaten nach OE-ID
 * @param odZustaendigkeiten OD-Zuständigkeiten gruppiert nach OD-ID und LB-ID
 * @param oeZustaendigkeiten OE-Zuständigkeiten gruppiert nach LB-ID
 * @param leikas             Leistungskatalog-Schlüssel je LB-ID
 */
record BatchRelationContext(
    OeStammdatenLookup oeStammdaten,
    OdZustaendigkeitenLookup odZustaendigkeiten,
    OeZustaendigkeitenLookup oeZustaendigkeiten,
    LeikaLookup leikas
) {}
