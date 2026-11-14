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

import static java.util.stream.Collectors.toSet;

import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.util.XzufiObjectProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Importiert die XZufi Zuständigkeiten. Diese werden z. B. in die Onlinedienste aufgelöst. Die
 * Onlinedienste werden dann mit den entsprechenden Informationen angereichert. Einen extra Index
 * für die Zuständigkeiten gibt es nicht.
 */

@Slf4j
@Service
public class RelationImportService {

    private final ElasticBulkWriteConnector writeConnector;
    private final DeleteRememberService deleteRememberService;

    public RelationImportService(
        ElasticBulkWriteConnector writeConnector,
        DeleteRememberService deleteRememberService
    ) {
        this.writeConnector = writeConnector;
        this.deleteRememberService = deleteRememberService;
    }

    /**
     * Löscht Dokumente aus dem Index suchdienst_zustaendigkeiten.
     *
     * @param operation Transfer-Operationen mit Löschnachrichen für Zuständigkeiten
     * @return Ergebnis der ES Bulk Operation
     */
    public List<ElasticBulkError> deleteRelations(
        XzufiObjectProvider operation,
        ImportRequest importRequest
    ) {
        Set<String> idsToDelete = Stream.of(
            operation.getDeleteIds(IdentificationType.ZSOE),
            operation.getDeleteIds(IdentificationType.ZSOD),
            operation.getDeleteIds(IdentificationType.ZSFO)
        )
            .flatMap(Collection::stream)
            .collect(toSet());
        if (!idsToDelete.isEmpty()) {
            deleteRememberService.addDeletedObjects(
                idsToDelete,
                IndexNames.ZUSTAENDIGKEITEN,
                importRequest.getRequestAcceptedAt()
            );
            IndexerUtil.logInChunks(
                log,
                idsToDelete,
                "Lösche Zuständigkeiten mit den IDs {} aus den Onlinediensten."
            );
            return writeConnector.bulkDelete(idsToDelete, IndexNames.ZUSTAENDIGKEITEN);
        }
        return List.of();
    }

    public List<ElasticBulkError> importXzufiRelations(Collection<ZustaendigkeitEntity> relations) {
        return writeConnector.bulkIndex(relations, IndexNames.ZUSTAENDIGKEITEN);
    }
}
