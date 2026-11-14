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

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularFileLink;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.FormularVL;
import de.dataport.suchdienst.suchen.model.out.formular.FoOutModel;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FormularMapper {
    FormularMapper INSTANCE = Mappers.getMapper(FormularMapper.class);

    @Mapping(source = "fileLink.uri", target = "uri")
    @Mapping(source = "fileLink.mimeCode", target = "mimeCode")
    FoOutModel map(FormularVL fo, FormularFileLink fileLink);

    default List<FoOutModel> map(List<FormularVL> formularVls) {
        return formularVls
            .stream()
            .flatMap(fo ->
                fo
                    .getFileLinks()
                    .stream()
                    .map(link -> map(fo, link))
            )
            .toList();
    }
}
