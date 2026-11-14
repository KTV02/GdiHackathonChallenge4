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
import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role.Source;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-Tests für die Organisations-Delegations-Logik in {@link OdRelationAssembler}.
 *
 * <p>Prüft, dass der Assembler die Organisations-Zuordnungen korrekt an
 * {@link RoleExtractor} und {@link OeStammdatenEnricher} delegiert und die
 * Ergebnisse in die {@link OnlinedienstRelationen} einfügt.</p>
 */
@ExtendWith(MockitoExtension.class)
class OdRelationAssemblerOrganisationsTest {

    private static final String SCHEME_AGENCY_ID = "de.bund";

    @Mock
    private RoleExtractor roleExtractor;

    @Mock
    private OeStammdatenEnricher oeStammdatenEnricher;

    @InjectMocks
    private OdRelationAssembler assembler;

    // -------------------------------------------------------------------------
    // createRelationEntities – Organisations werden korrekt befüllt
    // -------------------------------------------------------------------------

    /**
     * {@code createRelationEntities} überträgt das Ergebnis von
     * {@link RoleExtractor#extractRoles} und {@link OeStammdatenEnricher#enrich}
     * in das {@code organisationRelations}-Feld der erzeugten {@link OnlinedienstRelationen}.
     */
    @Test
    void createRelationEntities_organisationRelationsFromPipeline() {
        // given
        OrganisationseinheitRollenZuordnung oeZuordnung = new OrganisationseinheitRollenZuordnung(
            "de.bund.OE.oe-1",
            "Testbehörde",
            List.of(),
            List.of(
                new Role(
                    RoleType.COMPETENT_OFFICE,
                    Source.ZS_OE,
                    "de.bund.LB.lb-1",
                    Set.of(),
                    Set.of("091620000000")
                )
            )
        );
        List<OrganisationseinheitRollenZuordnung> enrichedOrganisations = List.of(oeZuordnung);

        when(roleExtractor.extractRoles(any(), any(), any())).thenReturn(Map.of());
        when(oeStammdatenEnricher.enrich(any(), any())).thenReturn(enrichedOrganisations);

        GenericOnlinedienst od = new GenericOnlinedienst(buildXod("od-1"));
        TransactionMetadata metadata = new TransactionMetadata(
            "uuid-1",
            "tx-1",
            "1",
            SCHEME_AGENCY_ID,
            "Test-System"
        );
        OnlinedienstMitMetadaten odMitMetadaten = new OnlinedienstMitMetadaten(metadata, od);

        BatchRelationContext context = new BatchRelationContext(
            new OeStammdatenLookup(Map.of()),
            new OdZustaendigkeitenLookup(Map.of()),
            new OeZustaendigkeitenLookup(Map.of()),
            new LeikaLookup(Map.of())
        );

        // when
        List<OnlinedienstRelationen> result = assembler.createRelationEntities(
            List.of(odMitMetadaten),
            context
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganisationRelations()).isSameAs(enrichedOrganisations);
    }

    /**
     * Wenn die Pipeline eine leere Organisations-Liste zurückgibt, wird diese ebenfalls
     * als leere Liste in den Relationen gesetzt.
     */
    @Test
    void createRelationEntities_emptyOrganisations_storedAsEmptyList() {
        // given
        when(roleExtractor.extractRoles(any(), any(), any())).thenReturn(Map.of());
        when(oeStammdatenEnricher.enrich(any(), any())).thenReturn(List.of());

        GenericOnlinedienst od = new GenericOnlinedienst(buildXod("od-2"));
        TransactionMetadata metadata = new TransactionMetadata(
            "uuid-2",
            "tx-2",
            "2",
            SCHEME_AGENCY_ID,
            "Test-System"
        );
        OnlinedienstMitMetadaten odMitMetadaten = new OnlinedienstMitMetadaten(metadata, od);

        BatchRelationContext context = new BatchRelationContext(
            new OeStammdatenLookup(Map.of()),
            new OdZustaendigkeitenLookup(Map.of()),
            new OeZustaendigkeitenLookup(Map.of()),
            new LeikaLookup(Map.of())
        );

        // when
        List<OnlinedienstRelationen> result = assembler.createRelationEntities(
            List.of(odMitMetadaten),
            context
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganisationRelations()).isEmpty();
    }

    /**
     * Bei mehreren Onlinediensten im Batch wird für jeden OD eine separate Relation erzeugt.
     */
    @Test
    void createRelationEntities_multiplOds_oneRelationEach() {
        // given
        when(roleExtractor.extractRoles(any(), any(), any())).thenReturn(Map.of());
        when(oeStammdatenEnricher.enrich(any(), any())).thenReturn(List.of());

        GenericOnlinedienst od1 = new GenericOnlinedienst(buildXod("od-multi-1"));
        GenericOnlinedienst od2 = new GenericOnlinedienst(buildXod("od-multi-2"));
        TransactionMetadata meta1 = new TransactionMetadata(
            "uuid-m1",
            "tx-m1",
            "1",
            SCHEME_AGENCY_ID,
            "System"
        );
        TransactionMetadata meta2 = new TransactionMetadata(
            "uuid-m2",
            "tx-m2",
            "2",
            SCHEME_AGENCY_ID,
            "System"
        );

        BatchRelationContext context = new BatchRelationContext(
            new OeStammdatenLookup(Map.of()),
            new OdZustaendigkeitenLookup(Map.of()),
            new OeZustaendigkeitenLookup(Map.of()),
            new LeikaLookup(Map.of())
        );

        // when
        List<OnlinedienstRelationen> result = assembler.createRelationEntities(
            List.of(
                new OnlinedienstMitMetadaten(meta1, od1),
                new OnlinedienstMitMetadaten(meta2, od2)
            ),
            context
        );

        // then
        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------

    private static OnlinedienstErweitert buildXod(String odValue) {
        OnlinedienstErweitert xod = new OnlinedienstErweitert();
        Identifikator id = new Identifikator();
        id.setValue(odValue);
        id.setSchemeAgencyID(SCHEME_AGENCY_ID);
        xod.setId(id);
        return xod;
    }
}
