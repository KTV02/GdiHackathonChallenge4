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
package de.dataport.suchdienst.jzufi.model.out;

import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

public class RelationPage22 extends PagedModel<ZustaendigkeitTransferObjekt> {

    public RelationPage22(Page<ZustaendigkeitTransferObjekt> page) {
        super(page);
    }

    public RelationPage22(
        List<ZustaendigkeitTransferObjekt> content,
        PageRequest pageRequest,
        long total
    ) {
        super(new PageImpl<>(content, pageRequest, total));
    }
}
