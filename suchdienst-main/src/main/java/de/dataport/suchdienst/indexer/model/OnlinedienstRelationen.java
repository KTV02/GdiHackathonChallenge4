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
package de.dataport.suchdienst.indexer.model;

import de.dataport.suchdienst.importer.model.AddressType;
import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.mapping.LinkDynamic;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OnlinedienstRelationen implements Identifiable {

    @EqualsAndHashCode.Include
    private String id;

    private TransactionMetadata metaData;
    private Instant lastUpdate;
    private Instant externalLastUpdate;

    private String name;
    private String description;
    private List<LinkDynamic> links = new ArrayList<>();
    private List<LeistungRegionZuordnung> serviceRelations = new ArrayList<>();
    private List<OrganisationseinheitRollenZuordnung> organisationRelations = new ArrayList<>();

    public record LeistungRegionZuordnung(String lbId, Set<String> leikakeys, Set<String> ars) {
        public LeistungRegionZuordnung {
            leikakeys = leikakeys != null ? leikakeys : Set.of();
            ars = ars != null ? ars : Set.of();
        }
    }

    public record OrganisationseinheitRollenZuordnung(
        String oeId,
        String name,
        List<Address> addresses,
        List<Role> rollen
    ) {
        public OrganisationseinheitRollenZuordnung {
            addresses = addresses != null ? addresses : List.of();
            rollen = rollen != null ? rollen : List.of();
        }

        public record Role(
            RoleType role,
            Source source,
            String lbId,
            Set<String> leikakeys,
            Set<String> ars
        ) {
            public Role {
                leikakeys = leikakeys != null ? leikakeys : Set.of();
                ars = ars != null ? ars : Set.of();
            }

            public enum Source {
                ZS_OE,
                OD,
            }
        }

        public record Address(
            AddressType type,
            String city,
            String zip,
            String street,
            String houseNumber,
            String postbox
        ) {}
    }
}
