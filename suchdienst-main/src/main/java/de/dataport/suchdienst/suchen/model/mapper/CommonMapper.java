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

import de.dataport.suchdienst.indexer.model.mapping.DynamischerParameter;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CommonMapper {
    CommonMapper INSTANCE = Mappers.getMapper(CommonMapper.class);

    @Mapping(target = "typ.code", source = "code")
    @Mapping(target = "parameterName", source = "parameterName")
    de.dataport.suchdienst.suchen.model.out.xzuficopy.OnlinedienstDynamischerParameter map(
        DynamischerParameter parameter
    );

    default List<ValidityPeriodOutModel> map(List<ValidityPeriod> validityPeriods) {
        return validityPeriods
            .stream()
            .map(validity -> new ValidityPeriodOutModel(validity.validFrom(), validity.validTo()))
            .toList();
    }

    @Named("validTo")
    default LocalDate mapValidTo(List<ValidityPeriod> validityPeriods) {
        return CollectionUtils.isNotEmpty(validityPeriods)
            ? validityPeriods.getFirst().validTo()
            : null;
    }

    @Named("validFrom")
    default LocalDate mapValidFrom(List<ValidityPeriod> validityPeriods) {
        return CollectionUtils.isNotEmpty(validityPeriods)
            ? validityPeriods.getFirst().validFrom()
            : null;
    }

    @Named("toSingletonList")
    default <T> List<T> toSingletonList(T element) {
        return element == null ? null : List.of(element);
    }
}
