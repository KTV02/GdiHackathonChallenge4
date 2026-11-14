/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.config;

import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.converters;
import static de.dataport.suchdienst.config.OpenApiSchemaTestSupport.schemas;
import static de.dataport.suchdienst.testutil.JzufiModelTestSupport.outputRoots;

import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Testziel: Zwölf gleichzeitig erzeugte Modell-Dokumente teilen wie Springdocs Gruppen denselben
 * produktiven Resolver. OpenAPI 3.1 muss für jede Root-Reihenfolge dem
 * sequenziellen Referenzexport entsprechen, einschließlich aller Listenfelder und Untertypen.
 * Alle Referenzexporte werden vor dem Start der Worker berechnet.
 */
class ConcurrentOpenApiModelResolutionTest {

    @Test
    @Timeout(120)
    void sharedResolverPreservesEverySchemaAcrossConcurrentDocuments() throws Exception {
        var shared = converters(true);
        var roots = outputRoots();
        var orders = new ArrayList<List<Class<?>>>();
        var expected = new ArrayList<Map<String, Schema>>();
        // Eigene sequenzielle Resolver verhindern, dass der Referenzgraph denselben Parallelitätsfehler übernimmt.
        for (int i = 0; i < roots.size(); i++) {
            var order = new ArrayList<>(roots);
            Collections.rotate(order, i);
            orders.add(order);
            expected.add(schemas(converters(true), order));
        }
        // Alle zwölf Worker starten gemeinsam, damit konkurrierende Auflösungen nicht zufällig seriell ablaufen.
        var start = new CyclicBarrier(12);
        try (var executor = Executors.newFixedThreadPool(12)) {
            var results = new ArrayList<Future<Map<String, Schema>>>();
            for (int i = 0; i < 12; i++) {
                var order = orders.get(i % orders.size());
                results.add(
                    executor.submit(() -> {
                        start.await(10, TimeUnit.SECONDS);
                        return schemas(shared, order);
                    })
                );
            }
            for (int i = 0; i < results.size(); i++) {
                var actual = results.get(i).get(60, TimeUnit.SECONDS);
                OpenApiSchemaTestSupport.assertSameSchemas(
                    expected.get(i % orders.size()),
                    actual,
                    true
                );
            }
        }
    }
}
