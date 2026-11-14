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

import de.dataport.suchdienst.util.ResultPageWithMeta;
import de.xoev.xzufi.v2_2_0.ZustaendigkeitTransferObjekt;
import java.io.Serializable;
import java.util.List;

/**
 * @deprecated
 * Wird für die OpenAPI Annotation als Wrapper für die ResultPage verwendet
 */
@Deprecated(forRemoval = true)
public class RelationPage
    extends ResultPageWithMeta<ZustaendigkeitTransferObjekt, RelationPage.RelationMeta>
{

    public record RelationMeta() implements Serializable {}

    public RelationPage(
        List<ZustaendigkeitTransferObjekt> content,
        long total,
        int page,
        int pageSize
    ) {
        super(content, total, page, pageSize);
    }
}
