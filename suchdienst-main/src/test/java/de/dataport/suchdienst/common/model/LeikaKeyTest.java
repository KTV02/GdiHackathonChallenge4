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
package de.dataport.suchdienst.common.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class LeikaKeyTest {

    private static final String LEIKA_LEISTUNGS_GRUPPIERUNG = "99008000000000";
    private static final String LEIKE_LEISTUNGS_GRUPPIERUNG_PREFIX = "99008";
    private static final String LEIKA_LEISTUNGS_KENNUNG = "99008001000000";
    private static final String LEIKA_LEISTUNGS_KENNUNG_PREFIX = "99008001";
    private static final String LEIKA_VER_KENNUNG = "99008001001000";
    private static final String LEIKA_VER_KENNUNG_PREFIX = "99008001001";
    private static final String LEIKA_VER_DETAIL = "99008001001020";
    private static final String LEIKA_VER_DETAIL_PRIVATE_LG = "11908001001020";
    private static final String LEIKA_VER_DETAIL_PRIVATE_LK = "11008901001020";
    private static final String LEIKA_VER_DETAIL_PRIVATE_VK = "11008001901020";
    private static final String LEIKA_VER_DETAIL_PRIVATE_VD = "11008001001920";
    private static final String LEIKA_VER_DETAIL_NONPRIVATE = "99099099099099";
    private static final String LEIKA_VER_KENNUNG_PRIVATE = "11908001001000";

    @Test
    void testGetDepth() {
        assertAll(
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL).getDepth(), is(14)),
            () -> assertThat(new LeikaKey(LEIKA_VER_KENNUNG).getDepth(), is(11)),
            () -> assertThat(new LeikaKey(LEIKA_LEISTUNGS_KENNUNG).getDepth(), is(8)),
            () -> assertThat(new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getDepth(), is(5))
        );
    }

    @Test
    void testGetPathPrefix() {
        assertAll(
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL).getPathPrefix(), is(Optional.empty())),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG).getPathPrefix().get().getKey(),
                    is(LEIKA_VER_KENNUNG_PREFIX)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_KENNUNG).getPathPrefix().get().getKey(),
                    is(LEIKA_LEISTUNGS_KENNUNG_PREFIX)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getPathPrefix().get().getKey(),
                    is(LEIKE_LEISTUNGS_GRUPPIERUNG_PREFIX)
                )
        );
    }

    @Test
    void testGetFullKey() {
        assertAll(
            // Vollständige Schlüssel bleiben unverändert
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL).getFullKey(), is(LEIKA_VER_DETAIL)),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LG).getFullKey(),
                    is(LEIKA_VER_DETAIL_PRIVATE_LG)
                ),
            // Verkürzte Präfixe werden rechts mit Nullen aufgefüllt
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG_PREFIX, false).getFullKey(),
                    is(LEIKA_VER_KENNUNG)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_KENNUNG_PREFIX, false).getFullKey(),
                    is(LEIKA_LEISTUNGS_KENNUNG)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKE_LEISTUNGS_GRUPPIERUNG_PREFIX, false).getFullKey(),
                    is(LEIKA_LEISTUNGS_GRUPPIERUNG)
                )
        );
    }

    /**
     * Das Auffüllen mit Nullen muss den durch {@link LeikaKey#getPathPrefix()} verkürzten Schlüssel
     * verlustfrei wiederherstellen. Darauf baut die Ausgabe der effektiv gesuchten LeiKa-Schlüssel in
     * der Hierarchie-Suche auf.
     */
    @Test
    void testGetPathPrefixIsLosslessWithGetFullKey() {
        assertAll(
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG).getPathPrefix().get().getFullKey(),
                    is(LEIKA_VER_KENNUNG)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_KENNUNG).getPathPrefix().get().getFullKey(),
                    is(LEIKA_LEISTUNGS_KENNUNG)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getPathPrefix().get().getFullKey(),
                    is(LEIKA_LEISTUNGS_GRUPPIERUNG)
                ),
            // auch für länderspezifische Schlüssel, bei denen die Instanz erhalten bleibt
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG_PRIVATE).getPathPrefix().get().getFullKey(),
                    is(LEIKA_VER_KENNUNG_PRIVATE)
                )
        );
    }

    @Test
    void testIsPrivateSpace() {
        assertAll(
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LG).isPrivateSpace(), is(true)),
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LK).isPrivateSpace(), is(true)),
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_VK).isPrivateSpace(), is(true)),
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LG).isPrivateSpace(), is(true)),
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_VD).isPrivateSpace(), is(true)),
            () -> assertThat(new LeikaKey(LEIKA_VER_DETAIL_NONPRIVATE).isPrivateSpace(), is(false))
        );
    }

    @Test
    void testGetSearchKey() {
        assertAll(
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LG).getSearchKey(),
                    is(LEIKA_VER_DETAIL_PRIVATE_LG)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_LK).getSearchKey(),
                    is(LEIKA_VER_DETAIL_PRIVATE_LK)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_VK).getSearchKey(),
                    is(LEIKA_VER_DETAIL_PRIVATE_VK)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL_PRIVATE_VD).getSearchKey(),
                    is(LEIKA_VER_DETAIL_PRIVATE_VD)
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getSearchKey(),
                    is(LEIKA_LEISTUNGS_GRUPPIERUNG.substring(2))
                )
        );
    }

    @Test
    void testGetParentKey() {
        assertAll(
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getParentKey(),
                    is(Optional.empty())
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_KENNUNG).getParentKey(),
                    is(Optional.of(new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG)))
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG).getParentKey(),
                    is(Optional.of(new LeikaKey(LEIKA_LEISTUNGS_KENNUNG)))
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL).getParentKey(),
                    is(Optional.of(new LeikaKey(LEIKA_VER_KENNUNG)))
                )
        );
    }

    @Test
    void testGetHierarchy() {
        assertAll(
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG).getHierarchy(),
                    Matchers.containsInAnyOrder(
                        new LeikaKey("99008", false),
                        new LeikaKey("99008000", false),
                        new LeikaKey("99008000000", false),
                        new LeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG, false)
                    )
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_LEISTUNGS_KENNUNG).getHierarchy(),
                    Matchers.containsInAnyOrder(
                        new LeikaKey("99008", false),
                        new LeikaKey("99008001", false),
                        new LeikaKey("99008001000", false),
                        new LeikaKey(LEIKA_LEISTUNGS_KENNUNG, false)
                    )
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_KENNUNG).getHierarchy(),
                    Matchers.containsInAnyOrder(
                        new LeikaKey("99008", false),
                        new LeikaKey("99008001", false),
                        new LeikaKey("99008001001", false),
                        new LeikaKey(LEIKA_VER_KENNUNG, false)
                    )
                ),
            () ->
                assertThat(
                    new LeikaKey(LEIKA_VER_DETAIL).getHierarchy(),
                    Matchers.containsInAnyOrder(
                        new LeikaKey("99008", false),
                        new LeikaKey("99008001", false),
                        new LeikaKey("99008001001", false),
                        new LeikaKey(LEIKA_VER_DETAIL, false)
                    )
                )
        );
    }

    @Test
    void testConstructorParameterValidation() {
        String tooShortForLeikaId = "0".repeat(13);
        String tooLongForLeikaId = "0".repeat(15);
        assertAll(
            () ->
                assertThrows(IllegalArgumentException.class, () ->
                    new LeikaKey(tooShortForLeikaId)
                ),
            () ->
                assertThrows(IllegalArgumentException.class, () -> new LeikaKey(tooLongForLeikaId)),
            () ->
                assertThrows(IllegalArgumentException.class, () -> new LeikaKey("1234567890123A")),
            () -> assertThrows(IllegalArgumentException.class, () -> new LeikaKey(null))
        );
    }

    @Test
    void testIsLeikaKey() {
        assertAll(
            () -> assertTrue(LeikaKey.isLeikaKey(LEIKA_LEISTUNGS_GRUPPIERUNG)),
            () -> assertFalse(LeikaKey.isLeikaKey("0".repeat(13))),
            () -> assertFalse(LeikaKey.isLeikaKey("X".repeat(14))),
            () -> assertFalse(LeikaKey.isLeikaKey("")),
            () -> assertFalse(LeikaKey.isLeikaKey(null))
        );
    }
}
