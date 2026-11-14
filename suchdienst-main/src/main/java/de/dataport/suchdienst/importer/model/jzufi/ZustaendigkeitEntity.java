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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dataport.suchdienst.importer.model.warteschlange.TransactionMetadata;
import de.dataport.suchdienst.indexer.model.mapping.RelationTyp;
import de.dataport.suchdienst.indexer.model.mapping.ResponsibilityRole;
import de.dataport.suchdienst.indexer.model.mapping.ValidityPeriod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Relation DTO Objekt welches aus der XZUFI-Relation erzeugt wird
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "version",
    defaultImpl = ZustaendigkeitEntity22.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ZustaendigkeitEntity22.class, name = "XZUFI22"),
    @JsonSubTypes.Type(value = ZustaendigkeitEntity23.class, name = "XZUFI23"),
})
@SuppressWarnings("java:S7027") // aufgrund von sealed and permits ein sonar false positive
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract sealed class ZustaendigkeitEntity
    implements MetaData, UpdatableEntity
    permits ZustaendigkeitEntity22, ZustaendigkeitEntity23
{

    @EqualsAndHashCode.Include
    private String id;

    @EqualsAndHashCode.Include
    private String lbId;

    @EqualsAndHashCode.Include
    private String ars;

    @EqualsAndHashCode.Include
    private String parentId;

    private ResponsibilityRole responsibility;
    private RelationTyp typ;

    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    private String arsFederalState;

    /**
     * Pflichtfeld!
     * Der Zeitpunkt des neuesten Upserts.
     */
    private final Instant lastUpdate;

    private TransactionMetadata metaData;

    /**
     * Methode setzt das indexierte Feld lbIdWithArs im Zuständigkeiten-Index
     */
    @JsonProperty("lbIdWithArs")
    public String getLbIdWithArs() {
        if (Objects.isNull(lbId) && Objects.isNull(ars)) {
            return null;
        } else {
            return getLbId() + "-" + getArs();
        }
    }
}
