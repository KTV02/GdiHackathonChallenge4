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
package de.dataport.suchdienst.indexer.parser.x23;

import de.dataport.suchdienst.indexer.parser.ParserSelector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("parserSelectorConfigX23")
public class ParserSelectorConfig {

    @Bean(name = "parserSelectorX23")
    public ParserSelector parserSelectorX23(
        @Qualifier("parserConfigX23") ParserConfig parserConfig
    ) {
        return ParserSelector.of(parserConfig);
    }
}
