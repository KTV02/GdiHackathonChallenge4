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

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Ein Auftrag für eine asynchron auszuführende Operation. Der Task
 * umfasst noch kein Callable. Dies wird erst zur Laufzeit indirekt vom TaskMgr ermittelt.
 * Vorteil ist, dass wir zukünftig so die Tasks serialisieren, persistieren oder an andere Prozesse
 * delegieren können.
 */
@Getter
@EqualsAndHashCode
public abstract class SudTask<T> implements Serializable, SudTaskHandle {

    protected final SudTaskType taskType;

    /**
     * Bietet Zugriff auf das Berechnungsergebnis des Tasks.
     */
    @Setter
    private transient CompletableFuture<T> future;

    protected SudTask(SudTaskType taskType) {
        this.taskType = taskType;
    }

    public abstract Class<T> getFutureType();
}
