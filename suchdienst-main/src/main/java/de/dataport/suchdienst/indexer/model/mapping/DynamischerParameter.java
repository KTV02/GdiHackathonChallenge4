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
package de.dataport.suchdienst.indexer.model.mapping;

/**
 * Vereinfachung des originalen XZuFi Modells für dynamischer Parameter in Onlinediensten
 * @see de.xoev.xzufi.v2_2_0.OnlinedienstDynamischerParameter
 */
public record DynamischerParameter(String code, String parameterName) {}
