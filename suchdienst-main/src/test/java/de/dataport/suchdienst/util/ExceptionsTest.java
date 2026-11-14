/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    @Test
    void hasCause() {
        RuntimeException t = new RuntimeException(new IllegalArgumentException("bad arg"));
        assertTrue(Exceptions.hasCause(t, IllegalArgumentException.class));
        assertFalse(Exceptions.hasCause(t, NullPointerException.class));
    }

    @Test
    void findCause() {
        RuntimeException t = new RuntimeException(new IllegalArgumentException("bad arg"));
        Optional<IllegalArgumentException> found = Exceptions.findCause(
            t,
            IllegalArgumentException.class
        );
        assertTrue(found.isPresent());
        // not present case
        var notFound = Exceptions.findCause(t, NullPointerException.class);
        assertFalse(notFound.isPresent());
    }
}
