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
package de.dataport.suchdienst.indexer.parser;

import de.dataport.suchdienst.indexer.model.verwaltungsleistung.AbstractServiceModule;
import de.dataport.suchdienst.indexer.util.TypeUtil;
import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Wählt anhand der XZuFi-Modulklasse einen {@link Parser} aus der {@link ParserRegistry} aus
 * und ordnet den geparsten Indexer-Modulen ihre Codes zu.
 */
@Slf4j
public class ParserSelector {

    // Präfix für die Codes von individuellen Textmodulen, um sie von den anderen Modulen in ModuleServiceCode unterscheiden zu können
    public static final String INDIVIDUAL_MODULE_PREFIX = "INDIVIDUELL_";

    private final ParserRegistry parserRegistry;

    /**
     * Erstellt einen Selektor mit der angegebenen Parser-Registrierung.
     *
     * @param parserRegistry Zuordnung der XZuFi-Modulklassen zu ihren Parsern
     */
    private ParserSelector(ParserRegistry parserRegistry) {
        this.parserRegistry = parserRegistry;
    }

    /**
     * Erstellt einen Selektor für die übergebene Parser-Registrierung.
     *
     * @param parserRegistry Zuordnung der XZuFi-Modulklassen zu ihren Parsern
     * @return neuer Parser-Selektor
     */
    public static ParserSelector of(ParserRegistry parserRegistry) {
        return new ParserSelector(parserRegistry);
    }

    /**
     * Parst ein einzelnes XZuFi-Modul und ordnet dem Ergebnis seinen Modulcode zu.
     * Unbekannte oder nicht übernommene Module werden ignoriert.
     *
     * @param <XzufiModule> Typ des XZuFi-Moduls
     * @param <IndexerModule> Typ des erzeugten Indexer-Moduls
     * @param module zu parsendes XZuFi-Modul
     * @param locale Sprache des gewünschten Ergebnisses
     * @param id Kennung der Leistungsbeschreibung oder Spezialisierung für Warnmeldungen
     * @return Code und geparstes Modul oder {@code null}, falls kein gültiges Ergebnis entsteht
     */
    @SuppressWarnings("java:S119")
    public <XzufiModule, IndexerModule extends AbstractServiceModule> Map.Entry<
        String,
        IndexerModule
    > accept(XzufiModule module, Locale locale, String id) {
        if (module == null) {
            log.warn("Null-Modul in LB/SP {} kann nicht geparst werden. Modul wird ignoriert.", id);
            return null;
        }

        Parser<XzufiModule, IndexerModule> parser = TypeUtil.castClass(
            parserRegistry.getParsers().get(module.getClass())
        );

        if (parser == null) {
            log.warn(
                "Unbekanntes Modul {} in LB/SP {}. Modul wird ignoriert.",
                module.getClass().getSimpleName(),
                id
            );
            return null;
        }

        IndexerModule parsedModule = parser.parse(module, locale);
        String code = resolveCode(parser, module, id);

        return code == null || parsedModule == null ? null : Map.entry(code, parsedModule);
    }

    /**
     * Parst ein XZuFi-Modul und gibt eine Liste von Code-Modul-Paaren zurück.
     * Bei einem {@link CompositeParser} wird {@link CompositeParser#parseComposite} aufgerufen,
     * ansonsten wird regulär über {@link Parser#parse} ein einzelnes Ergebnis erzeugt.
     */
    @SuppressWarnings("java:S119")
    public <XzufiModule, IndexerModule extends AbstractServiceModule> List<
        Map.Entry<String, AbstractServiceModule>
    > acceptList(XzufiModule module, Locale locale, String id) {
        if (module == null) {
            log.warn("Null-Modul in LB/SP {} kann nicht geparst werden. Modul wird ignoriert.", id);
            return List.of();
        }

        Parser<XzufiModule, IndexerModule> parser = TypeUtil.castClass(
            parserRegistry.getParsers().get(module.getClass())
        );

        if (parser == null) {
            log.warn(
                "Unbekanntes Modul {} in LB/SP {}. Modul wird ignoriert.",
                module.getClass().getSimpleName(),
                id
            );
            return List.of();
        }

        if (parser instanceof CompositeParser<XzufiModule, IndexerModule> compositeParser) {
            return compositeParser
                .parseComposite(module, locale)
                .stream()
                .map(entry -> Map.entry(entry.getKey().getCode(), entry.getValue()))
                .toList();
        }

        IndexerModule parsedModule = parser.parse(module, locale);
        String code = resolveCode(parser, module, id);

        if (code == null || parsedModule == null) {
            return List.of();
        }
        return List.of(Map.entry(code, parsedModule));
    }

    /**
     * Ermittelt den Code des Parsers oder bei Textmodulen den Code aus dem XZuFi-Modul.
     * Individuelle Textmodule erhalten zusätzlich das Präfix {@link #INDIVIDUAL_MODULE_PREFIX}.
     *
     * @param <XzufiModule> Typ des XZuFi-Moduls
     * @param <IndexerModule> Typ des erzeugten Indexer-Moduls
     * @param parser Parser des XZuFi-Moduls
     * @param module XZuFi-Modul, aus dem gegebenenfalls der Code gelesen wird
     * @param id Kennung der Leistungsbeschreibung oder Spezialisierung für Warnmeldungen
     * @return aufgelöster Modulcode oder {@code null}, falls kein Code bestimmt werden kann
     */
    @SuppressWarnings("java:S119") // Ein Generic mit nur einem Buchstaben ist schlechter lesbar
    private <XzufiModule, IndexerModule extends AbstractServiceModule> String resolveCode(
        Parser<XzufiModule, IndexerModule> parser,
        XzufiModule module,
        String id
    ) {
        String code = Optional.ofNullable(parser.getCode())
            .map(ServiceModuleCode::getCode)
            .orElse(null);

        // Textmodule und Textmodule-Individuell haben keine feste Code-Zuordnungen,
        // daher wird der Code aus dem Modul selbst verwendet.
        if (code == null) {
            switch (module) {
                case de.xoev.xzufi.v2_2_0.Textmodul txtModule -> code = txtModule
                    .getLeikaTextmodul()
                    .getCode();
                case de.xoev.xzufi.v2_3_1.Textmodul txtModule -> code = txtModule
                    .getLeikaTextmodul()
                    .getCode();
                case de.xoev.xzufi.v2_2_0.TextmodulIndividuell txtModuleIndividual -> code =
                    INDIVIDUAL_MODULE_PREFIX +
                    txtModuleIndividual.getIndividuellesTextmodultyp().getCode();
                case de.xoev.xzufi.v2_3_1.TextmodulIndividuell txtModuleIndividual -> code =
                    INDIVIDUAL_MODULE_PREFIX +
                    txtModuleIndividual.getIndividuellesTextmodultyp().getCode();
                default -> log.warn(
                    "Kein passender Code für Modul {} in LB/SP {} gefunden. Modul wird ignoriert.",
                    module.getClass().getSimpleName(),
                    id
                );
            }
        }

        return code;
    }

    /**
     * Sucht den registrierten Parser anhand der Klasse eines XZuFi-Moduls.
     *
     * @param <XzufiModule> Typ des XZuFi-Moduls
     * @param <IndexerModule> Typ des erzeugten Indexer-Moduls
     * @param module XZuFi-Modul, dessen Parser gesucht wird
     * @return der registrierte Parser oder ein leeres {@link Optional}, falls keiner vorhanden ist
     */
    @SuppressWarnings("java:S119") // Ein Generic mit nur einem Buchstaben ist schlechter lesbar
    public <XzufiModule, IndexerModule extends AbstractServiceModule> Optional<
        Parser<XzufiModule, IndexerModule>
    > getModuleParser(XzufiModule module) {
        Parser<XzufiModule, IndexerModule> parser = TypeUtil.castClass(
            parserRegistry.getParsers().get(module.getClass())
        );

        if (parser == null) {
            log.warn("Keinen passenden Parser für Modul {} in Spezialisierungen gefunden.", module);
            return Optional.empty();
        }

        return Optional.of(parser);
    }
}
