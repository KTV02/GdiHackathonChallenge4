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
package de.dataport.suchdienst.suchen.dynamicparams;

/**
 * Enthält die Werte und Datenstrukturen die man benötigt, um einen Onlinedienstlink um dynamische Parameter
 * zu erweitern.
 */
public record DynamicParamPair(String code, String parameterName, String parameterValue) {}
