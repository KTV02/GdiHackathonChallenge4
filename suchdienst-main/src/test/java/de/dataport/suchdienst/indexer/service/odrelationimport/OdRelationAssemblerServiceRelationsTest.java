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
package de.dataport.suchdienst.indexer.service.odrelationimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.importer.model.GenericOnlinedienst;
import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OdRelationAssemblerServiceRelationsTest {

    private static final String SCHEME_AGENCY_ID = "de.bund";
    private static final String OD_ID = SCHEME_AGENCY_ID + ".OD.od-1";
    private static final String LB_ID_1 = SCHEME_AGENCY_ID + ".LB.lb-1";
    private static final String LB_ID_2 = SCHEME_AGENCY_ID + ".LB.lb-2";

    @Mock
    private RoleExtractor roleExtractor;

    @Mock
    private OeStammdatenEnricher oeStammdatenEnricher;

    @InjectMocks
    private OdRelationAssembler assembler;

    @Test
    void createRelationEntities_buildsSortedServiceRelationsAndEmptyLinks() {
        when(roleExtractor.extractRoles(any(), any(), any())).thenReturn(Map.of());
        when(oeStammdatenEnricher.enrich(any(), any())).thenReturn(List.of());

        GenericOnlinedienst od = new GenericOnlinedienst(buildXod("od-1"));
        OnlinedienstMitMetadaten odMitMetadaten = new OnlinedienstMitMetadaten(
            new TransactionMetadata("uuid-1", "tx-1", "1", SCHEME_AGENCY_ID, "System"),
            od
        );

        BatchRelationContext context = new BatchRelationContext(
            new OeStammdatenLookup(Map.of()),
            new OdZustaendigkeitenLookup(
                Map.of(
                    OD_ID,
                    Map.of(
                        LB_ID_2,
                        List.of(odZustaendigkeit(LB_ID_2, "091620100000")),
                        LB_ID_1,
                        List.of(
                            odZustaendigkeit(LB_ID_1, "091620000000"),
                            odZustaendigkeit(LB_ID_1, "091620152000")
                        )
                    )
                )
            ),
            new OeZustaendigkeitenLookup(Map.of()),
            new LeikaLookup(
                Map.of(LB_ID_1, Set.of("99001001000000"), LB_ID_2, Set.of("99002002000000"))
            )
        );

        List<OnlinedienstRelationen> result = assembler.createRelationEntities(
            List.of(odMitMetadaten),
            context
        );

        assertThat(result).hasSize(1);
        OnlinedienstRelationen relation = result.getFirst();
        assertThat(relation.getLinks()).isEmpty();
        assertThat(relation.getServiceRelations())
            .extracting(OnlinedienstRelationen.LeistungRegionZuordnung::lbId)
            .containsExactly(LB_ID_1, LB_ID_2);
        assertThat(relation.getServiceRelations().getFirst().ars()).containsExactlyInAnyOrder(
            "091620000000",
            "091620152000"
        );
        assertThat(relation.getServiceRelations().getFirst().leikakeys()).containsExactly(
            "99001001000000"
        );
    }

    private static OnlinedienstErweitert buildXod(String odValue) {
        OnlinedienstErweitert xod = new OnlinedienstErweitert();
        xod.setId(identifikator(odValue, SCHEME_AGENCY_ID));
        return xod;
    }

    private static ConvertedZustaendigkeit odZustaendigkeit(String lbId, String ars) {
        ZustaendigkeitOnlinedienst zs = new ZustaendigkeitOnlinedienst();
        zs.setId(zsId("ZustaendigkeitOnlinedienst"));
        zs.getLeistungID().add(identifikator(rawValue(lbId), SCHEME_AGENCY_ID));
        zs.getGebietID().add(gebiet(ars));

        ZustaendigkeitTransferObjekt transfer = new ZustaendigkeitTransferObjekt();
        transfer.setUebergeordnetesObjektID(identifikator("od-1", SCHEME_AGENCY_ID));
        transfer.getZustaendigkeit().add(zs);
        return ConvertedZustaendigkeit.of(new GenericZustaendigkeitTransferObjekt(transfer));
    }

    private static String rawValue(String composedId) {
        return composedId.substring(composedId.lastIndexOf('.') + 1);
    }

    private static Identifikator identifikator(String value, String schemeAgencyId) {
        Identifikator id = new Identifikator();
        id.setValue(value);
        id.setSchemeAgencyID(schemeAgencyId);
        return id;
    }

    private static Identifikator gebiet(String ars) {
        Identifikator id = new Identifikator();
        id.setValue(ars);
        return id;
    }

    private static Identifikator zsId(String schemeId) {
        Identifikator id = new Identifikator();
        id.setValue("zs-" + schemeId);
        id.setSchemeID(schemeId);
        id.setSchemeAgencyID(SCHEME_AGENCY_ID);
        return id;
    }
}
