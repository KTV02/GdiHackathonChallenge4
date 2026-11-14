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
package de.dataport.suchdienst.indexer.util;

import de.dataport.suchdienst.indexer.model.AccessChannel;
import de.dataport.suchdienst.indexer.model.mapping.IdentificationType;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;

public final class TypeUtil {

    public static final String SCHEME_ID_OD = "ZustaendigkeitOnlinedienst";
    public static final String SCHEME_ID_OE = "ZustaendigkeitOrganisationseinheit";
    public static final String SCHEME_ID_FO = "ZustaendigkeitFormular";

    private TypeUtil() {}

    public static IdentificationType identificationTypeFrom(RelationTyp relationTyp) {
        return switch (relationTyp) {
            case ORGANISATION -> IdentificationType.OE;
            case ONLINESERVICE -> IdentificationType.OD;
            case FORM -> IdentificationType.FO;
        };
    }

    public static IdentificationType identificationTypeFromKlasse(String klasse, String schemeId) {
        return switch (klasse) {
            case "Leistung" -> IdentificationType.LB;
            case "LeistungSpezialisierung" -> IdentificationType.SP;
            case "Organisationseinheit" -> IdentificationType.OE;
            case "Formular" -> IdentificationType.FO;
            case "Zustaendigkeit" -> identificationTypeFromSchemeId(schemeId);
            case "Onlinedienst" -> IdentificationType.OD;
            default -> throw new IllegalArgumentException(
                "Kein Mapping von Klasse %s zu IdentificationType".formatted(klasse)
            );
        };
    }

    public static IdentificationType identificationTypeFromCode(String code, String schemeId) {
        return switch (code) {
            case "001" -> IdentificationType.LB;
            case "002" -> IdentificationType.SP;
            case "003" -> IdentificationType.OE;
            case "005" -> IdentificationType.FO;
            case "007" -> identificationTypeFromSchemeId(schemeId);
            case "009" -> IdentificationType.OD;
            default -> throw new IllegalArgumentException(
                "Kein Mapping von Code %s zu IdentificationType".formatted(code)
            );
        };
    }

    public static RelationTyp relationTypFromSchemeId(String schemeId) {
        return switch (schemeId) {
            case SCHEME_ID_OE -> RelationTyp.ORGANISATION;
            case SCHEME_ID_OD -> RelationTyp.ONLINESERVICE;
            case SCHEME_ID_FO -> RelationTyp.FORM;
            default -> throw new IllegalArgumentException(
                String.format(
                    "SchemeId '%s' einer Zuständigkeit kann nicht verarbeitet werden",
                    schemeId
                )
            );
        };
    }

    public static IdentificationType identificationTypeFromSchemeId(String schemeId) {
        return switch (schemeId) {
            case SCHEME_ID_OD -> IdentificationType.ZSOD;
            case SCHEME_ID_OE -> IdentificationType.ZSOE;
            case SCHEME_ID_FO -> IdentificationType.ZSFO;
            default -> throw new IllegalArgumentException(
                "Konnte keinen Zuständigkeitstyp aus der SchemeID ermitteln."
            );
        };
    }

    public static RelationTyp relationTypFromCode(String code) {
        return switch (code) {
            case "001" -> RelationTyp.FORM;
            case "003" -> RelationTyp.ONLINESERVICE;
            case "004" -> RelationTyp.ORGANISATION;
            default -> throw new IllegalArgumentException(
                String.format(
                    "Code.TransferZustaendigkeitTyp '%s' einer Zuständigkeit kann nicht verarbeitet werden",
                    code
                )
            );
        };
    }

    public static String transferZsTypCodeFromSchemeId(String schemeId) {
        return switch (schemeId) {
            case SCHEME_ID_OE -> "004";
            case SCHEME_ID_OD -> "003";
            case SCHEME_ID_FO -> "001";
            default -> throw new IllegalArgumentException(
                String.format(
                    "SchemeId '%s' einer Zuständigkeit kann nicht verarbeitet werden",
                    schemeId
                )
            );
        };
    }

    public static AccessChannel accessChannelFromCode(String code) {
        return switch (code) {
            case "100" -> AccessChannel.ONLINE;
            case "200" -> AccessChannel.OFFLINE;
            case null, default -> AccessChannel.ONLINE_OFFLINE;
        };
    }

    public static <T> T castClass(Object obj) {
        @SuppressWarnings("unchecked")
        var casted = (T) obj;
        return casted;
    }
}
