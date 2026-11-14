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
package de.dataport.suchdienst.suchen.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstParameter;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.OnlinedienstVL;
import de.dataport.suchdienst.suchen.model.out.leistung.detail.OnlineServiceLink;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OdOutModel;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceDetail;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.OnlineServiceStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OnlinedienstMapperTest {

    @ParameterizedTest
    @CsvSource({
        "00, UNDEFINED",
        "10, PLANNING",
        "20, INITIALIZATION",
        "30, TEST",
        "40, PRODUCTION",
        "50, MAINTENANCE",
        "99, DEACTIVATION",
    })
    void mapsOnlinedienstStatus(String code, OnlineServiceStatus expected) {
        OnlinedienstVL onlinedienst = onlinedienst("od1", true, "https://od1.example");
        onlinedienst.setOnlinedienstStatus(
            new CodelistKey(
                de.dataport.suchdienst.indexer.model.OnlinedienstStatus.CODELIST_URI,
                "1",
                code
            )
        );

        OnlineServiceLink result = OnlinedienstMapper.INSTANCE.mapOdOutModelV6(
            onlinedienst,
            onlinedienst.getLinks().getFirst()
        );

        assertEquals(expected, result.getStatus());
    }

    @Test
    void mapsMissingOnlinedienstStatusToUndefined() {
        OnlinedienstVL onlinedienst = onlinedienst("od1", true, "https://od1.example");

        OnlineServiceLink result = OnlinedienstMapper.INSTANCE.mapOdOutModelV6(
            onlinedienst,
            onlinedienst.getLinks().getFirst()
        );

        assertEquals(OnlineServiceStatus.UNDEFINED, result.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void mapsPublicDisplayAllowedToOnlineServiceLinkAndDetail(boolean publicDisplayAllowed) {
        OnlinedienstVL onlinedienst = onlinedienst(
            "od1",
            publicDisplayAllowed,
            "https://od1.example"
        );

        OnlineServiceLink link = OnlinedienstMapper.INSTANCE.mapOdOutModelV6(
            onlinedienst,
            onlinedienst.getLinks().getFirst()
        );
        OnlineServiceDetail detail = OnlinedienstMapper.INSTANCE.mapV3Beta1(onlinedienst, Set.of());

        assertEquals(publicDisplayAllowed, link.getPublicDisplayAllowed());
        assertEquals(publicDisplayAllowed, detail.getPublicDisplayAllowed());
    }

    @Test
    void mapsSeparateParametersToV3Beta1Detail() {
        OnlinedienstVL onlinedienst = onlinedienst("od1", true, "https://od1.example");
        OnlinedienstParameter efaParameter = new OnlinedienstParameter();
        efaParameter.setName("efa.datenschutzerklaerung.url");
        OnlinedienstParameter customParameter = new OnlinedienstParameter();
        customParameter.setName("custom.parameter");
        onlinedienst.setParametersEfa(List.of(efaParameter));
        onlinedienst.setParametersCustom(List.of(customParameter));

        OnlineServiceDetail detail = OnlinedienstMapper.INSTANCE.mapV3Beta1(onlinedienst, Set.of());

        assertEquals("efa.datenschutzerklaerung.url", detail.getParametersEfa().getFirst().name());
        assertEquals("custom.parameter", detail.getParametersCustom().getFirst().name());
    }

    @ParameterizedTest
    @CsvSource({
        "urn:unexpected, 1, 40",
        "urn:xoev-de:fim:codeliste:xzufi.onlinedienststatus, 2, 40",
        "urn:xoev-de:fim:codeliste:xzufi.onlinedienststatus, 1, 60",
    })
    void mapsUnknownOnlinedienstStatusToUndefined(String uri, String version, String code) {
        OnlinedienstVL onlinedienst = onlinedienst("od1", true, "https://od1.example");
        onlinedienst.setOnlinedienstStatus(new CodelistKey(uri, version, code));

        OnlineServiceLink result = OnlinedienstMapper.INSTANCE.mapOdOutModelV6(
            onlinedienst,
            onlinedienst.getLinks().getFirst()
        );

        assertEquals(OnlineServiceStatus.UNDEFINED, result.getStatus());
    }

    private static OnlinedienstVL onlinedienst(String id, boolean oeffentlichAnzeigen, String uri) {
        OnlinedienstVL od = new OnlinedienstVL();
        od.setId(id);
        od.setName("Onlinedienst " + id);
        od.setOeffentlichAnzeigen(oeffentlichAnzeigen);
        od.setLinks(List.of(LinkDynamic.of(uri, null, null)));
        return od;
    }

    @Test
    void liefertNichtOeffentlicheOnlinediensteAus() {
        List<OnlinedienstVL> onlinedienste = List.of(
            onlinedienst("od1", true, "https://od1.example"),
            onlinedienst("od2", false, "https://od2.example")
        );

        List<OdOutModel> result = OnlinedienstMapper.INSTANCE.getOnlineServiceLinks(onlinedienste);

        assertEquals(2, result.size());
        assertEquals("od2", result.getLast().getId());
    }
}
