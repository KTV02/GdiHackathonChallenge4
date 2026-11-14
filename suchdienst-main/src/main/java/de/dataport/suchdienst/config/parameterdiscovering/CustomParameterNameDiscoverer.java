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
package de.dataport.suchdienst.config.parameterdiscovering;

import org.springframework.core.PrioritizedParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;

public class CustomParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

    public CustomParameterNameDiscoverer() {
        this.addDiscoverer(new RequestParameterDiscoverer());
        this.addDiscoverer(new StandardReflectionParameterNameDiscoverer());
    }
}
