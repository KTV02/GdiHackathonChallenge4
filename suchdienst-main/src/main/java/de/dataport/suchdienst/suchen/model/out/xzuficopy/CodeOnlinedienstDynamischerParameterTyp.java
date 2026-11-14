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
package de.dataport.suchdienst.suchen.model.out.xzuficopy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @deprecated Kopie der Klasse {@link de.xoev.xzufi.v2_2_0.CodeOnlinedienstDynamischerParameterTyp}!
 * Wurde angelegt zur Entkopplung der Daten-Modelle und soll mit wachsenden API-Versionen entfernt werden!
 */
@Deprecated(forRemoval = true)
@EqualsAndHashCode(callSuper = true)
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Code.OnlinedienstDynamischerParameterTyp",
    namespace = "http://xoev.de/schemata/xzufi/2_2_0"
)
public class CodeOnlinedienstDynamischerParameterTyp extends Code {}
