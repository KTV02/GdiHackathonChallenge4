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
import com.tngtech.archunit.core.domain.Formatters;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

public class ReturnTypeGenericsCondition extends ArchCondition<JavaMethod> {

    private final Class<?>[] classes;
    private final DescribedPredicate<JavaClass> predicateForGenericType;

    public ReturnTypeGenericsCondition(
        DescribedPredicate<JavaClass> predicateForGenericType,
        Class<?>... classes
    ) {
        super(
            "returns any of %s with generic type that %s",
            Formatters.formatNamesOf(classes),
            predicateForGenericType.getDescription()
        );
        this.classes = classes;
        this.predicateForGenericType = predicateForGenericType;
    }

    public static ArchCondition<JavaMethod> haveGenericReturnType(
        DescribedPredicate<JavaClass> predicateForGenericType,
        Class<?>... classes
    ) {
        return new ReturnTypeGenericsCondition(predicateForGenericType, classes);
    }

    @Override
    public void check(JavaMethod method, ConditionEvents events) {
        var returnType = method.getReturnType();

        if (returnType instanceof JavaParameterizedType parameterizedType) {
            if (JavaClass.Predicates.belongToAnyOf(classes).test(parameterizedType.toErasure())) {
                List<JavaType> typeArguments = parameterizedType.getActualTypeArguments();

                if (typeArguments.isEmpty()) {
                    events.add(
                        SimpleConditionEvent.violated(
                            method,
                            String.format(
                                "Method %s returns raw ResponseEntity without generic type",
                                method.getFullName()
                            )
                        )
                    );
                } else {
                    boolean matches = checkGenericType(
                        predicateForGenericType,
                        parameterizedType,
                        events
                    );

                    if (!matches) {
                        events.add(
                            SimpleConditionEvent.violated(
                                method,
                                String.format(
                                    "Method %s returns ResponseEntity with type %s which does not match %s",
                                    method.getFullName(),
                                    typeArguments.getFirst().getName(),
                                    predicateForGenericType.getDescription()
                                )
                            )
                        );
                    }
                }
            } else {
                events.add(
                    SimpleConditionEvent.violated(
                        method,
                        String.format(
                            "Method %s does not return ResponseEntity, but %s",
                            method.getFullName(),
                            returnType.getName()
                        )
                    )
                );
            }
        } else {
            events.add(
                SimpleConditionEvent.violated(
                    method,
                    String.format(
                        "Method %s does not return a parameterized type",
                        method.getFullName()
                    )
                )
            );
        }
    }

    boolean checkGenericType(
        DescribedPredicate<JavaClass> predicateForGenericType,
        JavaParameterizedType item,
        ConditionEvents events
    ) {
        List<JavaType> typeArguments = item.getActualTypeArguments();

        if (typeArguments.isEmpty()) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    String.format("Class %s is without generic type", item.getName())
                )
            );
            return false;
        } else {
            JavaType javaType = typeArguments.getFirst();
            boolean matches = predicateForGenericType.test(javaType.toErasure());

            if (!matches && javaType instanceof JavaParameterizedType parameterizedType) {
                // Parametrisierter Typ erfüllt nicht die Bedingung, ist aber selber Parametrisierbar und wird rekursiv untersucht
                // Beispiel für eine solche typisierung: ResponseEntity<List<OutModel>>
                return checkGenericType(predicateForGenericType, parameterizedType, events);
            }

            if (!matches) {
                events.add(
                    SimpleConditionEvent.violated(
                        item,
                        String.format(
                            "Method %s returns ResponseEntity with type %s which does not match %s",
                            item.getName(),
                            javaType.getName(),
                            predicateForGenericType.getDescription()
                        )
                    )
                );
            }
            return matches;
        }
    }
}
