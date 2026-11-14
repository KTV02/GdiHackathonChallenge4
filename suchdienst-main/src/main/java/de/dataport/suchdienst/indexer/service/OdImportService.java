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
package de.dataport.suchdienst.indexer.service;

import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Importiert Onlinedienste in den Index für Onlinedienste
 */
@Service
@Slf4j
public class OdImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public OdImportService(
        DeleteRememberService deleteRememberService,
        ElasticBulkWriteConnector writeConnector
    ) {
        this.deleteRememberService = deleteRememberService;
        this.writeConnector = writeConnector;
    }

    /**
     * Importiert die Onlinedienste aus dem xzufi in den Index für Onlinedienste. Dabei werden die
     * Zuständigkeiten mit in die Onlinedienste und LBs importiert.
     */
    public List<ElasticBulkError> importXzufiOds(Collection<OnlinedienstEntity> ods) {
        return writeConnector.bulkIndex(ods, IndexNames.ONLINEDIENSTE);
    }

    public List<ElasticBulkError> deleteXzufiOds(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> idsToDelete = operation.getDeleteIds(IdentificationType.OD);
        if (!idsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                idsToDelete,
                IndexNames.ONLINEDIENSTE,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(log, idsToDelete, "Lösche Onlinedienste mit den IDs {}.");
            return writeConnector.bulkDelete(idsToDelete, IndexNames.ONLINEDIENSTE);
        }
        return List.of();
    }
}
