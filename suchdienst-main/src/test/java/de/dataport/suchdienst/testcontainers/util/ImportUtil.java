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
package de.dataport.suchdienst.testcontainers.util;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import de.dataport.pvog.testdatengenerator.generator.XzufiGenerator;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.MockMvc;

@TestComponent
public class ImportUtil {

    @Autowired
    public PushImportQueueConnector pushImportQueueConnector;

    @Autowired
    public MockMvc mockMvc;

    public void pushAndWait(XzufiFile xzufiFile) throws Exception {
        String id = push(xzufiFile);
        waitForImport(id);
    }

    public void waitForImport(String id) {
        waitForImport(id, Duration.ofSeconds(180), "DONE");
    }

    public void waitForImport(String id, Duration maxDuration, String status) {
        await("Warten auf Warteschlange")
            .atMost(maxDuration)
            .with()
            .pollInterval(Duration.ofMillis(1000))
            .until(
                () ->
                    pushImportQueueConnector.getQueueStatus(
                        Objects.requireNonNull(id),
                        Objects.requireNonNull(status)
                    ),
                is(1)
            );
        await("Warten auf Neuberechnung")
            .atMost(maxDuration)
            .with()
            .pollInterval(Duration.ofMillis(1000))
            .until(pushImportQueueConnector::getUpToDate, is("true"));
        // Warte statisch 1 Sekunde nachdem die Queue Verarbeitung abgeschlossen wurde
        await("Warte auf Verarbeitung")
            .pollDelay(Duration.ofSeconds(1))
            .until(() -> true);
    }

    public String push(XzufiFile xzufiFile) throws Exception {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile);
        return push(messages);
    }

    public String push(List<SaDMessage> messages) throws Exception {
        String id = null;
        for (SaDMessage message : messages) {
            id = pushImportQueueConnector.pushXzufi(message);
        }
        return id;
    }
}
