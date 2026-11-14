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
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Importiert FormularEntities in den entsprechenden Jzufi-Index.
 */
@Service
@Slf4j
public class FoImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public FoImportService(
        DeleteRememberService deleteRememberService,
        ElasticBulkWriteConnector writeConnector
    ) {
        this.deleteRememberService = deleteRememberService;
        this.writeConnector = writeConnector;
    }

    /**
     * Importiert die Formulare aus dem xzufi in den entsprechenden Jzufi-Index.
     */
    public List<ElasticBulkError> importXzufiFos(Collection<FormularEntity> fos) {
        return writeConnector.bulkIndex(fos, IndexNames.FORMULARE);
    }

    public List<ElasticBulkError> deleteXzufiFos(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> idsToDelete = operation.getDeleteIds(IdentificationType.FO);
        if (!idsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                idsToDelete,
                IndexNames.FORMULARE,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(log, idsToDelete, "Lösche Formulare mit den IDs {}.");
            return writeConnector.bulkDelete(idsToDelete, IndexNames.FORMULARE);
        }
        return List.of();
    }
}
