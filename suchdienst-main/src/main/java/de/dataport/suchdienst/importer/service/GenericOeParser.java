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
package de.dataport.suchdienst.importer.service;

import de.dataport.suchdienst.importer.model.GenericOrganisationseinheit;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.Bundeslaender;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GenericOeParser {

    public List<OrganisationseinheitEntity> parseOes(
        Stream<GenericOrganisationseinheit> unparsedOes,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        var mdcMap = MdcTemplate.currentContext();
        return unparsedOes
            .parallel()
            .map(xoe ->
                MdcTemplate.callWithContext(mdcMap, () -> {
                    try (var _ = MdcTemplate.scoped(MdcKeys.OEID, xoe.getId())) {
                        return parseOrganisationseinheitElastic(xoe, metaData, lastUpdate);
                    }
                })
            )
            .toList();
    }

    private OrganisationseinheitEntity parseOrganisationseinheitElastic(
        GenericOrganisationseinheit xoe,
        TransactionMetadata metaData,
        Instant lastUpdate
    ) {
        log.debug("Parse Organisationseinheit mit der ID {}.", xoe.getId());

        return switch (xoe.getVersion()) {
            case XZUFI22 -> {
                OrganisationseinheitEntity22 oe = new OrganisationseinheitEntity22(lastUpdate);
                oe.setJzufi(xoe.getXZuFi22());
                oe.setMetaData(metaData);
                oe.setId(xoe.getId());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    oe.getArsFederalState()::add
                );
                oe.setValidityPeriods(
                    XZuFiUtil.getValidityPeriods(xoe.getXZuFi22().getGueltigkeit())
                );
                yield oe;
            }
            case XZUFI23 -> {
                OrganisationseinheitEntity23 oe = new OrganisationseinheitEntity23(lastUpdate);
                oe.setJzufi(xoe.getXZuFi23());
                oe.setMetaData(metaData);
                oe.setId(xoe.getId());
                Bundeslaender.getArsBySchemeAgencyId(metaData.schemeAgencyId()).ifPresent(
                    oe.getArsFederalState()::add
                );
                oe.setValidityPeriods(
                    de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getValidityPeriods(
                        xoe.getXZuFi23().getGueltigkeit()
                    )
                );
                yield oe;
            }
        };
    }
}
