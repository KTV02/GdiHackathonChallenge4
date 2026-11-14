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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import de.dataport.suchdienst.importer.model.GenericLoescheObjekt;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericSchreibeObjekt;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.model.GenericXZuFiObjekt;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.AffectedLbIdsEntry;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.parser.XzufiParserUtils;
import de.dataport.suchdienst.taskmgr.model.VlUpdateTask;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import de.xoev.xzufi.v2_2_0.LoescheObjekt;
import de.xoev.xzufi.v2_2_0.SchreibeObjekt;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Ermittelt alle IDs von Leistungen, welche durch zu Verarbeitende {@link TransferOperation040502}
 * mittelbar und unmittelbar betroffen sind. Die Ermittlung unterstützt sowohl {@link SchreibeObjekt} als auch {@link LoescheObjekt}.
 * Das Auflösen auf die durch ein Geschäftsobjekt betroffene Leistung erfolgt sowohl durch die IDs in der {@link TransferOperation040502} selbst,
 * als auch durch Nachschlagen im Index der Zuständigkeiten.
 * Die ermittelten IDs werden zur Aktualisierung von Dokumenten im Verwaltungsleistung-Index benötigt.
 * <p>
 * Hintergrund: Alle zu einem Gebiet relevanten Geschäftsobjekte einer Leistung finden sich in einem {@link Verwaltungsleistung} Dokument wieder.
 * Bei örtlich unterschiedlichen Ausprägungen von Geschäftsobjekten entstehen mehrere {@link Verwaltungsleistung} Dokumente.
 * Diese Dokumente können über die Leistungs ID gefunden werden.
 * Wird dem Suchdienst gegenüber eine Änderung eines Geschäftsobjekts angezeigt, so müssen alle betroffenen Dokumente aktualisiert werden.
 * Diese Aktualisierung wird durch ein Löschen und neu Berechnen umgesetzt und benötigt die ID der Leistung dafür.
 *
 * @see VerwaltungsleistungImportService
 */
@Service
@Slf4j
public class AffectedLbResolver {

    private static final String ARS_FEDERAL_STATE = "arsFederalState";

    private final ElasticReadConnector readConnector;
    private final Executor executor;
    private final SudTaskMgr sudTaskMgr;

    public AffectedLbResolver(
        ElasticReadConnector readConnector,
        @Qualifier("ioExecutor") Executor executor,
        SudTaskMgr sudTaskMgr
    ) {
        this.readConnector = readConnector;
        this.executor = executor;
        this.sudTaskMgr = sudTaskMgr;
    }

    /**
     * Ermittelt alles LbIds, die von Upserts oder Löschungen von Fachobjekten betroffen sind und
     * erzeugt dann für jede LbId einen VLUpdateTask.
     * Dies ist also der Übergang vom Xzufi-Import zur darauf aufbauenden asynchronen Aktualisierung
     * der Verwaltungsleistungen.
     */
    public void markAffectedLbIds(
        GenericTransferOperation transferOperation,
        String schemeAgencyId,
        String producer
    ) {
        try {
            Stream<AffectedLb> lbsFromUpdate = affectedLbIdsFromUpdate(transferOperation);
            Stream<AffectedLb> lbsFromRemove = affectedLbIdsFromRemove(transferOperation);
            List<AffectedLb> affectedLbs = Stream.concat(lbsFromUpdate, lbsFromRemove)
                .distinct()
                .toList();
            affectedLbs.forEach(affectedLb ->
                sudTaskMgr.submit(
                    new VlUpdateTask(
                        schemeAgencyId,
                        producer,
                        affectedLb.arsFederalState(),
                        affectedLb.lbId()
                    )
                )
            );
        } catch (Exception e) {
            // PV-8946 catch-log-skip
            // Fahre mit der Verarbeitung beim bestimmten der VL-Update-Tasks fort.
            // Hier nicht bearbeitete Objekte werden bei der nächtlichen Neuberechnung korrigiert.
            log.warn(
                "Beim Bestimmen von VL-Update-Tasks ist ein fehler aufgetreten, es wird mit der Verarbeitung fortgefahren.",
                e
            );
        }
    }

    /**
     * Initialisiert den State aus den gegebenen Entries, dies wird beim Starten des Backends aufgerufen.
     */
    void init(List<AffectedLbIdsEntry> entries) {
        entries.forEach(affectedLbIdsEntry ->
            affectedLbIdsEntry
                .getLbIds()
                .forEach(lbId ->
                    sudTaskMgr.submit(
                        new VlUpdateTask(
                            affectedLbIdsEntry.getSchemeAgencyId(),
                            affectedLbIdsEntry.getProducer(),
                            affectedLbIdsEntry.getArsFederalState(),
                            lbId
                        )
                    )
                )
        );
    }

    /**
     * Ermittelt für die Remove-Anteile (LoescheObjekte) der Transfer-Nachricht, welche LbIds in welchen Gebieten gelöscht werden.
     */
    private Stream<AffectedLb> affectedLbIdsFromRemove(GenericTransferOperation transfer) {
        try {
            Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsByClass =
                collectRemoveObjectsByClass(transfer);

            var futureAffectedLLbIdFromLb = CompletableFuture.supplyAsync(
                () -> extractRemoveAffectedLbsFromLbs(removeObjectsByClass),
                executor
            );
            var futureAffectedLLbIdsFromSp = CompletableFuture.supplyAsync(
                () -> extractRemoveAffectedLbsFromSps(removeObjectsByClass),
                executor
            );
            var futureAffectedLbsFromZs = CompletableFuture.supplyAsync(
                () -> extractRemoveAffectedLbsFromZs(removeObjectsByClass),
                executor
            );
            var futureAffectedLbsFromOeOd = CompletableFuture.supplyAsync(
                () -> extractRemoveAffectedLbsFromOeOdFo(removeObjectsByClass),
                executor
            );

            CompletableFuture.allOf(
                futureAffectedLLbIdFromLb,
                futureAffectedLLbIdsFromSp,
                futureAffectedLbsFromZs,
                futureAffectedLbsFromOeOd
            ).get();

            return Stream.of(
                futureAffectedLLbIdFromLb,
                futureAffectedLLbIdsFromSp,
                futureAffectedLbsFromZs,
                futureAffectedLbsFromOeOd
            )
                .map(CompletableFuture::join)
                .flatMap(Collection::stream);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportException(
                ErrorCodeEnum.THREAD_ERROR,
                "Sammeln der von Löschnachrichten betroffenen LB-IDs wurde unterbrochen.",
                e
            );
        } catch (Exception e) {
            throw new ImportException(
                ErrorCodeEnum.MISSING_RESOURCE,
                "Fehler beim Sammeln der von Löschnachrichten betroffenen LB-IDs.",
                e
            );
        }
    }

    /**
     * Ermittelt, welche Lbs in welchen Gebieten von Löschungen auf Od, Oes und Fos betroffen sind.
     * Sammelt hierfür deren Ids zusammen und lädt dann die Relations zu diesen Ids.
     * Dort enthalten sind die gesuchten LbIds und Gebiete.
     */
    private List<AffectedLb> extractRemoveAffectedLbsFromOeOdFo(
        Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsByClass
    ) {
        Set<String> oeOdFoEntityIds = Stream.of(
            getDeleteIds(removeObjectsByClass, IdentificationType.OD),
            getDeleteIds(removeObjectsByClass, IdentificationType.OE),
            getDeleteIds(removeObjectsByClass, IdentificationType.FO)
        )
            .flatMap(Collection::stream)
            .collect(toSet());
        return StreamSupport.stream(
            Iterables.partition(oeOdFoEntityIds, Elastic.ENTITY_FETCH_PARTITION_SIZE).spliterator(),
            false
        )
            .map(relationPartition ->
                readConnector.searchRelations(
                    null,
                    relationPartition,
                    null,
                    List.of("id", "lbId", ARS_FEDERAL_STATE)
                )
            )
            .flatMap(Collection::stream)
            .flatMap(zsEntity ->
                AffectedLb.of(
                    XzufiParserUtils.arsToUse(zsEntity.getArsFederalState()),
                    zsEntity.getLbId()
                )
            )
            .toList();
    }

    private List<AffectedLb> extractRemoveAffectedLbsFromZs(
        Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsByClass
    ) {
        Set<String> zsEntityIds = Stream.of(
            getDeleteIds(removeObjectsByClass, IdentificationType.ZSOE),
            getDeleteIds(removeObjectsByClass, IdentificationType.ZSOD),
            getDeleteIds(removeObjectsByClass, IdentificationType.ZSFO)
        )
            .flatMap(Collection::stream)
            .collect(toSet());

        return readConnector
            .getByIdsWithRetry(
                IndexNames.ZUSTAENDIGKEITEN,
                zsEntityIds,
                List.of("id", "lbId", ARS_FEDERAL_STATE),
                List.of(),
                ZustaendigkeitEntity.class
            )
            .stream()
            .flatMap(zsEntity ->
                AffectedLb.of(
                    XzufiParserUtils.arsToUse(zsEntity.getArsFederalState()),
                    zsEntity.getLbId()
                )
            )
            .toList();
    }

    private List<AffectedLb> extractRemoveAffectedLbsFromSps(
        Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsByClass
    ) {
        Set<String> spEntityIds = getDeleteIds(removeObjectsByClass, IdentificationType.SP);
        return readConnector
            .getByIdsWithRetry(
                IndexNames.SPEZIALISIERUNGEN,
                spEntityIds,
                List.of("id", "idLB", ARS_FEDERAL_STATE),
                List.of(),
                SpezialisierungEntity.class
            )
            .stream()
            .flatMap(spEntity ->
                AffectedLb.of(
                    XzufiParserUtils.arsToUse(spEntity.getArsFederalState()),
                    spEntity.getIdLB()
                )
            )
            .toList();
    }

    private List<AffectedLb> extractRemoveAffectedLbsFromLbs(
        Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsByClass
    ) {
        Set<String> lbEntityIds = getDeleteIds(removeObjectsByClass, IdentificationType.LB);
        return readConnector
            .getByIdsWithRetry(
                IndexNames.LEISTUNGSBESCHREIBUNG,
                lbEntityIds,
                List.of("id", ARS_FEDERAL_STATE),
                List.of(),
                LeistungEntity.class
            )
            .stream()
            .flatMap(leistungEntity ->
                AffectedLb.of(
                    XzufiParserUtils.arsToUse(leistungEntity.getArsFederalState()),
                    leistungEntity.getId()
                )
            )
            .toList();
    }

    /**
     * Ermittelt für die Upsert-Anteile (SchreibeObjekte) der Transfer-Nachricht, welche LbIds in welchen Gebieten verändert werden.
     */
    private Stream<AffectedLb> affectedLbIdsFromUpdate(GenericTransferOperation transfer) {
        Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> writeObjectsByClass =
            collectWriteObjectsByClass(transfer);
        List<AffectedLb> lbIdStreamByLbSpZs = extractUpdateAffectedLbsFromLbSpZs(
            writeObjectsByClass
        );
        List<AffectedLb> lbIdStreamByOeOd = extractUpdateAffectedLbsByFetchingRelationsFromOeOdFo(
            writeObjectsByClass
        );
        return Stream.of(lbIdStreamByLbSpZs, lbIdStreamByOeOd).flatMap(Collection::stream);
    }

    /**
     * Ermittelt für die Upsert-LB-Nachrichten der Transfer-Nachricht, welche LbIds in welchen Gebieten verändert werden.
     */
    private List<AffectedLb> extractUpdateAffectedLbsFromLbSpZs(
        Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> writeObjectsByClass
    ) {
        // Mit einer LB gelten alle ihre Gebiete als geändert.
        Stream<AffectedLb> fromLb = writeObjectsByClass
            .getOrDefault(GenericLeistung.class, Collections.emptyList())
            .stream()
            .map(GenericLeistung.class::cast)
            .flatMap(lb ->
                AffectedLb.of(resolveArsFederalStates(lb.getArsFederalState()), lb.getId())
            );

        // Bei einer Spezialisierung gilt genau deren Gebiet als geändert
        // Sonderfall: Wurde ein Gebiet entfernt, so muss dies berücksichtigt werden. Deshalb werden die Spezialisierungen geladen
        Map<String, List<GenericLeistungSpezialisierung>> spByLbId = writeObjectsByClass
            .getOrDefault(GenericLeistungSpezialisierung.class, Collections.emptyList())
            .stream()
            .map(GenericLeistungSpezialisierung.class::cast)
            .collect(groupingBy(GenericLeistungSpezialisierung::getLbId));

        List<AffectedLb> fromSp = spByLbId
            .entrySet()
            .stream()
            .flatMap(entry -> {
                String lbId = entry.getKey();
                List<GenericLeistungSpezialisierung> spsForLbId = entry.getValue();

                Set<String> spIds = new HashSet<>();
                Set<String> arsFederalStatesByXzufi = new HashSet<>();
                spsForLbId.forEach(lsp -> {
                    spIds.add(lsp.getId());
                    arsFederalStatesByXzufi.addAll(
                        resolveArsFederalStates(lsp.getArsFederalState())
                    );
                });

                // Für den Fall, dass GebietIds entfernt werden müssen die Spezialisierungen einmal geladen werden
                Set<String> arsFederalStatesByFetch = StreamSupport.stream(
                    Iterables.partition(spIds, Elastic.ENTITY_FETCH_PARTITION_SIZE).spliterator(),
                    false
                )
                    .map(spParition ->
                        readConnector.getByIdsWithRetry(
                            IndexNames.SPEZIALISIERUNGEN,
                            spIds,
                            List.of("id", ARS_FEDERAL_STATE),
                            List.of(),
                            SpezialisierungEntity.class
                        )
                    )
                    .flatMap(Collection::stream)
                    .map(SpezialisierungEntity::getArsFederalState)
                    .flatMap(Collection::stream)
                    .collect(toSet());

                return AffectedLb.of(
                    Sets.union(arsFederalStatesByXzufi, arsFederalStatesByFetch),
                    lbId
                );
            })
            .toList();

        // Mit einer Zuständigkeit gilt die verknüpfte LB in dem Gebiet der Zuständigkeit als geändert
        Stream<AffectedLb> fromZs = writeObjectsByClass
            .getOrDefault(GenericZustaendigkeitTransferObjekt.class, Collections.emptyList())
            .stream()
            .map(GenericZustaendigkeitTransferObjekt.class::cast)
            .flatMap(zto ->
                zto
                    .getLbIds()
                    .stream()
                    .flatMap(lbId ->
                        AffectedLb.of(resolveArsFederalStates(zto.getArsFederalState()), lbId)
                    )
            );
        return Stream.of(fromLb, fromSp.stream(), fromZs).flatMap(Function.identity()).toList();
    }

    /**
     * Ermittelt, welche Lbs in welchen Gebieten von Upserts auf Od, Oes und Fos betroffen sind.
     * Sammelt hierfür deren Ids zusammen und lädt dann die Relations zu diesen Ids.
     * Dort enthalten sind die gesuchten LbIds und Gebiete.
     */
    private List<AffectedLb> extractUpdateAffectedLbsByFetchingRelationsFromOeOdFo(
        Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> writeObjectsByClass
    ) {
        Stream<String> odIdStream = writeObjectsByClass
            .getOrDefault(GenericOnlinedienst.class, Collections.emptyList())
            .stream()
            .map(GenericOnlinedienst.class::cast)
            .map(GenericOnlinedienst::getId);
        Stream<String> foIdStream = writeObjectsByClass
            .getOrDefault(GenericFormular.class, Collections.emptyList())
            .stream()
            .map(GenericFormular.class::cast)
            .map(GenericFormular::getId);
        Stream<String> oeIdStream = writeObjectsByClass
            .getOrDefault(GenericOrganisationseinheit.class, Collections.emptyList())
            .stream()
            .map(GenericOrganisationseinheit.class::cast)
            .map(GenericOrganisationseinheit::getId);
        List<String> parentIds = Stream.of(odIdStream, oeIdStream, foIdStream)
            .flatMap(Function.identity())
            .toList();

        Stream<AffectedLb> affectedLbs = StreamSupport.stream(
            Iterables.partition(parentIds, Elastic.ENTITY_FETCH_PARTITION_SIZE).spliterator(),
            false
        )
            .map(relationPartition ->
                readConnector.searchRelations(
                    null,
                    relationPartition,
                    null,
                    List.of("id", "lbId", ARS_FEDERAL_STATE)
                )
            )
            .flatMap(Collection::stream)
            .flatMap(zsEntity ->
                AffectedLb.of(
                    XzufiParserUtils.arsToUse(zsEntity.getArsFederalState()),
                    zsEntity.getLbId()
                )
            );

        return affectedLbs.toList();
    }

    private Set<String> getDeleteIds(
        Map<IdentificationType, List<GenericLoescheObjekt>> removeObjectsObjectsByClass,
        IdentificationType identifier
    ) {
        return removeObjectsObjectsByClass
            .getOrDefault(identifier, Collections.emptyList())
            .stream()
            .map(GenericLoescheObjekt::getIdentifikatorString)
            .collect(toSet());
    }

    private Map<Class<?>, List<GenericXZuFiObjekt<?, ?>>> collectWriteObjectsByClass(
        GenericTransferOperation transfer
    ) {
        return transfer
            .getTransferOperation()
            .stream()
            .filter(GenericSchreibeObjekt.class::isInstance)
            .map(GenericSchreibeObjekt.class::cast)
            .map(GenericSchreibeObjekt::data)
            .collect(Collectors.groupingBy(GenericXZuFiObjekt::getClass, Collectors.toList()));
    }

    private Map<IdentificationType, List<GenericLoescheObjekt>> collectRemoveObjectsByClass(
        GenericTransferOperation transfer
    ) {
        return transfer
            .getTransferOperation()
            .stream()
            .filter(GenericLoescheObjekt.class::isInstance)
            .map(GenericLoescheObjekt.class::cast)
            .collect(Collectors.groupingBy(GenericLoescheObjekt::getKlasse, Collectors.toList()));
    }

    /**
     * Beschreibt, dass es für eine LbId Änderungen in einigen Gebieten gibt.
     * Aufgrund der Nebenläufigkeit, darf die Menge an arsFederalState nicht verändert werden!
     */
    public record AffectedLb(@NonNull String arsFederalState, @NonNull String lbId) {
        public static Stream<AffectedLb> of(Set<String> arsFederalStates, String lbId) {
            return arsFederalStates
                .stream()
                .map(arsFederalState -> new AffectedLb(arsFederalState, lbId));
        }

        public static Stream<AffectedLb> of(String arsFederalState, Set<String> lbIds) {
            return lbIds.stream().map(lbId -> new AffectedLb(arsFederalState, lbId));
        }
    }

    /**
     * Extrahiert aus den Gebiets-Identifikatoren die ARS-FederalStates.
     * Ein ARS-Federal-State besteht aus zwei Zahlen, welche das Bundesland repräsentieren.
     * Wird ein ARS-Federal-State des Bundes (00) erkannt, so wird dieser zu allen Bundesländern hin aufgelöst (01, 02, ...)
     */
    private Set<String> resolveArsFederalStates(Collection<String> arsFederalState) {
        return arsFederalState
            .stream()
            .flatMap(fedState -> {
                // Wenn nach dem Bund gesucht wird sollen stattdessen alle Bundesländer berücksichtigt werden
                if (fedState.equals("00")) {
                    return XzufiParserUtils.ALL_FEDERAL_STATES.stream();
                } else {
                    return Stream.of(fedState);
                }
            })
            .collect(Collectors.toSet());
    }
}
