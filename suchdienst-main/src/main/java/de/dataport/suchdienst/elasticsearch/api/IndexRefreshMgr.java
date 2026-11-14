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
package de.dataport.suchdienst.elasticsearch.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Diese Komponente ist dafür zuständig, Index-Refreshes so zu koordinieren, dass bei Bedarf (lesende Zugriffe auf ES aus dem Anwendungscode)
 * ein refreshIndex ausgeführt wird, aber nur wenn dies auch nötig ist.
 * Dafür enthält die Komponente pro Index eine Verwaltungsinformation, in der hinterlegt ist, ob ein refresh nötig ist ("dirty").
 * Das Dirty-Flag eines Index wird von den Methoden "markAsDirty" gesetzt, i.a. muss dies nach schreibenden Aufrufen aus dem Anwendungscode
 * aufgerufen werden.
 * Bei lesenden Aufrufen kann "ensureFresh" aufgerufen werden, dies führt dann - falls der Index dirty ist - ein refreshIndex aus.
 * <p>
 * Das ganze kann als Ersatz für den wait-for-refresh Mechanismus beim Schreiben angesehen werden, der aber im Suchdienst nicht
 * konsequent verwendet wird.
 * <p>
 * Jeder Refresh mitsamt Dauer und ein bisschen Statistik wird auf DEBUG geloggt.
 */
@Slf4j
@Component
public class IndexRefreshMgr {

    private final ElasticsearchClient client;
    private final IndexConfiguration indexConfiguration;
    private final Map<String, IndexInfo> infoMap = new ConcurrentHashMap<>(); // beachtet concurrency z.b. wegen computeIfAbsent

    public IndexRefreshMgr(ElasticsearchClient client, IndexConfiguration indexConfiguration) {
        this.client = client;
        this.indexConfiguration = indexConfiguration;
    }

    public void markAsDirty(IndexNames... indexNames) {
        for (IndexNames idx : indexNames) {
            String indexName = indexConfiguration.getCurrentIndexName(idx);
            ensureDirty(indexName);
        }
    }

    public void markAsDirty(String... indexNames) {
        for (String indexName : indexNames) {
            ensureDirty(indexName);
        }
    }

    public void ensureFresh(IndexNames... indexNames) {
        for (IndexNames idx : indexNames) {
            String indexName = indexConfiguration.getCurrentIndexName(idx);
            ensureFresh(indexName);
        }
    }

    public void ensureFresh(String... indexNames) {
        for (String indexName : indexNames) {
            ensureFresh(indexName);
        }
    }

    private void ensureFresh(String indexName) {
        IndexInfo info = infoMap.computeIfAbsent(indexName, IndexInfo::new);
        ReentrantLock lock = info.getLock();
        lock.lock();
        try {
            if (info.isDirty()) {
                refreshIndex(info);
                info.markAsClean();
            }
        } catch (IOException | ElasticsearchException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Refresh des Index {}.", info.getIndexName(), e);
        } finally {
            lock.unlock();
        }
    }

    private void ensureDirty(String indexName) {
        IndexInfo info = infoMap.computeIfAbsent(indexName, IndexInfo::new);
        ReentrantLock lock = info.getLock();
        lock.lock();
        try {
            info.markAsDirty();
        } finally {
            lock.unlock();
        }
    }

    private void refreshIndex(IndexInfo indexInfo) throws IOException, ElasticsearchException {
        client.indices().refresh(r -> r.index(indexInfo.getIndexName()));
    }

    @Getter
    private static class IndexInfo {

        private final String indexName;
        private final ReentrantLock lock = new ReentrantLock(true); // fair: garantiert FIFO-Reihenfolge für ensureDirty/ensureFresh
        private boolean isDirty;

        public IndexInfo(String indexName) {
            this.indexName = indexName;
            this.isDirty = false;
        }

        public void markAsDirty() {
            this.isDirty = true;
        }

        public void markAsClean() {
            this.isDirty = false;
        }
    }
}
