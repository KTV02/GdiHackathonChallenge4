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
package de.dataport.suchdienst.suchen.service.legacy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import de.dataport.suchdienst.suchen.model.out.leistung.SdOutModelV5;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlock;
import de.dataport.suchdienst.suchen.model.out.leistung.SeDeBlockModule;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@Deprecated // muss entfernt werden, sobald die deprecated Methode sortModules nicht mehr genutzt wird
class SortingTest {

    private static SeDeBlockModule module(String code) {
        SeDeBlockModule m = new SeDeBlockModule(false, false);
        m.setCode(code);
        return m;
    }

    private static SdOutModelV5 modelWithDetails(SeDeBlock... blocks) {
        SdOutModelV5 model = new SdOutModelV5();
        model.setDetails(new ArrayList<>(List.of(blocks)));
        return model;
    }

    @Test
    void sortsModulesAccordingToPredefinedOrder() {
        var block1 = module("23");
        var block2 = module("24");
        var block3 = module("14");
        var block4 = module("07");
        var block5 = module("97");
        var model = modelWithDetails(block1, block2, block3, block4, block5);

        DetailSorting.sortModules(model);

        assertThat(extractCodes(model.getDetails()), contains("24", "23", "14", "97", "07"));
    }

    @Test
    void sortsUnmappedNumericModulesAfterMappedOnes() {
        var block1 = module("16");
        var block2 = module("99");
        var block3 = module("50");
        var block4 = module("23");
        var model = modelWithDetails(block1, block2, block3, block4);

        DetailSorting.sortModules(model);

        assertThat(extractCodes(model.getDetails()), contains("23", "16", "50", "99"));
    }

    @Test
    void sortsNonNumericUnmappedModulesAtEnd() {
        var block1 = module("23");
        var block2 = module("foo");
        var block3 = module("bar");
        var block4 = module("15");
        var model = modelWithDetails(block2, block1, block3, block4);

        DetailSorting.sortModules(model);

        assertThat(extractCodes(model.getDetails()), contains("23", "15", "foo", "bar"));
    }

    @Test
    void doesNotFailOnEmptyOrSingleElementList() {
        var v5 = new SdOutModelV5();
        v5.setDetails(new ArrayList<>());
        DetailSorting.sortModules(v5);
        assertThat(v5.getDetails(), empty());

        v5.getDetails().add(module("24"));
        DetailSorting.sortModules(v5);
        assertThat(extractCodes(v5.getDetails()), contains("24"));
    }

    private static List<String> extractCodes(List<? extends SeDeBlock> blocks) {
        return blocks.stream().map(SeDeBlock::getCode).toList();
    }
}
