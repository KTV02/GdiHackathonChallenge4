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

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;

/**
 * Bündelt einen {@link GenericOnlinedienst} mit den zugehörigen {@link TransactionMetadata}.
 *
 * @param metadata     Transaktionsmetadaten (Redaktionssystem, Transaktions-ID etc.)
 * @param onlinedienst Der gemappte Onlinedienst
 */
record OnlinedienstMitMetadaten(TransactionMetadata metadata, GenericOnlinedienst onlinedienst) {}
