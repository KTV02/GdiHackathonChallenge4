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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.elasticsearch.api.IndexRefreshMgr;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.ArsObject;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import de.dataport.suchdienst.indexer.model.geo.GeoItem;
import de.dataport.suchdienst.indexer.model.geo.GeoItemParser;
import de.dataport.suchdienst.indexer.model.geo.ZipAssigner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArsService {

    private final MessageSource messageSource;
    private final ElasticBulkWriteConnector writeConnector;
    private final ElasticInternalReadConnector internalReadConnector;
    private final ElasticReadConnector readConnector;
    private final IndexRefreshMgr indexRefreshMgr;

    public ArsService(
        ElasticBulkWriteConnector writeConnector,
        ElasticInternalReadConnector internalReadConnector,
        ElasticReadConnector readConnector,
        MessageSource messageSource,
        IndexRefreshMgr indexRefreshMgr
    ) {
        this.writeConnector = writeConnector;
        this.internalReadConnector = internalReadConnector;
        this.readConnector = readConnector;
        this.messageSource = messageSource;
        this.indexRefreshMgr = indexRefreshMgr;
    }

    /**
     * Importiert den Ars-Stream in den Ars-Index. Der Stream wird danach geschlossen.
     * Dies ist die "Erstbetankung" des Index.
     * Weiterhin gibt es hier die Methode {@link #updatePostcodes(Stream, RegionalKeyKind)}, die aus Geo-Information weitere PLZ bestimmt und
     * den Ars Index updatet.
     */
    public void importArsList(InputStream filestream, Charset charset) {
        try (
            var isr = new InputStreamReader(filestream, charset);
            var br = new BufferedReader(isr)
        ) {
            Stream<String> lines = br.lines();

            List<ArsObject> arsObjects = new ArrayList<>();
            lines.map(this::parseLine).filter(Objects::nonNull).forEach(arsObjects::add);

            writeConnector.bulkIndexThrowIfError(arsObjects, IndexNames.ARS);
            indexRefreshMgr.ensureFresh(IndexNames.ARS);
        } catch (StringIndexOutOfBoundsException | IOException e) {
            throw new ImportException(
                ErrorCodeEnum.INVALID_FILE,
                messageSource.getMessage(
                    "ars_service.exception_invalid_file_error ",
                    null,
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    private ArsObject parseLine(String line) {
        ArsObject obj = new ArsObject();
        // Regionalschlüssel ars
        obj.setArsAndAgs(line.substring(10, 15) + line.substring(18, 22) + line.substring(15, 18));
        // Bezeichnung
        obj.setName(line.substring(22, 72).trim());
        // Postleitzahl
        String zip = line.substring(165, 170).trim();
        obj.setDisplayZip(zip);
        obj.getZipCodes().add(zip);
        // Vorschläge anhand der Postleitzahl und des Namens
        obj.addSuggestion(obj.getName());
        if (!zip.isEmpty()) {
            obj.addSuggestion(zip + " " + obj.getName());
        }
        // Gewichtung (fuer die Vorschlaege) anhand der Einwohnerzahlen
        obj.setWeight(line.substring(139, 150));

        if (obj.getArs().trim().length() == 12 && !zip.isEmpty()) {
            // Die Ars muss 12 Zeichen haben, dabei darf die PLZ nicht leer sein
            // (Gemeindefreies Gebiet)
            return obj;
        }
        return null;
    }

    private void updatePostcodes(Stream<GeoItem> geoItems, RegionalKeyKind keyKind) {
        Query query = QueryBuilders.matchAll().build()._toQuery();
        SourceConfig config = SourceConfig.of(c -> c.fetch(true));
        List<ArsObject> arsObjects = readConnector.searchAllPit(
            IndexNames.ARS,
            1000,
            query,
            config,
            ArsObject.class
        );
        var zipAssigner = new ZipAssigner();
        var modifiedArsObjects = zipAssigner.assignZipCodes(arsObjects, geoItems, keyKind);
        if (!modifiedArsObjects.isEmpty()) {
            writeConnector.bulkIndexThrowIfError(modifiedArsObjects, IndexNames.ARS);
            log.info("PLZ von {} Ars in ElasticSearch aktualisiert", modifiedArsObjects.size());
        }
    }

    /**
     * Updatet die PLZ im Ars Index durch Analyse und Extraktion von weiteren Postleitzahlen aus Geo-Daten.
     * Die Anlieferung erfolgt als CSV-Daten.
     * Siehe PV-9580.
     */
    public void importPostcodes(Stream<String> lines, ZipcodeCsvDefinition def) {
        var startMillis = System.currentTimeMillis();
        log.info("Postleitzahlen werden importiert...");
        var geoItemParser = new GeoItemParser(messageSource);
        var itemStream = geoItemParser.parseCsvFileAsStream(lines, def);
        updatePostcodes(itemStream, def.getRsKind());
        log.info(
            "{} ms Postleitzahlen wurden erfolgreich importiert",
            System.currentTimeMillis() - startMillis
        );
    }

    /**
     * Sucht im Ars Index nach einem Eintrag zu einer Postleitzahl.
     *
     * @param zip Die Postleitzahl
     * @return Den ARS, bzw. empty, wenn kein Eintrag oder kein syntaktisch korrekter Eintrag existiert.
     */
    public Optional<String> getArsByZip(String zip) {
        HitsMetadata<ArsObject> response = internalReadConnector.searchByIndex(
            IndexNames.ARS,
            zip,
            true,
            0,
            1,
            null,
            null,
            List.of("zipCodes"),
            ArsObject.class
        );

        if (response.hits().size() == 1) {
            ArsObject arsObject = response.hits().getFirst().source();

            if (arsObject != null) {
                return Optional.ofNullable(arsObject.getArs());
            }
        }
        return Optional.empty();
    }
}
