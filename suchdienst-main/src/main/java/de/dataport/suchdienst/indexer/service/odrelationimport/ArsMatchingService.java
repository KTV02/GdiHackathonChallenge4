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

import de.dataport.suchdienst.common.model.ARS;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.util.x23.XZuFiUtil;
import de.xoev.xzufi.v2_3_1.OrganisationseinheitOnlinedienstMitRolle;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Ermittelt Zuordnungen zwischen Onlinediensten und Organisationseinheiten anhand
 * des ARS-Hierarchie-Abgleichs und direkter XZuFi-2.3-Referenzen.
 *
 * <p>Zuständigkeiten des Onlinedienstes (OD-Zuständigkeiten) werden mit den
 * Zuständigkeiten der Organisationseinheiten (OE-Zuständigkeiten) über ihren
 * Amtlichen Regionalschlüssel (ARS) hierarchisch verglichen. Zusätzlich werden
 * direkte OE-Referenzen aus XZuFi-2.3-OD-Zuständigkeiten (Source=OD) extrahiert.</p>
 */
@Service
class ArsMatchingService {

    /**
     * Vereinigt OE-IDs aus ARS-Hierarchie-Abgleich (Source=ZS_OE) und direkten
     * XZuFi-2.3-Referenzen in OD-Zuständigkeiten (Source=OD). Wird vor dem Laden
     * der OE-Stammdaten aufgerufen, um den ES-Fetch zu begrenzen.
     *
     * @param odZustaendigkeiten OD-Zuständigkeiten gruppiert nach OD-ID und LB-ID
     * @param oeZustaendigkeiten OE-Zuständigkeiten gruppiert nach LB-ID
     * @return Menge der benötigten OE-IDs
     */
    Set<String> collectNeededOeIds(
        OdZustaendigkeitenLookup odZustaendigkeiten,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {
        Set<String> fromArsAbgleich = collectOeIdsViaArsHierarchie(
            odZustaendigkeiten,
            oeZustaendigkeiten
        );
        Set<String> fromDirektReferenzen = collectDirectOeIds(odZustaendigkeiten);

        Set<String> result = new HashSet<>(fromArsAbgleich);
        result.addAll(fromDirektReferenzen);
        return result;
    }

    /**
     * Ruft {@code matchConsumer} für jeden ARS-Hierarchie-Treffer zwischen OD- und
     * OE-Zuständigkeiten auf. Der Consumer erhält jeweils lbId und die gematchte OE-Zuständigkeit.
     *
     * @param odZustaendigkeitenByLbId OD-Zuständigkeiten gruppiert nach LB-ID
     * @param oeZustaendigkeiten       OE-Zuständigkeiten des Batches
     * @param matchConsumer            Callback pro Treffer: (lbId, oeZustaendigkeit)
     */
    void forEachArsMatch(
        Map<String, List<ConvertedZustaendigkeit>> odZustaendigkeitenByLbId,
        OeZustaendigkeitenLookup oeZustaendigkeiten,
        BiConsumer<String, ConvertedZustaendigkeit> matchConsumer
    ) {
        for (var entry : odZustaendigkeitenByLbId.entrySet()) {
            String lbId = entry.getKey();
            List<ConvertedZustaendigkeit> odZsList = entry.getValue();
            List<ConvertedZustaendigkeit> oeZsList = oeZustaendigkeiten.forLeistung(lbId);
            for (ConvertedZustaendigkeit odZs : odZsList) {
                if (odZs.getArs() == null) {
                    continue;
                }
                for (ConvertedZustaendigkeit oeZs : oeZsList) {
                    if (hasArsMatch(odZs, oeZs)) {
                        matchConsumer.accept(lbId, oeZs);
                    }
                }
            }
        }
    }

    /**
     * Extrahiert die {@link ZustaendigkeitOnlinedienst} aus einer OD-Zuständigkeit,
     * sofern das XZuFi-2.3.1-Objekt vorhanden ist und eine enthält.
     *
     * @param zs die Zuständigkeit
     * @return die enthaltene {@link ZustaendigkeitOnlinedienst}, oder leer
     */
    Optional<ZustaendigkeitOnlinedienst> extractZsOd(ConvertedZustaendigkeit zs) {
        var xzufi23 = zs.getXzufi23();
        if (xzufi23 == null || xzufi23.getZustaendigkeit().isEmpty()) {
            return Optional.empty();
        }
        var first = xzufi23.getZustaendigkeit().getFirst();
        if (first instanceof ZustaendigkeitOnlinedienst zsOd) {
            return Optional.of(zsOd);
        }
        return Optional.empty();
    }

    /**
     * Sammelt OE-IDs über den ARS-Hierarchie-Abgleich (Source=ZS_OE).
     */
    private Set<String> collectOeIdsViaArsHierarchie(
        OdZustaendigkeitenLookup odZustaendigkeiten,
        OeZustaendigkeitenLookup oeZustaendigkeiten
    ) {
        Set<String> ids = new HashSet<>();
        for (Map<
            String,
            List<ConvertedZustaendigkeit>
        > odZustaendigkeitenByLbId : odZustaendigkeiten
            .groupedByOnlineServiceIdAndLeistungId()
            .values()) {
            forEachArsMatch(odZustaendigkeitenByLbId, oeZustaendigkeiten, (lbId, oeZs) ->
                ids.add(oeZs.getParentId())
            );
        }
        return ids;
    }

    /**
     * Sammelt OE-IDs aus direkten XZuFi-2.3-Referenzen (Source=OD).
     */
    private Set<String> collectDirectOeIds(OdZustaendigkeitenLookup odZustaendigkeiten) {
        return odZustaendigkeiten
            .groupedByOnlineServiceIdAndLeistungId()
            .values()
            .stream()
            .flatMap(byLbId -> byLbId.values().stream())
            .flatMap(List::stream)
            // Nur original XZuFi 2.3 OD-Zuständigkeiten haben direkte OE-Referenzen; konvertierte 2.2-Zuständigkeiten nicht
            .filter(zs -> zs.getOriginalVersion() == XZuFiVersion.XZUFI23)
            .map(this::extractZsOd)
            .flatMap(Optional::stream)
            .flatMap(zs -> zs.getOrganisationseinheit().stream())
            .map(OrganisationseinheitOnlinedienstMitRolle::getOrganisationseinheitID)
            .filter(Objects::nonNull)
            .map(id -> XZuFiUtil.getIdentificatorString(id, IdentificationType.OE))
            .collect(Collectors.toSet());
    }

    /**
     * Prüft, ob eine OD-Zuständigkeit und eine OE-Zuständigkeit über ihre ARS-Werte
     * hierarchisch zusammenpassen.
     */
    private static boolean hasArsMatch(ConvertedZustaendigkeit odZs, ConvertedZustaendigkeit oeZs) {
        return oeZs.getParentId() != null && ARS.matchesHierarchy(odZs.getArs(), oeZs.getArs());
    }
}
