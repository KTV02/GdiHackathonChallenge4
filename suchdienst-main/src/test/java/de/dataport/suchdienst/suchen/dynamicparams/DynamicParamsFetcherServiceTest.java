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
package de.dataport.suchdienst.suchen.dynamicparams;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.indexer.model.mapping.DynamischerParameter;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OrganisationseinheitVL;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Verwaltungsleistung;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DynamicParamsFetcherServiceTest {

    private static final String LEISTUNG_CODE = "001";
    private static final String GEBIET_CODE = "002";
    private static final String OE_ID_CODE = "003";
    private static final String LEIKA_CODE = "004";

    private DynamicParamsFetcherService dynamicParamsFetcherService =
        new DynamicParamsFetcherService();

    Verwaltungsleistung prepareTestVl() {
        Verwaltungsleistung vl = new Verwaltungsleistung();
        OnlinedienstVL odVl = new OnlinedienstVL();

        DynamischerParameter odDynParamLeistung = new DynamischerParameter(
            LEISTUNG_CODE,
            "leistung_id"
        );
        DynamischerParameter odDynParamGebiet = new DynamischerParameter(GEBIET_CODE, "gebiet_id");
        DynamischerParameter odDynParamOe = new DynamischerParameter(OE_ID_CODE, "oe_id");
        DynamischerParameter odDynParamLeika = new DynamischerParameter(LEIKA_CODE, "leika_id");

        List<DynamischerParameter> onlinedienstDynamischerParameters = new ArrayList<>();
        onlinedienstDynamischerParameters.add(odDynParamLeistung);
        onlinedienstDynamischerParameters.add(odDynParamGebiet);
        onlinedienstDynamischerParameters.add(odDynParamOe);
        onlinedienstDynamischerParameters.add(odDynParamLeika);

        LinkDynamic link = LinkDynamic.of(
            "www.dataport.de/onlinedienst",
            null,
            null,
            onlinedienstDynamischerParameters
        );

        List<LinkDynamic> linkList = new ArrayList<>();
        linkList.add(link);
        odVl.setLinks(linkList);

        List<OnlinedienstVL> onlinedienstVLs = new ArrayList<>();
        onlinedienstVLs.add(odVl);
        vl.setOnlinedienste(onlinedienstVLs);

        LeistungsbeschreibungVL lbVL = new LeistungsbeschreibungVL();
        lbVL.setId("schemeAgencyId.LB.123456");

        Set<String> arsSet = new HashSet<>();
        arsSet.add("020000000000");
        lbVL.setArs(arsSet);

        OrganisationseinheitVL organisationseinheitVL = new OrganisationseinheitVL();
        List<OrganisationseinheitVL> onlinedienstVLS = new ArrayList<>();
        onlinedienstVLS.add(organisationseinheitVL);
        organisationseinheitVL.setId("schemeAgencyId.OE.xxx");
        vl.setOrganisationseinheiten(onlinedienstVLS);

        Set<String> leikas = new HashSet<>();
        leikas.add("leika");
        lbVL.setLeikakeys(leikas);

        vl.setLeistungsbeschreibung(lbVL);
        return vl;
    }

    @Test
    void testMehrereParameterWerdenGefetched() {
        var vl = prepareTestVl();
        dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(
            vl,
            vl.getOnlinedienste(),
            "030000000000"
        );

        assertEquals(
            "www.dataport.de/onlinedienst?" +
                "leistung_id=123456&" +
                "gebiet_id=030000000000&" +
                "oe_id=xxx&" +
                "leika_id=leika",
            vl.getOnlinedienste().getFirst().getLinks().getFirst().uri()
        );
    }

    @Test
    void testeOhneDaten() {
        Verwaltungsleistung vl = new Verwaltungsleistung();

        assertDoesNotThrow(() ->
            dynamicParamsFetcherService.applyDynamicParamsToOnlinedienste(
                vl,
                vl.getOnlinedienste(),
                "03"
            )
        );
    }
}
