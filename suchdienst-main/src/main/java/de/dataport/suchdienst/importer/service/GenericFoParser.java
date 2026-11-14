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
package de.dataport.suchdienst.importer.service;

import de.dataport.suchdienst.importer.model.GenericFormular;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity22;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenericFoParser {

    public List<FormularEntity> parseFosElastic(
        Stream<GenericFormular> xzufiFos,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        var mdcMap = MdcTemplate.currentContext();
        return xzufiFos
            .parallel()
            .map(xfo ->
                MdcTemplate.callWithContext(mdcMap, () -> {
                    try (var _ = MdcTemplate.scoped(MdcKeys.FOID, xfo.getId())) {
                        return parseFormularElastic(xfo, metaData, lastUpdate);
                    }
                })
            )
            .toList();
    }

    private FormularEntity parseFormularElastic(
        GenericFormular xzufiFo,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        log.debug("Parse Formular mit der ID {}.", xzufiFo.getId());

        return switch (xzufiFo.getVersion()) {
            case XZUFI22 -> {
                FormularEntity22 fo = new FormularEntity22(lastUpdate);
                fo.setJzufi(xzufiFo.getXZuFi22());
                fo.setMetaData(metaData);
                fo.setId(xzufiFo.getId());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    fo.getArsFederalState()::add
                );
                yield fo;
            }
            case XZUFI23 -> {
                FormularEntity23 fo = new FormularEntity23(lastUpdate);
                fo.setJzufi(xzufiFo.getXZuFi23());
                fo.setMetaData(metaData);
                fo.setId(xzufiFo.getId());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    fo.getArsFederalState()::add
                );
                yield fo;
            }
        };
    }
}
