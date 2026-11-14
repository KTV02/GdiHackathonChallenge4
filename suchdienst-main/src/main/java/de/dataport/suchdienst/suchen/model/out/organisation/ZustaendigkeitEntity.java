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
package de.dataport.suchdienst.suchen.model.out.organisation;

import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriod;
import de.dataport.suchdienst.suchen.model.out.zustaendigkeit.RelationTyp;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.Value;

/**
 * @deprecated ACHTUNG: Kopie existiert nur zur Abwärtskompatibilität der OpenAPI Spec.
 * Modell wird nicht komplett gefüllt und sollte mit der nächsten API-Version bereingit werden!
 * Die mit "null" vorbelegten Werte werden nicht gesetzt. Sie sind nur aufgeführt um den OpenAPI Kontrakt zu erhalten
 */
@Deprecated(forRemoval = true)
@Value
public class ZustaendigkeitEntity {

    String id;
    String lbId;
    String ars;
    /**
     * Der Zeitpunkt des neuesten Upserts.
     */
    Instant lastUpdate;

    // wird nicht gesetzt
    @Setter(AccessLevel.NONE)
    String parentId = null;

    // wird nicht gesetzt
    @Setter(AccessLevel.NONE)
    ResponsibilityRole responsibility = null;

    // wird nicht gesetzt
    @Setter(AccessLevel.NONE)
    RelationTyp typ = null;

    // wird nicht gesetzt
    @Setter(AccessLevel.NONE)
    String arsFederalState = null;

    // wird nicht gesetzt
    @Setter(AccessLevel.NONE)
    List<ValidityPeriod> validityPeriods = null;
}
