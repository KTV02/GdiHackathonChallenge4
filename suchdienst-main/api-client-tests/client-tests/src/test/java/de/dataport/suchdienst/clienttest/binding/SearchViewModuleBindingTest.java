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
package de.dataport.suchdienst.clienttest.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import de.dataport.suchdienst.client.model.SdOutModelV7;
import de.dataport.suchdienst.client.model.SdOutModelV7Search;
import de.dataport.suchdienst.client.model.SeDeBlockGroupedModule;
import de.dataport.suchdienst.client.model.SeDeBlockGroupedModuleSearch;
import de.dataport.suchdienst.client.model.SeDeBlockModuleSearch;
import de.dataport.suchdienst.clienttest.support.StrictApiClientFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Testziel: V7-Suchmodule binden über ihre Discriminatoren an die Search-View-Untertypen. */
class SearchViewModuleBindingTest {

    @Test
    void bindsSearchViewSubtypesInsteadOfUnscopedModels() {
        var hit = StrictApiClientFactory.mapper().readValue(
            """
            {"details": [
              {"@type": "_module", "code": "06", "text": "SDK Volltext"},
              {"@type": "_moduleDetail", "code": "13", "details": [
                {"@type": "_module", "text": "SDK Kostenposition"}
              ]}
            ]}
            """,
            SdOutModelV7Search.class
        );
        var text = assertInstanceOf(SeDeBlockModuleSearch.class, hit.getDetails().get(0));
        assertEquals("SDK Volltext", text.getText());
        var costs = assertInstanceOf(SeDeBlockGroupedModuleSearch.class, hit.getDetails().get(1));
        assertEquals("SDK Kostenposition", costs.getDetails().getFirst().getText());
    }

    @Test
    void bindsUnscopedGroupedModuleWithoutDetailViewNameCollision() {
        var hit = StrictApiClientFactory.mapper().readValue(
            """
            {"details": [{"@type": "_moduleDetail", "code": "13",
              "validityPeriods": [{"gte": "2000-01-01", "lte": "2099-12-31"}],
              "details": [{"@type": "_module", "text": "SDK Kostenposition"}]
            }]}
            """,
            SdOutModelV7.class
        );
        var costs = assertInstanceOf(SeDeBlockGroupedModule.class, hit.getDetails().getFirst());
        assertEquals(LocalDate.of(2000, 1, 1), costs.getValidityPeriods().getFirst().getGte());
        assertEquals(LocalDate.of(2099, 12, 31), costs.getValidityPeriods().getFirst().getLte());
        assertEquals("SDK Kostenposition", costs.getDetails().getFirst().getText());
    }
}
