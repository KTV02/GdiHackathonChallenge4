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
package de.dataport.suchdienst.importer.model;

import de.dataport.suchdienst.importer.model.jzufi.FormularEntity;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity22;
import de.dataport.suchdienst.importer.model.jzufi.FormularEntity23;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.LeistungEntity23;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OnlinedienstEntity23;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.OrganisationseinheitEntity23;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity22;
import de.dataport.suchdienst.importer.model.jzufi.SpezialisierungEntity23;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity22;
import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity23;

public class GenericModelMapper {

    private GenericModelMapper() {}

    public static GenericLeistung mapLeistungEntity(LeistungEntity entity) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case LeistungEntity22 bo -> new GenericLeistung(bo.getJzufi());
            case LeistungEntity23 bo -> new GenericLeistung(bo.getJzufi());
        };
    }

    public static GenericOrganisationseinheit mapOrganisationseinheitEntity(
        OrganisationseinheitEntity entity
    ) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case OrganisationseinheitEntity22 bo -> new GenericOrganisationseinheit(bo.getJzufi());
            case OrganisationseinheitEntity23 bo -> new GenericOrganisationseinheit(bo.getJzufi());
        };
    }

    public static GenericLeistungSpezialisierung mapLeistungSpezialisierungEntity(
        SpezialisierungEntity entity
    ) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case SpezialisierungEntity22 bo -> new GenericLeistungSpezialisierung(bo.getJzufi());
            case SpezialisierungEntity23 bo -> new GenericLeistungSpezialisierung(bo.getJzufi());
        };
    }

    public static GenericOnlinedienst mapOnlinedienstEntity(OnlinedienstEntity entity) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case OnlinedienstEntity22 bo -> new GenericOnlinedienst(bo.getJzufi());
            case OnlinedienstEntity23 bo -> new GenericOnlinedienst(bo.getJzufi());
        };
    }

    public static GenericFormular mapFormularEntity(FormularEntity entity) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case FormularEntity22 bo -> new GenericFormular(bo.getJzufi());
            case FormularEntity23 bo -> new GenericFormular(bo.getJzufi());
        };
    }

    public static GenericZustaendigkeitTransferObjekt mapZustaendigkeitTransferObjektEntity(
        ZustaendigkeitEntity entity
    ) {
        return switch (entity) {
            case null -> throw new IllegalArgumentException("Entity darf nicht null sein");
            case ZustaendigkeitEntity22 bo -> new GenericZustaendigkeitTransferObjekt(
                bo.getJzufi()
            );
            case ZustaendigkeitEntity23 bo -> new GenericZustaendigkeitTransferObjekt(
                bo.getJzufi()
            );
        };
    }
}
