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

import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.xoev.xzufi.v2_2_0.Identifikator;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

public class IdentifikatorUtil {

    private IdentifikatorUtil() {}

    public static String getIdentificatorString(
        @NonNull Identifikator identifikator,
        @NonNull IdentificationType identificationType
    ) {
        Objects.requireNonNull(identifikator);

        if (
            StringUtils.isEmpty(identifikator.getValue()) ||
            StringUtils.isEmpty(identifikator.getSchemeAgencyID())
        ) {
            throw new ImportException(
                ErrorCodeEnum.BAD_REQUEST,
                "Das XZuFi muss durchgehend valide ID-Informationen enthalten",
                ExceptionType.FACHLICH
            );
        }
        return identifikatorAttributesToString(
            identifikator.getSchemeAgencyID(),
            identificationType,
            identifikator.getValue()
        );
    }

    public static String getIdentificatorString(
        de.xoev.xzufi.v2_3_1.@NonNull Identifikator identifikator,
        @NonNull IdentificationType identificationType
    ) {
        Objects.requireNonNull(identifikator);

        if (
            StringUtils.isEmpty(identifikator.getValue()) ||
            StringUtils.isEmpty(identifikator.getSchemeAgencyID())
        ) {
            throw new ImportException(
                ErrorCodeEnum.BAD_REQUEST,
                "Das XZufi muss durchgehend valide ID-Informationen enthalten",
                ExceptionType.FACHLICH
            );
        }
        return identifikatorAttributesToString(
            identifikator.getSchemeAgencyID(),
            identificationType,
            identifikator.getValue()
        );
    }

    public static String getIdentificatorValueByString(
        String identifikator,
        @NonNull IdentificationType identificationType
    ) {
        if (identifikator == null) {
            return null;
        }
        String token = ".%s.".formatted(identificationType);
        // Annahme: Eine Scheme-AgencyId enthält auf keinen Fall `token` als Teilstring.
        //  Unter dieser Annahme steht in `identifikator` rechts von Token die
        //  ursprünglich übermittelte XZuFi-ID.
        int index = identifikator.indexOf(token);
        return index < 0 ? null : identifikator.substring(index + token.length());
    }

    private static String identifikatorAttributesToString(
        String schemeAgencyId,
        IdentificationType identificationType,
        String value
    ) {
        return String.format("%s.%s.%s", schemeAgencyId, identificationType.name(), value);
    }
}
