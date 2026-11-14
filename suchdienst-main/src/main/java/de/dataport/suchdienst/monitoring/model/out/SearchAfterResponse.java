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
package de.dataport.suchdienst.monitoring.model.out;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
public class SearchAfterResponse<T, S> {

    private final List<T> content;

    private final S lastId;

    private final boolean isLastResponse;

    private SearchAfterResponse(@NonNull List<T> content, @NonNull Function<T, S> getId) {
        this.content = content;
        this.isLastResponse = false;

        if (!content.isEmpty()) {
            this.lastId = getId.apply(Iterables.getLast(content));
        } else {
            this.lastId = null;
        }
    }

    private SearchAfterResponse(SearchAfterResponse<T, S> response, boolean isLastResponse) {
        this.content = response.content;
        this.lastId = response.lastId;
        this.isLastResponse = isLastResponse;
    }

    public SearchAfterResponse<T, S> withLast(boolean isLast) {
        return new SearchAfterResponse<>(this, isLast);
    }

    public static <T, S> SearchAfterResponse<T, S> from(
        List<T> content,
        @NonNull Function<T, S> getId
    ) {
        return new SearchAfterResponse<>(content, getId);
    }
}
