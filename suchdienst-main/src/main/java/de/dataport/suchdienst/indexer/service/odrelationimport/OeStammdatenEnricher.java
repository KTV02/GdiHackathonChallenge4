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

import de.dataport.suchdienst.importer.model.AddressType;
import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Address;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.dataport.suchdienst.util.LocaleUtils;
import de.xoev.xzufi.v2_3_1.Anschrift;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Reichert extrahierte OE-Rollen mit Stammdaten (Name, Anschriften) an und erzeugt
 * die finalen {@link OrganisationseinheitRollenZuordnung}-Objekte.
 *
 * <p>Sind keine Stammdaten zur OE-ID vorhanden — z.B. weil die OE noch nicht importiert
 * wurde — wird eine minimale Zuordnung ohne Name und Anschriften zurückgegeben.
 * Die Rollen bleiben dabei erhalten, damit keine Zuständigkeitsinformation verloren geht.</p>
 */
@Service
class OeStammdatenEnricher {

    /**
     * Erzeugt die finalen OE-Zuordnungen aus den extrahierten Rollen und den Stammdaten.
     *
     * @param rollenByOeId  Rollen gruppiert nach OE-ID (Ergebnis des {@link RoleExtractor})
     * @param oeStammdaten  OE-Stammdaten des Batches
     * @return Aufgelöste OE-Zuordnungen, alphabetisch nach OE-ID sortiert
     */
    List<OrganisationseinheitRollenZuordnung> enrich(
        Map<String, List<Role>> rollenByOeId,
        OeStammdatenLookup oeStammdaten
    ) {
        return rollenByOeId
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
            .map(entry -> toOeZuordnung(entry.getKey(), entry.getValue(), oeStammdaten))
            .toList();
    }

    /**
     * Erzeugt eine {@link OrganisationseinheitRollenZuordnung} für eine OE.
     */
    private OrganisationseinheitRollenZuordnung toOeZuordnung(
        String oeId,
        List<Role> rollen,
        OeStammdatenLookup oeStammdaten
    ) {
        GenericOrganisationseinheit oe = oeStammdaten.get(oeId);
        if (oe == null) {
            return new OrganisationseinheitRollenZuordnung(oeId, null, List.of(), rollen);
        }
        OrganisationseinheitErweitert xoe = oe.getOrConvertXZuFi23();
        String name = XZuFiUtil.getOrganisationName(
            xoe.getName(),
            LocaleUtils.DEFAULT_LOCALE
        ).orElse(null);
        return new OrganisationseinheitRollenZuordnung(
            oeId,
            name,
            mapOeAddresses(xoe.getAnschrift()),
            rollen
        );
    }

    /** Mappt XZuFi-Anschriften auf das leichtgewichtige {@link Address}-Modell. */
    private List<Address> mapOeAddresses(List<? extends Anschrift> anschriften) {
        if (anschriften == null || anschriften.isEmpty()) {
            return List.of();
        }
        return anschriften
            .stream()
            .filter(a -> a.getTyp() != null)
            .map(a ->
                new Address(
                    AddressType.fromCode(a.getTyp().getListURI(), a.getTyp().getCode()),
                    a.getOrt(),
                    a.getPostleitzahl(),
                    a.getStrasse(),
                    a.getHausnummer(),
                    a.getPostfach()
                )
            )
            .toList();
    }
}
