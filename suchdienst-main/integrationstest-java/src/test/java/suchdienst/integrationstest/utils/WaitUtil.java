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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;

public class WaitUtil {

    private static final Duration LOG_POLL_INTERVAL = Duration.ofMillis(200);

    /**
     * Wartet Statisch in der Einheit <pre>unit</pre> angegebene Zeit <pre>delay</pre>.
     * Achtung: Die maximal mögliche Wartezeit liegt bei 60 Sekunden!
     *
     * @param delay - Anzahl an Einheiten die Gewartet werden soll
     * @param unit  - Zeiteinheit (z.B. {@link TimeUnit#SECONDS}
     */
    public static void waitFor(int delay, TimeUnit unit) {
        await("Static Wait")
            .timeout(60, TimeUnit.SECONDS)
            .pollDelay(delay, unit)
            .until(() -> true);
    }

    public static void waitForLeistungskatalogSuccessfulImported(ArtifactWriter writer) {
        String transferOperationenBeendet =
            "Update der Verwaltungsleistungen nach Leistungskatalog Update abgeschlossen";

        await("Leistungskatalog wird erfolgreich importiert")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(writer::getCurrentServerLog, hasItem(containsString(transferOperationenBeendet)));
    }

    public static void waitForLeistungskatalogImportFails404(ArtifactWriter writer) {
        String catalogError = "Leistungskatalog mit URL 'http://localhost:8001/leistungskatalog' existiert nicht.";
        String errorMessage = "Fehler beim Neuberechnen der Lebenslagen";
        await("Leistungskatalog Import scheitert - Katalog existiert nicht")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(writer::getCurrentServerLog, hasItems(containsString(catalogError), containsString(errorMessage)));
    }

    public static void waitForLeistungskatalogImportFails500(ArtifactWriter writer) {
        String firstRetry =
            "Versuch 1 gescheitert: Download von Leistungskatalog mit URL 'http://localhost:8001/leistungskatalog' mit Statuscode 500 fehlgeschlagen";
        String lastRetry =
            "Versuch 3 gescheitert: Download von Leistungskatalog mit URL 'http://localhost:8001/leistungskatalog' mit Statuscode 500 fehlgeschlagen";
        String errorMessage = "Fehler beim Neuberechnen der Lebenslagen";
        await("Leistungskatalog Import scheitert - Server Error")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(
                writer::getCurrentServerLog,
                hasItems(containsString(firstRetry), containsString(lastRetry), containsString(errorMessage))
            );
    }

    public static void waitForLeistungskatalogImportRetryOK(ArtifactWriter writer) {
        String firstRetry =
            "Versuch 1 gescheitert: Download von Leistungskatalog mit URL 'http://localhost:8001/leistungskatalog' mit Statuscode 500 fehlgeschlagen";
        String erfolgreichBerechnet = "Update der Verwaltungsleistungen nach Leistungskatalog Update abgeschlossen";
        String katalogImportiert = "Leistungskatalog importiert mit 5 Leistungen";
        await("Leistungskatalog Import scheitert - Server Error")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(
                writer::getCurrentServerLog,
                hasItems(
                    containsString(firstRetry),
                    containsString(erfolgreichBerechnet),
                    containsString(katalogImportiert)
                )
            );
    }

    public static void waitForLeistungskatalogImportFails(ArtifactWriter writer) {
        String errorMessage = "Fehler beim Neuberechnen der Lebenslagen";
        await("Leistungskatalog Import scheitert")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(writer::getCurrentServerLog, hasItem(containsString(errorMessage)));
    }

    public static void waitForReopenNotPossible(ArtifactWriter writer, List<String> blockedIdList) {
        List<Matcher<Iterable<? super String>>> matcherList = new ArrayList<>();
        for (String logEntry : blockedIdList) {
            String entry = "Import von wieder eröffnetem Element mit der ID " + logEntry + " nicht erlaubt";
            matcherList.add(hasItem(containsString(entry)));
        }

        await("Alle blockierten Ids wurden ausgegeben")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(LOG_POLL_INTERVAL)
            .until(writer::getCurrentServerLog, allOf(Lists.newArrayList(matcherList)));
    }
}
