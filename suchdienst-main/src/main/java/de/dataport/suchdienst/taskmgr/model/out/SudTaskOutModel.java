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
package de.dataport.suchdienst.taskmgr.model.out;

import de.dataport.suchdienst.taskmgr.model.ImportTask;
import de.dataport.suchdienst.taskmgr.model.OdRelRecalcTask;
import de.dataport.suchdienst.taskmgr.model.SudTaskHandle;
import de.dataport.suchdienst.taskmgr.model.SudTaskType;
import de.dataport.suchdienst.taskmgr.model.VlRecalcTask;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.util.Bundeslaender;
import lombok.Getter;

/**
 * Ausgabemodell für APIs. Enthält das Superset aller Attribute in der SudTask-Klassenhierarchie.
 * Felder sind teils nullable.
 * Es gibt pro Subklasse eine Factory Methode.
 */
@Getter
public class SudTaskOutModel {

    private final SudTaskType taskType;
    private final String schemeAgencyId;
    private final String arsFederalState;
    private final String producer;

    // Nachrichten UUID vom Sammlerdienst, nur bei Typ IMPORT
    private final String importRequestId;

    // Lb-Id, die im Verwaltungsleistung-Index upgedatet wird, nur bei VL_UPDATE
    private final String updateLbId;

    private SudTaskOutModel(
        SudTaskType taskType,
        String schemeAgencyId,
        String producer,
        String arsFederalState,
        String importRequestId,
        String updateLbId
    ) {
        this.taskType = taskType;
        this.schemeAgencyId = schemeAgencyId;
        this.producer = producer;
        this.arsFederalState = arsFederalState;
        this.importRequestId = importRequestId;
        this.updateLbId = updateLbId;
    }

    /**
     * Dies wird aufgerufen für unbekannte Task-Klassen, insbesondere haben wir in den UnitTests den "TestTask".
     */
    private static SudTaskOutModel unknown(SudTaskHandle task) {
        return new SudTaskOutModel(task.getTaskType(), null, null, null, null, null);
    }

    private static SudTaskOutModel ofImportTask(ImportTask importTask) {
        var arsFederalstate = Bundeslaender.getArsBySchemeAgencyId(importTask.getSchemeAgencyId());
        return new SudTaskOutModel(
            importTask.getTaskType(),
            importTask.getSchemeAgencyId(),
            importTask.getProducer(),
            arsFederalstate.orElse(null),
            importTask.getRequestId(),
            null
        );
    }

    private static SudTaskOutModel ofVlUpdateTask(VlUpdateTask vlUpdateTask) {
        return new SudTaskOutModel(
            vlUpdateTask.getTaskType(),
            vlUpdateTask.getSchemeAgencyId(),
            vlUpdateTask.getProducer(),
            vlUpdateTask.getTargetArsFederalState(),
            null,
            vlUpdateTask.getLbId()
        );
    }

    private static SudTaskOutModel ofVlRecalcTask(VlRecalcTask vlRecalcTask) {
        return new SudTaskOutModel(
            vlRecalcTask.getTaskType(),
            vlRecalcTask.getBundesland().getSchemeAgencyId(),
            null,
            vlRecalcTask.getBundesland().getArsFederalstate(),
            null,
            null
        );
    }

    private static SudTaskOutModel ofOdRelRecalcTask(OdRelRecalcTask odRelRecalcTask) {
        var arsFederalstate = Bundeslaender.getArsBySchemeAgencyId(
            odRelRecalcTask.getSchemeAgencyId()
        );
        return new SudTaskOutModel(
            odRelRecalcTask.getTaskType(),
            odRelRecalcTask.getSchemeAgencyId(),
            odRelRecalcTask.getProducer(),
            arsFederalstate.orElse(null),
            null,
            null
        );
    }

    public static SudTaskOutModel of(SudTaskHandle sudTask) {
        return switch (sudTask) {
            case ImportTask importTask -> ofImportTask(importTask);
            case VlUpdateTask vlUpdateTask -> ofVlUpdateTask(vlUpdateTask);
            case VlRecalcTask vlRecalcTask -> ofVlRecalcTask(vlRecalcTask);
            case OdRelRecalcTask odRelRecalcTask -> ofOdRelRecalcTask(odRelRecalcTask);
            default -> unknown(sudTask);
        };
    }
}
