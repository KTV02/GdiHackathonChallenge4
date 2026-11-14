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

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
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
public class GenericOdParser {

    public List<OnlinedienstEntity> parseOnlinediensteElastic(
        Stream<GenericOnlinedienst> xzufiOds,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        var mdcMap = MdcTemplate.currentContext();
        return xzufiOds
            .parallel()
            .map(xod ->
                MdcTemplate.callWithContext(mdcMap, () -> {
                    try (var _ = MdcTemplate.scoped(MdcKeys.ODID, xod.getId())) {
                        return parseOnlinedienstElastic(xod, metaData, lastUpdate);
                    }
                })
            )
            .toList();
    }

    private OnlinedienstEntity parseOnlinedienstElastic(
        GenericOnlinedienst xzufiOd,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        log.debug("Parse Onlinedienst mit der ID {}.", xzufiOd.getId());
        return switch (xzufiOd.getVersion()) {
            case XZUFI22 -> {
                OnlinedienstEntity22 od = new OnlinedienstEntity22(lastUpdate);
                od.setMetaData(metaData);
                od.setId(xzufiOd.getId());
                od.setJzufi(xzufiOd.getXZuFi22());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    od.getArsFederalState()::add
                );
                od.setValidityPeriods(
                    XZuFiUtil.getValidityPeriods(xzufiOd.getXZuFi22().getGueltigkeit())
                );
                yield od;
            }
            case XZUFI23 -> {
                OnlinedienstEntity23 od = new OnlinedienstEntity23(lastUpdate);
                od.setMetaData(metaData);
                od.setId(xzufiOd.getId());
                od.setJzufi(xzufiOd.getXZuFi23());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    od.getArsFederalState()::add
                );
                od.setValidityPeriods(
                    de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getValidityPeriods(
                        xzufiOd.getXZuFi23().getGueltigkeit()
                    )
                );
                yield od;
            }
        };
    }
}
