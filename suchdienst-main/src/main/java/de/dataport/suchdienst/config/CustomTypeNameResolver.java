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
package de.dataport.suchdienst.config;

import io.swagger.v3.core.jackson.TypeNameResolver;
import java.util.Collections;
import java.util.Set;

public class CustomTypeNameResolver extends TypeNameResolver {

    public CustomTypeNameResolver(boolean useFqn) {
        setUseFqn(useFqn);
    }

    @Override
    protected String nameForClass(Class<?> cls, Set<Options> options) {
        // Verhindern, dass SKIP_API_MODEL verwendet wird
        return super.nameForClass(cls, Collections.emptySet());
    }

    @Override
    protected String getNameOfClass(Class<?> cls) {
        var packageName = cls.getPackageName();
        var simpleName = cls.getSimpleName();

        if (packageName.equals("de.dataport.suchdienst.jzufi.model.v23")) {
            return "Jzufi23" + simpleName;
        }

        // Vollqualifiziert für alle XZuFi 2.3.1 im API Model
        if (packageName.startsWith("de.xoev.xzufi.v2_3")) {
            return super.getNameOfClass(cls);
        }

        return simpleName;
    }
}
