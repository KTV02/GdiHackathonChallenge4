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

import static de.dataport.suchdienst.util.StreamUtils.combineFiltersConjunctionally;
import static de.dataport.suchdienst.util.StreamUtils.distinctByKeys;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class StreamUtilsTest {

    @Test
    void testEven() {
        var s = List.of("1l", "1r", "2l", "2r", "3l", "3r");

        final Iterator<Pair<String, String>> iterator = StreamUtils.toPairs(s.stream()).iterator();
        assertEquals(iterator.next(), Pair.of("1l", "1r"));
        assertEquals(iterator.next(), Pair.of("2l", "2r"));
        assertEquals(iterator.next(), Pair.of("3l", "3r"));
        assertFalse(iterator.hasNext());
    }

    @Test
    void testOdd() {
        var s = List.of("1l", "1r", "2l", "2r", "3l");

        final Iterator<Pair<String, String>> iterator = StreamUtils.toPairs(s.stream()).iterator();
        assertEquals(iterator.next(), Pair.of("1l", "1r"));
        assertEquals(iterator.next(), Pair.of("2l", "2r"));
        assertEquals(iterator.next(), Pair.of("3l", null));
        assertFalse(iterator.hasNext());
    }

    @Test
    void testVoid() {
        final Iterator<Pair<String, String>> iterator = StreamUtils.toPairs(
            Stream.<String>empty()
        ).iterator();
        assertFalse(iterator.hasNext());
    }

    @Data
    @AllArgsConstructor
    public static class TestClass {

        private String propA;
        private String propB;

        @Nullable
        private String propC;
    }

    @Test
    void testDistinctBySingle() {
        var list = List.of(new TestClass("a", "b", "c"), new TestClass("a", "b", "c"));

        var filtered = list
            .stream()
            .filter(distinctByKeys(Set.of(TestClass::getPropA)))
            .toList();

        assertThat(filtered.isEmpty(), is(false));
        assertThat(filtered, hasSize(1));
        assertThat(filtered, hasItem(hasProperty("propA", is("a"))));
    }

    @Test
    void testDistinctByMulti() {
        var list = List.of(
            new TestClass("a", "b", "cc"),
            new TestClass("a", "b", "cc"),
            new TestClass("a", "b", "cc"),
            new TestClass("aa", "b", "ccc")
        );

        var filtered = list
            .stream()
            .filter(distinctByKeys(Set.of(TestClass::getPropA, TestClass::getPropC)))
            .toList();

        assertThat(filtered.isEmpty(), is(false));
        assertThat(filtered, hasSize(2));
        assertThat(
            filtered,
            hasItems(
                hasProperty("propA", is("a")),
                hasProperty("propC", is("cc")),
                hasProperty("propA", is("aa")),
                hasProperty("propC", is("ccc"))
            )
        );
    }

    @Test
    void testCombineFilterConjunctionally() {
        var list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Predicate<Integer> f1 = i -> i < 10;
        Predicate<Integer> f2 = i -> i > 8;

        var result1 = list
            .stream()
            .filter(combineFiltersConjunctionally(Set.of(f1, f2)))
            .toList();

        assertThat(result1, hasSize(1));
        assertThat(result1, contains(9));

        var result2 = list
            .stream()
            .filter(combineFiltersConjunctionally(Set.of(f1)))
            .toList();

        assertThat(result2, hasSize(9));
        assertThat(result2, contains(1, 2, 3, 4, 5, 6, 7, 8, 9));

        var result3 = list.stream().filter(combineFiltersConjunctionally(Set.of())).toList();

        assertThat(result3, hasSize(10));

        Predicate<Integer> f3 = i -> i < 8;
        var result4 = list
            .stream()
            .filter(combineFiltersConjunctionally(Set.of(f1, f2, f3)))
            .toList();

        assertThat(result4, hasSize(0));
    }
}
