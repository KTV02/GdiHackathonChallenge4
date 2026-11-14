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
package de.dataport.suchdienst.importer.model.jzufi;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Representation einer Organisationseinheit im Suchdienst
 * Wird eindeutig identifiziert durch
 * - ID der Organisationseinheit
 * - Relation zu einer Leistungsbeschreibung
 * - Liste an ARS (arsFederalstate)
 * <p>
 * Die Form der ID-Bildung soll sicherstellen, dass mehrere OrganisationUnit-Objekte zu einer ID in einem `Set`
 * gehalten werden können, um diese vor dem Upsert in ElasticSearch zusammenzufügen.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "version",
    defaultImpl = OrganisationseinheitEntity22.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrganisationseinheitEntity22.class, name = "XZUFI22"),
    @JsonSubTypes.Type(value = OrganisationseinheitEntity23.class, name = "XZUFI23"),
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("java:S7027") // aufgrund von sealed and permits ein sonar false positive
public abstract sealed class OrganisationseinheitEntity
    implements MetaData, UpdatableEntity
    permits OrganisationseinheitEntity22, OrganisationseinheitEntity23
{

    @EqualsAndHashCode.Include
    private String id;

    /**
     * Enthält den Ars des Bundeslandes. Wenn die OE von einem nicht-Bundesland-Redaktionssystem geliefert wird (z.B. IHK), dann ist die Liste leer.
     */
    private Set<String> arsFederalState = new HashSet<>();

    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    /**
     * Pflichtfeld!
     * Zeitstempel des ImportRequests der zum Upsert der Entität geführt hat. Wird benötigt, zum zu prüfen, ob
     * ein reopened ImportRequest diese Entität ändern darf, siehe PV-7703.
     */
    private final Instant lastUpdate;

    private TransactionMetadata metaData;
}
