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

import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenericLbParser {

    public List<LeistungEntity> parseLbsElastic(
        Stream<GenericLeistung> unparsedLbs,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        return unparsedLbs.map(xlb -> parseLbElastic(xlb, metaData, lastUpdate)).toList();
    }

    private LeistungEntity parseLbElastic(
        GenericLeistung xlb,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        String lbId = xlb.getId();

        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, lbId)) {
            log.debug("Parse Leistungsbeschreibung mit der ID {}.", xlb.getId());
            return switch (xlb.getVersion()) {
                case XZUFI22 -> {
                    LeistungEntity22 lb = new LeistungEntity22(lastUpdate);
                    lb.setJzufi(xlb.getXZuFi22());
                    lb.setMetaData(metaData);
                    lb.setId(lbId);
                    lb.getArsFederalState().addAll(xlb.getArsFederalState());
                    lb.setLeikaId(xlb.getLeikaId());
                    lb.setValidityPeriods(
                        XZuFiUtil.getValidityPeriods(xlb.getXZuFi22().getGueltigkeit())
                    );
                    yield lb;
                }
                case XZUFI23 -> {
                    LeistungEntity23 lb = new LeistungEntity23(lastUpdate);
                    lb.setJzufi(xlb.getXZuFi23());
                    lb.setMetaData(metaData);
                    lb.setId(lbId);
                    lb.getArsFederalState().addAll(xlb.getArsFederalState());
                    lb.setLeikaId(xlb.getLeikaId());
                    lb.setValidityPeriods(
                        de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getValidityPeriods(
                            xlb.getXZuFi23().getGueltigkeit()
                        )
                    );
                    yield lb;
                }
            };
        }
    }
}
