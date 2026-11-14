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

import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity23;
import de.dataport.suchdienst.importer.model.warteschlange.ImportRequest;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import de.dataport.suchdienst.indexer.service.CodelistService;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.LocaleUtils;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.xoev.xzufi.v2_2_0.CodeZustaendigkeitsrolle;
import de.xoev.xzufi.v2_2_0.Zustaendigkeit;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitOrganisationseinheit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenericZsParser {

    private final CodelistService codelistService;

    public GenericZsParser(CodelistService codelistService) {
        this.codelistService = codelistService;
    }

    public List<ZustaendigkeitEntity> parseRelations(
        Stream<GenericZustaendigkeitTransferObjekt> zustaendigkeiten,
        ImportRequest importRequest
    ) {
        var mdcMap = MdcTemplate.currentContext();
        return zustaendigkeiten
            .parallel()
            .map(zs ->
                MdcTemplate.callWithContext(mdcMap, () -> {
                    try (var _ = MdcTemplate.scoped(MdcKeys.ZSID, zs.getId())) {
                        return this.parseRelation(zs, importRequest);
                    }
                })
            )
            .toList();
    }

    private ZustaendigkeitEntity parseRelation(
        GenericZustaendigkeitTransferObjekt xzs,
        ImportRequest importRequest
    ) {
        log.debug("Parse Zuständigkeit mit der ID {}.", xzs.getId());
        return switch (xzs.getVersion()) {
            case XZUFI22 -> parseRelation22(xzs, importRequest);
            case XZUFI23 -> parseRelation23(xzs, importRequest);
        };
    }

    private ZustaendigkeitEntity23 parseRelation23(
        GenericZustaendigkeitTransferObjekt xzs,
        ImportRequest importRequest
    ) {
        ZustaendigkeitEntity23 relation = new ZustaendigkeitEntity23(
            importRequest.getRequestAcceptedAt()
        );
        relation.setParentId(xzs.getParentId());
        relation.setTyp(xzs.getParentTyp());
        relation.setMetaData(importRequest.getMetaData());
        relation.setArs(xzs.getGebietIds().getFirst());
        relation.setArsFederalState(StringUtils.substring(relation.getArs(), 0, 2));
        relation.setId(xzs.getId());
        relation.setLbId(xzs.getLbIds().getFirst());
        relation.setValidityPeriods(extractGueltigkeit(xzs));
        relation.setJzufi(xzs.getXZuFi23());

        if (xzs.getParentTyp() == RelationTyp.ORGANISATION) {
            var zsOe = (de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit) xzs
                .getXZuFi23()
                .getZustaendigkeit()
                .getFirst();
            var codeRolle = zsOe.getRolle().getFirst();

            if (codeRolle != null) {
                String uri =
                    codeRolle.getListURI() == null
                        ? XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE
                        : codeRolle.getListURI();
                String version =
                    codeRolle.getListVersionID() == null
                        ? XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE
                        : codeRolle.getListVersionID();
                CodelistEntry entry = codelistService.getCodelistEntry(
                    uri,
                    version,
                    codeRolle.getCode()
                );

                ResponsibilityRole role = new ResponsibilityRole();
                role.setCode(codeRolle.getCode());
                role.setName(entry.getText(Locale.GERMAN));
                role.setUri(uri);
                role.setVersion(version);
                LocaleUtils.SUPPORTED_LANGUAGES.forEach(lang -> {
                    if (!Locale.GERMAN.equals(lang)) {
                        role.getOtherLanguages().put(lang, entry.getText(lang));
                    }
                });
                relation.setResponsibility(role);
            }
        }
        return relation;
    }

    private ZustaendigkeitEntity22 parseRelation22(
        GenericZustaendigkeitTransferObjekt xzs,
        ImportRequest importRequest
    ) {
        ZustaendigkeitEntity22 relation = new ZustaendigkeitEntity22(
            importRequest.getRequestAcceptedAt()
        );
        relation.setParentId(xzs.getParentId());
        relation.setTyp(xzs.getParentTyp());
        relation.setMetaData(importRequest.getMetaData());
        relation.setArs(xzs.getGebietIds().getFirst());
        relation.setArsFederalState(StringUtils.substring(relation.getArs(), 0, 2));
        relation.setId(xzs.getId());
        relation.setLbId(xzs.getLbIds().getFirst());
        relation.setValidityPeriods(extractGueltigkeit(xzs));
        relation.setJzufi(xzs.getXZuFi22());

        if (xzs.getParentTyp() == RelationTyp.ORGANISATION) {
            ZustaendigkeitOrganisationseinheit zsOe = (ZustaendigkeitOrganisationseinheit) xzs
                .getXZuFi22()
                .getZustaendigkeit()
                .getFirst();
            CodeZustaendigkeitsrolle codeRolle = zsOe.getRolle();
            if (codeRolle != null) {
                String uri =
                    codeRolle.getListURI() == null
                        ? XZuFiUtil.DEFAULT_URI_ZUSTAENDIGKEITS_ROLLE
                        : codeRolle.getListURI();
                String version =
                    codeRolle.getListVersionID() == null
                        ? XZuFiUtil.DEFAULT_VERSION_ZUSTAENDIGKEITS_ROLLE
                        : codeRolle.getListVersionID();
                CodelistEntry entry = codelistService.getCodelistEntry(
                    uri,
                    version,
                    codeRolle.getCode()
                );

                ResponsibilityRole role = new ResponsibilityRole();
                role.setCode(codeRolle.getCode());
                role.setName(entry.getText(Locale.GERMAN));
                role.setUri(uri);
                role.setVersion(version);
                LocaleUtils.SUPPORTED_LANGUAGES.forEach(lang -> {
                    if (!Locale.GERMAN.equals(lang)) {
                        role.getOtherLanguages().put(lang, entry.getText(lang));
                    }
                });
                relation.setResponsibility(role);
            }
        }
        return relation;
    }

    private List<ValidityPeriod> extractGueltigkeit(GenericZustaendigkeitTransferObjekt xzs) {
        return switch (xzs.getVersion()) {
            case XZUFI22 -> {
                Zustaendigkeit bo = xzs.getXZuFi22().getZustaendigkeit().getFirst();
                yield XZuFiUtil.getValidityPeriods(bo.getGueltigkeit());
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Zustaendigkeit bo = xzs
                    .getXZuFi23()
                    .getZustaendigkeit()
                    .getFirst();
                yield de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getValidityPeriods(
                    bo.getGueltigkeit()
                );
            }
        };
    }
}
