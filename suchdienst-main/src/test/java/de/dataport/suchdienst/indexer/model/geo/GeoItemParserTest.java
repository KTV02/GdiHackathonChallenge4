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
package de.dataport.suchdienst.indexer.model.geo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

class GeoItemParserTest {

    @Test
    void simple() {
        var geoParser = new GeoItemParser(null);
        assertThat(geoParser, notNullValue());
        Stream<String> csvLines = new BufferedReader(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream("geo/simple.csv"))
        ).lines();
        assertThat(csvLines, notNullValue());
        var def = zipCodeDef();
        var geoItems = geoParser.parseCsvFileAsStream(csvLines, def).toList();
        assertThat(geoItems, notNullValue());
        assertThat(geoItems.size(), is(4));
        assertThat(
            geoItems,
            equalTo(
                List.of(
                    new GeoItem("01059018", "24866"),
                    new GeoItem("01059018", "24866"),
                    new GeoItem("01059019", "24867"),
                    new GeoItem("01059075", "24837")
                )
            )
        );
    }

    @Test
    void importException() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(anyString(), any(), any())).thenReturn("dummyMsg");
        var geoParser = new GeoItemParser(messageSource);
        assertThat(geoParser, notNullValue());
        Stream<String> csvLines = new BufferedReader(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream("geo/import-exception.csv"))
        ).lines();
        assertThat(csvLines, notNullValue());
        var def = zipCodeDef();
        Stream<GeoItem> itemStream = geoParser.parseCsvFileAsStream(csvLines, def);
        // Terminiere den Stream -> Exception wird geworfen
        assertThrows(ImportException.class, itemStream::toList);
    }

    private static ZipcodeCsvDefinition zipCodeDef() {
        return ZipcodeCsvDefinition.builder()
            .delimiter(";")
            .headerLine(true)
            .rsColumn(0)
            .zipColumn(1)
            .rsKind(RegionalKeyKind.AGS)
            .build();
    }
}
