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
package de.dataport.suchdienst.suchen.model.helper;

import de.dataport.suchdienst.util.ServiceModuleCode;
import java.util.Locale;

/**
 * @deprecated Wird perspektivisch entfernt
 */
@Deprecated(forRemoval = true)
public class MustContain {

    private String titleGerman;
    private String titleEnglish;
    public static final String INFO_GERMAN = "nicht angegeben";
    public static final String INFO_ENGLISH = "not specified";

    public MustContain(ServiceModuleCode code) {
        switch (code) {
            case FRIST -> {
                this.titleGerman = "Fristen";
                this.titleEnglish = "Deadlines";
            }
            case KOSTEN -> {
                this.titleGerman = "Kosten & Gebühren";
                this.titleEnglish = "Expenses & Charges";
            }
            case VOLLTEXT -> {
                this.titleGerman = "Beschreibung";
                this.titleEnglish = "Description";
            }
            case VORAUSSETZUNGEN -> {
                this.titleGerman = "Voraussetzungen";
                this.titleEnglish = "Prerequisites";
            }
            case ERFORDERLICHE_UNTERLAGEN -> {
                this.titleGerman = "Erforderliche Unterlagen";
                this.titleEnglish = "Required documents";
            }
            case RECHTSGRUNDLAGEN -> {
                this.titleGerman = "Rechtsgrundlage(n)";
                this.titleEnglish = "Legal basis(s)";
            }
            case URHEBER -> {
                this.titleGerman = "Urheber";
                this.titleEnglish = "Copyright";
            }
            case ZUSTAENDIGE_STELLE -> {
                this.titleGerman = "Zuständige Stelle";
                this.titleEnglish = "Competent office";
            }
            case ANSPRECHPUNKT -> {
                this.titleGerman = "Ansprechpunkt";
                this.titleEnglish = "Contact point";
            }
            case TEASER -> {
                this.titleGerman = "Kurzinfo";
                this.titleEnglish = "Quick info";
            }
            default -> {
                // no titles
            }
        }
    }

    public String getTitle(Locale locale) {
        return Locale.GERMAN.equals(locale) ? titleGerman : titleEnglish;
    }

    public String getInformation(Locale locale) {
        return Locale.GERMAN.equals(locale) ? INFO_GERMAN : INFO_ENGLISH;
    }

    public static MustContain create(ServiceModuleCode code) {
        return new MustContain(code);
    }
}
