/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RedisUtil {

    /**
     * Löscht die Redis-Datenbank, um einen sauberen Zustand vor jedem Test sicherzustellen.
     *
     * @throws RuntimeException wenn redis-cli nicht gefunden wird, ein Timeout auftritt oder ein Exit-Code ungleich Null zurückgegeben wird
     * @throws InterruptedException wenn der Thread während des Wartens unterbrochen wird
     */
    public static void flushRedisCache() throws InterruptedException {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String port = System.getenv().getOrDefault("REDIS_PORT", "6379");

        ProcessBuilder pb = new ProcessBuilder("redis-cli", "-h", host, "-p", port, "FLUSHDB");

        final Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Could not start redis-cli: " + e.getMessage(), e);
        }

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("redis-cli FLUSHDB timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("redis-cli FLUSHDB failed with exit code " + exitCode);
        }
    }
}
