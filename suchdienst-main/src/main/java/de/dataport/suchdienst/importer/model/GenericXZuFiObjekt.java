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

import java.util.NoSuchElementException;

@SuppressWarnings("java:S119")
public abstract class GenericXZuFiObjekt<X22, X23> {

    private final X22 x22;
    private final X23 x23;

    protected GenericXZuFiObjekt(X22 x22, X23 x23) {
        // xor
        if ((x22 == null) == (x23 == null)) {
            throw new IllegalArgumentException(
                "Entweder x22 (XZuFi 2.2.0) oder x23 (XZuFi 2.3.1) muss null sein, aber nicht beide oder keiner."
            );
        }
        validatePackage(x22, "de.xoev.xzufi.v2_2_0");
        validatePackage(x23, "de.xoev.xzufi.v2_3_1");
        this.x22 = x22;
        this.x23 = x23;
    }

    private void validatePackage(Object obj, String packageName) {
        if (obj != null && !packageName.equals(obj.getClass().getPackageName())) {
            throw new IllegalArgumentException(
                obj + " muss aus dem Package " + packageName + " sein"
            );
        }
    }

    public boolean isXZuFi22() {
        return x22 != null;
    }

    public boolean isXZuFi23() {
        return x23 != null;
    }

    public X22 getXZuFi22() {
        if (isXZuFi22()) {
            return x22;
        }
        throw new NoSuchElementException();
    }

    public X23 getXZuFi23() {
        if (isXZuFi23()) {
            return x23;
        }
        throw new NoSuchElementException();
    }

    public abstract String getId();

    public XZuFiVersion getVersion() {
        if (isXZuFi22()) {
            return XZuFiVersion.XZUFI22;
        } else if (isXZuFi23()) {
            return XZuFiVersion.XZUFI23;
        }
        throw new IllegalStateException("XZUFI Version konnte nicht bestimmt werden");
    }

    public abstract X22 getOrConvertXZuFi22();

    public abstract X23 getOrConvertXZuFi23();
}
