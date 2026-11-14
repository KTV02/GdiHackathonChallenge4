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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

public class Jackson3CustomTypeResolverBuilder extends DefaultTypeResolverBuilder {

    public Jackson3CustomTypeResolverBuilder(DefaultTyping t) {
        super(
            new MyLaissezFaireSubTypeValidator(),
            t,
            JsonTypeInfo.As.WRAPPER_ARRAY,
            JsonTypeInfo.Id.CLASS,
            "@type"
        );
    }

    @Override
    public boolean useForType(JavaType t) {
        return t.getRawClass().getName().startsWith("de.xoev.xzufi.v2");
    }

    private static class MyLaissezFaireSubTypeValidator extends PolymorphicTypeValidator {

        @Override
        public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(
            DatabindContext ctxt,
            JavaType baseType,
            String subClassName
        ) {
            return Validity.ALLOWED;
        }

        @Override
        public Validity validateSubType(DatabindContext ctxt, JavaType baseType, JavaType subType) {
            return Validity.ALLOWED;
        }
    }
}
