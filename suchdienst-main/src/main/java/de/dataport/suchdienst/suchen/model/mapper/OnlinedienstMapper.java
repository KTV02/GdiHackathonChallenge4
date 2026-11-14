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
package de.dataport.suchdienst.suchen.model.mapper;

import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.OnlinedienstStatus;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.OnlineServiceLink;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdOutModelV4;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdRelationOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceOutModelV2;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceStatus;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlinedienstParameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { CommonMapper.class })
public interface OnlinedienstMapper {
    OnlinedienstMapper INSTANCE = Mappers.getMapper(OnlinedienstMapper.class);

    @Mapping(target = "title", expression = "java(getTitle(od, link))")
    @Mapping(target = "uri", source = "link.uri")
    @Mapping(target = "description", expression = "java(getDescription(od, link))")
    @Mapping(
        target = "status",
        source = "od.onlinedienstStatus",
        qualifiedByName = "mapOnlinedienstStatus"
    )
    @Mapping(target = "publicDisplayAllowed", source = "od.oeffentlichAnzeigen")
    OnlineServiceLink mapOdOutModelV6(OnlinedienstVL od, LinkDynamic link);

    @Mapping(target = "title", expression = "java(getTitle(od, link))")
    @Mapping(target = "validityPeriods", source = "od.validityPeriods")
    @Mapping(target = "validTo", source = "od.validityPeriods", qualifiedByName = "validTo")
    @Mapping(target = "validFrom", source = "od.validityPeriods", qualifiedByName = "validFrom")
    @Mapping(target = "uri", source = "link.uri")
    @Mapping(target = "description", source = "link.description")
    @Mapping(target = "dynamicParameters", source = "link.dynamicParameters")
    OdOutModel mapOdOutModel(OnlinedienstVL od, LinkDynamic link);

    @InheritConfiguration
    OdOutModelV4 mapOdOutModelV4(OnlinedienstVL od, LinkDynamic link);

    @Mapping(
        source = "onlinedienst.ars",
        target = "arsFederalstate",
        qualifiedByName = "relationsToFederalState"
    )
    @Mapping(target = "parameters", expression = "java(getParameters(onlinedienst))")
    OnlineServiceOutModelV2 map(OnlinedienstVL onlinedienst, Set<OdRelationOutModel> relations);

    @Mapping(
        source = "onlinedienst.ars",
        target = "arsFederalstate",
        qualifiedByName = "relationsToFederalState"
    )
    @Mapping(
        source = "onlinedienst.onlinedienstStatus",
        target = "status",
        qualifiedByName = "mapOnlinedienstStatus"
    )
    @Mapping(
        target = "publicDisplayAllowed",
        source = "onlinedienst.oeffentlichAnzeigen",
        defaultValue = "true"
    )
    OnlineServiceDetail mapV3Beta1(OnlinedienstVL onlinedienst, Set<OdRelationOutModel> relations);

    @Mapping(target = "dynamicParameters", ignore = true)
    AdditionalLink mapHelpLink(Link link);

    default List<HyperLink> mapDynamicLinks(List<LinkDynamic> links) {
        return links.stream().map(this::mapDynamicLink).toList();
    }

    HyperLink mapDynamicLink(LinkDynamic link);

    HyperLink mapLink(Link link);

    default Set<OnlinedienstParameter> getParameters(OnlinedienstVL onlinedienst) {
        return Stream.concat(
            onlinedienst.getParametersEfa().stream(),
            onlinedienst.getParametersCustom().stream()
        )
            .map(parameter -> new OnlinedienstParameter(parameter.getName(), parameter.getValue()))
            .collect(Collectors.toSet());
    }

    @Named("relationsToFederalState")
    default Set<String> mapArs(Set<String> arss) {
        return arss
            .stream()
            .map(ars -> ars.substring(0, 2))
            .collect(Collectors.toSet());
    }

    @Named("mapOnlinedienstStatus")
    default OnlineServiceStatus mapOnlinedienstStatus(CodelistKey status) {
        return OnlineServiceStatus.valueOf(OnlinedienstStatus.from(status).name());
    }

    /** Mappt die Onlinedienste einer Leistung zu den Onlinedienst-Links der V7-Suche. */
    default List<OdOutModel> getOnlineServiceLinks(List<OnlinedienstVL> onlinedienste) {
        return onlinedienste
            .stream()
            .flatMap(od ->
                od
                    .getLinks()
                    .stream()
                    .map(link -> mapOdOutModel(od, link))
            )
            .toList();
    }

    default List<OdOutModelV4> getOnlineServiceLinksV4(List<OnlinedienstVL> onlinedienste) {
        return onlinedienste
            .stream()
            .flatMap(od ->
                od
                    .getLinks()
                    .stream()
                    .map(link -> mapOdOutModelV4(od, link))
            )
            .toList();
    }

    /** Mappt die Onlinedienste einer Leistung zu den Onlinedienst-Links der V6/V8Beta1-Suche. */
    default List<OnlineServiceLink> getOnlineServiceLinksV6(List<OnlinedienstVL> onlinedienste) {
        return onlinedienste
            .stream()
            .flatMap(od ->
                od
                    .getLinks()
                    .stream()
                    .map(link -> mapOdOutModelV6(od, link))
            )
            .toList();
    }

    /**
     * Gibt den als Titel zu verwendenden String zurück - PV-10067
     * URIs werden nur dann als String dargestellt, wenn kein Titel und keine Bezeichnung (aus OD) vorhanden sind.
     * Titel (da spezifischer) hat Vorrang vor Bezeichnung (aus OD).
     */
    default String getTitle(OnlinedienstVL od, LinkDynamic link) {
        String title;

        if (StringUtils.isNotEmpty(link.title())) {
            title = link.title();
        } else if (StringUtils.isNotEmpty(od.getName())) {
            title = od.getName();
        } else {
            title = link.uri();
        }

        return title;
    }

    /**
     * Gibt den als Beschreibung zu verwendenden String zurück. Beschreibung aus ODLink hat Vorrang vor Beschreibung aus OD.
     */
    default String getDescription(OnlinedienstVL od, LinkDynamic link) {
        String description;

        if (StringUtils.isNotEmpty(link.description())) {
            description = link.description();
        } else {
            description = od.getDescription();
        }

        return description;
    }
}
