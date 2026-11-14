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
package de.dataport.suchdienst.taskmgr.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Asynchroner Task im Kontext einer SchemeAgencyId.
 * Wir verwenden hier NICHT die Enumeration "SchemeAgancyId", weil zukünftig auch ad hoc Daten für neue, von der Enum nicht abgedeckte,
 * Werte importiert werden.
 * Die Konvention für den SudTaskMgr ist, dass zwei Tasks mit derselben SchemeAgencyId nicht parallel ausgeführt werden dürfen.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class SchemeAgencyIdTask<T> extends SudTask<T> {

    protected final String schemeAgencyId;

    protected final String producer;

    protected SchemeAgencyIdTask(SudTaskType taskType, String schemeAgencyId, String producer) {
        super(taskType);
        this.schemeAgencyId = schemeAgencyId;
        this.producer = producer;
    }
}
