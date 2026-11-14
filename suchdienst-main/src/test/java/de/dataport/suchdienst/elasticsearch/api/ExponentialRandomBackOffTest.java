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
package de.dataport.suchdienst.elasticsearch.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.Test;

class ExponentialRandomBackOffTest {

    @Test
    void testBackoffMultiplier2() {
        ExponentialRandomBackOff exponentialRandomBackOff = new ExponentialRandomBackOff(
            50,
            3000,
            2d
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(50L), lessThanOrEqualTo(100L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(100L), lessThanOrEqualTo(200L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(200L), lessThanOrEqualTo(400L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(400L), lessThanOrEqualTo(800L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(800L), lessThanOrEqualTo(1600L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(1600L), lessThanOrEqualTo(3000L))
        );
        assertThat(exponentialRandomBackOff.getSleepAndIncrement(), is(3000L));
    }

    @Test
    void testBackoffMultiplier3() {
        ExponentialRandomBackOff exponentialRandomBackOff = new ExponentialRandomBackOff(
            50,
            3000,
            3d
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(50L), lessThanOrEqualTo(150L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(150L), lessThanOrEqualTo(450L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(450L), lessThanOrEqualTo(1350L))
        );
        assertThat(
            exponentialRandomBackOff.getSleepAndIncrement(),
            allOf(greaterThanOrEqualTo(1350L), lessThanOrEqualTo(3000L))
        );
        assertThat(exponentialRandomBackOff.getSleepAndIncrement(), is(3000L));
    }
}
