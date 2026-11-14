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
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
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
public class OeImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public OeImportService(
        ElasticBulkWriteConnector writeConnector,
        DeleteRememberService deleteRememberService
    ) {
        this.writeConnector = writeConnector;
        this.deleteRememberService = deleteRememberService;
    }

    public List<ElasticBulkError> importXzufiOes(Collection<OrganisationseinheitEntity> oes) {
        return writeConnector.bulkIndex(oes, IndexNames.ORGANISATIONSEINHEITEN);
    }

    public List<ElasticBulkError> deleteXzufiOes(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> idsToDelete = operation.getDeleteIds(IdentificationType.OE);
        if (!idsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                idsToDelete,
                IndexNames.ORGANISATIONSEINHEITEN,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(
                log,
                idsToDelete,
                "Lösche Organisationseinheiten mit den IDs {}."
            );
            return writeConnector.bulkDelete(idsToDelete, IndexNames.ORGANISATIONSEINHEITEN);
        }
        return List.of();
    }
}
