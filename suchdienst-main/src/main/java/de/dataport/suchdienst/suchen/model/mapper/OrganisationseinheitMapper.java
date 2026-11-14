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

import de.dataport.suchdienst.indexer.model.mapping.Communication;
import de.dataport.suchdienst.indexer.model.mapping.Link;
import de.dataport.suchdienst.indexer.model.mapping.Location;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole;
import de.dataport.suchdienst.suchen.model.out.organisation.DetailOeOutModelV5;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { FormularMapper.class, CommonMapper.class, ResponsibilityMapper.class })
public interface OrganisationseinheitMapper {
    OrganisationseinheitMapper INSTANCE = Mappers.getMapper(OrganisationseinheitMapper.class);

    @Mapping(source = "oe.name", target = "name")
    @Mapping(source = "oe.forms", target = "internetAddresses")
    @Mapping(source = "mergedRole", target = "role")
    @Mapping(source = "communicationSystemsForArs", target = "communicationSystems")
    DetailOeOutModelV5 mapV5(
        OrganisationseinheitVL oe,
        ResponsibilityRole mergedRole,
        List<Communication> communicationSystemsForArs
    );

    @Mapping(target = "urls", ignore = true)
    @Mapping(target = "information", ignore = true)
    de.dataport.suchdienst.suchen.model.out.organisation.Location mapLocation(Location location);

    @Mapping(target = "dynamicParameters", ignore = true)
    de.dataport.suchdienst.suchen.model.out.common.AdditionalLink mapAdditionalLink(Link link);
}
