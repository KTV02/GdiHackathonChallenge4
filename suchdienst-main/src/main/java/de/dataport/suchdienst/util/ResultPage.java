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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * @deprecated Soll in Zukunft durch PagedModel ersetzt werden.
 * Gibt ein pageable Antwortobjekt mit generischem Inhalt zurück.
 * Bei der Serialisierung sollen keine internen, projektfremdem Klassen verwendet werden.
 * @param <T>
 */
@Deprecated(forRemoval = true)
@EqualsAndHashCode
public class ResultPage<T> implements Serializable {

    private final List<T> content;
    private final long total;

    @JsonIgnore
    @Getter
    private final Pageable pageable;

    /**
     * Custom Page Implementierung. Nur nutzen, wenn die Gesamtmenge der Daten bekannt ist
     * und der Inhalt bereits paginiert ist.*
     *
     * @param content  the content (only the data requested, i.e. already paged)
     * @param total    the total amount (of possible results)
     * @param page     the requested page (offset)
     * @param pageSize the size of the page to be returned
     */
    public ResultPage(List<T> content, long total, int page, int pageSize) {
        Assert.notNull(content, "Content must not be null!");
        Assert.isTrue(total > -1, "Total count must be greater or equal 0");

        if (content.size() > total) {
            total = content.size();
        }

        this.content = content;
        this.total = total;
        this.pageable = calcPageRequest(page, pageSize, total);
    }

    /**
     * Custom Page Implementierung. Nur nutzen, wenn die Gesamtmenge der Daten bekannt ist
     * und der Inhalt bereits paginiert ist.*
     *
     * @param content  the content (only the data requested, i.e. already paged)
     * @param total    the size of the page to be returned
     * @param page     the requested page (offset)
     * @param pageSize the size of the page to be returned
     * @param <T>      the type of the content elements
     * @return ResultPage
     */
    public static <T> ResultPage<T> from(List<T> content, long total, int page, int pageSize) {
        return new ResultPage<>(content, total, page, pageSize);
    }

    /**
     * Static factory method for custom page implementation, only use for tasks where the total amount
     * of data is known and the content is already paged
     *
     * @param content     the content (only the data requested, i.e. already paged)
     * @param total       the size of the page to be returned
     * @param pageRequest the original page request
     * @param <T>         the type of the content elements
     * @return ResultPage
     */
    public static <T> ResultPage<T> from(List<T> content, long total, PageRequest pageRequest) {
        return new ResultPage<>(
            content,
            total,
            pageRequest.getPageNumber(),
            pageRequest.getPageSize()
        );
    }

    public <M extends Serializable> ResultPageWithMeta<T, M> toMetaResultPage(M meta) {
        return new ResultPageWithMeta<T, M>(
            this.getContent(),
            this.getTotalElements(),
            this.getNumber(),
            this.getSize()
        ).withMeta(meta);
    }

    @JsonProperty("pageable")
    public ResultPagePageable getResultPagePageable() {
        return new ResultPagePageable(
            pageable.isUnpaged(),
            pageable.getPageNumber(),
            pageable.isPaged(),
            pageable.getPageSize(),
            pageable.getOffset(),
            new ResultPagePageableSort(
                pageable.getSort().isSorted(),
                pageable.getSort().isUnsorted(),
                pageable.getSort().isEmpty()
            )
        );
    }

    public int getTotalPages() {
        return calcTotalPages(pageable.getPageSize(), total);
    }

    public long getTotalElements() {
        return this.total;
    }

    @JsonIgnore
    public int getNumber() {
        return this.pageable.getPageNumber();
    }

    @JsonIgnore
    public int getSize() {
        return this.pageable.getPageSize();
    }

    public int getNumberOfElements() {
        return this.content.size();
    }

    @NonNull
    public List<T> getContent() {
        return this.content;
    }

    public boolean hasContent() {
        return this.content != null && !this.content.isEmpty();
    }

    @NonNull
    @JsonIgnore
    @Schema(hidden = true)
    public Sort getSort() {
        return this.pageable.getSort();
    }

    public boolean isFirst() {
        return pageable.getPageNumber() == 0;
    }

    public boolean isLast() {
        if (pageable.getPageSize() > total) {
            return true;
        }

        int pageNumber = pageable.getPageNumber();
        int totalPages = this.getTotalPages();

        if (pageNumber >= totalPages) {
            return pageNumber == totalPages;
        }
        return pageNumber == totalPages - 1;
    }

    public boolean isEmpty() {
        return this.content.isEmpty();
    }

    public boolean hasNext() {
        return !this.isLast();
    }

    public boolean hasPrevious() {
        return !this.isFirst();
    }

    @NonNull
    public Pageable nextPageable() {
        return this.hasNext() ? this.pageable.next() : this.getPageable();
    }

    @NonNull
    public Pageable previousPageable() {
        return this.pageable.previousOrFirst();
    }

    @NonNull
    public <U> ResultPage<U> map(@NonNull Function<? super T, U> converter) {
        return new ResultPage<>(
            this.content.stream().map(converter).toList(),
            total,
            this.pageable.getPageNumber(),
            this.getPageable().getPageSize()
        );
    }

    @NonNull
    public Iterator<T> iterator() {
        return this.content.iterator();
    }

    private static int calcTotalPages(int pageSize, long total) {
        return (int) Math.ceil(total / (double) pageSize);
    }

    private static PageRequest calcPageRequest(int page, int pageSize, long total) {
        if ((long) page * pageSize > total) {
            if (pageSize >= total) {
                page = (int) Math.floor(total / (double) pageSize);
            } else {
                page = (int) Math.ceil(total / (double) pageSize);
            }
        }
        return PageRequest.of(page, pageSize);
    }

    @JsonSerialize
    record ResultPagePageableSort(boolean sorted, boolean unsorted, boolean empty) {}

    @JsonSerialize
    record ResultPagePageable(
        boolean unpaged,
        long pageNumber,
        boolean paged,
        long pageSize,
        long offset,
        ResultPagePageableSort sort
    ) {}
}
