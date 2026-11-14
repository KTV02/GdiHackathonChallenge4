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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.model.internal.SearchARS;
import de.dataport.suchdienst.geo.service.BkgQueryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BkgQueryProviderTest {

    private BkgQueryProvider provider;

    @BeforeEach
    void setUp() {
        SuchdienstProperties properties = new SuchdienstProperties();
        provider = new BkgQueryProvider(properties);
    }

    @Test
    void searchByArsExact() {
        var searchArs = new SearchARS("064120000000");
        searchArs.setProcessedArs("06412*");

        var params = provider.getSearchByArsParams(searchArs, 10, "typ:ort", true);
        assertThat(params, hasEntry(is("query"), is("rs:06412* AND schluessel:064120000000")));
    }

    @Test
    void searchByArs() {
        var searchArs1 = new SearchARS("064120000000");
        searchArs1.setProcessedArs("06412*");
        var params1 = provider.getSearchByArsParams(searchArs1, 10, "typ:ort", false);
        assertThat(params1, hasEntry(is("query"), is("rs:064120000000")));

        var searchArs2 = new SearchARS("0641200000000000");
        var params2 = provider.getSearchByArsParams(searchArs2, 10, "typ:ort", false);
        assertThat(params2, hasEntry(is("query"), is("schluessel:0641200000000000")));

        var params3 = provider.getSearchByArsParams("064120000000", null, 10, "typ:ort");
        assertThat(params3, hasEntry(is("query"), is("rs:064120000000")));
        assertThat(params3, hasEntry(is("filter"), is("typ:ort")));
        assertThat(params3, hasEntry(is("count"), is("10")));
    }
}
