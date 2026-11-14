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
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "version",
    defaultImpl = FormularEntity22.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FormularEntity22.class, name = "XZUFI22"),
    @JsonSubTypes.Type(value = FormularEntity23.class, name = "XZUFI23"),
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("java:S7027") // aufgrund von sealed and permits ein sonar false positive
public abstract sealed class FormularEntity
    implements MetaData, UpdatableEntity
    permits FormularEntity22, FormularEntity23
{

    @EqualsAndHashCode.Include
    private String id;

    private Set<String> arsFederalState = new HashSet<>();

    /**
     * Pflichtfeld!
     * Der Zeitpunkt des neuesten Upserts.
     */
    private final Instant lastUpdate;

    private TransactionMetadata metaData;
}
