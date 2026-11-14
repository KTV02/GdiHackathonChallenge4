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
package de.dataport.suchdienst.config.conditions;

import de.dataport.suchdienst.config.InstanceType;
import java.util.Objects;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Bedingung für den Instanztyp "MASTER".
 * Kann z.B. mit '@Conditional(OnMasterCondition.class)' an Beans verwendet werden, um
 * bestimmte Komponenten nur für Master-Instanzen (und keine Slaves) zu initialisieren.
 */
public class OnMasterCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Objects.requireNonNull(
            context.getEnvironment().getProperty("de.dataport.suchdienst.instance.type")
        ).equalsIgnoreCase(InstanceType.MASTER.name());
    }
}
