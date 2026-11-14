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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Exponentielles Backoff mit Jitter im Bereich [interval, interval*multiplier].
 * <p>
 * Der Jitter ist wichtig, um zu verhindern, dass mehrere parallele Hintergrund-Threads
 * (z.B. die VlRecalcTasks der nächtlichen Neuberechnung) ihre Retries synchronisiert
 * absetzen und so anhaltende Ressourcenkonkurrenz oder Versionskonflikte beim
 * gleichzeitigen Schreiben/Löschen derselben Dokumente erzeugen.
 * <p>
 * Beispiel: initialInterval = 50 multiplier = 2.0 maxInterval = 3000 numRetries = 5
 * Ergebnis: [76, 151, 304, 580, 901] oder [53, 190, 267, 451, 815]
 * (zufällig verteilte Werte innerhalb der Bereiche [50-100, 100-200, 200-400, 400-800, 800-1600]).
 */
public class ExponentialRandomBackOff {

    private final double multiplier;
    private final long maxInterval;
    private final ReentrantLock lock = new ReentrantLock();
    private long interval;

    public ExponentialRandomBackOff(long initialInterval, long maxInterval, double multiplier) {
        this.interval = initialInterval;
        this.maxInterval = maxInterval;
        this.multiplier = multiplier;
    }

    public void sleep() {
        try {
            long sleepInterval = getSleepAndIncrement();
            if (sleepInterval > 0) {
                Thread.sleep(sleepInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while sleeping", e);
        }
    }

    // Die Verwendung eines PRNG ist hier vertretbar, da dieser lediglich zur Berechnung einer back-Off Zeit verwendet wird.
    // Ein möglicher Angreifer erhält keinen Vorteil daraus, den Wert vorherzusagen
    @SuppressWarnings("java:S2245")
    long getSleepAndIncrement() {
        lock.lock();
        try {
            // Jitter: Basis-Intervall mit einem Wert zwischen [1, multiplier] multiplizieren.
            long sleep = (long) (interval *
                (1 + ThreadLocalRandom.current().nextFloat() * (multiplier - 1)));
            // Basis-Intervall für die nächste Iteration erhöhen.
            this.interval = (long) Math.min(this.interval * multiplier, maxInterval);
            return Math.min(sleep, maxInterval);
        } finally {
            lock.unlock();
        }
    }
}
