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
package de.dataport.suchdienst.geo;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import de.dataport.suchdienst.common.model.ArsHierarchyEbene;
import de.dataport.suchdienst.geo.util.ArsSearchUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArsSearchUtilsTest {

    @Test
    void buildBkgArsWithHierarchy() {
        var ars1 = ArsSearchUtils.buildBkgArsSearchParam("064120000000", emptyMap());
        assertThat(ars1.getProcessedArs(), is("06412*"));
        assertThat(ars1.getSchluessel(), is("064120000000"));

        var ars2 = ArsSearchUtils.buildBkgArsSearchParam("073120000000", emptyMap());
        assertThat(ars2.getProcessedArs(), is("07312*"));
        assertThat(ars2.getSchluessel(), is("073120000000"));
    }

    @Test
    void buildBkgArs() {
        var ars1 = ArsSearchUtils.buildBkgArsSearchParam(
            "010515163016",
            Map.of("010515163016", "")
        );
        assertThat(ars1.getProcessedArs(), is("010515163016"));
        assertThat(ars1.getSchluessel(), is("010515163016"));
        assertThat(ars1.getArsEbene(), is(nullValue()));

        // Fallthrough
        var ars2 = ArsSearchUtils.buildBkgArsSearchParam("027010701701", emptyMap());
        assertThat(ars2.getProcessedArs(), is("027010701701"));
        assertThat(ars2.getSchluessel(), is("027010701701"));
        assertThat(ars2.getArsEbene(), is(nullValue()));

        // Stadtstaat mit OT
        var ars3 = ArsSearchUtils.buildBkgArsSearchParam(
            "027010701701",
            Map.of("027010701701", "")
        );
        assertThat(ars3.getProcessedArs(), is("027010701701"));
        assertThat(ars3.getSchluessel(), is("0200000000000000"));
        assertThat(ars3.getArsEbene(), is(ArsHierarchyEbene.ORTSTEIL));

        // Stadtstaat ohne OT/nur BL
        var ars4 = ArsSearchUtils.buildBkgArsSearchParam(
            "020000000000",
            Map.of("020000000000", "")
        );
        assertThat(ars4.getProcessedArs(), is("02*"));
        assertThat(ars4.getSchluessel(), is("020000000000"));
        assertThat(ars4.getArsEbene(), is(ArsHierarchyEbene.BUNDESLAND));
    }
}
