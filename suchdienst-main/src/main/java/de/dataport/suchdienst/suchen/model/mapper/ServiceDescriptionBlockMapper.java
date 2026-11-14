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

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.mapping.XZufiCategory;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModule;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDate;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetail;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleDetailContent;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.ServiceModuleLink;
import de.dataport.suchdienst.suchen.model.helper.MustContain;
import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModuleDetail;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = { CommonMapper.class },
    subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION
)
public interface ServiceDescriptionBlockMapper {
    ServiceDescriptionBlockMapper INSTANCE = Mappers.getMapper(ServiceDescriptionBlockMapper.class);

    default SeDeBlock map(AbstractServiceModule block, String code, XZufiCategory category) {
        return switch (block) {
            case ServiceModuleDetail m -> map(m, code, category);
            case ServiceModule m -> map(m, code, category);
            case ServiceModuleLink m -> map(m, code, category);
            // Die Ausprägung ServiceModuelDate wurde ab V6 eingeführt und wird vom SeDeBlock nicht unterstützt
            case ServiceModuleDate _ -> null;
        };
    }

    @Mapping(target = "title", source = "block", qualifiedByName = "mapSeDeBlockTitle")
    @Mapping(
        target = "placeholder",
        source = "block.text",
        qualifiedByName = "tempMapContentToPlaceholder"
    )
    @Mapping(target = "filled", expression = "java(block.hasContent())")
    // Wird als Known-Issue auf false gesetzt
    @Mapping(target = "replace", constant = "false")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    SeDeBlockModule map(ServiceModule block, String code, XZufiCategory category);

    @Mapping(target = "title", source = "block", qualifiedByName = "mapSeDeBlockTitle")
    @Mapping(
        target = "placeholder",
        source = "block.text",
        qualifiedByName = "tempMapContentToPlaceholder"
    )
    @Mapping(target = "filled", expression = "java(block.hasContent())")
    // Wird als Known-Issue auf false gesetzt
    @Mapping(target = "replace", constant = "false")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "additionalInformation", ignore = true)
    @Mapping(target = "links", source = "block.link", qualifiedByName = "toSingletonList")
    SeDeBlockModule map(ServiceModuleLink block, String code, XZufiCategory category);

    @Mapping(target = "title", source = "block", qualifiedByName = "mapSeDeBlockTitle")
    @Mapping(target = "filled", expression = "java(block.hasContent())")
    // Wird als Known-Issue auf false gesetzt
    @Mapping(target = "replace", constant = "false")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "code", source = "code")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    @Mapping(
        target = "details",
        expression = "java(mapContents(block.getDetails(), code, category))"
    )
    SeDeBlockModuleDetail map(ServiceModuleDetail block, String code, XZufiCategory category);

    @Mapping(target = "filled", expression = "java(block.hasContent())")
    @Mapping(target = "replace", constant = "false")
    @Mapping(target = "placeholder", constant = "false")
    // Wird durch Jacksons JsonSubTypes-Annotation bestimmt
    @Mapping(target = "type", ignore = true)
    SeDeBlockModule map(ServiceModuleDetailContent block, String code, XZufiCategory category);

    AdditionalLink map(LinkDynamic link);

    @Mapping(target = "dynamicParameters", ignore = true)
    AdditionalLink map(Link link);

    default Collection<SeDeBlockModule> mapContents(
        Collection<ServiceModuleDetailContent> contents,
        String code,
        XZufiCategory category
    ) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents
            .stream()
            .map(content -> map(content, code, category))
            .toList();
    }

    @Named("mapSeDeBlockTitle")
    default String mapSeDeBlockTitle(AbstractServiceModule block) {
        String title = block.getTitle();

        Optional<AccessChannel> accessChannel = switch (block) {
            case ServiceModuleDetail serviceModuleDetail -> Optional.ofNullable(
                serviceModuleDetail.getAccessChannel()
            );
            case ServiceModule serviceModule -> Optional.ofNullable(
                serviceModule.getAccessChannel()
            );
            default -> Optional.empty();
        };

        return accessChannel
            .map(
                channel ->
                    switch (channel) {
                        case ONLINE_OFFLINE -> title;
                        case ONLINE -> title + " (Online)";
                        case OFFLINE -> title + " (Offline)";
                    }
            )
            .orElse(title);
    }

    @Named("tempMapContentToPlaceholder")
    default boolean tempMapContentToPlaceholder(String text) {
        return MustContain.INFO_GERMAN.equals(text) || MustContain.INFO_ENGLISH.equals(text);
    }
}
