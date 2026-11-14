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

import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.util.ModulesStringExtractor;
import de.dataport.suchdienst.suchen.model.out.csv.SdCsvModel;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ServiceDescriptionCSVMapper {
    ServiceDescriptionCSVMapper INSTANCE = Mappers.getMapper(ServiceDescriptionCSVMapper.class);

    @Mapping(target = "id", source = "lb.id")
    @Mapping(target = "title", source = "lb", qualifiedByName = "lb_name")
    @Mapping(target = "title2", source = "lb", qualifiedByName = "lb_nameOther")
    @Mapping(target = "leikaIds", source = "lb.leikakeys")
    @Mapping(target = "odId", source = "od.id")
    @Mapping(target = "odName", source = "od.name")
    @Mapping(target = "urls", source = "od.links")
    SdCsvModel map(LeistungsbeschreibungVL lb, OnlinedienstVL od, Instant timestamp);

    default List<String> linksToUriStrings(List<LinkDynamic> links) {
        return links.stream().map(LinkDynamic::uri).toList();
    }

    @Named("lb_name")
    default String getName(LeistungsbeschreibungVL leistungsbeschreibung) {
        return ModulesStringExtractor.findName(leistungsbeschreibung.getDetails()).orElse(null);
    }

    @Named("lb_nameOther")
    default String getNameOther(LeistungsbeschreibungVL leistungsbeschreibung) {
        return ModulesStringExtractor.findNameOtherDescription(
            leistungsbeschreibung.getDetails()
        ).orElse(null);
    }
}
