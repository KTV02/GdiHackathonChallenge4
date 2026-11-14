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

import com.google.common.collect.ImmutableMap;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.parser.ParserRegistry;
import de.dataport.suchdienst.indexer.parser.x23.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.DokumentModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.FristModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.KostenModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulIndividuellParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.TextModulParser;
import de.dataport.suchdienst.indexer.parser.x23.modul.UrsprungsportalModulParser;
import de.xoev.xzufi.v2_3_1.Bearbeitungsdauermodul;
import de.xoev.xzufi.v2_3_1.Dokumentmodul;
import de.xoev.xzufi.v2_3_1.FachlicheFreigabeModul;
import de.xoev.xzufi.v2_3_1.Fristmodul;
import de.xoev.xzufi.v2_3_1.Kostenmodul;
import de.xoev.xzufi.v2_3_1.Textmodul;
import de.xoev.xzufi.v2_3_1.TextmodulIndividuell;
import de.xoev.xzufi.v2_3_1.UrsprungsportalModul;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration("parserConfigX23")
public class ParserConfig implements ParserRegistry {

    private final Map<Class<?>, Parser<?, ?>> parsers;

    public ParserConfig(
        @Qualifier("kostenModulParserX23") KostenModulParser kostenModulParser,
        @Qualifier(
            "textModulIndividuellParserX23"
        ) TextModulIndividuellParser textModulIndividuellParser,
        @Qualifier("dokumentModulParserX23") DokumentModulParser dokumentModulParser,
        @Qualifier("fristModulParserX23") FristModulParser fristModulParser,
        @Qualifier(
            "bearbeitungsdauerModulParserX23"
        ) BearbeitungsdauerModulParser bearbeitungsdauerModulParser,
        @Qualifier("textModulParserX23") TextModulParser textModulParser,
        @Qualifier("ursprungsportalModulParserX23") UrsprungsportalModulParser ursprungParser,
        @Qualifier("fachlicheFreigabeModulParserX23") FachlicheFreigabeModulParser freigabeParser
    ) {
        this.parsers = ImmutableMap.<Class<?>, Parser<?, ?>>builder()
            .put(Fristmodul.class, fristModulParser)
            .put(Bearbeitungsdauermodul.class, bearbeitungsdauerModulParser)
            .put(Kostenmodul.class, kostenModulParser)
            .put(Dokumentmodul.class, dokumentModulParser)
            .put(TextmodulIndividuell.class, textModulIndividuellParser)
            .put(Textmodul.class, textModulParser)
            .put(UrsprungsportalModul.class, ursprungParser)
            .put(FachlicheFreigabeModul.class, freigabeParser)
            .build();
    }
}
