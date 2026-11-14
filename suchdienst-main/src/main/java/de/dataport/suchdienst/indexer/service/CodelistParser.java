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

import static java.util.stream.Collectors.groupingBy;

import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.xrepository.genericodeservices.Annotation;
import de.xrepository.genericodeservices.AnyOtherContent;
import de.xrepository.genericodeservices.AnyOtherLanguageContent;
import de.xrepository.genericodeservices.CodeListDocument;
import de.xrepository.genericodeservices.Column;
import de.xrepository.genericodeservices.Data;
import de.xrepository.genericodeservices.Key;
import de.xrepository.genericodeservices.KeyColumnRef;
import de.xrepository.genericodeservices.Row;
import de.xrepository.genericodeservices.UseType;
import de.xrepository.genericodeservices.Value;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Slf4j
public class CodelistParser {

    private static final JAXBContext JAXB_CONTEXT;

    public static final String KANN_VALUE_SPALTE_IN_DEUTSCHER_SPRACHE_NICHT_IDENTIFIZIEREN =
        "Kann Value-Spalte in deutscher Sprache nicht identifizieren";

    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(CodeListDocument.class);
        } catch (JAXBException e) {
            // PV-8946 catch-rethrow
            throw new InternalError("Fehler beim Initialisieren von JAXBContext", e);
        }
    }

    private CodelistParser() {
        // Utility class
    }

    public static List<CodelistEntry> parseCodelistDocument(InputStream xmlCodeList) {
        try {
            Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<CodeListDocument> root =
                (JAXBElement<CodeListDocument>) unmarshaller.unmarshal(xmlCodeList);
            CodeListDocument codelist = root.getValue();
            return parseCodelistDocumentToEntries(codelist);
        } catch (JAXBException e) {
            // PV-8946 catch-rethrow
            throw new ImportException(
                ErrorCodeEnum.INVALID_FILE,
                "Fehler beim Importieren einer Codeliste.",
                e
            );
        }
    }

    public static List<CodelistEntry> parseCodelistDocumentToEntries(CodeListDocument codelist) {
        if (codelist == null || codelist.getColumnSet() == null) {
            throw new IllegalStateException("Codeliste ist leer");
        }

        final Column keyColumn = findKeyColumn(codelist);

        Map<Column, Locale> valueColumns = findValueColumns(codelist, keyColumn);

        final Column germanCommentColumnOrNull = findGermanCommentColumn(codelist);

        final Map<Column, Locale> commentColumnsOtherLanguages = findOtherLanguageColumns(
            codelist,
            germanCommentColumnOrNull
        );

        if (Objects.nonNull(germanCommentColumnOrNull)) {
            commentColumnsOtherLanguages.put(germanCommentColumnOrNull, Locale.GERMAN);
        }

        List<CodelistEntry> codelistEntries = new ArrayList<>();

        for (Row row : codelist.getSimpleCodeList().getRow()) {
            CodelistEntry entry = createCodelistEntryFromRow(
                codelist,
                row,
                keyColumn,
                valueColumns,
                commentColumnsOtherLanguages
            );
            codelistEntries.add(entry);
        }

        return codelistEntries;
    }

    private static CodelistEntry createCodelistEntryFromRow(
        CodeListDocument codelist,
        Row row,
        Column keyColumn,
        Map<Column, Locale> valueColumns,
        Map<Column, Locale> commentColumnsOtherLanguages
    ) {
        CodelistEntry entry = new CodelistEntry();
        for (Value value : row.getValue()) {
            Column column = (Column) value.getColumnRef();
            if (column == null) {
                log.warn(
                    "Wert einer Codeliste referenziert keine Spalte: {}",
                    value.getSimpleValue()
                );
                continue;
            }
            if (column.equals(keyColumn)) {
                entry.setCode(value.getSimpleValue());
            } else if (valueColumns.containsKey(column)) {
                entry.setText(valueColumns.get(column), value.getSimpleValue());
            } else if (commentColumnsOtherLanguages.containsKey(column)) {
                entry.setComment(commentColumnsOtherLanguages.get(column), value.getSimpleValue());
            } else {
                CodelistEntry.CodeListValue codeListValue = new CodelistEntry.CodeListValue(
                    column.getShortName().getValue(),
                    value.getSimpleValue(),
                    getLanguageLocale(
                        Objects.nonNull(column.getData())
                            ? column.getData().getLang()
                            : Locale.GERMAN.getLanguage()
                    ).orElse(Locale.GERMAN)
                );
                entry.getValues().add(codeListValue);
            }
            setRemainingFields(codelist, entry);
        }
        return entry;
    }

    /**
     * Helper zum Streamen ueber die Spalten
     */
    private static Stream<Column> columnStream(CodeListDocument codelist) {
        return codelist
            .getColumnSet()
            .getColumnRefOrColumn()
            .stream()
            .filter(Column.class::isInstance)
            .map(Column.class::cast);
    }

    /**
     * Findet eine Spalte anhand ihrer Annotation ({@literal "xoev-code"}, {@literal "xoev-codename"}
     * oder {@literal "empfohleneCodeSpalte"}) mit Fallback auf die ID des Keys
     *
     * @param codelist    die Codeliste
     * @param annotations Name der Annotationen, die gesucht werden
     * @return einen Optional, der die Spalte mit der entsprechenden Annotation oder nichts enthaelt,
     * niemals {@literal null}
     */
    private static Optional<Column> findColumnByKeyAnnotation(
        CodeListDocument codelist,
        String... annotations
    ) {
        List<Column> keys = codelist
            .getColumnSet()
            .getKeyRefOrKey()
            .stream()
            .filter(Key.class::isInstance)
            .map(Key.class::cast)
            .filter(k -> Objects.nonNull(k.getAnnotation()))
            .filter(k -> {
                Annotation a = k.getAnnotation();
                if (a != null) {
                    AnyOtherContent appInfo = a.getAppInfo();
                    boolean result = false;
                    for (Object o : appInfo.getAny()) {
                        Node n = (Node) o;
                        result |= Strings.CS.equalsAny(n.getLocalName(), annotations);
                    }
                    return result;
                }
                return false;
            })
            .map(k -> k.getColumnRef().getFirst().getRef())
            .filter(Objects::nonNull)
            .map(Column.class::cast)
            .toList();
        if (keys.size() > 1) {
            log.warn(
                "Konnte keine eindeutige Codespalte für Codeliste {}_{} finden!",
                codelist.getIdentification().getCanonicalUri(),
                codelist.getIdentification().getVersion()
            );
        }
        return Optional.ofNullable(!keys.isEmpty() ? keys.getFirst() : null);
    }

    /**
     * Findet eine Spalte anhand moeglicher Namen
     *
     * @param codelist die Codeliste
     * @param names    moegliche Namen fuer die Spalte
     * @return einen Optional, der die Spalte mit dem entsprechenden Namen oder nichts enthaelt, niemals
     * {@literal null}
     */
    private static Optional<Column> findColumnByName(CodeListDocument codelist, String... names) {
        List<Column> list = columnStream(codelist)
            .filter(c -> Strings.CI.equalsAny(c.getId(), names))
            .toList();
        return Optional.ofNullable(list.size() == 1 ? list.getFirst() : null);
    }

    @Nullable
    private static Column findGermanCommentColumn(CodeListDocument codelist) {
        return findColumnByName(codelist, "Kommentar", "comment-de-DE").orElse(null);
    }

    /**
     * Finde die Spalte fuer den (deutschen) Wert
     *
     * @param codelist  die Codeliste
     * @param keyColumn die Spalte, die den Key repraesentiert
     * @return die gefundene Spalte, niemals {@literal null}
     * @throws RuntimeException falls die Spalte nicht identifiziert werden kann
     */
    private static Column findGermanValueColumn(CodeListDocument codelist, Column keyColumn) {
        Optional<Column> byAnnotation = findColumnByKeyAnnotation(codelist, "xoev-codename");
        return byAnnotation
            .or(() ->
                findUniqueNonKeyColumn(codelist, keyColumn).or(() ->
                    findColumnByName(codelist, "bezeichnung", "wert", "description-de-de").or(() ->
                        findFirstValueColumn(codelist, keyColumn)
                    )
                )
            )
            .orElseThrow(() -> new RuntimeException("Kann Value-Spalte nicht identifizieren"));
    }

    /**
     * Findet die erste Spalte nach der Code Spalte in deutscher Sprache
     */
    private static Optional<Column> findFirstValueColumn(
        CodeListDocument codelist,
        Column keyColumn
    ) {
        return columnStream(codelist)
            .filter(Predicate.not(keyColumn::equals))
            .filter(c -> {
                String lang = c.getData().getLang();
                return Locale.GERMAN.equals(getLanguageLocale(lang).orElse(Locale.GERMAN));
            })
            .findFirst();
    }

    /**
     * Identifiziert die Key-Spalte der Codeliste oder wirft eine RuntimeException. <br>
     * Algorithm: <br>
     * 1) Es wird Key mit El. Key/Annotation/AppInfo und El. 'xoev-code', 'empfohleneCodeSpalte' oder 'recommendedKeyColumn' gesucht. <br>
     * El. Key/ColumnRef enthält Id der Code Spalte. Gibt es mehrere Ergebnisse, darf nicht sein, so wird erstes Ergebnis geliefert. *Eine Warnung wird geloggt.* <br>
     * 2) Ist Key mit der Code-Annotation nicht vorhanden, so werden alle Keys ohne Annotationen betrachtet. <br>
     * 2.1) Kommt Key Element nur einmal, so kann Code-Spalte ohne weiteres ermittelt werden. El. Key/ColumnRef enthält Id der Code Spalte. <br>
     * 2.2) Kommt Key mehrmals vor, so wird Key bevorzugt mit dem Ref. Column mit Use=required gekennzeichnet ist.<br>
     * 3) Suche Spalte mit Id 'code', 'schluessel', 'schlüssel' <br>
     * 4) Könnte keine Code-Spalte ermittelt werden, wird ein Fehler produziert. <br>
     *
     *
     * @param codelist die Codeliste
     * @return die Spalten, die den Key beinhaltet, niemals {@literal null}
     * @throws RuntimeException falls die Spalte nicht identifiziert werden kann
     */
    private static Column findKeyColumn(CodeListDocument codelist) {
        // Suche Key mit Code-Annotation
        Optional<Column> codeColumn = findColumnByKeyAnnotation(
            codelist,
            "xoev-code",
            "empfohleneCodeSpalte",
            "recommendedKeyColumn"
        );

        // Key mit Code-Annotation nicht vorhanden
        if (codeColumn.isEmpty()) {
            // Finde Keys ohne Annotation
            List<Key> keyList = codelist
                .getColumnSet()
                .getKeyRefOrKey()
                .stream()
                .filter(Key.class::isInstance)
                .map(Key.class::cast)
                .filter(k -> Objects.isNull(k.getAnnotation()))
                .toList();

            // Schlüssel hat zwar keine Annotation aber ist eindeutig
            if (keyList.size() == 1) {
                codeColumn = keyList
                    .getFirst()
                    .getColumnRef()
                    .stream()
                    .map(KeyColumnRef::getRef)
                    .filter(Objects::nonNull)
                    .map(Column.class::cast)
                    .findFirst();
            }
            // Schlüssel ist nicht eindeutig, es wird Spalte mit Use = required ausgewählt
            else if (keyList.size() > 1) {
                codeColumn = keyList
                    .stream()
                    .flatMap(k -> k.getColumnRef().stream())
                    .map(KeyColumnRef::getRef)
                    .filter(Objects::nonNull)
                    .map(Column.class::cast)
                    .filter(c -> UseType.REQUIRED.equals(c.getUse()))
                    .findFirst();
                log.warn(
                    "Konnte keine eindeutige Codespalte für Codeliste {}_{} finden!",
                    codelist.getIdentification().getCanonicalUri(),
                    codelist.getIdentification().getVersion()
                );
            } else {
                // Fallback, wenn nicht vorhanden ist: suche Spalten anhand des Namens
                codeColumn = findColumnByName(codelist, "code", "schluessel", "schlüssel");
            }
        }

        return codeColumn.orElseThrow(() ->
            new RuntimeException("Kann Key-Spalte nicht identifizieren")
        );
    }

    /**
     * Identifiziert die anderssprachigen Spalten, die eine Uebersetzung der deutschsprachigen
     * {@literal germanColumn} darstellen, und legt diese (zusammen mit der deutschsprachigen) in einer Map ab.
     *
     * @param codelist     die zu untersuchenden Codeliste
     * @param germanColumn die (deutsche) Spalte fuer die Werte
     * @return eine Map, deren Keys die identifizierten Spalten und deren Werte die Sprache dieser
     * Spalte darstellen
     */
    @NonNull
    private static Map<Column, Locale> findOtherLanguageColumns(
        CodeListDocument codelist,
        @Nullable Column germanColumn
    ) {
        if (germanColumn == null) {
            return new HashMap<>();
        }
        Map<Column, Locale> map = new HashMap<>();
        List<Column> list = columnStream(codelist).toList();
        Set<Locale> foundLocales = new HashSet<>();
        for (Column column : list) {
            if (column.equals(germanColumn)) {
                // Überspringe die deutsche Spalte
                continue;
            }

            // Identifikation über Referenz in Annotation
            Annotation annotation = column.getAnnotation();
            if (annotation != null) findLanguageColumnsByAnnotation(
                germanColumn,
                column,
                annotation,
                foundLocales,
                map
            );
            // Ids sind gleichnamig mit ausnahme Sprachsuffix. Bsp.: description-de-DE = description-en-GB, Bezeichnung = BezeichnungEnglisch
            else if (isEqualColumnIds(column, germanColumn)) {
                findLanguageColumnsByEqualColumnIds(germanColumn, column, foundLocales, map);
            }
            // Keine Möglichkeit die zusammenhängende Columns zu ermitteln. Keine Annotation und gleichnamige Ids
            else {
                findLanguageColumnsOther(germanColumn, column, foundLocales, map);
            }
        }
        return map;
    }

    private static void findLanguageColumnsOther(
        Column germanColumn,
        Column column,
        Set<Locale> foundLocales,
        Map<Column, Locale> map
    ) {
        var germanColumnIdWithoutLangauge = removeLanguageTagFromColumnId(
            germanColumn,
            Locale.GERMANY
        );
        var isEnglishLocaleMissing = !foundLocales.contains(Locale.ENGLISH);

        //TODO: Ausnahmen für Codelisten, die durchs Raster fallen. Codelisten-Parse-Methoden müssen über das PVOG-Projekt vereinheitlicht werden. Momentan sind die Implementierungen im Validator und Suchdienst unterschiedlich.
        // Ausnahmen greifen für die Values und nicht für Kommentare

        // Kommunikationssystem
        var isCommunicationSystemColumn =
            Strings.CI.equals(germanColumnIdWithoutLangauge, "bezeichnung") &&
            Strings.CI.equals(column.getId(), "designation");
        // Zustaendigkeitsrolle
        var isRelationRoleColumn =
            Strings.CI.equals(germanColumnIdWithoutLangauge, "rolle") &&
            Strings.CI.equals(column.getId(), "role");

        if (isEnglishLocaleMissing && (isCommunicationSystemColumn || isRelationRoleColumn)) {
            map.put(column, Locale.ENGLISH);
            foundLocales.add(Locale.ENGLISH);
        }
    }

    private static void findLanguageColumnsByEqualColumnIds(
        Column germanColumn,
        Column column,
        Set<Locale> foundLocales,
        Map<Column, Locale> map
    ) {
        Data data = column.getData();
        // Lang-Attribut ist gesetzt
        if (StringUtils.isNoneEmpty(data.getLang())) {
            Locale lang = getLanguageLocale(data.getLang()).orElse(Locale.GERMAN);
            // IDs von Spalten verschiedener Sprachen sind gleich
            if (!Locale.GERMAN.equals(lang) && !foundLocales.contains(lang)) {
                map.put(column, lang);
                foundLocales.add(lang);
            }
        } else {
            // Lang nicht gesetzt. Versuche die Sprache durch Suffix in Id zu ermitteln.
            // Suche gleichnamige Columns und werte Suffix aus. Bsp.: description-de-DE, description-en-GB, BezeichnungEnglisch
            String germanColumnIdWithoutLangauge = removeLanguageTagFromColumnId(
                germanColumn,
                Locale.GERMANY
            );
            // bestimme die Sprache aus Suffix
            String lang = Strings.CI.replace(
                column.getId(),
                germanColumnIdWithoutLangauge,
                ""
            ).trim();
            if (StringUtils.isNoneEmpty(lang)) {
                if (lang.startsWith("-")) {
                    lang = lang.replaceFirst("-", "");
                }
                Locale locale = getLanguageLocale(lang).orElse(Locale.GERMAN);
                // Ausnahme für SDGInformationsbereich. Besitzt als Suffix Wort Englisch. Bsp.: BezeichnungEnglisch
                if (Strings.CI.contains(lang, "Englisch")) {
                    map.put(column, Locale.ENGLISH);
                    foundLocales.add(Locale.ENGLISH);
                }
                // ISO-Konforme Suffix wie en, en-GB usw.
                else if (!Locale.GERMAN.equals(locale) && !foundLocales.contains(locale)) {
                    map.put(column, locale);
                    foundLocales.add(locale);
                }
            }
        }
    }

    private static void findLanguageColumnsByAnnotation(
        Column germanColumn,
        Column column,
        Annotation annotation,
        Set<Locale> foundLocales,
        Map<Column, Locale> map
    ) {
        Data data = column.getData();

        Optional<String> referencedColumn = annotation
            .getDescription()
            .stream()
            .map(AnyOtherLanguageContent::getAny)
            .flatMap(List::stream)
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .filter(e -> e.getLocalName().equals("isTranslation"))
            .map(e -> e.getAttribute("ref"))
            .findAny();

        if (referencedColumn.isPresent() && referencedColumn.get().equals(germanColumn.getId())) {
            // Übersetzung referenziert die deutsche Spalte
            if (StringUtils.isNoneEmpty(data.getLang())) {
                Locale locale = getLanguageLocale(data.getLang()).orElse(Locale.GERMAN);
                if (!Locale.GERMAN.equals(locale) && !foundLocales.contains(locale)) {
                    map.put(column, locale);
                    foundLocales.add(locale);
                }
            } else {
                // Lang ist nicht gesetzt. Setze Englisch als Standard
                if (!foundLocales.contains(Locale.ENGLISH)) {
                    map.put(column, Locale.ENGLISH);
                    foundLocales.add(Locale.ENGLISH);
                }
            }
        }
    }

    /**
     * Versucht die Spalte fuer den deutschen Wert daran zu identifizieren, dass es die einzige
     * erforderliche deutsche Spalte ist, die nicht der Key-Spalte entspricht
     *
     * @param codelist  die Codeliste
     * @param keyColumn die Key-Spalte
     * @return einen Optional, der die Spalte fuer den deutschen Wert oder nichts enthaelt, niemals
     * {@literal null}
     */
    private static Optional<Column> findUniqueNonKeyColumn(
        CodeListDocument codelist,
        Column keyColumn
    ) {
        List<Column> matching = columnStream(codelist)
            .filter(Predicate.not(keyColumn::equals))
            .filter(c -> {
                if (c.getUse() == UseType.REQUIRED) {
                    String lang = c.getData().getLang();
                    return (
                        lang == null || Strings.CI.equalsAny(c.getData().getLang(), "de-de", "de")
                    );
                }
                return false;
            })
            .toList();
        return Optional.ofNullable(matching.size() == 1 ? matching.getFirst() : null);
    }

    /**
     * Die Methode ermittelt die Spalten mit den Werten (Namen) eines Codes pro Sprache. Die Spalte mit dem Code wird ausgeschlossen.
     * Algorithm:<br>
     *  1) Gibt es nur zwei Spalten, so ist die nicht Code-Spalte ein deutscher Name des Codes.<br>
     *  2) Gibt es mehr als 2 Spalten und ist die Sprache nicht enthalten oder überall Deutsch, so handelt es um mehrere Info-Spalten in deutscher Sprache.<br>
     * Die Code-Name-Spalte kann nur anhand Use Element ermittelt werden. Die Spalte, ausgenommen Code-Spalte, muss 'Column/Use = required enthalten. <br>
     * Sonst kann die Name-Spalte nicht ermittelt werden. Der Fall wird protokolliert. <br>
     *  3) Gibt es mehr als 2 Spalten und enthalten alle Spalten, ausgenommen die Code-Spalte, verschiedene Sprachen, <br>
     *  so handelt es um verschiedene Sprachen der Name-Spalte. Diese pro Sprache verarbeiten. <br>
     *  4) Gibt es mehr als 2 Spalten und kommt die gleiche Sprache, ausgenommen die Code-Spalte, in mehreren Spalten vor, <br>
     *  so handelt es sich um mehrere Info-Spalten und nur einige davon sind die Name (Pro Sprache).<br>
     *  4.1) Um Name-Spalte zu ermitteln werden alle Spalten mit dem Attribut 'Column/Use = required ermittelt. <br>
     *  Enthalten die Spalten verschiedene Sprachen, so ist Name-Column ermittelt. <br>
     *  4.2) Sonst Element Key/Annotation/AppInfo mit El. 'xoev-codename' suchen (gilt nicht für alle Versionen). <br>
     *  Ist so ein El. vorhanden, so enthält El. Key/ColumnRef Id der Name-Spalte in 'deutscher (Annahme)' Sprache. <br>
     *  Jetzt noch die Übersetzungsspalten bestimmen. Dafür kann Element 'isTranslation' ausgewertet werden (siehe Liste oben) <br>
     *  oder die gleichnamige Spalte mit Sprachcode als Präfix suchen. <br>
     *  4.3) Konnten die Name-Spalte und die Übersetzung trotzdem nicht ermittelt werden, so wird versucht explizit die deutsche Name-Spalte zu ermitteln <br>
     *  und für die Spalte nach der Übersetzung gesucht. <br>
     *  Die Übersetzungen für die Spalte können mit 'isTranslation' oder durch gleichnamige Spalten mit Sprachcode als Suffix gesucht werden. <br>
     *  War das nicht erfolgreich, so wird es aufgegeben und nur die deutsche Spalte bleibt.
     *
     * @return Ein Map mit dem Value-Spalte und von der Spalte repräsentierte Sprache
     */
    private static Map<Column, Locale> findValueColumns(
        CodeListDocument codelist,
        Column codeColumn
    ) {
        Map<Column, Locale> map = new HashMap<>();

        // Spalten Liste ohne Code-Spalte
        List<Column> columnList = columnStream(codelist)
            .filter(k -> !codeColumn.equals(k))
            .toList();

        if (columnList.size() == 1) {
            // Es gibt nur eine zusätzliche Spalte. Nehme an, es geht um deutsche Value Spalte.
            Locale lang = getLanguageLocale(columnList.getFirst().getData().getLang()).orElse(
                Locale.GERMAN
            );
            if (!Locale.GERMAN.equals(lang)) {
                throw new IllegalStateException(
                    KANN_VALUE_SPALTE_IN_DEUTSCHER_SPRACHE_NICHT_IDENTIFIZIEREN
                );
            }
            map.put(columnList.getFirst(), lang);
        } else if (columnList.size() > 1) {
            boolean listUseLangAttribute = columnList
                .stream()
                .anyMatch(c -> StringUtils.isNoneEmpty(c.getData().getLang()));
            if (listUseLangAttribute) {
                return findValueColumnsWithLangAttribute(codelist, codeColumn, columnList);
            } else {
                // Liste nutzt Lang-Attribut nicht
                // Nutze zum Ermitteln von deutscher Value-Spalte die Annotation 'xoev-codename' und zum Ermitteln der Übersetzungen die Annotation 'isTranslation' oder die gleichnamigen Spalten mit Sprachcode.
                final Column germanValueColumn = findGermanValueColumn(codelist, codeColumn);
                final Map<Column, Locale> valueColumnsOtherLanguages = findOtherLanguageColumns(
                    codelist,
                    germanValueColumn
                );
                valueColumnsOtherLanguages.put(germanValueColumn, Locale.GERMAN);
                return valueColumnsOtherLanguages;
            }
        } else {
            throw new IllegalStateException("Kann Value-Spalte nicht identifizieren");
        }

        return map;
    }

    /**
     * Die Methode ist Teil in {@link #findValueColumns(CodeListDocument, Column)} beschriebenen Suchalgorithmus.
     * Behandelt die Fälle wo Lang-Attribut gesetzt ist und die Sprache der Spalten bekannt ist.
     */
    private static Map<Column, Locale> findValueColumnsWithLangAttribute(
        CodeListDocument codelist,
        Column codeColumn,
        List<Column> columnList
    ) {
        // Es gibt mehrere zusätzliche Spalten. Es können die Spalten in einer Sprache oder mehreren Sprachen sein.
        // Die Sprache kann über Lang-Attribut ermittelt werden

        List<Column> columnListWithLang = filterColumnsWithLang(columnList);
        if (columnListWithLang.isEmpty()) {
            return Map.of();
        }

        if (areAllColumnsGerman(columnListWithLang)) {
            return findValueColumnsWithLangAttributeOnlyGerman(columnListWithLang);
        }

        Map<String, List<Column>> columnsGroupedByLanguage = groupColumnsByLang(columnListWithLang);

        // Falls alle Spalten verschiedene Sprachen enthalten und deutsche ist auch dabei, so sind es die Übersetzungen einer Spalte.
        if (hasExactlyOneColumnPerLanguage(columnsGroupedByLanguage)) {
            Map<Column, Locale> result = toColumnLocaleMap(columnsGroupedByLanguage);
            assertContainsGerman(result);
            return result;
        }

        // Es sind mehrere Spalten pro Sprache enthalten. Liste enthält nicht nur Code und Value, sondern weitere
        // Spalten wie Kommentare, Hinweise unw. mit den Übersetzungen.
        // Betrachte zuerst die Pflicht-Spalten mit 'Use=required'
        Map<String, List<Column>> requiredColumnsGroupedByLanguage = groupRequiredColumnsByLang(
            columnListWithLang
        );

        if (
            hasExactlyOneRequiredColumnPerLanguage(
                columnsGroupedByLanguage,
                requiredColumnsGroupedByLanguage
            )
        ) {
            Map<Column, Locale> result = toColumnLocaleMap(requiredColumnsGroupedByLanguage);
            assertContainsGerman(result);
            return result;
        }

        // Pro vorhandene Sprache existieren mehrere Pflichtspalten
        // Nutze zum Ermitteln von deutscher Value-Spalte die Annotation 'xoev-codename'
        // und zum Ermitteln der Übersetzungen die Annotation 'isTranslation' oder die gleichnamigen Spalten mit Sprachcode.
        final Column germanValueColumn = findGermanValueColumn(codelist, codeColumn);
        final Map<Column, Locale> valueColumnsOtherLanguages = findOtherLanguageColumns(
            codelist,
            germanValueColumn
        );
        valueColumnsOtherLanguages.put(germanValueColumn, Locale.GERMAN);
        return valueColumnsOtherLanguages;
    }

    private static List<Column> filterColumnsWithLang(List<Column> columns) {
        return columns
            .stream()
            .filter(c -> StringUtils.isNoneEmpty(c.getData().getLang()))
            .toList();
    }

    private static boolean areAllColumnsGerman(List<Column> columnsWithLang) {
        return columnsWithLang
            .stream()
            .allMatch(c ->
                Locale.GERMAN.getLanguage().equals(
                    Locale.forLanguageTag(c.getData().getLang()).getLanguage()
                )
            );
    }

    private static Map<String, List<Column>> groupColumnsByLang(List<Column> columnsWithLang) {
        return columnsWithLang.stream().collect(groupingBy(c -> c.getData().getLang()));
    }

    private static Map<Column, Locale> findValueColumnsWithLangAttributeOnlyGerman(
        List<Column> columnListWithLang
    ) {
        Map<Column, Locale> map = new HashMap<>();
        // Die Value-Spalte ist eine, die Use = required enthält.
        Optional<Column> valueColumn = columnListWithLang
            .stream()
            .filter(c -> Objects.nonNull(c.getUse()) && UseType.REQUIRED.equals(c.getUse()))
            .findAny();
        map.put(
            valueColumn.orElseThrow(() ->
                new RuntimeException(KANN_VALUE_SPALTE_IN_DEUTSCHER_SPRACHE_NICHT_IDENTIFIZIEREN)
            ),
            Locale.GERMAN
        );
        return map;
    }

    private static boolean hasExactlyOneColumnPerLanguage(
        Map<String, List<Column>> columnsGroupedByLanguage
    ) {
        return columnsGroupedByLanguage
            .values()
            .stream()
            .allMatch(c -> c.size() == 1);
    }

    private static Map<String, List<Column>> groupRequiredColumnsByLang(
        List<Column> columnsWithLang
    ) {
        return columnsWithLang
            .stream()
            .filter(
                c ->
                    StringUtils.isNoneEmpty(c.getData().getLang()) &&
                    Objects.nonNull(c.getUse()) &&
                    UseType.REQUIRED.equals(c.getUse())
            )
            .collect(groupingBy(c -> c.getData().getLang()));
    }

    private static boolean hasExactlyOneRequiredColumnPerLanguage(
        Map<String, List<Column>> columnsGroupedByLanguage,
        Map<String, List<Column>> requiredColumnsGroupedByLanguage
    ) {
        boolean oneRequiredColumnPerLanguage = requiredColumnsGroupedByLanguage
            .values()
            .stream()
            .allMatch(c -> c.size() == 1);

        // Pro vorhandene Sprache existiert genau eine Pflicht-Spalte
        return (
            columnsGroupedByLanguage.size() == requiredColumnsGroupedByLanguage.size() &&
            oneRequiredColumnPerLanguage
        );
    }

    private static Map<Column, Locale> toColumnLocaleMap(
        Map<String, List<Column>> columnsGroupedByLanguage
    ) {
        Map<Column, Locale> map = new HashMap<>();
        columnsGroupedByLanguage
            .values()
            .forEach(columns ->
                map.put(
                    columns.getFirst(),
                    getLanguageLocale(columns.getFirst().getData().getLang()).orElse(Locale.GERMAN)
                )
            );
        return map;
    }

    private static void assertContainsGerman(Map<Column, Locale> map) {
        if (!map.containsValue(Locale.GERMAN)) {
            throw new IllegalStateException(
                KANN_VALUE_SPALTE_IN_DEUTSCHER_SPRACHE_NICHT_IDENTIFIZIEREN
            );
        }
    }

    private static Optional<Locale> getLanguageLocale(String lang) {
        if (StringUtils.isEmpty(lang)) {
            return Optional.empty();
        }

        Locale locale = Locale.forLanguageTag(lang);

        if (StringUtils.isEmpty(locale.getLanguage())) {
            return Optional.empty();
        }

        return Optional.of(Locale.forLanguageTag(locale.getLanguage()));
    }

    /**
     * Testet ob Ids ausgenommen Suffix mit dem Sprachcode Bsp.: description-de-DE, description-en-GB, BezeichnungEnglisch gleich sind
     */
    private static boolean isEqualColumnIds(Column column, Column germanColumn) {
        String germanColumnIdWithoutLangauge = removeLanguageTagFromColumnId(
            germanColumn,
            Locale.GERMANY
        );
        return Strings.CI.contains(column.getId(), germanColumnIdWithoutLangauge);
    }

    /**
     * Die Methode entfernt ein Sprachsuffix vom Id. Bsp.: description-en-GB -> description oder description-en -> description
     */
    private static String removeLanguageTagFromColumnId(Column column, Locale locale) {
        // xx_YY (de_DE, en_GB) Format
        String lang = locale.toLanguageTag();
        if (column.getId().endsWith(lang)) {
            return Strings.CS.remove(column.getId(), "-" + lang);
        }
        // xx (de, en) Format
        lang = locale.getLanguage();
        if (column.getId().endsWith(lang)) {
            return Strings.CS.remove(column.getId(), "-" + lang);
        }
        return column.getId();
    }

    /**
     * factored out from {@link #parseCodelistDocumentToEntries}.
     */
    private static void setRemainingFields(CodeListDocument codelist, CodelistEntry entry) {
        entry.setUri(codelist.getIdentification().getCanonicalUri());
        entry.setVersion(codelist.getIdentification().getVersion());
        entry.setId(entry.getUri() + "_" + entry.getVersion() + "_" + entry.getCode());
    }
}
