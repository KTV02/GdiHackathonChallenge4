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
package de.dataport.suchdienst.suchen.model.out.leistung;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import de.dataport.suchdienst.suchen.model.out.common.AdditionalLink;
import de.dataport.suchdienst.suchen.model.out.common.ValidityPeriod;
import de.dataport.suchdienst.suchen.model.views.JsonViews;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Data;

@Schema(
    name = "SeDeBlock",
    title = "Service Description (detail block)",
    subTypes = { SeDeBlockModule.class, SeDeBlockModuleDetail.class },
    discriminatorMapping = {
        @DiscriminatorMapping(value = "_module", schema = SeDeBlockModule.class),
        @DiscriminatorMapping(value = "_moduleDetail", schema = SeDeBlockModuleDetail.class),
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
    @JsonSubTypes.Type(value = SeDeBlockModule.class, name = "_module"),
    @JsonSubTypes.Type(value = SeDeBlockModuleDetail.class, name = "_moduleDetail"),
})
@Data
public abstract class SeDeBlock {

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
    private Collection<AdditionalLink> links = new ArrayList<>();

    @Schema(
        description = "Zeigt an, ob ersetzende Spezialisierung",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean replace;

    @Schema(description = "XZuFi Kategorie", example = "TEXT_MODUL")
    @JsonProperty("xZufiCategory")
    private XZufiCategory category;

    @Schema(
        description = "leikaTextmodul, xzufi:Code.Leistungstextmodul",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    // ValidityPeriods scheint von der Intention durch die JsonView nicht für die Out-Modelle gedacht zu sein.
    // Leider wurden nicht alle Schnittstellen im ServiceDescriptionController mit der Annotation versehen.
    // Dadurch kommt es dazu, dass diese Property bei den betroffenen Schnittstellen ausgegeben wird.
    // FIXME Bei neuen Schnittstellen darauf achten, dass diese Property schleichend entfernt wird!
    @JsonView(JsonViews.Internal.class)
    @Schema(description = "gueltigkeit, xzufi:Zeitraum")
    private List<ValidityPeriod> validityPeriods = new ArrayList<>();

    @Schema(
        description = "Zeigt an, ob das Modul mindestens minimalen Inhalt aufweist",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    public abstract boolean isFilled();

    @Schema(
        description = "Zeigt an, ob das Modul erzeugt wurde, um Anforderungen an Vollständigkeit zu genügen"
    )
    public abstract boolean isPlaceholder();
}
