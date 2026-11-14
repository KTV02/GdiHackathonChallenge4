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

import de.dataport.suchdienst.util.Bundeslaender;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Asynchroner Task mit Bundesland-Kontext. Das Bundesland wird hier repräsentiert durch die Enum {@link Bundeslaender}.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class BundeslandTask<T> extends SudTask<T> {

    protected final Bundeslaender bundesland;

    protected BundeslandTask(SudTaskType taskType, Bundeslaender bundesland) {
        super(taskType);
        this.bundesland = bundesland;
    }
}
