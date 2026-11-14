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
package de.dataport.suchdienst.suchen.model.out.leistung;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Enthält eine Aggregation von Lebenslagen und Unterlagen, wird bei der Suche zusätzlich geliefert, siehe PV-8658.
 */
@Schema(description = "Ein Lebenslageneintrag")
@Data
@EqualsAndHashCode(callSuper = true)
public final class LifeSituationAggOutModel
    extends AbstractLifeSituationAggOutModel<LifeSituationAggOutModel> {}
