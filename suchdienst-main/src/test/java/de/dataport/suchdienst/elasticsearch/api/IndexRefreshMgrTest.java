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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class IndexRefreshMgrTest {

    private ElasticsearchClient client;
    private ElasticsearchIndicesClient indicesClient;
    private IndexConfiguration indexConfiguration;
    private IndexRefreshMgr mgr;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        indicesClient = mock(ElasticsearchIndicesClient.class);
        indexConfiguration = mock(IndexConfiguration.class);
        when(client.indices()).thenReturn(indicesClient);
        mgr = new IndexRefreshMgr(client, indexConfiguration);
    }

    @Test
    void ensureFresh_whenDirty_fuehrtRefreshAus() throws IOException {
        mgr.markAsDirty("my-index");
        mgr.ensureFresh("my-index");

        verify(indicesClient).refresh(any(Function.class));
    }

    @Test
    void ensureFresh_whenClean_ueberspringtRefresh() throws IOException {
        mgr.ensureFresh("my-index");

        verify(indicesClient, never()).refresh(any(Function.class));
    }

    @Test
    void ensureFresh_nachZweiMarksDirty_refreshtNurEinmal() throws IOException {
        mgr.markAsDirty("my-index");
        mgr.markAsDirty("my-index"); // Idempotenz: Mehrfaches Markieren darf keinen zusätzlichen Refresh auslösen.
        mgr.ensureFresh("my-index");
        mgr.ensureFresh("my-index"); // zweiter Aufruf: Index ist clean, kein Refresh

        verify(indicesClient, times(1)).refresh(any(Function.class));
    }

    @Test
    void ensureFresh_beiRefreshFehler_bleibtIndexDirty() throws IOException {
        when(indicesClient.refresh(any(Function.class))).thenThrow(
            new IOException("ES nicht erreichbar")
        );

        mgr.markAsDirty("my-index");
        mgr.ensureFresh("my-index"); // Refresh schlägt fehl

        // Index muss dirty geblieben sein → zweites ensureFresh versucht erneut
        mgr.ensureFresh("my-index");
        verify(indicesClient, times(2)).refresh(any(Function.class));
    }

    @Test
    @Timeout(10)
    // await(),until kann hier nicht verwendet werden,
    // da wir darauf warten, dass ein Lock gesetzt wird,
    // welches private innerhalb des IndexRefreshMgrs liegt
    @SuppressWarnings("java:S2925")
    void ensureDirty_waehrendRefresh_wartetUndSetztDanachDirty() throws Exception {
        CountDownLatch refreshGestartet = new CountDownLatch(1);
        CountDownLatch refreshFreigabe = new CountDownLatch(1);

        when(indicesClient.refresh(any(Function.class))).thenAnswer(inv -> {
            refreshGestartet.countDown();
            refreshFreigabe.await();
            return null;
        });

        mgr.markAsDirty("my-index");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Thread 1: startet ensureFresh (blockiert im I/O-Mock)
            executor.submit(() -> mgr.ensureFresh("my-index"));

            // Warten bis der Refresh-Thread den I/O-Call begonnen hat
            assertTrue(
                refreshGestartet.await(5, TimeUnit.SECONDS),
                "Refresh startete nicht rechtzeitig"
            );

            // Thread 2: ensureDirty muss hinter dem Lock warten
            CountDownLatch dirtyGesetzt = new CountDownLatch(1);
            executor.submit(() -> {
                mgr.markAsDirty("my-index");
                dirtyGesetzt.countDown();
            });

            // Thread 2 sollte noch blockiert sein, da der Lock gehalten wird
            // Kurze Wartezeit um sicherzustellen, dass Thread 2 den Lock versucht zu erwerben
            Thread.sleep(50);

            // Refresh abschließen → Thread 2 kann Lock erwerben und dirty setzen
            refreshFreigabe.countDown();

            assertTrue(
                dirtyGesetzt.await(5, TimeUnit.SECONDS),
                "markAsDirty schloss nicht rechtzeitig ab"
            );

            // Jetzt ist der Index wieder dirty → ensureFresh muss erneut refreshen
            mgr.ensureFresh("my-index");
            verify(indicesClient, times(2)).refresh(any(Function.class));
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @Timeout(10)
    void ensureFresh_verschiedeneIndizes_blockierenSichNichtGegenseitig() throws Exception {
        CountDownLatch index1Gestartet = new CountDownLatch(1);
        CountDownLatch index1Freigabe = new CountDownLatch(1);

        when(indicesClient.refresh(any(Function.class))).thenAnswer(inv -> {
            index1Gestartet.countDown();
            index1Freigabe.await();
            return null;
        });

        mgr.markAsDirty("index-1");
        // index-2 bewusst nicht dirty gesetzt

        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        ExecutorService executor2 = Executors.newSingleThreadExecutor();
        try {
            executor1.submit(() -> mgr.ensureFresh("index-1"));

            assertTrue(
                index1Gestartet.await(5, TimeUnit.SECONDS),
                "index-1 Refresh startete nicht rechtzeitig"
            );

            // ensureFresh für index-2 (clean) darf nicht durch den Lock von index-1 blockiert werden
            CountDownLatch index2Fertig = new CountDownLatch(1);
            executor2.submit(() -> {
                mgr.ensureFresh("index-2");
                index2Fertig.countDown();
            });

            assertTrue(
                index2Fertig.await(2, TimeUnit.SECONDS),
                "ensureFresh für index-2 wurde durch index-1 blockiert"
            );

            index1Freigabe.countDown();
        } finally {
            executor1.shutdown();
            executor2.shutdown();
            executor1.awaitTermination(5, TimeUnit.SECONDS);
            executor2.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Szenario: T0=ensureFresh (läuft), T1=markAsDirty, T2=ensureFresh — in dieser Reihenfolge.
     * T1 muss vor T2 den Lock erwerben (fairer Lock), sodass T2 den dirty-State von T1 sieht
     * und einen zweiten Refresh auslöst.
     */
    @Test
    @Timeout(10)
    // await(),until kann hier nicht verwendet werden,
    // da wir darauf warten, dass ein Lock gesetzt wird,
    // welches private innerhalb des IndexRefreshMgrs liegt
    @SuppressWarnings("java:S2925")
    void ensureFresh_T2_nachMarkAsDirty_T1_loestZweitenRefreshAus() throws Exception {
        CountDownLatch refreshGestartet = new CountDownLatch(1);
        CountDownLatch refreshFreigabe = new CountDownLatch(1);
        CountDownLatch t1InQueue = new CountDownLatch(1);
        CountDownLatch t2InQueue = new CountDownLatch(1);

        when(indicesClient.refresh(any(Function.class))).thenAnswer(inv -> {
            refreshGestartet.countDown();
            refreshFreigabe.await();
            return null;
        });

        mgr.markAsDirty("my-index");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            // T0: ensureFresh startet und hält den Lock während I/O
            executor.submit(() -> mgr.ensureFresh("my-index"));
            assertTrue(refreshGestartet.await(5, TimeUnit.SECONDS), "T0 Refresh startete nicht");

            // T1: markAsDirty — wartet hinter T0 im Lock
            executor.submit(() -> {
                t1InQueue.countDown();
                mgr.markAsDirty("my-index");
            });
            assertTrue(t1InQueue.await(5, TimeUnit.SECONDS), "T1 erreichte Queue nicht");
            Thread.sleep(50); // T1 muss lock.lock() aufgerufen haben

            // T2: ensureFresh — wartet hinter T0 und T1 im Lock (fairer Lock: T1 vor T2)
            CountDownLatch t2Fertig = new CountDownLatch(1);
            executor.submit(() -> {
                t2InQueue.countDown();
                mgr.ensureFresh("my-index");
                t2Fertig.countDown();
            });
            assertTrue(t2InQueue.await(5, TimeUnit.SECONDS), "T2 erreichte Queue nicht");
            Thread.sleep(50); // T2 muss lock.lock() aufgerufen haben

            // T0 freigeben
            refreshFreigabe.countDown();
            assertTrue(t2Fertig.await(5, TimeUnit.SECONDS), "T2 ensureFresh schloss nicht ab");

            // Fair-Lock-Garantie: T1 (markAsDirty) lief vor T2 (ensureFresh)
            // → T2 sah isDirty=true und hat einen zweiten Refresh ausgelöst
            verify(indicesClient, times(2)).refresh(any(Function.class));
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
