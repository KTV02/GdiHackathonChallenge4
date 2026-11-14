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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
@JsonTypeName("ResultPage")
// Wir müssen Equals nicht in der Subclass überschreiben, da das zur
// Superclass hinzugefügte Attribut meta nicht für equlas berücksichtigt werden soll
@SuppressWarnings("java:S2160")
public class ResultPageWithMeta<T, M extends Serializable> extends ResultPage<T> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode.Exclude
    private M meta;

    /**
     * Custom Page Implementierung. Nur nutzen, wenn die Gesamtmenge der Daten bekannt ist
     * und der Inhalt bereits paginiert ist.*
     *
     * @param content  the content (only the data requested, i.e. already paged)
     * @param total    the total amount (of possible results)
     * @param page     the requested page (offset)
     * @param pageSize the size of the page to be returned
     */
    public ResultPageWithMeta(List<T> content, long total, int page, int pageSize) {
        super(content, total, page, pageSize);
    }

    public ResultPageWithMeta<T, M> withMeta(@NonNull M meta) {
        this.meta = meta;
        return this;
    }
}
