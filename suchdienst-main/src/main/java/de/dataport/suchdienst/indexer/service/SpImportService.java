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
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Services fuer Spezializierungen importiert xzufi SPs in unsere Elasticsearch dabei werden die LBs
 * und die Suggestions der verschiedenen Sprachen aktualisiert und im Index gespeichert
 */
@Slf4j
@Service
public class SpImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public SpImportService(
        ElasticBulkWriteConnector writeConnector,
        DeleteRememberService deleteRememberService
    ) {
        this.writeConnector = writeConnector;
        this.deleteRememberService = deleteRememberService;
    }

    public List<ElasticBulkError> importXzufiSps(List<SpezialisierungEntity> sps) {
        return writeConnector.bulkIndex(sps, IndexNames.SPEZIALISIERUNGEN);
    }

    public List<ElasticBulkError> deleteXzufiSps(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> idsToDelete = operation.getDeleteIds(IdentificationType.SP);
        if (!idsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                idsToDelete,
                IndexNames.SPEZIALISIERUNGEN,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(log, idsToDelete, "Lösche Spezialisierungen mit den IDs {}.");
            return writeConnector.bulkDelete(idsToDelete, IndexNames.SPEZIALISIERUNGEN);
        }
        return List.of();
    }
}
