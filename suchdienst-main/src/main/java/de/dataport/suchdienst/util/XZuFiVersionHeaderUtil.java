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

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;

import de.dataport.suchdienst.importer.model.XZuFiVersion;
import java.util.Optional;
import org.springframework.http.MediaType;

public final class XZuFiVersionHeaderUtil {

    private static final String XZUFI_VERSION_22 = ";xzufi-version=2.2.0";
    private static final String XZUFI_VERSION_23 = ";xzufi-version=2.3.1";

    public static final String MEDIA_TYPE_XML_22 = APPLICATION_XML_VALUE + XZUFI_VERSION_22;

    /**
     * @deprecated Die Nutzung von Accept-Headern in dieser Form hat je nach Generator zu inkompatiblen OpenAPI-Clients geführt, in denen keine XZuFi-Version gesetzt werden konnte.
     */
    @Deprecated(since = "11.12.0", forRemoval = true)
    public static final String MEDIA_TYPE_JSON_22 = APPLICATION_JSON_VALUE + XZUFI_VERSION_22;

    public static final String MEDIA_TYPE_XML_23 = APPLICATION_XML_VALUE + XZUFI_VERSION_23;

    /**
     * @deprecated Die Nutzung von Accept-Headern in dieser Form hat je nach Generator zu inkompatiblen OpenAPI-Clients geführt, in denen keine XZuFi-Version gesetzt werden konnte.
     */
    @Deprecated(since = "11.12.0", forRemoval = true)
    public static final String MEDIA_TYPE_JSON_23 = APPLICATION_JSON_VALUE + XZUFI_VERSION_23;

    private XZuFiVersionHeaderUtil() {
        // Utility class
    }

    public static Optional<XZuFiVersion> getXZuFiVersionFromHeader(MediaType header) {
        String xzufiVersion = header.getParameter("xzufi-version");
        if (xzufiVersion == null) {
            return Optional.empty();
        }
        return switch (xzufiVersion) {
            case "2.2.0" -> Optional.of(XZuFiVersion.XZUFI22);
            case "2.3.1" -> Optional.of(XZuFiVersion.XZUFI23);
            default -> Optional.empty();
        };
    }
}
