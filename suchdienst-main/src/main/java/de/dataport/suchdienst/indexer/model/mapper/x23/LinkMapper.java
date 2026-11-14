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
package de.dataport.suchdienst.indexer.model.mapper.x23;

import de.dataport.suchdienst.indexer.model.mapping.DynamischerParameter;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.xoev.xzufi.v2_3_1.DynamischerURLParameter;
import de.xoev.xzufi.v2_3_1.OnlinedienstLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LinkMapper {
    LinkMapper INSTANCE = Mappers.getMapper(LinkMapper.class);

    @Mapping(target = "uri", source = "link")
    @Mapping(target = "description", source = "beschreibung")
    @Mapping(target = "title", source = "titel")
    @Mapping(target = "dynamicParameters", source = "dynamischeParameter")
    LinkDynamic map(OnlinedienstLink link);

    @Mapping(target = "code", source = "typ.code")
    @Mapping(target = "parameterName", source = "parameterName")
    DynamischerParameter map(DynamischerURLParameter parameter);
}
