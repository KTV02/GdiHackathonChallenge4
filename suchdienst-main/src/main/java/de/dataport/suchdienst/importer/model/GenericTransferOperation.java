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

import de.xoev.xzufi.v2_2_0.LoescheObjekt;
import de.xoev.xzufi.v2_2_0.OrganisationseinheitErweitert;
import de.xoev.xzufi.v2_2_0.SchreibeObjekt;
import de.xoev.xzufi.v2_2_0.TransferOperation040502;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import de.xoev.xzufi.v2_3_1.OnlinedienstErweitert;
import de.xoev.xzufi.v2_3_1.TransferOperation050502;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class GenericTransferOperation {

    protected final List<GenericXZuFiOperation> transferOperation = new ArrayList<>();

    public GenericTransferOperation() {}

    public GenericTransferOperation(TransferOperation040502 transferOperation22) {
        transferOperation22.getTransferOperation().forEach(o -> {
            if (o instanceof SchreibeObjekt schreibeObjekt) {
                verarbeiteSchreibeObjekt(schreibeObjekt);
            } else if (o instanceof LoescheObjekt loescheObjekt) {
                transferOperation.add(new GenericLoescheObjekt(loescheObjekt));
            }
        });
    }

    public GenericTransferOperation(TransferOperation050502 transferOperation23) {
        transferOperation23.getTransferOperation().forEach(o -> {
            if (o.getValue() instanceof de.xoev.xzufi.v2_3_1.SchreibeObjekt schreibeObjekt) {
                verarbeiteSchreibeObjekt(schreibeObjekt);
            } else if (o.getValue() instanceof de.xoev.xzufi.v2_3_1.LoescheObjekt loescheObjekt) {
                transferOperation.add(new GenericLoescheObjekt(loescheObjekt));
            }
        });
    }

    private void verarbeiteSchreibeObjekt(SchreibeObjekt schreibeObjekt) {
        if (schreibeObjekt.getLeistung() != null) {
            addSchreibeObjekt(new GenericLeistung(schreibeObjekt.getLeistung()));
        }

        if (schreibeObjekt.getOrganisationseinheit() != null) {
            addSchreibeObjekt(
                new GenericOrganisationseinheit(
                    (OrganisationseinheitErweitert) schreibeObjekt.getOrganisationseinheit()
                )
            );
        }

        if (schreibeObjekt.getFormular() != null) {
            addSchreibeObjekt(new GenericFormular(schreibeObjekt.getFormular()));
        }

        if (schreibeObjekt.getOnlinedienst() != null) {
            addSchreibeObjekt(
                new GenericOnlinedienst(
                    (de.xoev.xzufi.v2_2_0.OnlinedienstErweitert) schreibeObjekt.getOnlinedienst()
                )
            );
        }

        if (schreibeObjekt.getSpezialisierung() != null) {
            addSchreibeObjekt(
                new GenericLeistungSpezialisierung(schreibeObjekt.getSpezialisierung())
            );
        }

        if (schreibeObjekt.getZustaendigkeitTransferObjekt() != null) {
            ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt =
                schreibeObjekt.getZustaendigkeitTransferObjekt();
            zustaendigkeitTransferObjekt.getZustaendigkeit().forEach(zs -> {
                ZustaendigkeitTransferObjekt zsTransferObjektMitEinerZustaendigkeit =
                    new ZustaendigkeitTransferObjekt();
                zsTransferObjektMitEinerZustaendigkeit.setUebergeordnetesObjektID(
                    zustaendigkeitTransferObjekt.getUebergeordnetesObjektID()
                );
                zsTransferObjektMitEinerZustaendigkeit.getZustaendigkeit().add(zs);
                addSchreibeObjekt(
                    new GenericZustaendigkeitTransferObjekt(zsTransferObjektMitEinerZustaendigkeit)
                );
            });
        }
    }

    private void verarbeiteSchreibeObjekt(de.xoev.xzufi.v2_3_1.SchreibeObjekt schreibeObjekt) {
        if (schreibeObjekt.getLeistung() != null) {
            addSchreibeObjekt(new GenericLeistung(schreibeObjekt.getLeistung()));
        }

        if (schreibeObjekt.getOrganisationseinheit() != null) {
            addSchreibeObjekt(
                new GenericOrganisationseinheit(
                    (de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert) schreibeObjekt.getOrganisationseinheit()
                )
            );
        }

        if (schreibeObjekt.getFormular() != null) {
            addSchreibeObjekt(new GenericFormular(schreibeObjekt.getFormular()));
        }

        if (schreibeObjekt.getOnlinedienst() != null) {
            addSchreibeObjekt(
                new GenericOnlinedienst((OnlinedienstErweitert) schreibeObjekt.getOnlinedienst())
            );
        }

        if (schreibeObjekt.getSpezialisierung() != null) {
            addSchreibeObjekt(
                new GenericLeistungSpezialisierung(schreibeObjekt.getSpezialisierung())
            );
        }

        if (schreibeObjekt.getZustaendigkeitTransferObjekt() != null) {
            de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt zustaendigkeitTransferObjekt =
                schreibeObjekt.getZustaendigkeitTransferObjekt();
            zustaendigkeitTransferObjekt.getZustaendigkeit().forEach(zs -> {
                de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt zsTransferObjektMitEinerZustaendigkeit =
                    new de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt();
                zsTransferObjektMitEinerZustaendigkeit.setUebergeordnetesObjektID(
                    zustaendigkeitTransferObjekt.getUebergeordnetesObjektID()
                );
                zsTransferObjektMitEinerZustaendigkeit.getZustaendigkeit().add(zs);
                zsTransferObjektMitEinerZustaendigkeit.setUebergeordnetesObjektKlasse(
                    zustaendigkeitTransferObjekt.getUebergeordnetesObjektKlasse()
                );
                addSchreibeObjekt(
                    new GenericZustaendigkeitTransferObjekt(zsTransferObjektMitEinerZustaendigkeit)
                );
            });
        }
    }

    private void addSchreibeObjekt(GenericXZuFiObjekt<?, ?> xZuFiObjekt) {
        transferOperation.add(new GenericSchreibeObjekt(xZuFiObjekt));
    }
}
