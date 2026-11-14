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
package de.dataport.suchdienst.geo.service;

import de.dataport.suchdienst.config.SuchdienstProperties;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Stellt für verschiedene Use Cases UriBuilder mit vordefinierten Platzhaltern für den BKG bereit.
 */
public interface BkgQueryBuilder {
    default UriComponentsBuilder buildForwardGeocodeQuery(SuchdienstProperties props) {
        return UriComponentsBuilder.fromUriString(props.getBkg().getGdzGeokodierungBasisUrl())
            .pathSegment("geosearch")
            .query("outputformat=json")
            .query("srsName={srsName}")
            .query("query={query}");
    }

    default UriComponentsBuilder buildReverseGeocodeQuery(SuchdienstProperties props) {
        return UriComponentsBuilder.fromUriString(props.getBkg().getGdzGeokodierungBasisUrl())
            .pathSegment("geosearch")
            .query("outputformat=json")
            .query("filter=typ:(haus strasse ort)") // nur Objekte, die einen gueltigen ARS haben
            .query("distance=1000") // distanz von 1000 m sollte reichen, um immer etwas zu finden
            .query("count=1") // der erste Treffer reicht
            .query("srsName={srsName}")
            .query("lon={lon}")
            .query("lat={lat}");
    }

    default UriComponentsBuilder buildSuggestionQuery(SuchdienstProperties props) {
        return UriComponentsBuilder.fromUriString(props.getBkg().getGdzGeokodierungBasisUrl())
            .pathSegment("suggest")
            .query("outputformat=json")
            .query("minScore=1.5")
            .query("query={query}")
            .query("filter={filter}")
            .query("count={count}");
    }

    default UriComponentsBuilder buildGeosearchQuery(SuchdienstProperties props) {
        return UriComponentsBuilder.fromUriString(props.getBkg().getGdzGeokodierungBasisUrl())
            .pathSegment("geosearch")
            .query("outputformat=json")
            .query("filter={filter}")
            .query("query={query}")
            .query("count={count}")
            .query("properties={properties}");
    }

    default UriComponentsBuilder buildGeosearchArsQuery(SuchdienstProperties props) {
        return UriComponentsBuilder.fromUriString(props.getBkg().getGdzGeokodierungBasisUrl())
            .pathSegment("geosearch")
            .query("outputformat=json")
            .query("filter={filter}")
            .query("query={query}")
            .query("count={count}")
            .query("properties={properties}");
    }

    default UriComponentsBuilder buildStructuredQuery(
        SuchdienstProperties props,
        boolean includeOrt
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
            props.getBkg().getGdzGeokodierungBasisUrl()
        )
            .pathSegment("geocode")
            .query("outputformat=json")
            .query("minScore=0.90")
            .query("count={count}")
            .query("properties={properties}");

        if (includeOrt) {
            builder.query("ort={ort}");
        }

        // im Filter stecken alle Filterkriterien ort, plz, strasse, haus und der gesuchte Typ
        builder.query("filter={filter}");
        // mind. ein Query-Parameter wird vom BKG erwartet
        // es wird zunächst die PLZ verwendet
        builder.query("plz={plz}");
        return builder;
    }

    List<Character> bkgSpecialChars = List.of(
        '+',
        '-',
        '|',
        '&',
        '!',
        '(',
        ')',
        '{',
        '}',
        '[',
        ']',
        '^',
        '\"',
        '~',
        '*',
        '?',
        ':',
        '\\'
    );

    default boolean isBkgSpecialCharacter(char character) {
        return bkgSpecialChars.contains(character);
    }

    /**
     * Escaped Sonderzeichen laut BKG-Doku.
     * @param s ein String mit potenziellen Sonderzeichen
     * @return Eingabe mit escapten Sonderzeichen
     */
    default String escapeSpecialChars(String s) {
        return processSpecialChars(s, (sb, c) -> {
            if (isBkgSpecialCharacter(c)) {
                if (c == '\\') {
                    // FIXME: Ein BKG-Request mit zwei Backslashes (escaptem Backslash) führt aktuell zu einer 400 Response. (BKG-API Bug?)
                    // Also fügen wir ein Leerzeichen nach einem Backslash ein, damit keine zwei Backslashes aneinander stehen.
                    sb.append(c);
                    sb.append(' ');
                } else {
                    sb.append('\\');
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        });
    }

    /**
     * Ersetzt Sonderzeichen laut BKG-Doku durch Leerzeichen.
     * @param s ein String mit potenziellen Sonderzeichen
     * @return Eingabe mit ersetzten Sonderzeichen
     */
    default String replaceSpecialChars(String s) {
        var result = processSpecialChars(s, (sb, c) -> {
            if (isBkgSpecialCharacter(c)) {
                if (sb.isEmpty() || sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        });
        return result.trim();
    }

    /**
     * Entfernt alle Sonderzeichen laut BKG-Doku aus dem String.
     * @param s ein String mit potenziellen Sonderzeichen
     * @return Eingabe ohne Sonderzeichen
     */
    default String removeSpecialChars(String s) {
        var result = processSpecialChars(s, (sb, c) -> {
            if (!isBkgSpecialCharacter(c)) {
                sb.append(c);
            }
        });
        return result.trim();
    }

    @FunctionalInterface
    interface CharProcessor {
        void process(StringBuilder sb, char c);
    }

    private String processSpecialChars(String s, CharProcessor processor) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            processor.process(sb, c);
        }
        return sb.toString();
    }
}
