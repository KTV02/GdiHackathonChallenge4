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
package de.dataport.suchdienst.elasticsearch.api.exception;

import de.dataport.suchdienst.error.model.ErrorCodeEnum;

/**
 * Exception für interne Adminendpunkte. Diese kann genutzt werden um genauere Informationen über die Schnittstelle rauszugeben. Die Exception darf nicht an öffentlichen Schnittstellen verwendet werden.
 */
// Die Exception erbt von sehr vielen Klassen, damit sie zuverlässig gecacht werden kann
@SuppressWarnings("java:S110")
public class AdminElasticSearchException extends ElasticSearchException {

    public AdminElasticSearchException(ErrorCodeEnum code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
