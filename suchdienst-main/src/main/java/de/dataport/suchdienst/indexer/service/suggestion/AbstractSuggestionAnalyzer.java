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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.FlattenGraphFilter;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Slf4j
public class AbstractSuggestionAnalyzer extends Analyzer {

    private static final SynonymMap synonymMap;

    // Matcht alle Zeichen außer den Zeichen in SuggestionHelper.TOKEN_SPLIT_LETTERS
    private final Pattern specialCharacterPattern = Pattern.compile(
        "[^" + SuggestionHelper.TOKEN_SPLIT_LETTERS + "]"
    );
    private final CharArraySet stopWords;

    static {
        synonymMap = buildSynonymMap(
            new PathMatchingResourcePatternResolver().getResource("classpath:/synonyms.txt")
        );
    }

    protected AbstractSuggestionAnalyzer(CharArraySet stopWords) {
        this.stopWords = stopWords;
    }

    public List<String> analyze(String text) {
        try (TokenStream tokenStream = this.tokenStream(null, text)) {
            List<String> result = new ArrayList<>();
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                result.add(attr.toString());
            }
            tokenStream.end();
            return result;
        } catch (IOException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Analyzieren von den Suggestions.", e);
        }
        return List.of();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        KeywordTokenizer src = new KeywordTokenizer();
        TokenStream result = new PatternReplaceFilter(src, specialCharacterPattern, "", true);
        result = new StopFilter(result, stopWords);
        result = new SynonymGraphFilter(result, synonymMap, true);
        result = new FlattenGraphFilter(result);
        return new TokenStreamComponents(src, result);
    }

    private static SynonymMap buildSynonymMap(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            CustomAnalyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(KeywordTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .build();
            SolrSynonymParser parser = new SolrSynonymParser(true, false, analyzer);

            parser.parse(reader);
            return parser.build();
        } catch (IOException | ParseException e) {
            // PV-8946 catch-log-rethrow
            log.warn("Fehler beim Einlesen der Synonyme.", e);
            throw new IllegalStateException(e);
        }
    }
}
