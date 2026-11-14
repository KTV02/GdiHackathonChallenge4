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
package de.dataport.suchdienst.importer.model;

import de.dataport.pv_onlinegw.xzufi_konverter.XZuFiKonverter;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.util.TypeUtil;
import de.dataport.suchdienst.indexer.util.x22.XZuFiUtil;
import de.dataport.suchdienst.util.IdentifikatorUtil;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.Zustaendigkeit;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public class GenericZustaendigkeitTransferObjekt
    extends GenericXZuFiObjekt<
        ZustaendigkeitTransferObjekt,
        de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt
    >
{

    private final String id;
    private final List<String> gebietIds;
    private final List<String> lbIds;
    private final Set<String> arsFederalState;
    private final String parentId;
    private final RelationTyp parentTyp;

    public GenericZustaendigkeitTransferObjekt(
        ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt
    ) {
        super(zustaendigkeitTransferObjekt, null);
        if (zustaendigkeitTransferObjekt.getZustaendigkeit().size() > 1) {
            throw new IllegalArgumentException(
                "Für die weitere Verarbeitung darf das interne ZustaendigkeitTransferObjekt nur eine Zustaendigkeit enthalten."
            );
        }

        this.parentTyp = TypeUtil.relationTypFromSchemeId(
            zustaendigkeitTransferObjekt
                .getZustaendigkeit()
                .stream()
                .findAny()
                .orElseThrow(IllegalArgumentException::new)
                .getId()
                .getSchemeID()
        );
        this.parentId = XZuFiUtil.getIdentificatorString(
            zustaendigkeitTransferObjekt.getUebergeordnetesObjektID(),
            TypeUtil.identificationTypeFrom(parentTyp)
        );
        Zustaendigkeit bo = zustaendigkeitTransferObjekt.getZustaendigkeit().getFirst();
        this.id = IdentifikatorUtil.getIdentificatorString(
            bo.getId(),
            TypeUtil.identificationTypeFromSchemeId(bo.getId().getSchemeID())
        );
        this.gebietIds = extractGebietIds();
        this.lbIds = extractLeistungIdentifikatorStringIds();
        this.arsFederalState = extractArsFederalStates();
    }

    public GenericZustaendigkeitTransferObjekt(
        de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt
    ) {
        super(null, zustaendigkeitTransferObjekt);
        if (zustaendigkeitTransferObjekt.getZustaendigkeit().size() > 1) {
            throw new IllegalArgumentException(
                "Für die weitere Verarbeitung darf das interne ZustaendigkeitTransferObjekt nur eine Zustaendigkeit enthalten."
            );
        }

        this.parentTyp =
            zustaendigkeitTransferObjekt.getUebergeordnetesObjektKlasse() != null
                ? TypeUtil.relationTypFromCode(
                      zustaendigkeitTransferObjekt.getUebergeordnetesObjektKlasse().getCode()
                  )
                : TypeUtil.relationTypFromSchemeId(
                      zustaendigkeitTransferObjekt
                          .getZustaendigkeit()
                          .stream()
                          .findAny()
                          .orElseThrow(IllegalArgumentException::new)
                          .getId()
                          .getSchemeID()
                  );

        this.parentId = de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
            zustaendigkeitTransferObjekt.getUebergeordnetesObjektID(),
            TypeUtil.identificationTypeFrom(parentTyp)
        );
        de.xoev.xzufi.v2_3_1.Zustaendigkeit bo = zustaendigkeitTransferObjekt
            .getZustaendigkeit()
            .getFirst();
        this.id = IdentifikatorUtil.getIdentificatorString(
            bo.getId(),
            TypeUtil.identificationTypeFromSchemeId(bo.getId().getSchemeID())
        );
        this.gebietIds = extractGebietIds();
        this.lbIds = extractLeistungIdentifikatorStringIds();
        this.arsFederalState = extractArsFederalStates();

        if (
            this.parentTyp == RelationTyp.ORGANISATION &&
            !(bo instanceof ZustaendigkeitOrganisationseinheit)
        ) {
            throw new IllegalStateException(
                "Die Zustaendigkeit ist für eine OE, aber die übertragene ZS ist keine Instanz von ZSOE."
            );
        }

        if (
            this.parentTyp == RelationTyp.ONLINESERVICE &&
            !(bo instanceof ZustaendigkeitOnlinedienst)
        ) {
            throw new IllegalStateException(
                "Die Zustaendigkeit ist für einen OD, aber die übertragene ZS ist keine Instanz von ZSOD."
            );
        }
    }

    public ZustaendigkeitTransferObjekt getOrConvertXZuFi22() {
        return switch (getVersion()) {
            case XZUFI22 -> super.getXZuFi22();
            case XZUFI23 -> XZuFiKonverter.konvertiere(super.getXZuFi23());
        };
    }

    public de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt getOrConvertXZuFi23() {
        return switch (getVersion()) {
            case XZUFI23 -> super.getXZuFi23();
            case XZUFI22 -> XZuFiKonverter.konvertiere(super.getXZuFi22());
        };
    }

    private List<String> extractLeistungIdentifikatorStringIds() {
        return switch (getVersion()) {
            case XZUFI22 -> {
                Zustaendigkeit bo = getXZuFi22().getZustaendigkeit().getFirst();
                yield bo
                    .getLeistungID()
                    .stream()
                    .map(lb -> XZuFiUtil.getIdentificatorString(lb, IdentificationType.LB))
                    .toList();
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Zustaendigkeit bo = getXZuFi23()
                    .getZustaendigkeit()
                    .getFirst();
                yield bo
                    .getLeistungID()
                    .stream()
                    .map(lb ->
                        de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getIdentificatorString(
                            lb,
                            IdentificationType.LB
                        )
                    )
                    .toList();
            }
        };
    }

    private List<String> extractGebietIds() {
        return switch (getVersion()) {
            case XZUFI22 -> {
                Zustaendigkeit bo = getXZuFi22().getZustaendigkeit().getFirst();
                yield bo.getGebietID().stream().map(Identifikator::getValue).toList();
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Zustaendigkeit bo = getXZuFi23()
                    .getZustaendigkeit()
                    .getFirst();
                yield bo
                    .getGebietID()
                    .stream()
                    .map(de.xoev.xzufi.v2_3_1.Identifikator::getValue)
                    .toList();
            }
        };
    }

    private Set<String> extractArsFederalStates() {
        return switch (getVersion()) {
            case XZUFI22 -> {
                var zs = getXZuFi22().getZustaendigkeit().getFirst();
                yield XZuFiUtil.getArsFederalState(zs.getGebietID());
            }
            case XZUFI23 -> {
                var zs = getXZuFi23().getZustaendigkeit().getFirst();
                yield de.dataport.suchdienst.indexer.util.x23.XZuFiUtil.getArsFederalState(
                    zs.getGebietID()
                );
            }
        };
    }
}
