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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity23;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity23;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity23;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.Leistung;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OdRelationDataLoaderTest {

    private static final Instant NOW = Instant.parse("2026-03-07T10:15:30Z");
    private static final String SCHEME_AGENCY_ID = "de.bund";
    private static final String OD_ID = SCHEME_AGENCY_ID + ".OD.od-1";
    private static final String OE_ID = SCHEME_AGENCY_ID + ".OE.oe-1";
    private static final String LB_ID = SCHEME_AGENCY_ID + ".LB.lb-1";
    private static final String LEIKA_ID = "99001001000000";
    private static final String ARS = "091620000000";

    @Mock
    private ElasticReadConnector readConnector;

    @Mock
    private ArsMatchingService arsMatchingService;

    @InjectMocks
    private OdRelationDataLoader dataLoader;

    @Test
    void loadBatchReturnsNullWhenNoOnlineServicesExist() {
        when(readConnector.fetchOnlinedienstEntities(SCHEME_AGENCY_ID, 50, null)).thenReturn(
            List.of()
        );

        OdRelationDataLoader.BatchData result = dataLoader.loadBatch(SCHEME_AGENCY_ID, null, 50);

        assertThat(result).isNull();
        verify(readConnector).fetchOnlinedienstEntities(SCHEME_AGENCY_ID, 50, null);
        verifyNoMoreInteractions(readConnector);
    }

    @Test
    void loadBatchBuildsLookupsAndOnlyLoadsNeededOrganisationseinheiten() {
        OnlinedienstEntity23 odEntity = onlinedienstEntity(OD_ID);
        ZustaendigkeitEntity23 odZustaendigkeit = odZustaendigkeitEntity(OD_ID, LB_ID, ARS);
        ZustaendigkeitEntity23 oeZustaendigkeit = oeZustaendigkeitEntity(OE_ID, LB_ID, ARS);
        LeistungEntity23 leistungEntity = leistungEntity(LB_ID, LEIKA_ID);
        OrganisationseinheitEntity23 organisationseinheitEntity = organisationseinheitEntity(OE_ID);

        when(readConnector.fetchOnlinedienstEntities(SCHEME_AGENCY_ID, 50, null)).thenReturn(
            List.of(odEntity)
        );
        when(readConnector.searchRelations(null, List.of(OD_ID), null, List.of())).thenReturn(
            List.of(odZustaendigkeit)
        );
        when(
            readConnector.getByIdsWithRetry(
                IndexNames.LEISTUNGSBESCHREIBUNG,
                Set.of(LB_ID),
                List.of("id", "leikaId"),
                List.of(),
                LeistungEntity.class
            )
        ).thenReturn(List.of(leistungEntity));
        when(readConnector.searchOeZustaendigkeitenByLbIds(Set.of(LB_ID))).thenReturn(
            List.of(oeZustaendigkeit)
        );
        when(arsMatchingService.collectNeededOeIds(any(), any())).thenReturn(Set.of(OE_ID));
        when(
            readConnector.getByIdsWithRetry(
                IndexNames.ORGANISATIONSEINHEITEN,
                Set.of(OE_ID),
                List.of(),
                List.of(),
                OrganisationseinheitEntity.class
            )
        ).thenReturn(List.of(organisationseinheitEntity));

        OdRelationDataLoader.BatchData result = dataLoader.loadBatch(SCHEME_AGENCY_ID, null, 50);

        assertThat(result).isNotNull();
        assertThat(result.lastId()).isEqualTo(OD_ID);
        assertThat(result.odsById()).containsKey(OD_ID);
        assertThat(result.context().odZustaendigkeiten().forOnlinedienst(OD_ID)).containsKey(LB_ID);
        assertThat(result.context().leikas().forLeistung(LB_ID)).containsExactly(LEIKA_ID);
        assertThat(result.context().oeZustaendigkeiten().forLeistung(LB_ID)).hasSize(1);
        assertThat(result.context().oeStammdaten().get(OE_ID)).isNotNull();
        verify(readConnector).fetchOnlinedienstEntities(SCHEME_AGENCY_ID, 50, null);
        verify(readConnector).searchRelations(null, List.of(OD_ID), null, List.of());
        verify(readConnector).searchOeZustaendigkeitenByLbIds(Set.of(LB_ID));
        verify(readConnector).getByIdsWithRetry(
            IndexNames.LEISTUNGSBESCHREIBUNG,
            Set.of(LB_ID),
            List.of("id", "leikaId"),
            List.of(),
            LeistungEntity.class
        );
        verify(readConnector).getByIdsWithRetry(
            IndexNames.ORGANISATIONSEINHEITEN,
            Set.of(OE_ID),
            List.of(),
            List.of(),
            OrganisationseinheitEntity.class
        );
        verify(arsMatchingService).collectNeededOeIds(
            result.context().odZustaendigkeiten(),
            result.context().oeZustaendigkeiten()
        );
    }

    private static OnlinedienstEntity23 onlinedienstEntity(String odId) {
        OnlinedienstErweitert xod = new OnlinedienstErweitert();
        xod.setId(identifikator("od-1", SCHEME_AGENCY_ID));

        OnlinedienstEntity23 entity = new OnlinedienstEntity23(NOW);
        entity.setId(odId);
        entity.setMetaData(
            new TransactionMetadata("uuid-1", "tx-1", "1", SCHEME_AGENCY_ID, "System")
        );
        entity.setJzufi(xod);
        return entity;
    }

    private static ZustaendigkeitEntity23 odZustaendigkeitEntity(
        String parentId,
        String lbId,
        String ars
    ) {
        ZustaendigkeitOnlinedienst zs = new ZustaendigkeitOnlinedienst();
        zs.setId(zsId("ZustaendigkeitOnlinedienst"));
        zs.getLeistungID().add(identifikator("lb-1", SCHEME_AGENCY_ID));
        zs.getGebietID().add(gebiet(ars));

        ZustaendigkeitTransferObjekt transfer = new ZustaendigkeitTransferObjekt();
        transfer.setUebergeordnetesObjektID(identifikator("od-1", SCHEME_AGENCY_ID));
        transfer.getZustaendigkeit().add(zs);

        ZustaendigkeitEntity23 entity = new ZustaendigkeitEntity23(NOW);
        entity.setId("zs-od-1");
        entity.setParentId(parentId);
        entity.setLbId(lbId);
        entity.setArs(ars);
        entity.setTyp(RelationTyp.ONLINESERVICE);
        entity.setJzufi(transfer);
        return entity;
    }

    private static ZustaendigkeitEntity23 oeZustaendigkeitEntity(
        String parentId,
        String lbId,
        String ars
    ) {
        ZustaendigkeitOrganisationseinheit zs = new ZustaendigkeitOrganisationseinheit();
        zs.setId(zsId("ZustaendigkeitOrganisationseinheit"));
        zs.getLeistungID().add(identifikator("lb-1", SCHEME_AGENCY_ID));
        zs.getGebietID().add(gebiet(ars));

        ZustaendigkeitTransferObjekt transfer = new ZustaendigkeitTransferObjekt();
        transfer.setUebergeordnetesObjektID(identifikator("oe-1", SCHEME_AGENCY_ID));
        transfer.getZustaendigkeit().add(zs);

        ZustaendigkeitEntity23 entity = new ZustaendigkeitEntity23(NOW);
        entity.setId("zs-oe-1");
        entity.setParentId(parentId);
        entity.setLbId(lbId);
        entity.setArs(ars);
        entity.setTyp(RelationTyp.ORGANISATION);
        entity.setJzufi(transfer);
        return entity;
    }

    private static LeistungEntity23 leistungEntity(String lbId, String leikaId) {
        Leistung leistung = new Leistung();
        leistung.setId(identifikator("lb-1", SCHEME_AGENCY_ID));

        LeistungEntity23 entity = new LeistungEntity23(NOW);
        entity.setId(lbId);
        entity.setLeikaId(Set.of(leikaId));
        entity.setJzufi(leistung);
        return entity;
    }

    private static OrganisationseinheitEntity23 organisationseinheitEntity(String oeId) {
        OrganisationseinheitErweitert xoe = new OrganisationseinheitErweitert();
        xoe.setId(identifikator("oe-1", SCHEME_AGENCY_ID));

        OrganisationseinheitEntity23 entity = new OrganisationseinheitEntity23(NOW);
        entity.setId(oeId);
        entity.setJzufi(xoe);
        return entity;
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
