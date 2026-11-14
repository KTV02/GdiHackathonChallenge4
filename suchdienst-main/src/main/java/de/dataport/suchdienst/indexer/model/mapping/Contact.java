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
package de.dataport.suchdienst.indexer.model.mapping;

import de.dataport.suchdienst.importer.model.jzufi.ZustaendigkeitEntity;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.Publisher;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import lombok.Data;

@Data
public class Contact {

    private String title;
    private String salutation;
    private String name;
    private String lastname;

    private String room;
    private String position;
    private String speakingTimes;

    private Location location;
    private Set<ZustaendigkeitEntity> relations;
    private Collection<Link> additionalLinks;
    private Publisher publisher;

    private boolean oeffentlichAnzeigen = true;

    public Contact() {
        additionalLinks = Collections.emptySet();
        relations = Collections.emptySet();
    }
}
