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

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.LeistungRegionZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.LocaleUtils;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Assembliert {@link OnlinedienstRelationen} aus bereits geladenen Onlinedienst-,
 * Zuständigkeits- und Organisationseinheits-Daten.
 *
 * <p>Die Klasse enthält ausschließlich Logik zur Transformation und Verknüpfung der
 * Quelldaten – es finden keine Elasticsearch-Zugriffe statt. Pro Onlinedienst werden
 * drei klar getrennte fachliche Schritte ausgeführt:</p>
 * <ol>
 *   <li>Leistungsinformationen zusammenstellen ({@link LeistungRegionZuordnung})</li>
 *   <li>Rollen der Organisationseinheiten extrahieren (via {@link RoleExtractor})</li>
 *   <li>OE-Stammdaten anreichern (via {@link OeStammdatenEnricher})</li>
 * </ol>
 */
@Service
@Slf4j
public class OdRelationAssembler {

    /**
     * Eingangsmodell für die Assemblierung eines einzelnen Onlinedienstes.
     */
    private record OnlinedienstAssemblyInput(
        GenericOnlinedienst onlinedienst,
        TransactionMetadata metadata,
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId
    ) {}

    /**
     * Fachliche Teilergebnisse der Assemblierung vor dem Befüllen des Zielmodells.
     */
    private record RelationParts(
        List<LeistungRegionZuordnung> serviceRelations,
        List<OrganisationseinheitRollenZuordnung> organisationRelations
    ) {}

    private final RoleExtractor roleExtractor;
    private final OeStammdatenEnricher oeStammdatenEnricher;

    OdRelationAssembler(RoleExtractor roleExtractor, OeStammdatenEnricher oeStammdatenEnricher) {
        this.roleExtractor = roleExtractor;
        this.oeStammdatenEnricher = oeStammdatenEnricher;
    }

    /**
     * Erstellt für eine Menge von Onlinediensten die finalen Relation-Entitäten.
     *
     * @param ods     Onlinedienste inklusive Transaktionsmetadaten
     * @param context Gebündelter Lookup-Kontext des Batches
     * @return Liste der zu indexierenden Onlinedienst-Relationen
     */
    List<OnlinedienstRelationen> createRelationEntities(
        Collection<OnlinedienstMitMetadaten> ods,
        BatchRelationContext context
    ) {
        return ods
            .stream()
            .map(od -> createOnlinediensteRelationen(od, context))
            .toList();
    }

    /**
     * Baut die Relation für einen Onlinedienst als Pipeline:
     * Eingabedaten auflösen → Service-Relationen und OE-Relationen berechnen → Zielmodell befüllen.
     *
     * @param odMitMetadaten Onlinedienst inklusive Transaktionsmetadaten
     * @param context        Lookup-Kontext des Batches
     * @return vollständig aufgebaute Relation-Entität
     */
    private OnlinedienstRelationen createOnlinediensteRelationen(
        OnlinedienstMitMetadaten odMitMetadaten,
        BatchRelationContext context
    ) {
        OnlinedienstAssemblyInput input = resolveAssemblyInput(odMitMetadaten, context);
        RelationParts relationParts = assembleRelationParts(
            input.odZustaendigkeitenByLbId(),
            context
        );
        return buildOnlinediensteRelationen(input.onlinedienst(), relationParts, input.metadata());
    }

    private OnlinedienstAssemblyInput resolveAssemblyInput(
        OnlinedienstMitMetadaten odMitMetadaten,
        BatchRelationContext context
    ) {
        GenericOnlinedienst onlinedienst = odMitMetadaten.onlinedienst();
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId = context
            .odZustaendigkeiten()
            .forOnlinedienst(onlinedienst.getId());
        if (odZustaendigkeitenByLbId.isEmpty()) {
            log.info(
                "OD-Relation Berechnung: Keine Zuständigkeiten für OD '{}' gefunden. Erstelle leere Relation.",
                onlinedienst.getId()
            );
        }
        return new OnlinedienstAssemblyInput(
            onlinedienst,
            odMitMetadaten.metadata(),
            odZustaendigkeitenByLbId
        );
    }

    private RelationParts assembleRelationParts(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        BatchRelationContext context
    ) {
        return new RelationParts(
            buildServiceRelations(odZustaendigkeitenByLbId, context.leikas()),
            buildOrganisationRelations(odZustaendigkeitenByLbId, context)
        );
    }

    private List<OrganisationseinheitRollenZuordnung> buildOrganisationRelations(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        BatchRelationContext context
    ) {
        Map<String, List<Role>> rollenByOeId = roleExtractor.extractRoles(
            odZustaendigkeitenByLbId,
            context.oeZustaendigkeiten(),
            context.leikas()
        );
        return oeStammdatenEnricher.enrich(rollenByOeId, context.oeStammdaten());
    }

    /**
     * Überführt die gruppierten Zuständigkeiten je LB-ID in Service-Relationen.
     * Ergebnis ist alphabetisch nach LB-ID sortiert.
     */
    private static List<LeistungRegionZuordnung> buildServiceRelations(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        LeikaLookup leikas
    ) {
        return odZustaendigkeitenByLbId
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry ->
                new LeistungRegionZuordnung(
                    entry.getKey(),
                    leikas.forLeistung(entry.getKey()),
                    entry
                        .getValue()
                        .stream()
                        .map(ConvertedZustaendigkeit::getArs)
                        .collect(Collectors.toSet())
                )
            )
            .toList();
    }

    /**
     * Befüllt das persistierbare {@link OnlinedienstRelationen}-Modell.
     *
     * @param od            Onlinedienst (XZuFi)
     * @param relationParts vorberechnete Service- und Organisations-Relationen
     * @param metadata      Transaktionsmetadaten
     * @return befüllte {@link OnlinedienstRelationen}
     */
    private static OnlinedienstRelationen buildOnlinediensteRelationen(
        GenericOnlinedienst od,
        RelationParts relationParts,
        TransactionMetadata metadata
    ) {
        OnlinedienstRelationen relation = new OnlinedienstRelationen();
        OnlinedienstErweitert xod = od.getOrConvertXZuFi23();
        relation.setId(XZuFiUtil.getIdentificatorString(xod.getId(), IdentificationType.OD));
        relation.setMetaData(metadata);
        relation.setName(
            XZuFiUtil.getFirstStringLocalizedByLanguage(
                xod.getBezeichnung(),
                LocaleUtils.DEFAULT_LOCALE
            ).orElse(null)
        );
        relation.setDescription(
            XZuFiUtil.getFirstStringLocalizedHTMLByLanguage(
                xod.getBeschreibung(),
                LocaleUtils.DEFAULT_LOCALE
            ).orElse(null)
        );
        relation.setLinks(mapOnlinedienstLinks(xod));
        relation.setLastUpdate(Instant.now());
        relation.setExternalLastUpdate(
            XZuFiUtil.getVersionUpdateDate(xod.getVersionsinformation())
        );
        relation.setServiceRelations(relationParts.serviceRelations());
        relation.setOrganisationRelations(relationParts.organisationRelations());
        return relation;
    }

    /**
     * Vermeidet unnötige Initialisierung des Link-Mappers für Onlinedienste ohne Links.
     */
    private static List<LinkDynamic> mapOnlinedienstLinks(OnlinedienstErweitert xod) {
        if (xod.getLink() == null || xod.getLink().isEmpty()) {
            return List.of();
        }
        return XZuFiUtil.getOnlinedienstHyperlinks(xod.getLink(), LocaleUtils.DEFAULT_LOCALE);
    }
}
