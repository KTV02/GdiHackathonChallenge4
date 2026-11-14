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

import com.google.common.collect.Iterables;
import de.dataport.suchdienst.elasticsearch.api.model.ElasticBulkError;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.exception.ImportException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class IndexerUtil {

    private IndexerUtil() {
        // Util Method
    }

    public static void throwIfError(Logger log, Iterable<ElasticBulkError> errors)
        throws ImportException {
        boolean errorExists = false;

        for (ElasticBulkError error : errors) {
            errorExists = true;

            error
                .oFailure()
                .ifPresentOrElse(
                    failure ->
                        log.error(
                            "IMPORT - FEHLER: Beim Import in Elasticsearch ist ein Fehler aufgetreten: {}",
                            error.errorText(),
                            failure
                        ),
                    () ->
                        log.error(
                            "IMPORT - FEHLER: Beim Import in Elasticsearch ist ein Fehler aufgetreten: {}",
                            error.errorText()
                        )
                );
        }

        if (errorExists) {
            throw new ImportException(
                ErrorCodeEnum.XZUFI_IMPORT_ERROR,
                "Import ist fehlgeschlagen, da die ELSE-Antwort Fehler enthält.",
                ExceptionType.FACHLICH
            );
        }
    }

    public static void logInChunks(Logger log, Set<String> ids, String message) {
        List<String> sortedIds = ids.stream().sorted(String::compareToIgnoreCase).toList();
        Iterable<List<String>> partitions = Iterables.partition(sortedIds, 100);
        for (List<String> partition : partitions) {
            log.info(message, partition);
        }
    }
}
