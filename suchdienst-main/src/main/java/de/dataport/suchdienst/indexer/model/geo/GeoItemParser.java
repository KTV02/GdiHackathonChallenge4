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
package de.dataport.suchdienst.indexer.model.geo;

import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.context.MessageSource;

public class GeoItemParser {

    private final MessageSource messageSource;

    public GeoItemParser(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Parsed CSV Daten, liefert aber einen Stream von GeoItem, d.h. hier werden nicht
     * alle Items in Memory gehalten. Ist bei großen Datenmengen zu bevorzugen.
     */
    public Stream<GeoItem> parseCsvFileAsStream(Stream<String> lines, ZipcodeCsvDefinition def) {
        var delimiter = def.getDelimiter();
        var rsColumn = def.getRsColumn();
        var zipColumn = def.getZipColumn();

        if (def.isHeaderLine()) {
            lines = lines.skip(1);
        }

        return lines.map(line -> {
            if (def.validateLine(line)) {
                var columns = line.split(delimiter);
                return new GeoItem(columns[rsColumn], columns[zipColumn]);
            } else {
                throw new ImportException(
                    ErrorCodeEnum.INVALID_CSV,
                    messageSource.getMessage(
                        "ars_service.exception_invalid_cvs_error",
                        null,
                        Locale.GERMAN
                    ),
                    new Throwable(line)
                );
            }
        });
    }
}
