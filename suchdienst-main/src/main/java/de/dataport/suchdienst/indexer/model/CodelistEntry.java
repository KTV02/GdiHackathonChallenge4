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
package de.dataport.suchdienst.indexer.model;

import static de.dataport.suchdienst.indexer.model.I18nTextHelper.getBestMatch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Repräsentiert einen einzelnen Eintrag (also einen konkreten Code) einer XoeV-Codeliste (von
 * <a href="https://xrepository.de/">https://xrepository.de/</a>).
 * <p>
 * Im XRepository werden Codelisten anhand ihrer URI und Versionsnummer identifiziert. Wir gehen
 * davon aus, dass alle (fuer PVOG relevanten) Codelisten eine eindeutige Schlüssel-Spalte ("Code")
 * enthalten, mit dem dann jeder Eintrag identifiziert werden kann.
 */
@Slf4j
@Data
public class CodelistEntry implements Identifiable, Serializable {

    public record CodeListValue(String key, String value, Locale locale) implements Serializable {}

    /**
     * interne ID, entspricht der Konkatenation von {@link #uri} + '_' + {@link #version} + '_' + {@link #code}
     */
    private String id;

    /**
     * wenn dieses Feld nicht NULL ist, repräsentiert diese Instanz einen nicht durch den SuD
     * aufloesbaren Code. Der Wert gibt an, ob die gesamte Liste oder nur der einzelne Code nicht
     * aufgelöst werden kann.
     */
    @JsonIgnore
    private ErrorTypeCodelist nonExistentCode;

    /**
     * die URI, wie sie in XRepository hinterlegt ist, Aufbau ist typischerweise {@literal urn:...}
     */
    private String uri;
    /**
     * Version der Codeliste, kann beliebige(?) alphanumerische Zeichenketten enthalten
     */
    private String version;
    /**
     * der eigentliche Code, also der Wert der identifizierten Schluessel-Spalte (ueblicherweise in XML
     * mit {@literal Key Id="codeKey"} ausgezeichnet).
     */
    private String code;
    /**
     * Enthält die (sprach-abhängigen) Klartexte. Der deutsche Klartext steht im XML in der Spalte,
     * die ueblicherweise mit {@literal <Key Id="codenameKey">} ausgezeichnet ist.
     */
    private Map<Locale, String> texts = new HashMap<>();

    /**
     * PV-9431: Enthält die (sprach-abhängigen) Kommentare.
     */
    private Map<Locale, String> comments = new HashMap<>();

    private List<CodeListValue> values = new ArrayList<>();

    public final void setComment(Locale locale, String comment) {
        this.comments.put(locale, removeWhitespaces(comment));
    }

    private String logHint(String field) {
        return String.format("%s in Code %s aus Codeliste %s_%s", field, code, uri, version);
    }

    @Nullable
    public String getComment(Locale locale) {
        return getBestMatch(this.comments, locale, logHint("Kommentar"));
    }

    /**
     * Liefert den Text zu der locale. Dabei wird zuerst die locale inklusive country probiert, z.b. "en-US" und als Fallback
     * dann ohne county, z.b. "en".
     * Auch andersherum gibt es einen Fallback. Wenn z.b. mit "en" angefragt wird und wir haben nur "en-GB", so wird dieser Text geliefert.
     * Falls gar kein Text gefunden wird, liefert die Methode null.
     */
    public String getText(Locale locale) {
        return getBestMatch(this.texts, locale, logHint("Text"));
    }

    public void setText(Locale locale, String text) {
        this.getTexts().put(locale, removeWhitespaces(text));
    }

    public CodelistKey getKey() {
        return new CodelistKey(uri, version, code);
    }

    /**
     * Liefert den Wert-Eintrag einer weiteren (Nicht-Text)-Spalte
     *
     * @param columnName der Name der gesuchten Spalte
     * @return Optional mit der gesuchten Spalte oder leer
     */
    public Optional<CodeListValue> getValue(String columnName) {
        return values
            .stream()
            .filter(v -> columnName.equals(v.key()))
            .findAny();
    }

    /**
     * @return liefert {@literal true} gdw. diese Instanz einen nicht aufloesbaren Code repraesentiert.
     */
    @JsonIgnore
    public boolean isNonExistent() {
        return getNonExistentCode() != null;
    }

    /**
     * @return liefert {@literal true} gdw. diese Instanz einen aufloesbaren Code repraesentiert.
     */
    @JsonIgnore
    public boolean isExistent() {
        return getNonExistentCode() == null;
    }

    /**
     * Erstellt einen Dummy mit leerem Inhalt
     *
     * @param code          der Code der Liste
     * @param listURI       uri
     * @param listVersionID version
     * @param error         Fehlerursache
     * @return den Dummy-Eintrag, niemals NULL
     */
    public static CodelistEntry empty(
        String code,
        String listURI,
        String listVersionID,
        ErrorTypeCodelist error
    ) {
        log.debug(
            "Code konnte nicht aufgeloest werden (uri_version:code) = {}_{}:{}",
            listURI,
            listVersionID,
            code
        );
        CodelistEntry emptyEntry = new CodelistEntry();
        emptyEntry.setText(
            Locale.GERMAN,
            String.format("Code %s konnte nicht aufgelöst werden.", code)
        );
        emptyEntry.setText(Locale.ENGLISH, String.format("Code %s could not be resolved.", code));
        emptyEntry.setCode(code);
        emptyEntry.setUri(listURI);
        emptyEntry.setVersion(listVersionID);
        emptyEntry.setNonExistentCode(error);
        return emptyEntry;
    }

    private static String removeWhitespaces(String xmlContent) {
        // Entfernt die in Codelisten vorkommenden Zeilenumbrüche und Einrückungen.
        return xmlContent == null ? null : xmlContent.replaceAll("\\s+", " ").trim();
    }
}
