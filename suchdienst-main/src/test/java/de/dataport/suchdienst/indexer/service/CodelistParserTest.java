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
package de.dataport.suchdienst.indexer.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.xrepository.genericodeservices.CodeListDocument;
import de.xrepository.genericodeservices.Column;
import de.xrepository.genericodeservices.ColumnSet;
import de.xrepository.genericodeservices.Data;
import de.xrepository.genericodeservices.Identification;
import de.xrepository.genericodeservices.Row;
import de.xrepository.genericodeservices.SimpleCodeList;
import de.xrepository.genericodeservices.Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CodelistParserTest {

    /**
     * Teste Codelisten, die nur deutschen Text enthalten (kein Englisch).
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "urn__de__bund__destatis__bevoelkerungsstatistik__schluessel__staat_2024-08-01.xml",
            "urn__de__fim__codeliste__dokumenttyp_20180301.xml",
        }
    )
    void nurDeutschTest(String fileName) throws IOException {
        Resource[] codeLists = new PathMatchingResourcePatternResolver().getResources(
            "classpath:/codelisten/" + fileName
        );
        List<CodelistEntry> codelistEntries = CodelistParser.parseCodelistDocument(
            codeLists[0].getInputStream()
        );
        assertThat(codelistEntries.size(), greaterThan(0));

        for (CodelistEntry cl : codelistEntries) {
            String errorText = "Fehler in %s".formatted(cl.getId());
            assertAll(
                "CodelistEntry",
                () -> assertThat("Code: " + errorText, cl.getCode(), not(emptyOrNullString())),
                () ->
                    assertThat(
                        "Deutsch: " + errorText,
                        cl.getTexts().get(Locale.GERMAN),
                        not(emptyOrNullString())
                    )
            );
        }
    }

    /**
     * Teste Codelisten, die sowohl deutschen als auch englischen Text enthalten, anhand verschiedener Sprachkennzeichnungsstrategien.
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "urn__xoev-de__fim__codeliste__xzufi.tagestyp_1.0.xml",
            "urn__de__xoev__codeliste__erreichbarkeit_3.xml",
            "urn__de__xzufi__codeliste__leistungstextmodulleika_20240216.xml",
            "urn__xoev-de__fim__codeliste__sdginformationsbereich_4.xml",
            "urn__xoev-de__tsa__infodienste__codeliste__kommunikationssystem_2.xml",
            "urn__de__fim__codeliste__dokumenttyp_4.xml",
            "urn__de__xzufi__codeliste__zahlungsweise_20240612.xml",
        }
    )
    void deutschUndEnglischTest(String fileName) throws IOException {
        Resource[] codeLists = new PathMatchingResourcePatternResolver().getResources(
            "classpath:/codelisten/" + fileName
        );

        List<CodelistEntry> codelistEntries = CodelistParser.parseCodelistDocument(
            codeLists[0].getInputStream()
        );
        assertThat(codelistEntries.size(), greaterThan(0));

        for (CodelistEntry cl : codelistEntries) {
            String errorText = "Fehler in %s".formatted(cl.getId());
            assertAll(
                "CodelistEntry",
                () -> assertThat("Code: " + errorText, cl.getCode(), not(emptyOrNullString())),
                () ->
                    assertThat(
                        "Deutsch: " + errorText,
                        cl.getTexts().get(Locale.GERMAN),
                        not(emptyOrNullString())
                    ),
                () ->
                    assertThat(
                        "Englisch: " + errorText,
                        cl.getTexts().get(Locale.ENGLISH),
                        not(emptyOrNullString())
                    )
            );
        }
    }

    @Test
    @DisplayName("Teste Ausnahme mit dem Kommentarfeld")
    void pvLagenTest() throws IOException {
        Resource[] codeLists = new PathMatchingResourcePatternResolver().getResources(
            "classpath:/codelisten/urn__xoev-de__fim__codeliste__pvlagen_20211001.xml"
        );

        List<CodelistEntry> codelistEntries = CodelistParser.parseCodelistDocument(
            codeLists[0].getInputStream()
        );

        assertThat(codelistEntries.size(), greaterThan(0));

        for (CodelistEntry cl : codelistEntries) {
            String errorText = "Fehler in %s".formatted(cl.getId());
            assertAll(
                "CodelistEntry",
                () -> assertThat("Code: " + errorText, cl.getCode(), not(emptyOrNullString())),
                () ->
                    assertThat(
                        "Deutsch Text: " + errorText,
                        cl.getTexts().get(Locale.GERMAN),
                        not(emptyOrNullString())
                    ),
                () ->
                    assertThat(
                        "Englisch Text: " + errorText,
                        cl.getTexts().get(Locale.ENGLISH),
                        not(emptyOrNullString())
                    )
            );
        }

        assertThat(
            "Deutsch Kommentar nicht ermittelt:",
            codelistEntries
                .stream()
                .anyMatch(e -> StringUtils.isNoneEmpty(e.getComment(Locale.GERMAN))),
            equalTo(true)
        );
        assertThat(
            "Englisch Kommentar nicht ermittelt:",
            codelistEntries
                .stream()
                .anyMatch(e -> StringUtils.isNoneEmpty(e.getComment(Locale.ENGLISH))),
            equalTo(true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCodelistResources")
    void allCodelistsTest(Resource resource) throws IOException {
        List<CodelistEntry> codelistEntries = CodelistParser.parseCodelistDocument(
            resource.getInputStream()
        );
        assertThat(codelistEntries.size(), greaterThan(0));

        boolean containsEN = containsEn(resource);

        for (CodelistEntry cl : codelistEntries) {
            String errorText = "Fehler in %s. Dateiname: %s".formatted(
                cl.getId(),
                resource.getFilename()
            );
            assertAll(
                "CodelistEntry",
                () -> assertThat("Code: " + errorText, cl.getCode(), not(emptyOrNullString())),
                () ->
                    assertThat(
                        "Deutsch: " + errorText,
                        cl.getTexts().get(Locale.GERMAN),
                        not(emptyOrNullString())
                    ),
                () -> {
                    if (containsEN) {
                        assertThat(
                            "Englisch: " + errorText,
                            cl.getTexts().get(Locale.ENGLISH),
                            not(emptyOrNullString())
                        );
                    }
                }
            );
        }
    }

    private static boolean containsEn(Resource resource) throws IOException {
        String filename = resource.getFilename();
        String listFile = Files.readString(
            resource.getFile().toPath(),
            StandardCharsets.UTF_8
        ).toLowerCase();
        return (
            filename != null &&
            !filename.equals("urn__de__xzufi__codeliste__zahlungsweise_20190214.xml") &&
            !filename.equals("urn__de__xzufi__codeliste__leistungstextmodulleika_20190605.xml") &&
            (listFile.contains("en-en") ||
                listFile.contains("en-gb") ||
                listFile.contains("en-us") ||
                listFile.contains("en-ca") ||
                listFile.contains("englisch") ||
                listFile.contains("designation") ||
                listFile.contains("lang=\"en\""))
        );
    }

    private static Stream<Resource> provideCodelistResources() throws IOException {
        Resource[] allCodeLists = new PathMatchingResourcePatternResolver().getResources(
            "classpath*:/codelisten/*.xml"
        );

        return Stream.of(allCodeLists).filter(res -> {
            try {
                return res.getFile().exists(); // Nur Dateien im Dateisystem zulassen
            } catch (IOException _) {
                return false; // Ressourcen aus JARs werden hier ausgeschlossen
            }
        });
    }

    @Test
    @DisplayName("Programmatischer Aufbau ohne Key-Spalte wirft spezifische RuntimeException")
    void parseCodelistDocumentToEntries_missingKeyColumn_throwsRuntimeException() {
        // Aufbau eines minimalen CodeListDocument ohne irgendeine Key-Definition oder Spaltennamen-Fallback
        CodeListDocument doc = new CodeListDocument();
        Identification identification = new Identification();
        identification.setCanonicalUri("urn:test:missing-key");
        identification.setVersion("1");
        doc.setIdentification(identification);

        // ColumnSet mit nur einer Spalte "value" (kein Name 'code', keine Annotation)
        ColumnSet columnSet = new ColumnSet();
        Column column = new Column();
        column.setId("value");
        // Data setzen (optional), damit nachgelagerte Zugriffe nicht auf null laufen
        Data data = new Data();
        data.setLang("de");
        column.setData(data);
        columnSet.getColumnRefOrColumn().add(column);
        doc.setColumnSet(columnSet);

        // SimpleCodeList + eine Row mit einem Value, das auf die oben definierte Spalte verweist
        SimpleCodeList scl = new SimpleCodeList();
        Row row = new Row();
        Value value = new Value();
        value.setSimpleValue("abc");
        value.setColumnRef(column); // Referenziere Column direkt
        row.getValue().add(value);
        scl.getRow().add(row);
        doc.setSimpleCodeList(scl);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            CodelistParser.parseCodelistDocumentToEntries(doc)
        );
        assertEquals("Kann Key-Spalte nicht identifizieren", ex.getMessage());
    }
}
