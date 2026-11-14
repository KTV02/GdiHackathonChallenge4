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

import com.google.common.collect.ImmutableMap;
import de.dataport.suchdienst.indexer.parser.Parser;
import de.dataport.suchdienst.indexer.parser.ParserRegistry;
import de.dataport.suchdienst.indexer.parser.x22.modul.BearbeitungsdauerModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.DokumentModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.FachlicheFreigabeModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.FristModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.KostenModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulIndividuellParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.TextModulParser;
import de.dataport.suchdienst.indexer.parser.x22.modul.UrsprungsportalModulParser;
import de.xoev.xzufi.v2_2_0.Bearbeitungsdauermodul;
import de.xoev.xzufi.v2_2_0.Dokumentmodul;
import de.xoev.xzufi.v2_2_0.FachlicheFreigabeModul;
import de.xoev.xzufi.v2_2_0.Fristmodul;
import de.xoev.xzufi.v2_2_0.Kostenmodul;
import de.xoev.xzufi.v2_2_0.Textmodul;
import de.xoev.xzufi.v2_2_0.TextmodulIndividuell;
import de.xoev.xzufi.v2_2_0.UrsprungsportalModul;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration("parserConfigX22")
public class ParserConfig implements ParserRegistry {

    private final Map<Class<?>, Parser<?, ?>> parsers;

    public ParserConfig(
        @Qualifier("kostenModulParserX22") KostenModulParser kostenParser,
        @Qualifier(
            "textModulIndividuellParserX22"
        ) TextModulIndividuellParser textIndividuellParser,
        @Qualifier("dokumentModulParserX22") DokumentModulParser dokumentParser,
        @Qualifier("fristModulParserX22") FristModulParser fristParser,
        @Qualifier(
            "bearbeitungsdauerModulParserX22"
        ) BearbeitungsdauerModulParser bearbeitungsdauerParser,
        @Qualifier("textModulParserX22") TextModulParser textParser,
        @Qualifier("ursprungsportalModulParserX22") UrsprungsportalModulParser ursprungParser,
        @Qualifier("fachlicheFreigabeModulParserX22") FachlicheFreigabeModulParser freigabeParser
    ) {
        this.parsers = ImmutableMap.<Class<?>, Parser<?, ?>>builder()
            .put(Fristmodul.class, fristParser)
            .put(Bearbeitungsdauermodul.class, bearbeitungsdauerParser)
            .put(Kostenmodul.class, kostenParser)
            .put(Dokumentmodul.class, dokumentParser)
            .put(TextmodulIndividuell.class, textIndividuellParser)
            .put(Textmodul.class, textParser)
            .put(UrsprungsportalModul.class, ursprungParser)
            .put(FachlicheFreigabeModul.class, freigabeParser)
            .build();
    }
}
