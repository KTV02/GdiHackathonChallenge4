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
package de.dataport.suchdienst.suchen.model.out.leistung.detail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.dataport.suchdienst.suchen.model.out.common.HyperLink;
import de.dataport.suchdienst.suchen.model.out.leistung.AccessChannel;
import de.dataport.suchdienst.suchen.model.out.leistung.XZufiCategory;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import lombok.Data;

/**
 * Gemeinsamer Vertrag der Leistungsdetail-Module.
 * Die Untertypen beschränken {@code @type} mit Patterns statt einwertigen Enums,
 * damit der generierte oneOf-Vertrag einheitlich {@code String getAtType()} verwendet.
 * Der negative Lookahead am Patternende verbietet auch einen abschließenden Zeilenumbruch.
 */
@Schema(
    subTypes = { ServiceModule.class, ServiceModuleDetail.class, ServiceModuleDate.class },
    discriminatorMapping = {
        @DiscriminatorMapping(value = "_module", schema = ServiceModule.class),
        @DiscriminatorMapping(value = "_moduleDetail", schema = ServiceModuleDetail.class),
        @DiscriminatorMapping(value = "_moduleDate", schema = ServiceModuleDate.class),
    },
    discriminatorProperty = "@type"
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceModule.class, name = "_module"),
    @JsonSubTypes.Type(value = ServiceModuleDetail.class, name = "_moduleDetail"),
    @JsonSubTypes.Type(value = ServiceModuleDate.class, name = "_moduleDate"),
})
@Data
@SuppressWarnings("java:S7027") // aufgrund von sealed and permits ein sonar false positive
public abstract sealed class AbstractServiceModule
    permits ServiceModule, ServiceModuleDetail, ServiceModuleDate
{

    @JsonProperty("@type")
    @Schema(
        description = "Discriminator zur Feststellung des spezifischen Subtyps",
        accessMode = Schema.AccessMode.READ_ONLY,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String type;

    @JsonIgnore
    public void setType(String type) {} // Verwaltung durch Jackson JsonSubTypes Annotation, darf nicht überschrieben werden

    @Schema(
        description = "Inhalt der Codeliste für den Code in leikaTextmodul",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @Schema(description = "inhalt, xzufi:String.Localized")
    private String text;

    @Schema(description = "weiterfuehrenderLink, xzufi:HyperlinkErweitert")
    private Collection<HyperLink> links;

    @Schema(description = "XZuFi Kategorie", example = "TEXT_MODUL")
    @JsonProperty("xZufiCategory")
    private XZufiCategory category;

    @Schema(
        description = "leikaTextmodul, xzufi:Code.Leistungstextmodul",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    @Schema(
        description = "Zeigt den Zugangskanal des Moduls",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    AccessChannel accessChannel;
}
