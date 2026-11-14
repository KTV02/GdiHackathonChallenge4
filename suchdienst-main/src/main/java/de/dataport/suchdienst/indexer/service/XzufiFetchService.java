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
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import de.dataport.suchdienst.elasticsearch.api.Elastic.QueryHelper;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericSchreibeObjekt;
import de.dataport.suchdienst.importer.model.GenericTransferOperation;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity23;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.LogUtil;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.xoev.xzufi.v2_2_0.Formular;
import de.xoev.xzufi.v2_2_0.Kontaktperson;
import de.xoev.xzufi.v2_2_0.OnlinedienstErweitert;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Stellt zu einer Leistungs ID alle mittelbar und unmittelbar verknüpften Geschäftsobjekte in einer {@link TransferOperation040502} bereit.
 * Diese Form der Datenstruktur wird benötigt um eine nach ARS partitionierte Menge an {@link Verwaltungsleistung}-Dokumenten zu berechnen.
 *
 * @see VerwaltungsleistungImportService
 */
@Service
@Slf4j
@SuppressWarnings("java:S1301") // If statements würden in diesen Fällen die Lesbarkeit nicht verbessern
public class XzufiFetchService {

    private final ElasticReadConnector readConnector;
    private final Executor executor;

    public XzufiFetchService(
        ElasticReadConnector readConnector,
        @Qualifier("ioExecutor") Executor executor
    ) {
        this.readConnector = readConnector;
        this.executor = executor;
    }

    /**
     * Lädt eine Leistung aus dem XZuFi-Index und dazu dann alle anhängigen Entitäten, die zu dem Bundesland-Ars eine Zuständigkeit haben.
     * Fügt alle diese Entitäten als TransferOperation040502 zusammen.
     * Die TransferOperation wird dann weiterverarbeitet bei der Berechnung der Verwaltungsleistungen zu der LB.
     */
    public Optional<GenericTransferOperation> fetchTransferOperation(
        String lbId,
        String arsFederalState
    ) {
        Instant start = Instant.now();
        Optional<GenericTransferOperation> transferOperation = fetchLeistungById(lbId).flatMap(lb ->
            fetchRelatedObjectsBuildTransferOperation(lb, arsFederalState)
        );

        transferOperation.ifPresentOrElse(
            op ->
                log.debug(
                    "{}, Laden von TransferOperation zur Leistung '{}' (Anzahl: {}).",
                    LogUtil.millisToSeconds(Duration.between(start, Instant.now()).toMillis()),
                    lbId,
                    op.getTransferOperation().size()
                ),
            () ->
                log.info(
                    "{}, TransferOperation zur Leistung '{}' konnte nicht geladen werden",
                    LogUtil.millisToSeconds(Duration.between(start, Instant.now()).toMillis()),
                    lbId
                )
        );
        return transferOperation;
    }

    public Optional<Pair<String, GenericTransferOperation>> fetchNextTransferOperation(
        String lastLbId,
        String targetArsFederalState
    ) {
        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, lastLbId)) {
            Instant start = Instant.now();
            // Der Bund gehört zur Menge der Leistungen in den Ländern dazu
            List<String> arsFederalState = List.of(
                targetArsFederalState,
                Bundeslaender.BUND.getArsFederalstate()
            );
            Optional<GenericLeistung> oLeistung = fetchNextLeistung(lastLbId, arsFederalState);
            if (oLeistung.isEmpty()) {
                log.info(
                    "{}, ARS '{}', Iteration abgeschlossen, keine weitere Leistung verfügbar, Letzte verarbeitete Leistung '{}'",
                    LogUtil.millisToSeconds(Duration.between(start, Instant.now()).toMillis()),
                    targetArsFederalState,
                    lastLbId
                );
                // Iteration ist abgeschlossen, wichtige Abbruchbedingung für Rekursion
                return Optional.empty();
            }

            String currentLbId = oLeistung.get().getId();

            // Hole alle verknüpften Geschäftsobjekte zur aktuellen Leistung
            Optional<GenericTransferOperation> transferOperation = oLeistung.flatMap(lb ->
                fetchRelatedObjectsBuildTransferOperation(lb, targetArsFederalState)
            );

            if (transferOperation.isPresent()) {
                // Eine Transferoperation wurde gebildet
                log.debug(
                    "{}, ARS '{}', Laden von Geschäftsobjekten zur Leistung '{}' (Anzahl: {}).",
                    LogUtil.millisToSeconds(Duration.between(start, Instant.now()).toMillis()),
                    targetArsFederalState,
                    currentLbId,
                    transferOperation.get().getTransferOperation().size()
                );
                return transferOperation.map(to -> Pair.of(currentLbId, to));
            } else {
                log.info(
                    "{}, ARS '{}', Transferoperation zur Leistung '{}' konnte nicht gebildet werden.",
                    LogUtil.millisToSeconds(Duration.between(start, Instant.now()).toMillis()),
                    targetArsFederalState,
                    lastLbId
                );
                // Es wird nach der nächsten LB-ID gesucht.
                return fetchNextTransferOperation(currentLbId, targetArsFederalState);
            }
        }
    }

    /**
     * Lädt zu einer LB alle Entitäten, die eine Zuständigkeit in das Bundesland haben und fügt all diese Entitäten wieder
     * in eine TransferOperation040502 zusammen.
     * Lädt hier erst die Zuständigkeiten zu der LB und dann asynchron-parallel die dazu passenden Entitäten.
     *
     * @param lb Leistung zu der die TransferOperation gebildet werden soll
     */
    private Optional<GenericTransferOperation> fetchRelatedObjectsBuildTransferOperation(
        GenericLeistung lb,
        String arsFederalState
    ) {
        try {
            String lbId = lb.getId();
            var futureXzufiZss = CompletableFuture.supplyAsync(
                () -> fetchRelations(lbId, arsFederalState),
                executor
            );
            var futureRelationHolder = futureXzufiZss.thenApply(RelationHolder::new);
            var futureXzufiOes = futureRelationHolder.thenApplyAsync(
                relationHolder ->
                    fetchOrganisationseinheitenMapZustaendigkeiten(relationHolder, lbId),
                executor
            );
            var futureXzufiOds = futureRelationHolder.thenApplyAsync(
                this::fetchOnlinediensteMapZustaendigkeiten,
                executor
            );
            var futureXzufiFos = futureRelationHolder.thenApplyAsync(
                this::fetchFormulareMapZustaendigkeiten,
                executor
            );
            var futureXzufiSps = CompletableFuture.supplyAsync(
                () -> fetchSpezialisierungen(lbId, arsFederalState),
                executor
            );

            CompletableFuture.allOf(
                futureXzufiOes,
                futureXzufiOds,
                futureXzufiFos,
                futureXzufiSps
            ).get();

            List<GenericOrganisationseinheit> xzufiOes = futureXzufiOes.get();
            List<GenericOnlinedienst> xzufiOds = futureXzufiOds.get();
            List<GenericFormular> xzufiFos = futureXzufiFos.get();
            List<GenericLeistungSpezialisierung> xzufiSps = futureXzufiSps.get();

            if (xzufiOes.isEmpty() && xzufiOds.isEmpty()) {
                // Es sollen nur Verwaltungsleistungen zu LBs berechnet werden, die auch verknüpfte Geschäftsobjekte haben
                log.debug(
                    "Leistung '{}' konnten weder Organisationseinheiten, noch Onlinedienste zugewiesen werden. " +
                        "Dadurch kann keine Verwaltungsleistung berechnet werden. Gefundene Zuständigkeiten '{}'.",
                    lbId,
                    futureXzufiZss.join().size()
                );
                return Optional.empty();
            } else {
                return Optional.of(
                    createTransferOperation(lb, xzufiOes, xzufiOds, xzufiFos, xzufiSps)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportException(
                ErrorCodeEnum.THREAD_ERROR,
                "Bauen der Transferoperation wurde unterbrochen.",
                e
            );
        } catch (Exception e) {
            throw new ImportException(
                ErrorCodeEnum.MISSING_RESOURCE,
                "Fehler beim Bauen der Transferoperation.",
                e
            );
        }
    }

    private GenericTransferOperation createTransferOperation(
        GenericLeistung xzufiLb,
        List<GenericOrganisationseinheit> xzufiOes,
        List<GenericOnlinedienst> xzufiOds,
        List<GenericFormular> xzufiFos,
        List<GenericLeistungSpezialisierung> xzufiSps
    ) {
        GenericTransferOperation op = new GenericTransferOperation();

        // Add LB
        GenericSchreibeObjekt lbSchreiber = new GenericSchreibeObjekt(xzufiLb);
        op.getTransferOperation().add(lbSchreiber);

        // Add OE
        xzufiOes.forEach(oe -> {
            GenericSchreibeObjekt schreiber = new GenericSchreibeObjekt(oe);
            op.getTransferOperation().add(schreiber);
        });

        // Add OD
        xzufiOds.forEach(od -> {
            GenericSchreibeObjekt schreiber = new GenericSchreibeObjekt(od);
            op.getTransferOperation().add(schreiber);
        });

        // Add FO
        xzufiFos.forEach(fo -> {
            GenericSchreibeObjekt schreiber = new GenericSchreibeObjekt(fo);
            op.getTransferOperation().add(schreiber);
        });

        // Add SP
        xzufiSps.forEach(sp -> {
            GenericSchreibeObjekt schreiber = new GenericSchreibeObjekt(sp);
            op.getTransferOperation().add(schreiber);
        });

        return op;
    }

    private Optional<GenericLeistung> fetchLeistungById(String id) {
        Optional<LeistungEntity> optionalLeistung = readConnector.getById(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            id,
            LeistungEntity.class
        );
        return optionalLeistung.map(GenericModelMapper::mapLeistungEntity);
    }

    private Optional<GenericLeistung> fetchNextLeistung(
        String fromId,
        List<String> arsFederalState
    ) {
        Query query = QueryHelper.terms("arsFederalState", arsFederalState);
        List<Hit<LeistungEntity>> hits = readConnector
            .searchAfter(
                IndexNames.LEISTUNGSBESCHREIBUNG,
                fromId,
                1,
                query,
                SourceConfig.of(s -> s.fetch(true)),
                false,
                LeistungEntity.class
            )
            .hits();
        if (ObjectUtils.isNotEmpty(hits)) {
            Hit<LeistungEntity> hit = hits.getFirst();
            if (Objects.nonNull(hit) && Objects.nonNull(hit.source())) {
                return Optional.of(GenericModelMapper.mapLeistungEntity(hit.source()));
            }
        }
        return Optional.empty();
    }

    private List<GenericZustaendigkeitTransferObjekt> fetchRelations(
        String lbId,
        String arsFederalState
    ) {
        Set<String> arsToFetch = new HashSet<>();
        // Die Bundesleistungen sollen immer mitberücksichtigt werden
        arsToFetch.add(Bundeslaender.BUND.getArsFederalstate());
        arsToFetch.add(arsFederalState);
        List<ZustaendigkeitEntity> zustaendigkeitEntities = readConnector.searchRelations(
            lbId,
            arsToFetch
        );
        return zustaendigkeitEntities
            .stream()
            .map(GenericModelMapper::mapZustaendigkeitTransferObjektEntity)
            .toList();
    }

    /**
     * Fetcht alle OEs, die zu den zuvor geladenen Zuständigkeiten gehören.
     * Fügt dann der OE eine "Gesamtzuständigkeit" hinzu, die alle ARS der Zuständigkeiten enthält.
     * Reduziert weiterhin die an der OE geschachtelten Formulare und Kontaktpersonen so, dass nur diejenigen übrig bleiben, die eine Zuständigkeit auf die
     * lbId haben.
     *
     * @param relationHolder Alle Zuständigkeiten, die zu der LB gehören
     * @param lbId  Die lbId, in deren Kontext hier gefetcht wird.
     * @return Die so vorbereitete OE
     */
    private List<GenericOrganisationseinheit> fetchOrganisationseinheitenMapZustaendigkeiten(
        RelationHolder relationHolder,
        String lbId
    ) {
        RelationTyp relationTyp = RelationTyp.ORGANISATION;
        Set<String> oeIds = relationHolder.getParentIds(relationTyp);
        if (oeIds.isEmpty()) {
            return List.of();
        }
        Stream<GenericOrganisationseinheit> oes = readConnector
            .getByIdsWithRetry(
                IndexNames.ORGANISATIONSEINHEITEN,
                oeIds,
                List.of(),
                List.of(),
                OrganisationseinheitEntity.class
            )
            .stream()
            .map(entity -> {
                List<GenericZustaendigkeitTransferObjekt> zss = relationHolder.getRelations(
                    relationTyp,
                    entity.getId()
                );
                if (zss.isEmpty()) {
                    // Es gibt keine Zuständigkeit zu dieser OE, also kann sie nicht weiterverarbeitet werden
                    return null;
                }
                GenericOrganisationseinheit oe = GenericModelMapper.mapOrganisationseinheitEntity(
                    entity
                );
                addZustaendigkeitToOe(oe, zss);
                filterOeFormulareByLbId(oe, lbId);
                filterOeKontaktpersonenByLbId(oe, lbId);
                return oe;
            })
            .filter(Objects::nonNull);
        return oes.toList();
    }

    /**
     * Reduziert die an der Oe geschachtelten Formulare so, dass nur diejenigen übrig bleiben, die eine Zuständigkeit auf die LB haben.
     * Das benötigen wir, um bei der späteren VL-Generierung garantiert auch nur mit geschachtelten Formularen arbeiten, die zur jeweils
     * berechneten LB gehören. Das XzuFi-Fetching aus ES allein kann dies in diesem Fall nicht leisten.
     */
    private void filterOeFormulareByLbId(GenericOrganisationseinheit oe, String lbId) {
        switch (oe.getVersion()) {
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert oe23 = oe.getXZuFi23();
                List<de.xoev.xzufi.v2_3_1.Formular> filteredForms = oe23
                    .getFormular()
                    .stream()
                    .filter(fo23 ->
                        fo23
                            .getZustaendigkeit()
                            .stream()
                            .anyMatch(xzs ->
                                de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.zusaendigkeitHasLbId(
                                    xzs,
                                    lbId
                                )
                            )
                    )
                    .toList();
                oe23.getFormular().clear();
                oe23.getFormular().addAll(filteredForms);
            }
            case XZUFI22 -> {
                OrganisationseinheitErweitert oe22 = oe.getXZuFi22();
                List<Formular> filteredForms = oe22
                    .getFormular()
                    .stream()
                    .filter(fo22 ->
                        fo22
                            .getZustaendigkeit()
                            .stream()
                            .anyMatch(xzs -> XZuFiUtil.zusaendigkeitHasLbId(xzs, lbId))
                    )
                    .toList();
                oe22.getFormular().clear();
                oe22.getFormular().addAll(filteredForms);
            }
        }
    }

    /**
     * Reduziert die Kontaktpersonen der OE so, dass nur diejenigen übrig bleiben, die eine Zuständigkeit auf die LB haben.
     * Das benötigen wir, um bei der späteren VL-Generierung garantiert auch nur mit Kontaktpersonen arbeiten, die zur jeweils
     * berechneten LB gehören. Das XZuFi-Fetching aus ES allein kann dies in diesem Fall nicht leisten.
     */
    private void filterOeKontaktpersonenByLbId(GenericOrganisationseinheit oe, String lbId) {
        switch (oe.getVersion()) {
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert oe23 = oe.getXZuFi23();
                List<de.xoev.xzufi.v2_3_1.Kontaktperson> filteredKontaktpersonen = oe23
                    .getKontaktperson()
                    .stream()
                    .filter(kp23 ->
                        kp23
                            .getZustaendigkeit()
                            .stream()
                            .anyMatch(xzs ->
                                de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.zusaendigkeitHasLbId(
                                    xzs,
                                    lbId
                                )
                            )
                    )
                    .toList();
                oe23.getKontaktperson().clear();
                oe23.getKontaktperson().addAll(filteredKontaktpersonen);
            }
            case XZUFI22 -> {
                OrganisationseinheitErweitert oe22 = oe.getXZuFi22();
                List<Kontaktperson> filteredKontaktpersonen = oe22
                    .getKontaktperson()
                    .stream()
                    .filter(kp22 ->
                        kp22
                            .getZustaendigkeit()
                            .stream()
                            .anyMatch(xzs -> XZuFiUtil.zusaendigkeitHasLbId(xzs, lbId))
                    )
                    .toList();
                oe22.getKontaktperson().clear();
                oe22.getKontaktperson().addAll(filteredKontaktpersonen);
            }
        }
    }

    private List<GenericOnlinedienst> fetchOnlinediensteMapZustaendigkeiten(
        RelationHolder relationHolder
    ) {
        RelationTyp relationTyp = RelationTyp.ONLINESERVICE;
        Set<String> odIds = relationHolder.getParentIds(relationTyp);
        if (odIds.isEmpty()) {
            // Es gibt keine Zuständigkeit zu diesem OD, also kann sie nicht weiterverarbeitet werden
            return List.of();
        }
        Stream<GenericOnlinedienst> oes = readConnector
            .getByIdsWithRetry(
                IndexNames.ONLINEDIENSTE,
                odIds,
                List.of(),
                List.of(),
                OnlinedienstEntity.class
            )
            .stream()
            .map(entity -> {
                List<GenericZustaendigkeitTransferObjekt> zss = relationHolder.getRelations(
                    relationTyp,
                    entity.getId()
                );
                if (zss.isEmpty()) {
                    // Es gibt keine Zuständigkeit zu dieser OE, also kann sie nicht weiterverarbeitet werden
                    return null;
                }
                GenericOnlinedienst od = GenericModelMapper.mapOnlinedienstEntity(entity);
                addZustaendigkeitToOd(od, zss);
                return od;
            })
            .filter(Objects::nonNull);
        return oes.toList();
    }

    private List<GenericFormular> fetchFormulareMapZustaendigkeiten(RelationHolder relationHolder) {
        RelationTyp relationTyp = RelationTyp.FORM;
        Set<String> foIds = relationHolder.getParentIds(relationTyp);
        if (foIds.isEmpty()) {
            // Es gibt keine Zuständigkeit zu diesem FO, also kann sie nicht weiterverarbeitet werden
            return List.of();
        }
        Stream<GenericFormular> fos = readConnector
            .getByIdsWithRetry(
                IndexNames.FORMULARE,
                foIds,
                List.of(),
                List.of(),
                FormularEntity.class
            )
            .stream()
            .map(entity -> {
                List<GenericZustaendigkeitTransferObjekt> zss = relationHolder.getRelations(
                    relationTyp,
                    entity.getId()
                );
                if (zss.isEmpty()) {
                    // Es gibt keine Zuständigkeit zu dieser OE, also kann sie nicht weiterverarbeitet werden
                    return null;
                }
                GenericFormular fo = GenericModelMapper.mapFormularEntity(entity);
                addZustaendigkeitToFo(fo, zss);
                return fo;
            })
            .filter(Objects::nonNull);
        return fos.toList();
    }

    private List<GenericLeistungSpezialisierung> fetchSpezialisierungen(
        String lbId,
        String arsFederalState
    ) {
        return readConnector
            .searchSpecializationsWithSearchAfter(lbId, arsFederalState)
            .stream()
            .map(
                entity ->
                    // Spezialisierungen können ARS aus verschiedenen Gebieten enthalten,
                    // für die Weiterverarbeitung ist es aber relevant, nur die Gebiete des gesuchten Bundeslandes in den Algorithmus
                    // zur Partitionierung (VerwaltungsleistungenBuilder) geben.
                    switch (entity) {
                        case SpezialisierungEntity22 sp22 -> {
                            sp22.getJzufi()
                                .getIdGebiet()
                                .removeIf(id -> !id.getValue().startsWith(arsFederalState));
                            yield entity;
                        }
                        case SpezialisierungEntity23 sp23 -> {
                            sp23.getJzufi()
                                .getGebietID()
                                .removeIf(id -> !id.getValue().startsWith(arsFederalState));
                            yield entity;
                        }
                    }
            )
            .map(GenericModelMapper::mapLeistungSpezialisierungEntity)
            .toList();
    }

    private void addZustaendigkeitToOe(
        GenericOrganisationseinheit oe,
        List<GenericZustaendigkeitTransferObjekt> oZs
    ) {
        // Zuständigkeiten werden originär innerhalb der OE geliefert, also sollten sie in der gleichen XZuFI-Version vorliegen, wie die OE selbst.
        // Eine Abweichung kann nur bei nicht korrekt migrierten Datensätzen vorkommen, in diesem Falle wird behelfsweise eine konvertierung durchgeführt
        switch (oe.getVersion()) {
            case XZUFI23 -> {
                var oe23 = oe.getXZuFi23();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23)
                    .map(ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .map(de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class::cast)
                    .forEach(zs -> oe23.getZustaendigkeit().add(zs));
            }
            case XZUFI22 -> {
                var oe22 = oe.getXZuFi22();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi22)
                    .map(de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .map(de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit.class::cast)
                    .forEach(zs -> oe22.getZustaendigkeit().add(zs));
            }
        }
    }

    private void addZustaendigkeitToOd(
        GenericOnlinedienst od,
        List<GenericZustaendigkeitTransferObjekt> oZs
    ) {
        switch (od.getVersion()) {
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.OnlinedienstErweitert od23 = od.getXZuFi23();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23)
                    .map(ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .map(de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst.class::cast)
                    .forEach(zs -> od23.getZustaendigkeit().add(zs));
            }
            case XZUFI22 -> {
                OnlinedienstErweitert od22 = od.getXZuFi22();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi22)
                    .map(de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .forEach(zs -> od22.getZustaendigkeit().add(zs));
            }
        }
    }

    private void addZustaendigkeitToFo(
        GenericFormular fo,
        List<GenericZustaendigkeitTransferObjekt> oZs
    ) {
        switch (fo.getVersion()) {
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Formular fo23 = fo.getXZuFi23();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi23)
                    .map(ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .forEach(zs -> fo23.getZustaendigkeit().add(zs));
            }
            case XZUFI22 -> {
                Formular fo22 = fo.getXZuFi22();
                oZs.stream()
                    .map(GenericZustaendigkeitTransferObjekt::getOrConvertXZuFi22)
                    .map(de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt::getZustaendigkeit)
                    .flatMap(Collection::stream)
                    .forEach(zs -> fo22.getZustaendigkeit().add(zs));
            }
        }
    }

    private record RelationHolder(
        Map<
            RelationTyp,
            Map<String, List<GenericZustaendigkeitTransferObjekt>>
        > relationsByTypeAndId
    ) {
        public RelationHolder(List<GenericZustaendigkeitTransferObjekt> relations) {
            this(
                relations
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            GenericZustaendigkeitTransferObjekt::getParentTyp,
                            Collectors.groupingBy(
                                GenericZustaendigkeitTransferObjekt::getParentId,
                                Collectors.toUnmodifiableList()
                            )
                        )
                    )
            );
        }

        public Set<String> getParentIds(RelationTyp relationTyp) {
            return relationsByTypeAndId.getOrDefault(relationTyp, Map.of()).keySet();
        }

        public List<GenericZustaendigkeitTransferObjekt> getRelations(
            RelationTyp relationTyp,
            String parentId
        ) {
            return relationsByTypeAndId
                .getOrDefault(relationTyp, Map.of())
                .getOrDefault(parentId, List.of());
        }
    }
}
