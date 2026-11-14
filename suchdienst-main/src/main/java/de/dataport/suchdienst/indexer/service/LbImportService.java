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
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LbImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public LbImportService(
        ElasticBulkWriteConnector writeConnector,
        DeleteRememberService deleteRememberService
    ) {
        this.writeConnector = writeConnector;
        this.deleteRememberService = deleteRememberService;
    }

    public List<ElasticBulkError> deleteXzufiLbs(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> lbIdsToDelete = operation.getDeleteIds(IdentificationType.LB);
        if (!lbIdsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                lbIdsToDelete,
                IndexNames.LEISTUNGSBESCHREIBUNG,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(
                log,
                lbIdsToDelete,
                "Lösche Leistungsbeschreibungen mit den IDs {}."
            );
            return writeConnector.bulkDelete(lbIdsToDelete, IndexNames.LEISTUNGSBESCHREIBUNG);
        }
        return List.of();
    }

    public List<ElasticBulkError> importXzufiLbs(Collection<LeistungEntity> lbs) {
        return writeConnector.bulkIndex(lbs, IndexNames.LEISTUNGSBESCHREIBUNG);
    }
}
