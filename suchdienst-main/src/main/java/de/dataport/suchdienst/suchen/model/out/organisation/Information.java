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
package de.dataport.suchdienst.suchen.model.out.organisation;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Collection;
import java.util.Collections;
import lombok.Data;

@Data
public class Information {

    private String title;
    private String text;

    // Die Annotation wird benötigt, damit die Collection im
    // OpenAPI-Schema dokumentiert wird. Das Suchdienst-Frontend
    // verwendet dieses Feld ohnehin schon (aufgrund des internen Wissens)
    @JsonBackReference
    private Collection<Information> informationContents = Collections.emptySet();
}
