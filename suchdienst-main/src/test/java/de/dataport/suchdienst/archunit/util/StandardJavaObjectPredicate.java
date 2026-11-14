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
package de.dataport.suchdienst.archunit.util;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Set;

public class StandardJavaObjectPredicate extends DescribedPredicate<JavaClass> {

    // Set mit allen Standard-Java-Klassen, die als "Standard-Objekte" gelten sollen
    private static final Set<Class<?>> standardJavaClasses = Set.of(
        Integer.class,
        Void.class,
        String.class,
        Long.class,
        Boolean.class,
        Double.class,
        Float.class,
        Character.class,
        Short.class,
        Byte.class,
        Object.class
    );

    public StandardJavaObjectPredicate() {
        super("is a standard Java object (like String, Integer, Void, etc.)");
    }

    // Statische Hilfsmethode, um den Predicate einfach verwenden zu können
    public static DescribedPredicate<JavaClass> isStandardJavaObject() {
        return new StandardJavaObjectPredicate();
    }

    @Override
    public boolean test(JavaClass javaClass) {
        // Prüfen, ob die JavaClass einem der Standard-Java-Typen entspricht
        boolean isStandardWrapperType = standardJavaClasses
            .stream()
            .anyMatch(type -> javaClass.reflect().equals(type));
        if (!isStandardWrapperType) {
            return javaClass.getName().equals("void");
        }
        return isStandardWrapperType;
    }
}
