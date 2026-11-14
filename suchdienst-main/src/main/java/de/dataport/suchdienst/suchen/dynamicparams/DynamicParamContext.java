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
package de.dataport.suchdienst.suchen.dynamicparams;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Hält Referenzen zu den verschiedenen Verwaltungsobjekten, welche für die dynamische Parametrisierung
 * von Onlinedienst URLs nötig sind.
 */
@Value
@Builder
@JsonDeserialize(builder = DynamicParamContext.DynamicParamContextBuilder.class)
public class DynamicParamContext {

    String lbId;
    String leikaId;
    String oeId;
    String ars;

    public Optional<String> getLeistungId() {
        return Optional.ofNullable(lbId);
    }

    public Optional<String> getOrganisationseinheitId() {
        return Optional.ofNullable(oeId);
    }

    public Optional<String> getLeikaId() {
        return leikaId == null ? Optional.empty() : Optional.of(leikaId);
    }

    public Optional<String> getArs() {
        return ars == null ? Optional.empty() : Optional.of(ars);
    }
}
