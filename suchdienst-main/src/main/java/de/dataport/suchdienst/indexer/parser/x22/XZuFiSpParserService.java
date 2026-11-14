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
package de.dataport.suchdienst.indexer.parser.x22;

import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.SpezialisierungVL;
import de.dataport.suchdienst.indexer.parser.x22.modul.LeistungSpezialisierungParser;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.xoev.xzufi.v2_2_0.LeistungSpezialisierung;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * handles parsing of specialization
 */
@Slf4j
@Service("xZuFiSpParserServiceX22")
public class XZuFiSpParserService {

    private final LeistungSpezialisierungParser leistungSpezialisierungParser;

    public XZuFiSpParserService(
        @Qualifier(
            "leistungSpezialisierungParserX22"
        ) LeistungSpezialisierungParser leistungSpezialisierungParser
    ) {
        this.leistungSpezialisierungParser = leistungSpezialisierungParser;
    }

    public SpezialisierungVL parseSpezialisierung(
        LeistungSpezialisierung ls,
        String lbId,
        String spId
    ) {
        log.debug("Parse Spezialisierung mit der ID {}", spId);
        if (spId == null) {
            spId = XZuFiUtil.getIdentificatorString(ls.getIdLeistung(), IdentificationType.LB)
                .concat(".SP.")
                .concat(
                    XZuFiUtil.getIdentificatorString(
                        ls.getIdGebiet().getFirst(),
                        IdentificationType.SP
                    )
                );
        }
        return leistungSpezialisierungParser.parse(ls, lbId, spId);
    }
}
