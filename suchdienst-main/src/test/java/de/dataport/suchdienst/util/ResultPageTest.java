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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("removal")
@Deprecated // muss entfernt werden sobald die deprecated ResultPage nicht mehr verwendet wird
class ResultPageTest {

    private final List<Integer> results = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    @Test
    void testPages() {
        ResultPage<Integer> page1 = new ResultPage<>(results, 9, 0, 4);
        ResultPage<Integer> page3 = new ResultPage<>(results, 9, 2, 4);

        assertTrue(page1.isFirst());
        assertTrue(page3.isLast());
    }

    @Test
    void testFirstPage() {
        ResultPage<Integer> page = new ResultPage<>(results, 9, 0, 2);

        assertNotNull(page);
        assertTrue(page.isFirst());
        assertFalse(page.isLast());

        assertEquals(9, page.getTotalElements());
        assertEquals(5, page.getTotalPages());

        assertEquals(9, page.getNumberOfElements());
        assertEquals(0, page.getNumber());
    }

    @Test
    void testMiddlePage() {
        ResultPage<Integer> page = new ResultPage<>(results, 9, 3, 2);

        assertNotNull(page);
        assertFalse(page.isFirst());
        assertFalse(page.isLast());

        assertEquals(9, page.getTotalElements());
        assertEquals(5, page.getTotalPages());

        assertEquals(9, page.getNumberOfElements());
        assertEquals(3, page.getNumber());
    }

    @Test
    void testLastPage() {
        ResultPage<Integer> page = new ResultPage<>(results, 9, 5, 2);

        assertNotNull(page);
        assertFalse(page.isFirst());
        assertTrue(page.isLast());

        assertEquals(9, page.getTotalElements());
        assertEquals(5, page.getTotalPages());

        assertEquals(9, page.getNumberOfElements());
        assertEquals(5, page.getNumber());
    }

    @Test
    void testEmptyPage() {
        ResultPage<Integer> page = new ResultPage<>(List.of(), 0, 1, 2);

        assertNotNull(page);
        assertTrue(page.isFirst());
        assertTrue(page.isLast());

        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());

        assertEquals(0, page.getNumberOfElements());
        assertEquals(0, page.getNumber());
    }

    @Test
    void testWrongPage() {
        ResultPage<Integer> page = new ResultPage<>(List.of(1, 2, 3), 0, 8, 5);

        assertNotNull(page);
        assertTrue(page.isFirst());
        assertTrue(page.isLast());

        assertEquals(3, page.getTotalElements());
        assertEquals(1, page.getTotalPages());

        assertEquals(3, page.getNumberOfElements());
        assertEquals(0, page.getNumber());
    }
}
