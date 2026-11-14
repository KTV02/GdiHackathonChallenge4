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

import de.dataport.suchdienst.suchen.model.out.leistung.CodelistEntry;
import de.dataport.suchdienst.suchen.model.out.leistung.LeikaTyping;
import de.dataport.suchdienst.suchen.model.out.leistung.XZufiCategory;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = {
        CommonMapper.class,
        FormularMapper.class,
        ServiceDescriptionBlockMapper.class,
        ServiceDescriptionBlockV6Mapper.class,
        OnlinedienstMapper.class,
    }
)
public interface ServiceDescriptionMapper {
    ServiceDescriptionMapper INSTANCE = Mappers.getMapper(ServiceDescriptionMapper.class);

    Set<CodelistEntry> mapCodelistEntries(
        Set<de.dataport.suchdienst.indexer.model.CodelistEntry> codelistEntries
    );

    List<LeikaTyping> map(List<de.dataport.suchdienst.indexer.model.mapping.LeikaTyping> source);

    XZufiCategory map(de.dataport.suchdienst.indexer.model.mapping.XZufiCategory source);
}
