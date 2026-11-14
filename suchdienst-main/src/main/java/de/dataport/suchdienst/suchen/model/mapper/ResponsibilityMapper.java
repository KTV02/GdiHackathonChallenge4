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

import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.suchen.model.out.organisation.ResponsibilityCriterion;
import java.util.List;
import java.util.Locale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ResponsibilityMapper {
    ResponsibilityMapper INSTANCE = Mappers.getMapper(ResponsibilityMapper.class);

    ResponsibilityCriterion mapCriterion(
        de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion responsibilityCriterion
    );

    @Mapping(
        target = "name",
        expression = "java(responsibilityCriterion.getOtherLanguages().get(locale))"
    )
    ResponsibilityCriterion mapCriterion(
        de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion responsibilityCriterion,
        Locale locale
    );

    default List<ResponsibilityCriterion> mapCriterions(
        List<de.dataport.suchdienst.indexer.model.mapping.ResponsibilityCriterion> responsibilityCriterion,
        Locale locale
    ) {
        return responsibilityCriterion
            .stream()
            .map(role -> {
                if (Locale.GERMAN.equals(locale)) {
                    return mapCriterion(role);
                } else {
                    return mapCriterion(role, locale);
                }
            })
            .toList();
    }

    de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole mapRole(
        ResponsibilityRole responsibility
    );

    @Mapping(target = "name", expression = "java(responsibility.getOtherLanguages().get(locale))")
    de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole mapRole(
        ResponsibilityRole responsibility,
        Locale locale
    );

    default List<de.dataport.suchdienst.suchen.model.out.common.ResponsibilityRole> mapRoles(
        List<ResponsibilityRole> responsibility,
        Locale locale
    ) {
        return responsibility
            .stream()
            .map(role -> {
                if (Locale.GERMAN.equals(locale)) {
                    return mapRole(role);
                } else {
                    return mapRole(role, locale);
                }
            })
            .toList();
    }
}
