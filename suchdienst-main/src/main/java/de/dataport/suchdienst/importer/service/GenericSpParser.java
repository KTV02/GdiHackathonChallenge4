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

import de.dataport.suchdienst.importer.model.GenericLeistungSpezialisierung;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenericSpParser {

    public List<SpezialisierungEntity> parseSpsElastic(
        Stream<GenericLeistungSpezialisierung> xzufiSps,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        var mdcMap = MdcTemplate.currentContext();
        return xzufiSps
            .parallel()
            .map(xsp ->
                MdcTemplate.callWithContext(mdcMap, () -> {
                    String spId = xsp.getId();
                    String lbId = xsp.getLbId();
                    try (var _ = MdcTemplate.scoped(MdcKeys.SPID, spId)) {
                        return this.parseSpElastic(xsp, lbId, spId, metaData, lastUpdate);
                    }
                })
            )
            .toList();
    }

    public SpezialisierungEntity parseSpElastic(
        GenericLeistungSpezialisierung ls,
        String lbId,
        String spId,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        log.debug("Parse Spezialisierung mit der ID {}", spId);
        return switch (ls.getVersion()) {
            case XZUFI22 -> {
                SpezialisierungEntity22 sp = new SpezialisierungEntity22(lastUpdate);
                sp.setJzufi(ls.getXZuFi22());
                sp.setMetaData(metaData);
                sp.setIdLB(lbId);
                sp.setId(spId);
                Set<String> arsFederalStates = ls.getArsFederalState();
                sp.getArsFederalState().addAll(arsFederalStates);
                yield sp;
            }
            case null, default -> {
                SpezialisierungEntity23 sp = new SpezialisierungEntity23(lastUpdate);
                sp.setJzufi(ls.getXZuFi23());
                sp.setMetaData(metaData);
                sp.setIdLB(lbId);
                sp.setId(spId);
                Set<String> arsFederalStates = ls.getArsFederalState();
                sp.getArsFederalState().addAll(arsFederalStates);
                yield sp;
            }
        };
    }
}
