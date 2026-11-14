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
package de.dataport.suchdienst.indexer.model.verwaltungsleistung;

import de.dataport.suchdienst.indexer.model.Identifiable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class Verwaltungsleistung implements Identifiable {

    private String id;
    private Instant lastUpdate;
    private Set<String> ars = new HashSet<>();
    // Enthält nur die ARS die genauso vom Sammlerdienst geliefert wurden
    private Set<String> exactArs = new HashSet<>();

    private LeistungsbeschreibungVL leistungsbeschreibung;
    private List<OrganisationseinheitVL> organisationseinheiten = new ArrayList<>();
    private List<OnlinedienstVL> onlinedienste = new ArrayList<>();
    private List<FormularVL> formulare = new ArrayList<>();
    private List<SpezialisierungVL> spezialisierungen = new ArrayList<>();

    /**
     * Sprachabhängige Felder für die Suche nach Verwaltungsleistungen.
     */
    private Map<Locale, SearchTerms> searchTerms = new HashMap<>();

    /**
     * Es ist möglich, dass eine leere VL durch unseren Algorithmus erzeugt wird. Dazu müssen bei exakter Ars die OE und die anhängigen
     * geschachtelten Formulare auf unterschiedlichen ARS-Ebenen angeliefert werden und sonst keine BOs vorhanden sein in dem ARS.
     * Dann entstünde für die Ebene der geschachtelten Formulare eine leere VL. Vielleicht ist das ein akademischer Fall, in den Unit-Tests
     * trat er jedenfalls auf.
     * Leere VLs werden am Ende der Berechnungen ausgefiltert, siehe VerwaltungsleistungsBuilder.
     */
    public boolean isEmpty() {
        return (
            organisationseinheiten.isEmpty() &&
            onlinedienste.isEmpty() &&
            formulare.isEmpty() &&
            spezialisierungen.isEmpty()
        );
    }

    public String contextString() {
        return String.format(
            "VL[id=%s, lbId=%s, arsSize=%d, oeSize=%d, odSize=%d, foSize=%d, spSize=%d]",
            id,
            leistungsbeschreibung.getId(),
            ars.size(),
            organisationseinheiten.size(),
            onlinedienste.size(),
            formulare.size(),
            spezialisierungen.size()
        );
    }

    public String toString() {
        return contextString();
    }
}
