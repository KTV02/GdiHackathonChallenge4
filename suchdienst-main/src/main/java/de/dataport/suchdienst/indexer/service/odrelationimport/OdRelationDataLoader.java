/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.indexer.service.odrelationimport;

import de.dataport.suchdienst.elasticsearch.api.ElasticInternalReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Lädt und transformiert die Rohdaten eines OD-Relation-Batches aus Elasticsearch.
 *
 * <p>Kapselt alle ES-Leseoperationen und die Transformation in Lookup-Objekte
 * ({@link OdZustaendigkeitenLookup}, {@link OeZustaendigkeitenLookup},
 * {@link LeikaLookup}, {@link OeStammdatenLookup}). Nutzt {@link ArsMatchingService},
 * um vor dem Laden der OE-Stammdaten die tatsächlich benötigten OE-IDs zu ermitteln.</p>
 */
@Service
public class OdRelationDataLoader {

    private final ElasticInternalReadConnector internalReadConnector;
    private final ElasticReadConnector readConnector;
    private final ArsMatchingService arsMatchingService;

    OdRelationDataLoader(
        ElasticInternalReadConnector internalReadConnector,
        ElasticReadConnector readConnector,
        ArsMatchingService arsMatchingService
    ) {
        this.internalReadConnector = internalReadConnector;
        this.readConnector = readConnector;
        this.arsMatchingService = arsMatchingService;
    }

    /**
     * Ergebnis des Batch-Ladens: gemappte Onlinedienste und gebündelter Lookup-Kontext.
     *
     * @param odsById  Onlinedienste indexiert nach OD-ID
     * @param context  Gebündelter Lookup-Kontext für den Assembler
     * @param lastId   Letzte verarbeitete OD-ID als Cursor für die nächste Seite
     */
    record BatchData(
        Map<String, OnlinedienstMitMetadaten> odsById,
        BatchRelationContext context,
        String lastId
    ) {}

    /**
     * Zwischenstand des Batch-Ladens vor dem Nachladen der tatsächlich benötigten OE-Stammdaten.
     */
    private record LoadedBatchPrerequisites(
        Map<String, OnlinedienstMitMetadaten> odsById,
        OdZustaendigkeitenLookup odZustaendigkeiten,
        LeikaLookup leikas,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {}

    /**
     * Lädt eine Batch-Seite Onlinedienste samt aller zugehörigen Zuständigkeiten,
     * Leika-IDs und OE-Stammdaten.
     *
     * @param schemeAgencyId Eindeutige Kennung des Redaktionssystems
     * @param fromId         Cursor-ID für die nächste Seite; {@code null} startet am Anfang
     * @param pageSize       Maximale Anzahl Onlinedienste pro Seite
     * @return Batch-Daten oder {@code null}, falls keine Onlinedienste mehr vorhanden
     */
    BatchData loadBatch(String schemeAgencyId, String fromId, int pageSize) {
        List<OnlinedienstEntity> odEntities = fetchOnlinedienstBatch(
            schemeAgencyId,
            pageSize,
            fromId
        );
        if (odEntities.isEmpty()) {
            return null;
        }

        LoadedBatchPrerequisites prerequisites = loadBatchPrerequisites(odEntities);
        BatchRelationContext context = buildBatchContext(prerequisites);
        return new BatchData(prerequisites.odsById(), context, determineLastId(odEntities));
    }

    /**
     * Gibt die Gesamtanzahl der Onlinedienste eines Redaktionssystems zurück (für Fortschritts-Logging).
     */
    long countOnlinedienste(String schemeAgencyId) {
        return internalReadConnector.countDocsSchemeAgencyIdBy(
            IndexNames.ONLINEDIENSTE,
            schemeAgencyId
        );
    }

    private List<OnlinedienstEntity> fetchOnlinedienstBatch(
        String schemeAgencyId,
        int pageSize,
        String fromId
    ) {
        return readConnector.fetchOnlinedienstEntities(schemeAgencyId, pageSize, fromId);
    }

    /**
     * Lädt alle Batch-Bausteine, die nur von den Onlinedienst-IDs bzw. deren LB-IDs abhängen.
     */
    private LoadedBatchPrerequisites loadBatchPrerequisites(List<OnlinedienstEntity> odEntities) {
        Map<String, OnlinedienstMitMetadaten> odsById = mapToGenericOnlinedienst(odEntities);
        OdZustaendigkeitenLookup odZustaendigkeiten = fetchOdZustaendigkeiten(odsById.keySet());
        LeikaLookup leikas = fetchLeikasFor(odZustaendigkeiten);
        OeZustaendigkeitenLookup oeZustaendigkeiten = fetchOeZustaendigkeitenFor(
            odZustaendigkeiten.allLbIds()
        );
        return new LoadedBatchPrerequisites(
            odsById,
            odZustaendigkeiten,
            leikas,
            oeZustaendigkeiten
        );
    }

    /**
     * Baut aus den geladenen Vorbedingungen den vollständigen Lookup-Kontext des Batches.
     */
    private BatchRelationContext buildBatchContext(LoadedBatchPrerequisites prerequisites) {
        Set<String> neededOeIds = determineNeededOeIds(
            prerequisites.odZustaendigkeiten(),
            prerequisites.oeZustaendigkeiten()
        );
        OeStammdatenLookup oeStammdaten = fetchOeStammdaten(neededOeIds);
        return new BatchRelationContext(
            oeStammdaten,
            prerequisites.odZustaendigkeiten(),
            prerequisites.oeZustaendigkeiten(),
            prerequisites.leikas()
        );
    }

    private Set<String> determineNeededOeIds(
        OdZustaendigkeitenLookup odZustaendigkeiten,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {
        // Erst ARS-Abgleich und direkte OE-Referenzen in-memory auswerten, dann nur die
        // tatsächlich benötigten OE-Stammdaten aus Elasticsearch laden.
        return arsMatchingService.collectNeededOeIds(odZustaendigkeiten, oeZustaendigkeiten);
    }

    private static String determineLastId(List<OnlinedienstEntity> odEntities) {
        return odEntities.getLast().getId();
    }

    /**
     * Mappt Onlinedienst-Entities auf das generische Modell und indexiert nach OD-ID.
     */
    private Map<String, OnlinedienstMitMetadaten> mapToGenericOnlinedienst(
        List<OnlinedienstEntity> odEntities
    ) {
        return odEntities
            .stream()
            .map(entity ->
                new OnlinedienstMitMetadaten(
                    entity.getMetaData(),
                    GenericModelMapper.mapOnlinedienstEntity(entity)
                )
            )
            .collect(
                Collectors.toMap(
                    odm -> odm.onlinedienst().getId(),
                    Function.identity(),
                    // Bei doppelter OD-ID behalten wir den ersten Treffer, um den Batch stabil zu halten.
                    (first, ignoredDuplicate) -> first
                )
            );
    }

    /**
     * Lädt Zuständigkeiten für eine Menge von OD-IDs und gruppiert nach OD-ID und LB-ID.
     */
    private OdZustaendigkeitenLookup fetchOdZustaendigkeiten(Set<String> odsIds) {
        Map<String, Map<String, List<ConvertedZustaendigkeit>>> grouped = readConnector
            .searchRelations(null, List.copyOf(odsIds), null, List.of())
            .stream()
            .map(this::toConvertedZustaendigkeit)
            .collect(
                Collectors.groupingBy(
                    ConvertedZustaendigkeit::getParentId,
                    Collectors.groupingBy(ConvertedZustaendigkeit::getLbId)
                )
            );
        return new OdZustaendigkeitenLookup(grouped);
    }

    /**
     * Ermittelt zu allen LB-IDs die zugehörigen Leika-IDs.
     */
    private LeikaLookup fetchLeikasFor(OdZustaendigkeitenLookup odZustaendigkeiten) {
        Set<String> lbIds = odZustaendigkeiten.allLbIds();
        if (lbIds.isEmpty()) {
            return new LeikaLookup(Map.of());
        }
        Map<String, Set<String>> grouped = readConnector
            .getByIdsWithRetry(
                IndexNames.LEISTUNGSBESCHREIBUNG,
                lbIds,
                List.of("id", "leikaId"),
                List.of(),
                LeistungEntity.class
            )
            .stream()
            .collect(Collectors.toMap(LeistungEntity::getId, LeistungEntity::getLeikaId));
        return new LeikaLookup(grouped);
    }

    /**
     * Lädt alle OE-Zuständigkeiten (typ=ORGANISATION) für die angegebenen LB-IDs
     * und überführt sie in {@link ConvertedZustaendigkeit}.
     */
    private OeZustaendigkeitenLookup fetchOeZustaendigkeitenFor(Set<String> lbIds) {
        if (lbIds.isEmpty()) {
            return new OeZustaendigkeitenLookup(Map.of());
        }
        List<ZustaendigkeitEntity> entities = readConnector.searchOeZustaendigkeitenByLbIds(lbIds);
        Map<String, List<ConvertedZustaendigkeit>> grouped = entities
            .stream()
            .map(this::toConvertedZustaendigkeit)
            .collect(Collectors.groupingBy(ConvertedZustaendigkeit::getLbId));
        return new OeZustaendigkeitenLookup(grouped);
    }

    /**
     * Überführt eine rohe Zuständigkeits-Entity in den einheitlichen Batch-Wrapper.
     */
    private ConvertedZustaendigkeit toConvertedZustaendigkeit(ZustaendigkeitEntity entity) {
        GenericZustaendigkeitTransferObjekt generic =
            GenericModelMapper.mapZustaendigkeitTransferObjektEntity(entity);
        return ConvertedZustaendigkeit.of(generic);
    }

    /**
     * Lädt OE-Stammdaten für die angegebenen Organisationseinheiten.
     */
    private OeStammdatenLookup fetchOeStammdaten(Set<String> oeIds) {
        if (oeIds.isEmpty()) {
            return new OeStammdatenLookup(Map.of());
        }
        Map<String, GenericOrganisationseinheit> grouped = readConnector
            .getByIdsWithRetry(
                IndexNames.ORGANISATIONSEINHEITEN,
                oeIds,
                List.of(),
                List.of(),
                OrganisationseinheitEntity.class
            )
            .stream()
            .map(GenericModelMapper::mapOrganisationseinheitEntity)
            .collect(Collectors.toMap(GenericOrganisationseinheit::getId, Function.identity()));
        return new OeStammdatenLookup(grouped);
    }
}
