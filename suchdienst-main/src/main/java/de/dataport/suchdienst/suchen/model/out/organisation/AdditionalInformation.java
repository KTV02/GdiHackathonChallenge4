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

import de.dataport.suchdienst.suchen.model.out.common.KeyValueObject;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;

@Data
public class AdditionalInformation {

    private AdditionalInformationType type;
    private String title;
    private String text;

    @SuppressWarnings("java:S1700") // Das Feld kann nicht automatisch umbenannt werden, gehört zu einem Out-Model.
    private Collection<KeyValueObject> additionalInformation = new ArrayList<>();
}
