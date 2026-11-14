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
package de.dataport.suchdienst.indexer.service.suggestion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class SuggestionHelper {

    // Matcht alle Unicode-Buchstaben (\p{L}), sowie ASCII-Zeichen (\w => a-z, A-Z, 0-9) sowie die Sonderzeichen '§-'
    public static final String TOKEN_SPLIT_LETTERS = "\\p{L}\\w§-";

    // Wir verarbeiten nur existierende Texte, die weniger als 250 Zeichen enthalten.
    // Dadurch wird sichergestellt, dass wir unsere Suggestions nicht mit irrtümlich gepflegten Volltexten fluten
    private static final int TEXT_LENGTH_LIMIT = 250;

    private static final Set<String> ROMAN_NUMBERS = Set.of(
        "I",
        "II",
        "III",
        "IV",
        "V",
        "VI",
        "VII",
        "VIII",
        "IX",
        "X",
        "XI",
        "XII",
        "XIII",
        "XIV",
        "XV"
    );
    private static final Set<String> specialCharacters = Set.of("§");

    private SuggestionHelper() {}

    /**
     * Erzeugt aus dem gegebenen Text eine Suggestion. Dafür werden folgende Schritte durchgeführt:
     * <ul>
     *     <li>Fügt Leerzeichen zwischen Text und Zahlen hinzu, z.B. 'Hartz4' zu ' Hartz 4'</li>
     *     <li>Splitten der Wörter anhand von Zeichen die keine Buchstaben sind. Dabei werden Begriffe wie Hartz 4 oder ab 18 nicht gesplitted.</li>
     *     <li>Splitten anhand von Leerzeichen, z. B. 'Hunde Katzen' zu 'Hunde', 'Katzen'</li>
     *     <li>Begriffe mit weniger als 3 Zeichen löschen. Wenn ein Begriff gelöscht wurde und eine Zahl folgte wird diese auch gelöscht.</li>
     *     <li>Alles was mit einem Großbuchstaben anfängt und klein weiter geht, lowercase machen.</li>
     *     <li>Durch den Analyzer schicken. Hier werden deutsche Stopwörter entfernt und der Begriff wird mit der Synonymliste abgeglichen.</li>
     *     <li>Wenn Stopwörter vom Analyzer entfernt werden könnte es sein, dass einzelne Zahlen übrig bleiben. Diese werden im nachhinein noch gelöscht</li>
     * </ul>
     */
    public static <T extends AbstractSuggestionAnalyzer> Set<String> extractTokens(
        String rawText,
        T analyzer
    ) {
        if (Objects.isNull(rawText) || rawText.length() > TEXT_LENGTH_LIMIT) {
            return Set.of();
        }
        // Wir ergänzen Leerzeichen zwischen Wort und Zahl
        String parsedText = addSpaceBetweenWordAndNumber(rawText);

        // Hier tokenizen von z. B. "Hartz 4 und ALG 2 Untersuchung ab 18" in "Hartz 4, und, ALGII, Untersuchung, ab 18"
        var tokenSplitRegex = "[\\p{L}§\\w()-]";
        var regex = Pattern.compile(
            tokenSplitRegex + "++(?: (?:\\(?+\\d+|I?[XIV]I{0,3})\\)?+(?!" + tokenSplitRegex + "))?+"
        );
        var matcher = regex.matcher(parsedText);

        Set<String> suggestions = new HashSet<>();

        while (matcher.find()) {
            var s = matcher.group();
            // Splittet den String an Whitespaces und Sonderzeichen (mit Ausnahme des Bindestrichs)
            @SuppressWarnings("RegExpSimplifiable")
            String[] whitespaceSplit = s.split("[\\p{P}\\s&&[^-]]");
            // Jetzt bei Leerzeichen/Punktionen/Klammern splitten und schauen, dass Wörter, die kürzer als 3 Zeichen sind, gelöscht werden.
            List<Integer> removeFromWhitespaceSplit = removeShortWords(whitespaceSplit);
            whitespaceSplit = ArrayUtils.removeAll(
                whitespaceSplit,
                ArrayUtils.toPrimitive(removeFromWhitespaceSplit.toArray(Integer[]::new), -1)
            );

            List<String> tokenStrings = Stream.of(whitespaceSplit)
                .map(toLowerCaseIfStartsCaseInsensitive())
                .map(analyzer::analyze)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            // Wenn Stopwörter entfernt wurden, kann es sein, dass Zahlen alleine übrig
            // bleiben, die sollen auch weg
            if (tokenStrings.size() == 1) {
                tokenStrings.removeIf(
                    term -> StringUtils.isNumeric(term) || ROMAN_NUMBERS.contains(term)
                );
            }

            String suggestion = String.join(" ", tokenStrings);
            if (!suggestion.isEmpty()) {
                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }

    private static List<Integer> removeShortWords(String[] whitespaceSplit) {
        List<Integer> removeFromWhitespaceSplit = new ArrayList<>();
        for (
            int indexWhitespaceSplit = 0;
            indexWhitespaceSplit < whitespaceSplit.length;
            indexWhitespaceSplit++
        ) {
            if (
                whitespaceSplit[indexWhitespaceSplit].length() < 3 &&
                !StringUtils.isNumeric(whitespaceSplit[indexWhitespaceSplit]) &&
                !ROMAN_NUMBERS.contains(whitespaceSplit[indexWhitespaceSplit]) &&
                !specialCharacters.contains(whitespaceSplit[indexWhitespaceSplit])
            ) {
                removeFromWhitespaceSplit.add(indexWhitespaceSplit);
                // Wenn nach einem kurzen Wort eine Zahl steht, soll diese auch entfernt werden
                if (
                    whitespaceSplit.length > indexWhitespaceSplit + 1 &&
                    (StringUtils.isNumeric(whitespaceSplit[indexWhitespaceSplit + 1]) ||
                        ROMAN_NUMBERS.contains(whitespaceSplit[indexWhitespaceSplit + 1]) ||
                        specialCharacters.contains(whitespaceSplit[indexWhitespaceSplit + 1]))
                ) {
                    removeFromWhitespaceSplit.add(indexWhitespaceSplit + 1);
                    indexWhitespaceSplit++; // NOSONAR
                }
            }
        }
        return removeFromWhitespaceSplit;
    }

    private static Function<String, String> toLowerCaseIfStartsCaseInsensitive() {
        return token -> {
            if (token.length() > 1 && token.substring(1).equals(token.substring(1).toLowerCase())) {
                token = token.toLowerCase();
            }
            return token;
        };
    }

    public static Set<String> addSpaceBetweenWordAndNumber(Collection<String> inputs) {
        return inputs
            .stream()
            .map(SuggestionHelper::addSpaceBetweenWordAndNumber)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private static final Pattern WORD_NUMBER_BOUNDARY = Pattern.compile("([A-Za-z])(\\d+(?!\\w))");

    public static String addSpaceBetweenWordAndNumber(String input) {
        if (Objects.isNull(input)) {
            return "";
        }

        // Zahlen am Ende eines Wortes sollen durch ein Leerzeichen abgetrennt werden.
        return WORD_NUMBER_BOUNDARY.matcher(input).replaceAll("$1 $2");
    }
}
