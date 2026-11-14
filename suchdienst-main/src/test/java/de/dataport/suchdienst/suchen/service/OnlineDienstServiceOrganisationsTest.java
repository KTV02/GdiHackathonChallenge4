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
package de.dataport.suchdienst.suchen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.AliasNames;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Address;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.AddressType;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationAddressResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResponse;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.OrganisationRoleResult;
import de.dataport.suchdienst.suchen.model.out.onlinedienst.powerapi.RoleType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class OnlineDienstServiceOrganisationsTest {

    private static final String OD_ID = "de.bund.OD.test";

    @Mock
    private ElasticReadConnector readConnector;

    @Mock
    private MessageSource messageSource;

    @Mock
    private PvogRequestContext context;

    @InjectMocks
    private OnlineDienstService service;

    // --- not found ---

    @Test
    void unknownOdId_throwsNotFound() {
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.empty());
        when(messageSource.getMessage("api.404", null, Locale.GERMAN)).thenReturn("Nicht gefunden");

        assertThatThrownBy(() -> service.getOrganisationsByOdId(OD_ID))
            .isInstanceOf(SuchenException.class)
            .satisfies(e ->
                assertThat(((SuchenException) e).getCode()).isEqualTo(ErrorCodeEnum.NOT_FOUND)
            );
    }

    // --- empty organisations ---

    @Test
    void emptyOrganisations_returnsEmptyList() {
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of())));

        OrganisationResponse response = service.getOrganisationsByOdId(OD_ID);

        assertThat(response.organisations()).isEmpty();
        assertThat(response.externalLastUpdate()).isNull();
    }

    // --- mapping of organisations ---

    @Test
    void withOrganisations_mapsOeIdAndName() {
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Musteramt",
            List.of(),
            List.of()
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationResponse response = service.getOrganisationsByOdId(OD_ID);

        assertThat(response.organisations()).hasSize(1);
        assertThat(response.organisations().get(0).oeId()).isEqualTo("oe-1");
        assertThat(response.organisations().get(0).name()).isEqualTo("Musteramt");
    }

    @Test
    void multipleOrganisations_allIncludedInOrder() {
        var org1 = new OrganisationseinheitRollenZuordnung("oe-1", "Amt A", List.of(), List.of());
        var org2 = new OrganisationseinheitRollenZuordnung("oe-2", "Amt B", List.of(), List.of());
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org1, org2))));

        List<OrganisationResult> organisations = service
            .getOrganisationsByOdId(OD_ID)
            .organisations();

        assertThat(organisations)
            .extracting(OrganisationResult::oeId)
            .containsExactly("oe-1", "oe-2");
    }

    // --- mapping of addresses ---

    @Test
    void withOrganisations_mapsAddress() {
        var address = new Address(
            de.dataport.suchdienst.importer.model.AddressType.ADDRESS,
            "München",
            "80335",
            "Prielmayerstr.",
            "7",
            null
        );
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Amt",
            List.of(address),
            List.of()
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationAddressResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .addresses()
            .get(0);

        assertThat(result.type()).isEqualTo(AddressType.ADDRESS);
        assertThat(result.city()).isEqualTo("München");
        assertThat(result.zip()).isEqualTo("80335");
        assertThat(result.street()).isEqualTo("Prielmayerstr.");
        assertThat(result.houseNumber()).isEqualTo("7");
        assertThat(result.postbox()).isNull();
    }

    @Test
    void address_houseNumberCanContainLetters() {
        var address = new Address(
            de.dataport.suchdienst.importer.model.AddressType.ADDRESS,
            "Berlin",
            "10115",
            "Unter den Linden",
            "17a",
            null
        );
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Amt",
            List.of(address),
            List.of()
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationAddressResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .addresses()
            .get(0);

        assertThat(result.houseNumber()).isEqualTo("17a");
    }

    @Test
    void address_postboxMapped_whenPresent() {
        var address = new Address(
            de.dataport.suchdienst.importer.model.AddressType.POSTAL_ADDRESS,
            "Hamburg",
            "20000",
            null,
            null,
            "123456"
        );
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Amt",
            List.of(address),
            List.of()
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationAddressResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .addresses()
            .get(0);

        assertThat(result.postbox()).isEqualTo("123456");
        assertThat(result.street()).isNull();
        assertThat(result.houseNumber()).isNull();
    }

    // --- mapping of roles ---

    @Test
    void withOrganisations_mapsRole() {
        var role = new Role(
            de.dataport.suchdienst.importer.model.RoleType.COMPETENT_OFFICE,
            Role.Source.ZS_OE,
            "lb-1",
            Set.of("99014001002000"),
            Set.of("091620000000")
        );
        var org = new OrganisationseinheitRollenZuordnung("oe-1", "Amt", List.of(), List.of(role));
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationRoleResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .roles()
            .get(0);

        assertThat(result.role()).isEqualTo(RoleType.COMPETENT_OFFICE);
        assertThat(result.lbId()).isEqualTo("lb-1");
        assertThat(result.leikakeys()).containsExactly("99014001002000");
        assertThat(result.ars()).containsExactly("091620000000");
    }

    @Test
    void role_multipleArs_allIncluded() {
        var role = new Role(
            de.dataport.suchdienst.importer.model.RoleType.PROFESSIONAL_POINT_OF_CONTACT,
            Role.Source.OD,
            "lb-2",
            Set.of(),
            Set.of("010000000000", "091620000000")
        );
        var org = new OrganisationseinheitRollenZuordnung("oe-1", "Amt", List.of(), List.of(role));
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationRoleResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .roles()
            .get(0);

        assertThat(result.ars()).containsExactlyInAnyOrder("010000000000", "091620000000");
        assertThat(result.leikakeys()).isEmpty();
    }

    @Test
    void role_multipleLeikakeys_allIncluded() {
        var role = new Role(
            de.dataport.suchdienst.importer.model.RoleType.COMPETENT_OFFICE,
            Role.Source.ZS_OE,
            "lb-1",
            Set.of("99014001002000", "99014001003000"),
            Set.of("091620000000")
        );
        var org = new OrganisationseinheitRollenZuordnung("oe-1", "Amt", List.of(), List.of(role));
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationRoleResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .roles()
            .get(0);

        assertThat(result.leikakeys()).containsExactlyInAnyOrder(
            "99014001002000",
            "99014001003000"
        );
    }

    @Test
    void organisation_multipleRoles_allPreservedInOrder() {
        var role1 = new Role(
            de.dataport.suchdienst.importer.model.RoleType.COMPETENT_OFFICE,
            Role.Source.ZS_OE,
            "lb-1",
            Set.of(),
            Set.of("01")
        );
        var role2 = new Role(
            de.dataport.suchdienst.importer.model.RoleType.PROFESSIONAL_POINT_OF_CONTACT,
            Role.Source.OD,
            "lb-1",
            Set.of(),
            Set.of("01")
        );
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Amt",
            List.of(),
            List.of(role1, role2)
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        List<OrganisationRoleResult> roles = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .roles();

        assertThat(roles)
            .extracting(OrganisationRoleResult::role)
            .containsExactly(RoleType.COMPETENT_OFFICE, RoleType.PROFESSIONAL_POINT_OF_CONTACT);
    }

    // --- AddressType/RoleType Fallback auf UNBESTIMMT (via Service) ---

    @Test
    void address_unknownIndexerType_mapsToUnbestimmt() {
        // UNBESTIMMT im Indexer-Modell muss auf UNBESTIMMT im Out-Modell gemappt werden
        var address = new Address(
            de.dataport.suchdienst.importer.model.AddressType.UNBESTIMMT,
            "Berlin",
            "10115",
            null,
            null,
            null
        );
        var org = new OrganisationseinheitRollenZuordnung(
            "oe-1",
            "Amt",
            List.of(address),
            List.of()
        );
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationAddressResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .addresses()
            .get(0);

        assertThat(result.type()).isEqualTo(AddressType.UNBESTIMMT);
    }

    @Test
    void role_unknownIndexerType_mapsToUnbestimmt() {
        // UNBESTIMMT im Indexer-Modell muss auf UNBESTIMMT im Out-Modell gemappt werden
        var role = new Role(
            de.dataport.suchdienst.importer.model.RoleType.UNKNOWN,
            Role.Source.ZS_OE,
            "lb-1",
            Set.of(),
            Set.of("01")
        );
        var org = new OrganisationseinheitRollenZuordnung("oe-1", "Amt", List.of(), List.of(role));
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entityWithOrganisations(List.of(org))));

        OrganisationRoleResult result = service
            .getOrganisationsByOdId(OD_ID)
            .organisations()
            .get(0)
            .roles()
            .get(0);

        assertThat(result.role()).isEqualTo(RoleType.UNKNOWN);
    }

    // --- externalLastUpdate ---

    @Test
    void externalLastUpdate_passedThrough() {
        Instant timestamp = Instant.parse("2026-01-15T10:00:00Z");
        var org = new OrganisationseinheitRollenZuordnung("oe-1", "Amt", List.of(), List.of());
        var entity = entityWithOrganisations(List.of(org));
        entity.setExternalLastUpdate(timestamp);
        when(
            readConnector.getById(
                AliasNames.ONLINEDIENSTE_RELATIONEN,
                OD_ID,
                OnlinedienstRelationen.class
            )
        ).thenReturn(Optional.of(entity));

        OrganisationResponse response = service.getOrganisationsByOdId(OD_ID);

        assertThat(response.externalLastUpdate()).isEqualTo(timestamp);
    }

    // --- helper ---

    private static OnlinedienstRelationen entityWithOrganisations(
        List<OrganisationseinheitRollenZuordnung> orgs
    ) {
        var entity = new OnlinedienstRelationen();
        entity.setId(OD_ID);
        entity.setOrganisationRelations(new ArrayList<>(orgs));
        return entity;
    }
}
