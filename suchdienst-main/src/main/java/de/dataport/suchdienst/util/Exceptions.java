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
package de.dataport.suchdienst.util;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Exceptions {

    /**
     * Prueft, ob t eine Ursache vom Typ clazz hat
     *
     * @param <T>   Typ der Ursache
     * @param t     der aufgetretene Fehler
     * @param clazz die Klasse der Ursache
     * @return true gdw. eine Exception der Klasse clazz in den Ursachen von t vorhanden ist
     */
    public static <T extends Throwable> boolean hasCause(Throwable t, Class<T> clazz) {
        return ExceptionUtils.throwableOfType(t, clazz) != null;
    }

    /**
     * Sucht in den Ursachen von t nach einem Fehler vom Typ clazz
     *
     * @param <T>   Typ der Ursache
     * @param t     der aufgetretene Fehler
     * @param clazz die Klasse der Ursache
     * @return einen ggf. leeren Optional mit der Ursache
     */
    public static <T extends Throwable> Optional<T> findCause(Throwable t, Class<T> clazz) {
        return Optional.ofNullable(ExceptionUtils.throwableOfType(t, clazz));
    }
}
