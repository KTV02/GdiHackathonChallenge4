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

import de.dataport.suchdienst.importer.model.GenericZustaendigkeitTransferObjekt;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Einheitlicher Wrapper für Zuständigkeiten in der OD-Relation-Verarbeitung.
 *
 * <p>Kapselt Metadaten (parentId, ars, lbId) und bietet lazy Zugriff auf das
 * XZuFi-2.3.1-Objekt via {@link #getXzufi23()}. Für XZuFi-2.2-Daten wird die
 * Konvertierung beim ersten Zugriff durchgeführt und das Quellobjekt danach freigegeben.
 * XZuFi-2.3-Daten werden direkt referenziert, ohne Konvertierung.</p>
 */
@Getter(AccessLevel.PACKAGE)
class ConvertedZustaendigkeit {

    private final String id;
    private final String parentId;
    private final String ars;
    private final String lbId;
    private final XZuFiVersion originalVersion;

    /**
     * Für XZuFi-2.2-Daten: hält das generische Objekt bis zur ersten Konvertierung.
     * Wird nach Konvertierung auf {@code null} gesetzt, um den Raw-JAXB-Baum für GC freizugeben.
     */
    private GenericZustaendigkeitTransferObjekt sourceForDeferredConversion;

    /** Gecachtes XZuFi-2.3.1-Ergebnis – direkte Referenz (2.3) oder konvertierte Kopie (2.2). */
    private ZustaendigkeitTransferObjekt cachedXzufi23;

    private ConvertedZustaendigkeit(GenericZustaendigkeitTransferObjekt generic) {
        this.id = generic.getId();
        this.parentId = generic.getParentId();
        this.ars = generic.getGebietIds().isEmpty() ? null : generic.getGebietIds().getFirst();
        this.lbId = requireLeistungId(generic);
        this.originalVersion = generic.getVersion();

        this.cachedXzufi23 = resolveEagerlyIfPossible(generic);
        this.sourceForDeferredConversion = cachedXzufi23 == null ? generic : null;
    }

    /**
     * Erzeugt eine {@link ConvertedZustaendigkeit} aus dem generischen Objekt.
     * Für XZuFi-2.2-Daten erfolgt die Konvertierung zu 2.3.1 erst beim ersten
     * Zugriff auf {@link #getXzufi23()}.
     *
     * @param generic das generische Zuständigkeitsobjekt
     */
    static ConvertedZustaendigkeit of(GenericZustaendigkeitTransferObjekt generic) {
        return new ConvertedZustaendigkeit(generic);
    }

    /**
     * Gibt das XZuFi-2.3.1-Objekt zurück. Für XZuFi-2.2-Daten wird die Konvertierung
     * beim ersten Zugriff durchgeführt und das Quellobjekt anschließend freigegeben.
     */
    ZustaendigkeitTransferObjekt getXzufi23() {
        if (cachedXzufi23 == null && sourceForDeferredConversion != null) {
            cachedXzufi23 = sourceForDeferredConversion.getOrConvertXZuFi23();
            sourceForDeferredConversion = null;
        }
        return cachedXzufi23;
    }

    /**
     * Löst XZuFi-2.3-Objekte sofort auf; für XZuFi-2.2 bleibt die Konvertierung aufgeschoben.
     */
    private static ZustaendigkeitTransferObjekt resolveEagerlyIfPossible(
        GenericZustaendigkeitTransferObjekt generic
    ) {
        if (!generic.isXZuFi23()) {
            return null;
        }
        // XZuFi-2.3: direkte Referenz, kein Kopiervorgang, Generic sofort freigeben.
        return generic.getOrConvertXZuFi23();
    }

    /**
     * Zuständigkeiten ohne Leistungs-ID verletzen die Dateninvariante der OD-Relation-Verarbeitung.
     */
    private static String requireLeistungId(GenericZustaendigkeitTransferObjekt generic) {
        if (generic.getLbIds().isEmpty()) {
            throw new IllegalStateException(
                "Zustaendigkeit '%s' hat keine Leistungs-ID".formatted(generic.getId())
            );
        }
        return Objects.requireNonNull(generic.getLbIds().getFirst(), () ->
            "Zustaendigkeit '%s' hat eine null-Leistungs-ID".formatted(generic.getId())
        );
    }
}
