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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import de.dataport.suchdienst.indexer.model.ArsObject;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import de.dataport.suchdienst.indexer.model.ZipcodeCsvDefinition;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.MessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ZipAssignerTest {

    @MockitoBean
    private MessageSource messageSource = null;

    /**
     * Ein Test mit wenigen Daten, addressiert pathologische Fälle aus PV-9580.
     */
    @Test
    void simple() {
        ArsObject busdorf = createArsObject("01059018", "010595915018", "24866");
        ArsObject dannewerk = createArsObject("01059019", "010595915019", "24867");
        ArsObject schleswig = createArsObject("01059075", "010590075075", "24837");

        var geoItems = parseCsvFile("geo/zip-update-1.csv");
        var zipAssigner = new ZipAssigner();
        zipAssigner.assignZipCodes(Collections.emptyList(), geoItems, RegionalKeyKind.AGS);
        // Die PLZ von Busdorf, Dannewerk und Schleswig haben sich alle nicht geändert, obwohl es Grenzgebiete gibt:
        assertThat(busdorf.getZipCodes(), equalTo(Set.of("24866")));
        assertThat(dannewerk.getZipCodes(), equalTo(Set.of("24867")));
        assertThat(schleswig.getZipCodes(), equalTo(Set.of("24837")));
    }

    /**
     * Ein Test mit den echten S-H Daten und prüfen, ob einige bekannte pathologische Fälle jetzt bereinigt sind.
     */
    @Test
    void schleswigHolsteinEchtdaten() {
        // So sind die Daten der Orte initial, nach dem Einlesen der Destatis Daten:
        ArsObject busdorf = createArsObject("01059018", "010595915018", "24866");
        ArsObject dannewerk = createArsObject("01059019", "010595915019", "24867");
        ArsObject schleswig = createArsObject("01059075", "010590075075", "24837");
        ArsObject kiel = createArsObject("01002000", "010020000000", "24103");
        ArsObject kronshagen = createArsObject("01058092", "010580092092", "24119");
        ArsObject altenholz = createArsObject("01058005", "010580005005", "24161");
        ArsObject quarnbek = createArsObject("01058130", "010585803130", "24107");

        var geoItems = parseCsvFile("geo/plz-sh.csv");
        var zipAssigner = new ZipAssigner();
        zipAssigner.assignZipCodes(
            List.of(busdorf, dannewerk, schleswig, kiel, kronshagen, altenholz, quarnbek),
            geoItems,
            RegionalKeyKind.AGS
        );

        // Die PLZ von Busdorf, Dannewerk und Schleswig haben sich alle nicht geändert, obwohl es Grenzgebiete gibt:
        assertThat(busdorf.getZipCodes(), equalTo(Set.of("24866")));
        assertThat(dannewerk.getZipCodes(), equalTo(Set.of("24867")));
        assertThat(schleswig.getZipCodes(), equalTo(Set.of("24837")));

        // Kiel hat folgende Postleitzahlen erhalten:
        assertThat(
            kiel.getZipCodes(),
            equalTo(
                Set.of(
                    "24103",
                    "24105",
                    "24106",
                    "24107",
                    "24109",
                    "24111",
                    "24113",
                    "24114",
                    "24116",
                    "24118",
                    "24143",
                    "24145",
                    "24146",
                    "24147",
                    "24148",
                    "24149",
                    "24159"
                )
            )
        );

        // Altenholz und Kronshagen haben trotz Grenzgebiet Kiel keine weiteren PLZ bekommen:
        assertThat(altenholz.getZipCodes(), equalTo(Set.of("24161")));
        assertThat(kronshagen.getZipCodes(), equalTo(Set.of("24119")));

        // Quarnbek hat seine 24107 nicht verloren, obwohl diese nun auch in Kiel vergeben wurde:
        assertThat(quarnbek.getZipCodes(), equalTo(Set.of("24107")));
    }

    /**
     * Kiel hat vor dem Update die 24103, es kommt ein Update mit 24103 und 24114,
     * erwartet wird, dass Kiel dadurch beide PLZ bekommt.
     */
    @Test
    void simpleUpdateKiel1() {
        ArsObject kiel = createArsObject("01002000", "010020000000", "24103");
        var geoItems = Stream.of(
            new GeoItem("01002000", "24103"),
            new GeoItem("01002000", "24114")
        );
        var zipAssigner = new ZipAssigner();
        var modifiedArsObjects = zipAssigner.assignZipCodes(
            List.of(kiel),
            geoItems,
            RegionalKeyKind.AGS
        );
        assertThat(modifiedArsObjects, Matchers.contains(kiel));
        assertThat(kiel.getZipCodes(), equalTo(Set.of("24103", "24114")));
    }

    /**
     * Kiel hat vor dem Update die 24111, es kommt ein Update mit 24103 und 24114,
     * erwartet wird, dass Kiel danach 24103 und 24114 hat, aber nicht mehr 24111.
     */
    @Test
    void simpleUpdateKiel2() {
        ArsObject kiel = createArsObject("01002000", "010020000000", "24111");
        var geoItems = Stream.of(
            new GeoItem("01002000", "24103"),
            new GeoItem("01002000", "24114")
        );
        var zipAssigner = new ZipAssigner();
        var modifiedArsObjects = zipAssigner.assignZipCodes(
            List.of(kiel),
            geoItems,
            RegionalKeyKind.AGS
        );
        assertThat(modifiedArsObjects, Matchers.contains(kiel));
        assertThat(kiel.getZipCodes(), equalTo(Set.of("24103", "24114")));
        assertThat(kiel.getDisplayZip(), equalTo("241**"));
    }

    /**
     * Testet eine Situation wie sie nach dem Einspielen der destatis Daten vorkommt:
     * Kiel hat die 24103, Quarnbek die 24107.
     * Dann gibt es ein Update aus den HKDE Daten.
     * In den HKDE Daten tritt die 24107 aber häufiger in Kiel auf als in Quarnbek (im realen Fall ist es 2663 mal vs 697 mal).
     * Dadurch bekommt Kiel die 24107 zusätzlich. Quarnbek behält aber die 24107, da es im internen Verlauf vom ZipAssigner
     * gar nicht angefasst wird, denn es gibt ja keine PLZ die am häufigsten in Quarnbek auftritt.
     */
    @Test
    void updateKielIgnoreQuarnbek() {
        var agsKiel = "01002000";
        ArsObject kiel = createArsObject(agsKiel, "010020000000", "24103");
        var agsQuarnbek = "01058130";
        ArsObject quarnbek = createArsObject(agsQuarnbek, "010585803130", "24107");
        var geoItems = Stream.of(
            new GeoItem(agsKiel, "24103"),
            new GeoItem(agsKiel, "24107"),
            new GeoItem(agsKiel, "24107"),
            new GeoItem(agsKiel, "24107"),
            new GeoItem(agsQuarnbek, "24107"),
            new GeoItem(agsQuarnbek, "24107")
        );
        var zipAssigner = new ZipAssigner();
        var modifiedArsObjects = zipAssigner.assignZipCodes(
            List.of(kiel, quarnbek),
            geoItems,
            RegionalKeyKind.AGS
        );
        assertThat(modifiedArsObjects, equalTo(List.of(kiel)));
        assertThat(kiel.getZipCodes(), equalTo(Set.of("24103", "24107")));
        assertThat(quarnbek.getZipCodes(), equalTo(Set.of("24107")));
    }

    /**
     * Legt einen Ort an, so wie er nach dem Import der Destatis Daten vorliegt.
     */
    private static ArsObject createArsObject(String ags, String ars, String plzFromDestatis) {
        var kiel = new ArsObject();
        kiel.setAgs(ags);
        kiel.setArs(ars);
        kiel.setDisplayZip(plzFromDestatis);
        kiel.getZipCodes().add(plzFromDestatis);
        return kiel;
    }

    private Stream<GeoItem> parseCsvFile(String csvFile) {
        var goItemParser = new GeoItemParser(messageSource);
        assertThat(goItemParser, notNullValue());
        Stream<String> csvLines = new BufferedReader(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream(csvFile))
        ).lines();
        assertThat(csvLines, notNullValue());
        var def = zipCodeDef();
        return goItemParser.parseCsvFileAsStream(csvLines, def);
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
