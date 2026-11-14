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

import de.dataport.suchdienst.indexer.model.mapping.XZufiCategory;
import de.dataport.suchdienst.indexer.parser.ParserSelector;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Enum zur Definition von Service-Modul-Codes und deren zugehörigen Kategorien. Vereint
 * bestehende Enums zur Angabe von Textmodul-Codes und Category.
 */

@Getter
public enum ServiceModuleCode {
    // Name
    LEISTUNGSBEZEICHNUNG("02", XZufiCategory.TEXT_MODUL),
    // NameOtherDescription
    LEISTUNGSNAME("03", XZufiCategory.TEXT_MODUL),

    TEASER("24", XZufiCategory.TEXT_MODUL),

    VOLLTEXT("06", XZufiCategory.TEXT_MODUL),
    RECHTSGRUNDLAGEN("07", XZufiCategory.TEXT_MODUL),
    ERFORDERLICHE_UNTERLAGEN("08", XZufiCategory.TEXT_MODUL),
    VORAUSSETZUNGEN("09", XZufiCategory.TEXT_MODUL),

    VERFAHRENSABLAUF("11", XZufiCategory.TEXT_MODUL),

    FORMULARE("14", XZufiCategory.TEXT_MODUL),
    WEITERE_INFORMATIONEN("15", XZufiCategory.TEXT_MODUL),
    HINWEISE("16", XZufiCategory.TEXT_MODUL),

    URHEBER("18", XZufiCategory.TEXT_MODUL),

    ZUSTAENDIGE_STELLE("22", XZufiCategory.TEXT_MODUL),
    ANSPRECHPUNKT("23", XZufiCategory.TEXT_MODUL),
    RECHTSBEHELF("25", XZufiCategory.TEXT_MODUL),

    /**
     *  Codes für "komplexe" (nicht Text-) Module, die im Rahmen der Anforderung PV-6674
     *  eingeführt wurden.
     *  Haben keine Entsprechung in urn:xoev-de:fim:codeliste:xzufi.leistungstextmodul.
     */
    FACHLICHE_FREIGABE("93", XZufiCategory.FACHLICHE_FREIGABE),
    URSPRUNGSPORTAL("94", XZufiCategory.URSPRUNGSPORTAL_MODUL),
    BEARBEITUNGSDAUER("95", XZufiCategory.BEARBEITUNGSDAUER),
    DOKUMENT("96", XZufiCategory.DOKUMENT),
    FRIST("97", XZufiCategory.FRIST),
    KOSTEN("98", XZufiCategory.KOSTEN),

    INDIVIDUELL("NOT_DEFINED", XZufiCategory.TEXT_MODUL_INDIVIDUELL);

    private final String code;
    private final XZufiCategory category;

    @Getter(AccessLevel.NONE)
    private static final Map<String, ServiceModuleCode> CODE_TO_MODULE_MAP = new HashMap<>();

    static {
        for (ServiceModuleCode module : ServiceModuleCode.values()) {
            CODE_TO_MODULE_MAP.put(module.code, module);
        }
    }

    ServiceModuleCode(String code, XZufiCategory category) {
        this.code = code;
        this.category = category;
    }

    public static Optional<ServiceModuleCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return code.startsWith(ParserSelector.INDIVIDUAL_MODULE_PREFIX)
            ? Optional.of(INDIVIDUELL)
            : Optional.ofNullable(CODE_TO_MODULE_MAP.get(code));
    }
}
