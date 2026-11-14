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

import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.mapping.XZufiCategory;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.AbstractServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModule;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDate;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDetail;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.ServiceModuleDetailContent;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = { CommonMapper.class },
    subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION
)
public interface ServiceDescriptionBlockV6Mapper {
    ServiceDescriptionBlockV6Mapper INSTANCE = Mappers.getMapper(
        ServiceDescriptionBlockV6Mapper.class
    );

    default AbstractServiceModule map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule block,
        String code,
        XZufiCategory category
    ) {
        return switch (block) {
            case de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule m -> map(
                m,
                code,
                category
            );
            case de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail m -> map(
                m,
                code,
                category
            );
            case de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate m -> map(
                m,
                code,
                category
            );
            case de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink m -> map(
                m,
                code,
                category
            );
        };
    }

    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "details", expression = "java(mapContents(block.getDetails()))")
    ServiceModuleDetail map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail block,
        String code,
        XZufiCategory category
    );

    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    ServiceModule map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule block,
        String code,
        XZufiCategory category
    );

    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    //ServiceModuleDate kann fachlich keine Links darstellen
    @Mapping(target = "links", ignore = true)
    ServiceModuleDate map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate block,
        String code,
        XZufiCategory category
    );

    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "links", source = "block.link", qualifiedByName = "mapToList")
    //ServiceModuleDate kann fachlich keine weiterführenden Informationen darstellen
    @Mapping(target = "additionalInformation", ignore = true)
    ServiceModule map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink block,
        String code,
        XZufiCategory category
    );

    default Collection<ServiceModuleDetailContent> mapContents(
        Collection<de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent> contents
    ) {
        if (contents == null) {
            return null;
        }
        return contents.stream().map(this::map).toList();
    }

    ServiceModuleDetailContent map(
        de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent module
    );

    @Named("mapToList")
    default List<de.dataport.suchdienst.suchen.model.out.common.HyperLink> mapToList(
        LinkDynamic link
    ) {
        return List.of(map(link));
    }

    de.dataport.suchdienst.suchen.model.out.common.HyperLink map(Link link);

    de.dataport.suchdienst.suchen.model.out.common.HyperLink map(LinkDynamic link);
}
