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

import de.dataport.suchdienst.importer.model.RoleType;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role;
import de.dataport.suchdienst.indexer.model.OnlinedienstRelationen.OrganisationseinheitRollenZuordnung.Role.Source;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.xoev.xzufi.v2_3_1.CodeZustaendigkeitsrolle;
import de.xoev.xzufi.v2_3_1.Identifikator;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitOnlinedienstMitRolle;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Extrahiert und aggregiert Rollen von Organisationseinheiten für einen Onlinedienst.
 *
 * <p>Kombiniert zwei Rollenquellen:
 * <ul>
 *   <li>{@link Source#ZS_OE} — Rollen aus OE-Zuständigkeiten, ermittelt über ARS-Hierarchie-Matching</li>
 *   <li>{@link Source#OD} — Rollen aus direkten OE-Referenzen in XZuFi-2.3-OD-Zuständigkeiten</li>
 * </ul>
 * Identische Rollen (gleiche OE, Quelle, LB-ID und Rollentyp) werden zusammengeführt,
 * wobei deren ARS-Werte vereinigt werden.</p>
 */
@Service
class RoleExtractor {

    private final ArsMatchingService arsMatchingService;

    RoleExtractor(ArsMatchingService arsMatchingService) {
        this.arsMatchingService = arsMatchingService;
    }

    /**
     * Extrahiert OE-Rollen aus ARS-Hierarchie-Abgleich (Source=ZS_OE) und direkten
     * OE-Referenzen in OD-Zuständigkeiten (Source=OD), gruppiert nach OE-ID.
     *
     * @param odZustaendigkeitenByLbId OD-Zuständigkeiten gruppiert nach LB-ID
     * @param oeZustaendigkeiten       OE-Zuständigkeiten des Batches
     * @param leikas                   Leika-Schlüssel des Batches
     * @return Rollen gruppiert nach OE-ID
     */
    Map<String, List<Role>> extractRoles(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        OeZustaendigkeitenLookup oeZustaendigkeiten,
        LeikaLookup leikas
    ) {
        List<RoleContributions.RoleContribution> contributions = collectRoleContributions(
            odZustaendigkeitenByLbId,
            oeZustaendigkeiten
        );
        return RoleContributions.toRolesByOeId(contributions, leikas);
    }

    private List<RoleContributions.RoleContribution> collectRoleContributions(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {
        return Stream.concat(
            collectArsMatchedContributions(odZustaendigkeitenByLbId, oeZustaendigkeiten).stream(),
            collectDirectOeRoleContributions(odZustaendigkeitenByLbId).stream()
        ).toList();
    }

    /**
     * Aggregiert OE-Rollen aus dem ARS-Hierarchie-Abgleich zwischen OD- und OE-Zuständigkeiten.
     *
     * <p>OE-Zuständigkeiten sind pro ARS denormalisiert, daher werden identische
     * Rollen hier zusammengeführt.</p>
     */
    private List<RoleContributions.RoleContribution> collectArsMatchedContributions(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {
        List<RoleContributions.RoleContribution> contributions = new ArrayList<>();
        arsMatchingService.forEachArsMatch(
            odZustaendigkeitenByLbId,
            oeZustaendigkeiten,
            (lbId, oeZs) -> contributions.addAll(extractRolesFromMatchedOe(lbId, oeZs))
        );
        return List.copyOf(contributions);
    }

    /**
     * Ergänzt Rollen aus direkten OE-Referenzen in XZuFi-2.3-OD-Zuständigkeiten.
     */
    private List<RoleContributions.RoleContribution> collectDirectOeRoleContributions(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId
    ) {
        return odZustaendigkeitenByLbId
            .values()
            .stream()
            .flatMap(List::stream)
            // Nur original XZuFi 2.3 OD-Zuständigkeiten haben direkte OE-Referenzen; konvertierte 2.2-Zuständigkeiten nicht
            .filter(zs -> zs.getOriginalVersion() == XZuFiVersion.XZUFI23)
            .map(arsMatchingService::extractZsOd)
            .flatMap(Optional::stream)
            .flatMap(zs -> extractDirectRoleContributions(zs).stream())
            .toList();
    }

    /**
     * Verarbeitet eine einzelne {@link ZustaendigkeitOnlinedienst} zu direkten Rollenbeiträgen.
     */
    private List<RoleContributions.RoleContribution> extractDirectRoleContributions(
        ZustaendigkeitOnlinedienst zs
    ) {
        List<String> lbIds = requireLeistungIds(zs);

        Set<String> directArs = zs
            .getGebietID()
            .stream()
            .map(Identifikator::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());

        return zs
            .getOrganisationseinheit()
            .stream()
            .flatMap(oeMitRolle ->
                createDirectRoleContributions(oeMitRolle, lbIds, directArs).stream()
            )
            .toList();
    }

    private static List<String> requireLeistungIds(ZustaendigkeitOnlinedienst zs) {
        List<String> lbIds = zs
            .getLeistungID()
            .stream()
            .map(id -> XZuFiUtil.getIdentificatorString(id, IdentificationType.LB))
            .toList();
        if (lbIds.isEmpty()) {
            throw new IllegalStateException(
                "ZustaendigkeitOnlinedienst '%s' hat keine Leistungs-ID".formatted(
                    resolveZustaendigkeitId(zs)
                )
            );
        }
        return lbIds;
    }

    private static String resolveZustaendigkeitId(ZustaendigkeitOnlinedienst zs) {
        if (zs.getId() == null || zs.getId().getValue() == null) {
            return "<unbekannt>";
        }
        return zs.getId().getValue();
    }

    private static List<RoleContributions.RoleContribution> extractRolesFromMatchedOe(
        String lbId,
        ConvertedZustaendigkeit oeZs
    ) {
        String oeId = oeZs.getParentId();
        String oeArs = oeZs.getArs();
        List<CodeZustaendigkeitsrolle> rollen = extractOeRoles(oeZs);
        if (rollen.isEmpty()) {
            return List.of(
                new RoleContributions.RoleContribution(
                    oeId,
                    Source.ZS_OE,
                    lbId,
                    RoleType.UNKNOWN,
                    Collections.singleton(oeArs)
                )
            );
        }

        return rollen
            .stream()
            .map(rolle ->
                new RoleContributions.RoleContribution(
                    oeId,
                    Source.ZS_OE,
                    lbId,
                    RoleType.fromCode(rolle.getListURI(), rolle.getCode()),
                    Collections.singleton(oeArs)
                )
            )
            .toList();
    }

    private static List<RoleContributions.RoleContribution> createDirectRoleContributions(
        OrganisationseinheitOnlinedienstMitRolle oeMitRolle,
        List<String> lbContexts,
        Set<String> directArs
    ) {
        Identifikator oeIdentifikator = oeMitRolle.getOrganisationseinheitID();
        if (oeIdentifikator == null) {
            return List.of();
        }

        String oeId = XZuFiUtil.getIdentificatorString(oeIdentifikator, IdentificationType.OE);
        return oeMitRolle
            .getRolle()
            .stream()
            .flatMap(rolle ->
                lbContexts
                    .stream()
                    .map(lbId ->
                        new RoleContributions.RoleContribution(
                            oeId,
                            Source.OD,
                            lbId,
                            RoleType.fromCode(rolle.getListURI(), rolle.getCode()),
                            directArs
                        )
                    )
            )
            .toList();
    }

    /**
     * Extrahiert die OE-Rollen aus einer vorkonvertierten OE-Zuständigkeit.
     */
    private static List<CodeZustaendigkeitsrolle> extractOeRoles(ConvertedZustaendigkeit oeZs) {
        var xzufi23 = oeZs.getXzufi23();
        if (xzufi23 == null || xzufi23.getZustaendigkeit().isEmpty()) {
            return List.of();
        }
        var first = xzufi23.getZustaendigkeit().getFirst();
        if (first instanceof ZustaendigkeitOrganisationseinheit zsOe) {
            return zsOe.getRolle();
        }
        return List.of();
    }
}
