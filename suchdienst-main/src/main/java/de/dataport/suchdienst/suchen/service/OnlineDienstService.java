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

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.elasticsearch.api.ElasticPublicSearchConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.importer.model.GenericModelMapper;
import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.dataport.suchdienst.indexer.model.mapper.BusinessObjectMapper;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.mapper.OnlinedienstMapper;
import de.dataport.suchdienst.suchen.model.mapper.ZustaendigkeitMapper;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdRelationOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceOutModelV2;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.AddressType;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OnlineServiceIteratorResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OnlineServiceResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationAddressResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationRoleResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.RoleType;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.ServiceDescriptionInfo;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnlineDienstService {

    public static final int WINDOW_SIZE = 1_000;
    private final ElasticPublicSearchConnector publicSearchConnector;
    private final ElasticReadConnector readConnector;
    private final MessageSource messageSource;
    private final PvogRequestContext context;

    public OnlineDienstService(
        ElasticPublicSearchConnector publicSearchConnector,
        ElasticReadConnector readConnector,
        MessageSource messageSource,
        PvogRequestContext context
    ) {
        this.publicSearchConnector = publicSearchConnector;
        this.readConnector = readConnector;
        this.messageSource = messageSource;
        this.context = context;
    }

    /**
     * Liefert das OutModel des Onlinedienstes.
     *
     * @param odId ID des Onlinedienstes (muss mindestens zwei Zeichen lang sein). Darf nicht null sein.
     * @param validDate optionales Datum, an dem der Onlinedienst existieren soll.
     * @return OutModel des Onlinedienstes
     * @throws NullPointerException wenn {@code odId} null ist
     * @throws IllegalArgumentException wenn {@code odId} kürzer als 2 Zeichen ist
     */
    public OnlineServiceOutModelV2 getOnlineServiceV2ById(@NonNull String odId, LocalDate validDate)
        throws NullPointerException, IllegalArgumentException {
        if (odId.length() < 2) {
            throw new IllegalArgumentException("odId must be at least 2 characters long");
        }
        Optional<OnlinedienstVL> oOd = publicSearchConnector.searchOnlinedienst(odId, validDate);
        if (oOd.isPresent()) {
            OnlinedienstVL od = oOd.get();

            // Lädt alle Relationen zum Onlinedienst
            List<ZustaendigkeitEntity> loadedRelations = readConnector.searchRelations(
                null,
                List.of(od.getId()),
                null,
                List.of("id", "lbId", "ars", "parentId")
            );

            Set<OdRelationOutModel> odRelations = loadedRelations
                .stream()
                .map(ZustaendigkeitMapper.INSTANCE::map)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            applyLanguageOverride(od);

            try {
                return OnlinedienstMapper.INSTANCE.map(od, odRelations);
            } catch (Exception e) {
                throw new SuchenException(
                    ErrorCodeEnum.UNKNOWN_ERROR,
                    "Onlinedienst " +
                        OnlineServiceOutModelV2.class.getSimpleName() +
                        " konnte nicht instanziiert werden. OnlinedienstVL: " +
                        od +
                        ", Zuständigkeiten: " +
                        odRelations,
                    e
                );
            }
        } else {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                messageSource.getMessage("api.404", null, Locale.GERMAN)
            );
        }
    }

    public OnlineServiceDetail getOnlineServiceV3Beta1ById(@NonNull String odId) {
        if (odId.length() < 2) {
            throw new IllegalArgumentException("odId must be at least 2 characters long");
        }
        LocalDate validDate = LocalDate.now();
        OnlinedienstVL od = publicSearchConnector
            .searchOnlinedienst(odId, validDate)
            .orElseThrow(() ->
                new SuchenException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage("api.404", null, Locale.GERMAN)
                )
            );
        Set<OdRelationOutModel> odRelations = loadRelations(od);
        applyLanguageOverride(od);

        try {
            return OnlinedienstMapper.INSTANCE.mapV3Beta1(od, odRelations);
        } catch (Exception e) {
            throw new SuchenException(
                ErrorCodeEnum.UNKNOWN_ERROR,
                "Onlinedienst " +
                    OnlineServiceDetail.class.getSimpleName() +
                    " konnte nicht instanziiert werden. OnlinedienstVL: " +
                    od +
                    ", Zuständigkeiten: " +
                    odRelations,
                e
            );
        }
    }

    private Set<OdRelationOutModel> loadRelations(OnlinedienstVL od) {
        return readConnector
            .searchRelations(
                null,
                List.of(od.getId()),
                null,
                List.of("id", "lbId", "ars", "parentId")
            )
            .stream()
            .map(ZustaendigkeitMapper.INSTANCE::map)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Liefert den Onlinedienst im generischen JZuFi-Modell, damit er wahlweise
     * in der XZuFi-Version 2.2.0 oder 2.3.1 ausgeliefert werden kann.
     *
     * @param odId      ID des Onlinedienstes
     * @param validDate optionales Gültigkeitsdatum
     * @return das gefundene generische Onlinedienst-Modell, sofern vorhanden
     */
    public Optional<GenericOnlinedienst> getJzufi(String odId, LocalDate validDate) {
        return readConnector
            .searchById(
                odId,
                List.of(),
                List.of(),
                IndexNames.ONLINEDIENSTE,
                validDate,
                OnlinedienstEntity.class
            )
            .map(GenericModelMapper::mapOnlinedienstEntity);
    }

    private void applyLanguageOverride(OnlinedienstVL od) {
        Locale language = context.getLocale();
        if (!language.equals(Locale.GERMAN)) {
            OnlinedienstContent content = od.getOtherLanguages().get(language);
            if (content != null) {
                BusinessObjectMapper.INSTANCE.copyContent(content, od);
            }
        }
    }

    public OnlineServiceIteratorResponse getOnlineServicesByArsViaOdRelationIndex(
        String arsPrefix,
        List<String> leikaIds,
        String nextPageToken
    ) {
        var hierarchy = new ARS(arsPrefix).getArsHierarchy();
        hierarchy.add(ARS.BUND);
        var queryBuilder = QueryBuilders.bool()
            .should(Elastic.QueryHelper.terms("serviceRelations.ars", hierarchy))
            .should(Elastic.QueryHelper.prefix("serviceRelations.ars.prefix", arsPrefix))
            .minimumShouldMatch("1");

        if (ObjectUtils.isNotEmpty(leikaIds)) {
            queryBuilder.filter(
                Elastic.QueryHelper.terms(
                    "serviceRelations.leikakeys",
                    Elastic.QueryHelper.escape(leikaIds)
                )
            );
        }

        var query = queryBuilder.build()._toQuery();

        SourceConfig sourceConfig = SourceConfig.of(c ->
            c.filter(
                Elastic.QueryHelper.sourceFilter(
                    List.of(
                        "id",
                        "name",
                        "description",
                        "links",
                        "serviceRelations",
                        "externalLastUpdate"
                    ),
                    null
                )
            )
        );

        HitsMetadata<OnlinedienstRelationen> result = readConnector.searchAfter(
            AliasNames.ONLINEDIENSTE_RELATIONEN,
            nextPageToken,
            WINDOW_SIZE,
            query,
            sourceConfig,
            true,
            OnlinedienstRelationen.class
        );

        Long totalHits = result.total() != null ? result.total().value() : null;
        if (result.hits().isEmpty()) {
            return new OnlineServiceIteratorResponse("", 0L, totalHits, List.of());
        }

        // lastId ist eine OD-ID, da OnlinediensteRelationen das Interface Identifiable implementiert.
        //  Anderenfalls wäre es die von ES vergebene Dokumenten-ID.
        String lastId = result.hits().size() < WINDOW_SIZE ? "" : result.hits().getLast().id();
        List<OnlineServiceResult> onlineServiceResults = result
            .hits()
            .stream()
            .map(Hit::source)
            .filter(Objects::nonNull)
            .map(entity -> {
                List<ServiceDescriptionInfo> relations = entity
                    .getServiceRelations()
                    .stream()
                    .map(r -> new ServiceDescriptionInfo(r.lbId(), r.ars(), r.leikakeys()))
                    .toList();
                List<HyperLink> hyperlinks = entity
                    .getLinks()
                    .stream()
                    .map(uri -> new HyperLink(uri.uri(), uri.title(), uri.description()))
                    .toList();
                return new OnlineServiceResult(
                    entity.getId(),
                    entity.getName(),
                    entity.getDescription(),
                    hyperlinks,
                    entity.getExternalLastUpdate(),
                    relations
                );
            })
            .toList();

        long hits = onlineServiceResults.size();
        return new OnlineServiceIteratorResponse(lastId, hits, totalHits, onlineServiceResults);
    }

    /**
     * Liefert die zuständigen Organisationseinheiten eines Onlinedienstes
     * aus dem OD-Relation-Index.
     *
     * @param odId ID des Onlinedienstes
     * @return Organisations-Response mit der Liste der OEs
     * @throws SuchenException mit NOT_FOUND wenn kein OD mit der ID existiert
     */
    public OrganisationResponse getOrganisationsByOdId(String odId) {
        Optional<OnlinedienstRelationen> result = readConnector.getById(
            AliasNames.ONLINEDIENSTE_RELATIONEN,
            odId,
            OnlinedienstRelationen.class
        );

        if (result.isEmpty()) {
            throw new SuchenException(
                ErrorCodeEnum.NOT_FOUND,
                messageSource.getMessage("api.404", null, Locale.GERMAN)
            );
        }

        OnlinedienstRelationen entity = result.get();
        List<OnlinedienstRelationen.OrganisationseinheitRollenZuordnung> oeRelations =
            entity.getOrganisationRelations();

        if (oeRelations.isEmpty()) {
            return new OrganisationResponse(List.of(), null);
        }

        List<OrganisationResult> organisations = oeRelations
            .stream()
            .map(OnlineDienstService::toOrganisationResult)
            .toList();

        return new OrganisationResponse(organisations, entity.getExternalLastUpdate());
    }

    private static OrganisationResult toOrganisationResult(
        OnlinedienstRelationen.OrganisationseinheitRollenZuordnung organisation
    ) {
        return new OrganisationResult(
            organisation.oeId(),
            organisation.name(),
            toAddressResults(organisation.addresses()),
            toRolleResults(organisation.rollen())
        );
    }

    private static List<OrganisationAddressResult> toAddressResults(
        List<OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Address> addresses
    ) {
        return addresses
            .stream()
            .map(address ->
                new OrganisationAddressResult(
                    AddressType.fromIndexerType(address.type()),
                    address.city(),
                    address.zip(),
                    address.street(),
                    address.houseNumber(),
                    address.postbox()
                )
            )
            .toList();
    }

    private static List<OrganisationRoleResult> toRolleResults(
        List<OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role> rollen
    ) {
        return rollen
            .stream()
            .map(rolle ->
                new OrganisationRoleResult(
                    RoleType.fromIndexerType(rolle.role()),
                    rolle.lbId(),
                    rolle.leikakeys(),
                    rolle.ars()
                )
            )
            .toList();
    }
}
