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
package suchdienst.integrationstest.common;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Lists;
import de.dataport.pvog.testdatengenerator.creator.BundeslandConfig;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig22;
import de.dataport.pvog.testdatengenerator.generator.CodelistConfig23;
import de.dataport.pvog.testdatengenerator.generator.XzufiGenerator;
import de.dataport.pvog.testdatengenerator.generator.distributor.XzufiFileDistributor;
import de.dataport.pvog.testdatengenerator.generator.model.SaDMessage;
import de.dataport.pvog.testdatengenerator.model.XzufiFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import suchdienst.integrationstest.connectors.AdminConnector;
import suchdienst.integrationstest.connectors.ImportQueueConnector;
import suchdienst.integrationstest.connectors.LocationsConnector;

@Slf4j
public class ImportUtil {

    private static final Duration IMPORT_POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration IMPORT_IDLE_STABILITY_DURATION = Duration.ofSeconds(1);
    private static final Duration ADMIN_IMPORT_MAX_DURATION = Duration.ofSeconds(30);

    /**
     * Versendet XZUFI-XML an den Suchdienst. Erwartet ein internes Modell des PV-Testdatengenerators
     * ({@link XzufiFile}), generiert XZuFi-Dokumente im Sammlerdienst-Format und wartet auf den
     * Import im Backend. Wartet bis zu 120 Sekunden auf die abschließende Verarbeitung
     *
     * @param xzufiFile   - internes Modell als Basis fuer die Generierung
     * @param distributor - Spezifische Distributor
     */
    public static void pushAndWait(XzufiFile xzufiFile, XzufiFileDistributor distributor) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, distributor);
        waitForImports(pushAndReturnIdList(messages));
    }

    public static void pushAndWait23(XzufiFile xzufiFile, XzufiFileDistributor distributor) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, distributor);
        waitForImports(pushAndReturnIdList(messages));
    }

    /**
     * Versendet XZUFI-XML an den Suchdienst. Erwartet ein internes Modell des PV-Testdatengenerators
     * ({@link XzufiFile}), generiert XZuFi-Dokumente im Sammlerdienst-Format und wartet auf den
     * Import im Backend. Wartet bis zu 120 Sekunden auf die abschließende Verarbeitung
     *
     * @param xzufiFile   - internes Modell als Basis fuer die Generierung
     * @param distributor - Spezifische Distributor
     * @return NachrichtenID des zuletzt versendeten Objekts
     */

    public static String pushAndWaitWithId(XzufiFile xzufiFile, XzufiFileDistributor distributor) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, distributor);
        List<String> idList = pushAndReturnIdList(messages);
        waitForImports(idList);
        return idList.getLast();
    }

    /**
     * Versendet XZUFI-XML an den Suchdienst. Erwartet ein internes Modell des PV-Testdatengenerators
     * ({@link XzufiFile}), generiert XZuFi-Dokumente im Sammlerdienst-Format und wartet auf den
     * Import im Backend. Wartet bis zu 120 Sekunden auf die abschließende Verarbeitung
     *
     * @param xzufiFile      - internes Modell als Basis fuer die Generierung
     * @param codeListConfig - Spezifische Konfiguration von Codelisten
     */
    public static void pushAndWait(XzufiFile xzufiFile, CodelistConfig22 codeListConfig) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, codeListConfig);
        waitForImports(pushAndReturnIdList(messages));
    }

    public static void pushAndWait23(XzufiFile xzufiFile, CodelistConfig23 codeListConfig) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, codeListConfig);
        waitForImports(pushAndReturnIdList(messages));
    }

    /**
     * Funktioniert wie {@link ImportUtil#pushAndWait(XzufiFile, CodelistConfig22)} mit der Ausnahme, dass keine
     * eigene {@link CodelistConfig22} übergeben werden kann.
     *
     * @param xzufiFile - internes Modell als Basis fuer die Generierung
     * @see ImportUtil#pushAndWait(XzufiFile, CodelistConfig22)
     */
    public static void pushAndWait(XzufiFile xzufiFile) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile);
        waitForImports(pushAndReturnIdList(messages));
    }

    public static void pushAndWait23(XzufiFile xzufiFile) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile);
        waitForImports(pushAndReturnIdList(messages));
    }

    /**
     * Funktioniert wie {@link ImportUtil#pushAndWait(XzufiFile, CodelistConfig22)} mit der Ausnahme, dass keine
     * eigene {@link CodelistConfig22} übergeben werden kann.
     *
     * @param xzufiFile - internes Modell als Basis fuer die Generierung
     * @param config    - BundeslandConfig aus der der Producer abgeleitet wird
     * @see ImportUtil#pushAndWait(XzufiFile, CodelistConfig22)
     */
    public static void pushAndWait(XzufiFile xzufiFile, BundeslandConfig config) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, config.getProducer());
        waitForImports(pushAndReturnIdList(messages));
    }

    public static void pushAndWait23(XzufiFile xzufiFile, BundeslandConfig config) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, config.getProducer());
        waitForImports(pushAndReturnIdList(messages));
    }

    /**
     * Versendet XZUFI-XML an den Suchdienst. Erwartet ein internes Modell des PV-Testdatengenerators
     * ({@link XzufiFile}), und ein {@link XzufiFileDistributor}, generiert XZuFi-Dokumente im Sammlerdienst-Format und wartet auf den
     * Import im Backend.
     *
     * @param xzufiFile   - internes Modell als Basis fuer die Generierung
     * @param distributor - Spezifische Distributor
     * @return NachrichtenID des zuletzt versendeten Objekts
     */
    public static String push(XzufiFile xzufiFile, XzufiFileDistributor distributor) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, distributor);
        return push(messages);
    }

    public static String push23(XzufiFile xzufiFile, XzufiFileDistributor distributor) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, distributor);
        return push(messages);
    }

    /**
     * Versendet XZUFI-XML an den Suchdienst. Erwartet ein internes Modell des PV-Testdatengenerators
     * ({@link XzufiFile}), und eine {@link CodelistConfig22}, generiert XZuFi-Dokumente im Sammlerdienst-Format und wartet auf den
     * Import im Backend.
     *
     * @param xzufiFile      - internes Modell als Basis fuer die Generierung
     * @param codeListConfig - Spezifische Konfiguration von Codelisten
     * @return NachrichtenID des zuletzt versendeten Objekts
     */
    public static String push(XzufiFile xzufiFile, CodelistConfig22 codeListConfig) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, codeListConfig);
        return push(messages);
    }

    public static String push23(XzufiFile xzufiFile, CodelistConfig23 codeListConfig) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, codeListConfig);
        return push(messages);
    }

    /**
     * Funktioniert wie {@link ImportUtil#push(XzufiFile, CodelistConfig22)} mit der Ausnahme, dass keine
     * eigene {@link CodelistConfig22} übergeben werden kann.
     *
     * @param xzufiFile - internes Modell als Basis fuer die Generierung
     * @return NachrichtenID des zuletzt versendeten Objekts
     * @see ImportUtil#push(XzufiFile, CodelistConfig22)
     */
    public static String push(XzufiFile xzufiFile) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile);
        return push(messages);
    }

    public static String push23(XzufiFile xzufiFile) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile);
        return push(messages);
    }

    /**
     * Funktioniert wie {@link ImportUtil#push(XzufiFile, CodelistConfig22)} mit der Ausnahme, dass keine
     * eigene {@link CodelistConfig22} übergeben werden kann.
     *
     * @param xzufiFile - internes Modell als Basis fuer die Generierung
     * @param config    - BundeslandConfig aus der der Producer abgeleitet wird
     * @return NachrichtenID des zuletzt versendeten Objekts
     * @see ImportUtil#push(XzufiFile, CodelistConfig22)
     */
    public static String push(XzufiFile xzufiFile, BundeslandConfig config) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages(xzufiFile, config.getProducer());
        return push(messages);
    }

    public static String push23(XzufiFile xzufiFile, BundeslandConfig config) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile, config.getProducer());
        return push(messages);
    }

    public static String push(List<SaDMessage> messages) {
        return pushAndReturnIdList(messages).getLast();
    }

    public static List<String> pushAndReturnIdList23(XzufiFile xzufiFile) {
        List<SaDMessage> messages = XzufiGenerator.generateSaDMessages23(xzufiFile);
        return pushAndReturnIdList(messages);
    }

    private static List<String> pushAndReturnIdList(List<SaDMessage> messages) {
        List<String> idList = new ArrayList<>();
        for (SaDMessage message : messages) {
            String id = ImportQueueConnector.pushXzufi(message);
            idList.add(id);
        }
        return idList;
    }

    public static void waitForImport(String id) {
        waitForImport(id, Duration.ofSeconds(180), "DONE");
    }

    public static void waitForImport(String id, String status) {
        waitForImport(id, Duration.ofSeconds(120), status);
    }

    public static void waitForImport(String id, Duration maxDuration, String status) {
        waitForImportStatus(id, maxDuration, status);
        if ("DONE".equals(status)) {
            waitForImportRecalculation(maxDuration);
        }
    }

    public static void waitForImports(List<String> idList) {
        waitForImportStatus(idList.getLast(), Duration.ofSeconds(180), "DONE");
        waitForImportRecalculation(Duration.ofSeconds(180));
    }

    private static void waitForImportStatus(String id, Duration maxDuration, String status) {
        await("Warten auf Warteschlange")
            .atMost(maxDuration)
            .with()
            .pollInterval(IMPORT_POLL_INTERVAL)
            .until(
                () ->
                    ImportQueueConnector.getQueueStatus(
                        Objects.requireNonNull(id),
                        Objects.requireNonNull(status)
                    ).getInt("totalElements"),
                is(1)
            );
    }

    private static void waitForImportRecalculation(Duration maxDuration) {
        await("Warten auf stabile Neuberechnung")
            .atMost(maxDuration)
            .during(IMPORT_IDLE_STABILITY_DURATION)
            .with()
            .pollInterval(IMPORT_POLL_INTERVAL)
            .until(ImportQueueConnector::getUpToDate, is(true));
    }

    public static void waitFor(int time, TimeUnit unit) {
        await("waitFor")
            .pollDelay(time, unit)
            .until(() -> true);
    }

    public static void waitForLocationCount(String query, int expectedCount) {
        await("Warten auf Ortsdaten")
            .atMost(ADMIN_IMPORT_MAX_DURATION)
            .with()
            .pollInterval(IMPORT_POLL_INTERVAL)
            .until(
                () -> LocationsConnector.getLocationsV2(query, 10).jsonPath().getList("$").size(),
                is(expectedCount)
            );
    }

    public static void waitForLocationSuggestionCount(String query, int expectedCount) {
        await("Warten auf Ortsvorschlaege")
            .atMost(ADMIN_IMPORT_MAX_DURATION)
            .with()
            .pollInterval(IMPORT_POLL_INTERVAL)
            .until(
                () -> LocationsConnector.getLocationsSuggestionsV2(query, false).jsonPath().getList("$").size(),
                is(expectedCount)
            );
    }

    public static void waitForImportDoneForAll(List<String> idList, String acceptedAfter) {
        List<Matcher<Iterable<? super String>>> matcherList = new ArrayList<>();
        for (String id : idList) {
            matcherList.add(hasItem(id));
        }

        await("Alle Ids wurden importiert")
            .atMost(Duration.ofSeconds(30))
            .with()
            .pollInterval(IMPORT_POLL_INTERVAL)
            .until(
                () -> ImportQueueConnector.getQueueStatusByAcceptedAfter("DONE", acceptedAfter).getList("content.id"),
                allOf(Lists.newArrayList(matcherList))
            );
        waitForImportRecalculation(Duration.ofSeconds(30));
    }

    /**
     * Ersetzt die ARS-Werte im Backend durch die ARS_init.txt Datei.
     * Dadurch werden auch die Änderungen durch PLZ-Dateien zurückgesetzt.
     */
    @SneakyThrows
    public static void resetArsData() {
        Path arsBasis = Paths.get(ClassLoader.getSystemResource("./upload/ARS_init.txt").toURI());
        AdminConnector.uploadArs(arsBasis.toFile());
        ImportUtil.waitFor(2, TimeUnit.SECONDS);
    }
}
