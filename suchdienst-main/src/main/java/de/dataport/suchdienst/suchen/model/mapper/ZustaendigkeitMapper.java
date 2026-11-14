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

import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdRelationOutModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ZustaendigkeitMapper {
    ZustaendigkeitMapper INSTANCE = Mappers.getMapper(ZustaendigkeitMapper.class);

    //FIXME Typo responsiblity in Zukünftigen API-Versionen korrigieren!
    @Mapping(target = "responsiblity", source = "responsibility")
    OdRelationOutModel map(ZustaendigkeitEntity zustaendigkeit);
}
