/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.suchen.model.out.onlinedienst;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriodOutModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OdRelations {

    @Schema(
        description = "id, xzufi:Identifikator",
        example = "L100042.ZSOD.LB_xzufi22DeleteZSOD_77777_0003.OD_xzufi22DeleteZSOD_77777_0001.095620000000"
    )
    @EqualsAndHashCode.Include
    private String id;

    @Schema(
        description = "leistungID, xzufi:Identifikator",
        example = "L100042.LB.LB_xzufi22DeleteZSOD_77777_0001"
    )
    private String lbId;

    @Schema(description = "gebietID, xzufi:Identifikator", example = "012012012012")
    private String ars;

    @Schema(description = "Onlinedienst-ID", example = "L100042.OD.OD_xzufi22DeleteZSOD_77777_0001")
    private String parentId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ValidityPeriodOutModel> validityPeriods = new ArrayList<>();
}
