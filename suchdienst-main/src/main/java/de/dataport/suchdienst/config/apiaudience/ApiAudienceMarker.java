// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Marker-Annotation zur Kennzeichnung der Zielgruppe eines API-Endpunkts.
package de.dataport.suchdienst.config.apiaudience;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kennzeichnet einen REST-Controller oder eine Handler-Methode mit seiner {@link ApiAudience}.
 *
 * <p>Die Annotation kann auf Klassen- und Methodenebene gesetzt werden. Eine Methodenannotation
 * hat Vorrang vor der Klassenannotation (ausgewertet durch {@link ApiAudienceResolver}).
 *
 * <p>Verwendungsbeispiel:
 * <pre>{@code
 * @ApiAudienceMarker(ApiAudience.PUBLIC)
 * public class ServiceDescriptionController { ... }
 * }</pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiAudienceMarker {
    ApiAudience value();
}
