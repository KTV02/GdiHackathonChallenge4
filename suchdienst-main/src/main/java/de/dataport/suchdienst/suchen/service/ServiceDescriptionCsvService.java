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
package de.dataport.suchdienst.suchen.service;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.common.collect.Iterables;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.helper.LbOdCsvMappingsStrategy;
import de.dataport.suchdienst.suchen.model.mapper.ServiceDescriptionCSVMapper;
import de.dataport.suchdienst.suchen.model.out.csv.SdCsvModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServiceDescriptionCsvService {

    private final ElasticPublicSearchConnector publicSearchConnector;

    public ServiceDescriptionCsvService(ElasticPublicSearchConnector publicSearchConnector) {
        this.publicSearchConnector = publicSearchConnector;
    }

    public ByteArrayOutputStream searchServiceDescriptionAsCSV(String ars, LocalDate validDate) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8)) {
            StatefulBeanToCsv<SdCsvModel> sbc = new StatefulBeanToCsvBuilder<SdCsvModel>(writer)
                .withSeparator('|')
                .withEscapechar(ICSVWriter.DEFAULT_ESCAPE_CHARACTER)
                .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
                .withMappingStrategy(new LbOdCsvMappingsStrategy())
                .build();

            Optional<Instant> timestamp = publicSearchConnector.getMaxLastUpdate(ars, validDate);
            List<String> includes = List.of(
                "id",
                "leistungsbeschreibung.id",
                "leistungsbeschreibung.details.02",
                "leistungsbeschreibung.details.03",
                "onlinedienste",
                "leistungsbeschreibung.leikakeys"
            );
            if (timestamp.isPresent()) {
                final int size = 500;
                List<Hit<Verwaltungsleistung>> currentHits;
                String fromId = null;
                do {
                    currentHits = publicSearchConnector
                        .searchServiceDescriptionsByArsWithSearchAfter(
                            ars,
                            validDate,
                            fromId,
                            includes,
                            List.of(),
                            size
                        )
                        .hits();
                    if (ObjectUtils.isNotEmpty(currentHits)) {
                        fromId = Iterables.getLast(currentHits)
                            .source()
                            .getLeistungsbeschreibung()
                            .getId();
                        Stream<Verwaltungsleistung> vlStream = currentHits
                            .stream()
                            .map(Hit::source);
                        Stream<SdCsvModel> csvLines = createCsvModel(vlStream, timestamp.get());
                        sbc.write(csvLines);
                    }
                } while (currentHits.size() == size);
            }
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            //PV-8946 catch-rethrow
            throw new SuchenException(
                ErrorCodeEnum.CSV_CREATION_ERROR,
                "Beim Erstellen der Csv-Datei ist ein Fehler aufgetreten.",
                e
            );
        }
        return outStream;
    }

    private Stream<SdCsvModel> createCsvModel(Stream<Verwaltungsleistung> vls, Instant timestamp) {
        return vls.flatMap(vl -> {
            LeistungsbeschreibungVL lb = vl.getLeistungsbeschreibung();
            if (ObjectUtils.isEmpty(vl.getOnlinedienste())) {
                return Stream.of(ServiceDescriptionCSVMapper.INSTANCE.map(lb, null, timestamp));
            }
            return vl
                .getOnlinedienste()
                .stream()
                .map(od -> ServiceDescriptionCSVMapper.INSTANCE.map(lb, od, timestamp));
        });
    }
}
