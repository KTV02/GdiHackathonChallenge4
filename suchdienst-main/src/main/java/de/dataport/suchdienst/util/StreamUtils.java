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
package de.dataport.suchdienst.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public final class StreamUtils {

    private StreamUtils() {}

    /**
     * Erstellt eine Liste von Pairs aus einem Stream.
     * Beispiel:
     * Eingabe: Stream.of("a", "b", "c")
     * Ausgabe: List.of(Pair.of("a","b"), Pair.of("c", null))
     * <p>
     * <a href="https://gist.github.com/ktejnora/98c736212ddd023ce420">Quelle</a>
     */
    public static <T> List<Pair<T, T>> toPairs(final Stream<T> s) {
        final AtomicInteger counter = new AtomicInteger(0);
        return s
            .collect(
                Collectors.groupingBy(
                    item -> {
                        final int i = counter.getAndIncrement();
                        return i % 2 == 0 ? i : i - 1;
                    },
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            )
            .values()
            .stream()
            .map(a -> Pair.of(a.getFirst(), a.size() == 2 ? a.get(1) : null))
            .toList();
    }

    public static <P, T> Predicate<P> distinctByKeys(final Set<Function<P, T>> keyExtractors) {
        Map<List<T>, Boolean> seen = new ConcurrentHashMap<>();
        return t -> {
            List<T> keys = keyExtractors
                .stream()
                .map(ex -> ex.apply(t))
                .toList();
            return seen.putIfAbsent(keys, Boolean.TRUE) == null;
        };
    }

    public static <T> Predicate<T> combineFiltersConjunctionally(Set<Predicate<T>> predicates) {
        return predicates.stream().reduce(x -> true, Predicate::and);
    }
}
