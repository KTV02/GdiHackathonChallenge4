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
package de.dataport.suchdienst.validator;

import org.springframework.test.web.servlet.ResultActions;

public class ServiceDescriptionResultListValidator extends ResultListValidator {

    public ServiceDescriptionResultListValidator(ResultActions response) {
        super(response, "serviceDescriptions.");
    }
}
