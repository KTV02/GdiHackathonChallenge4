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
package de.dataport.suchdienst.config;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.taskmgr.service.SudTaskMgr;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "de.dataport.suchdienst")
@Data
public class SuchdienstProperties {

    /**
     * Verwaltet die Gewichtungen der einzelnen Spalten beim Suchen (nach Leistungen).
     */
    @Data
    public static class Boost {

        /**
         * Boost für die Suche nach dem Namen (Titel) einer Leistung.
         */
        float name = 1.8F;
        /**
         * Boost für die Suche nach der (Kurz-)Beschreibung einer Leistung.
         */
        float description = 1.5F;
        /**
         * Boost für die Suche nach Synonymen einer Leistung.
         */
        float synonyms = 1.2F;
        /**
         * Boost für die decompounded Suchbegriffe
         */
        float decompounder = 1.2F;
        /**
         * Boost für die englischen Suchbegriffe
         */
        float english = 1F;
    }

    /**
     * Einstellungen für den HttpClient zum Zugriff auf Elastic.
     */
    @Data
    static class Http {

        /**
         * Die Anzahl an parallelen HTTP-Verbindungen je Zielhost. <b>Achtung</b>: wird der SuD über einen
         * LoadBalancer mit mehreren ElasticSearch-Instanzen verbunden, limitiert diese Einstellung die Anzahl der Verbindungen zu allen
         * ElasticSearch-Instanzen hinter dem LoadBalancer, weil der LoadBalancer dann aus Sicht des
         * HttpClients <b>eine</b> Route ist!
         * <p>
         * Default ist 10.
         *
         * @see PoolingAsyncClientConnectionManagerBuilder#setMaxConnPerRoute(int)
         */
        int maxConnPerRoute = 10;
        /**
         * Gesamtzahl der HTTP-Verbindungen zu allen verbundenen ElasticSearch-Instanzen. <b>Achtung</b>:
         * beachte Anmerkungen zu LoadBalancer-Setup in {@link #maxConnPerRoute}!
         * <p>
         * Default ist 50.
         *
         * @see PoolingAsyncClientConnectionManagerBuilder#setMaxConnTotal(int)
         */
        int maxConnTotal = 50;

        /**
         * Wie lange eine Verbindung im Pool inaktiv sein darf, bevor sie vor der Wiederverwendung aktiv
         * geprüft wird. Das reduziert sporadische "Connection reset"-Fehler bei von ElasticSearch,
         * LoadBalancern oder Proxies bereits geschlossenen Idle-Verbindungen.
         * <p>
         * Default ist 5 Sekunden.
         *
         * @see ConnectionConfig.Builder#setValidateAfterInactivity(org.apache.hc.core5.util.TimeValue)
         */
        Duration validateAfterInactivity = Duration.ofSeconds(5L);
    }

    /**
     * Einstellungen für die Retry-Policy bei BulkRequests zu ElasticSearch
     */
    @Data
    public static class Retry {

        /**
         * Anzahl Versuche für ein Retry, die Initiale Verbindung zählt nicht mit.
         */
        int maxRetries = 4;
        /**
         * initiale Wartezeit nach dem ersten Fehlversuch
         */
        Duration initialInterval = Duration.ofSeconds(2L);
        /**
         * maximale Wartezeit
         */
        Duration maxInterval = Duration.ofSeconds(300L);
        /**
         * Faktor mit dem die Wartezeit zwischen Versuchen multipliziert wird
         */
        double multiplier = 2d;
    }

    @Data
    public static class Elastic {

        Http http = new Http();

        /**
         * Maximale Größe von Bulk-Requests, die an ElasticSearch gesendet werden. Angabe in ganzen Zahlen
         * inkl. Einheit, also z.B. 25MB.
         */
        private DataSize maxBulkRequestSize = DataSize.ofMegabytes(25L);

        /**
         * Maximale Anzahl von Requests innerhalb eines BulkRequests. Da ElasticSearch insbesondere
         * Skript-Parameter nicht bei der Berechnung der Größe von BulkRequests berücksichtigt, limitieren
         * wir zusätzlich die Anzahl.
         */
        private int maxBulkRequestActions = 1_000;

        /**
         * Maximale Anzahl an gleichzeitigen Requests, welche der {@link co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester} absendet.
         * Das Limit gilt nicht global.
         * Wenn mehrere Threads gleichzeitig Bulk-Anfragen einlasten, dann steht jedem Thread diese Anzahl an gleichzeitigen Requests zur verfügung.
         */
        private int maxConcurrentRequests = 4;
        /**
         * Socket Timeout für BulkRequests in Millisekunden
         */
        private int socketTimeout = 60_000;

        /**
         * Timeout für das Herstellen einer neuen TCP Verbindung (Handshake, ggf. TLS) in Millisekunden.
         * Kürzer halten als socketTimeout. Zu niedrig führt bei Netzwerkspikes zu vermeidbaren Fehlern.
         */
        private int connectTimeout = 5_000;

        /**
         * Timeout für das Anfordern einer Verbindung aus dem Connection-Pool (falls alle belegt). In Millisekunden.
         * Verhindert unendliches Warten bei Pool-Exhaustion.
         */
        private int connectionRequestTimeout = 2_000;

        Retry retry = new Retry();

        /**
         * Die Batch-Größe bei Reindex-Operationen während der Index-Migration.
         */
        private int reindexBatchSize = 50;

        /**
         * Anzahl der Reindex-Requests pro Sekunde während der Index-Migration, Default ist kein Throttling.
         * Siehe https://www.elastic.co/guide/en/elasticsearch/reference/master/docs-reindex.html#docs-reindex-throttle
         */
        private float reindexRequestsPerSecond = -1L;
    }

    @Data
    static class Evolution {

        /**
         * Wie oft maximal bei einem Fehler versucht werden die Indexevolution durchzuführen
         */
        private int retries = 5;

        /**
         * Das Zeitintervall in Minuten für das die Retries gelten
         */
        private int timeRange = 10;
    }

    @Data
    static class Instance {

        /**
         * Gibt an um was für eine Instanz des Suchdienstes es sich handelt. Entweder MASTER oder SLAVE.
         * Dabei darf es insgesamt nur einen MASTER geben.
         */
        private InstanceType type;
    }

    /**
     * Einstellungen für Webservices des Bundesamtes für Kartographie und Geodaesie (BKG)
     */
    @Data
    public static class BKG {

        /**
         * URL zur Suche des BKG exkl. Authentifizierungsinformationen (Aufbau der URL:
         * https://hostName/Pfad__{kennung}/Pfad). Die UUID zur Authentifizierung in der URL muss durch
         * <code>{kennung}</code> ersetzt werden (inklusive der geschweiften Klammern!).
         */
        private String gdzGeokodierungBasisUrl =
            "https://sg.geodatenzentrum.de/gdz_geokodierung_bund__{kennung}";

        /**
         * Die Kennung für die Authentifizierung beim BKG (wird in die URL substituiert).
         */
        private String password;

        /**
         * Anzahl an max. parallelen Connections zum BKG.
         */
        private int maxConnections = 32;

        /**
         * Lebensdauer (TTL) von Cache-Einträgen (Antworten vom BKG dürfen nicht "dauerhaft" gespeichert
         * werden).
         */
        private Duration ttl = Duration.of(14, ChronoUnit.DAYS);

        /**
         * Timeout für das Auflösen der Future geschachtelter BKG-Aufrufe.
         */
        private Duration keyResolutionTimeout = Duration.of(10, ChronoUnit.SECONDS);

        /**
         * Timeout für einzelne BKG-Aufrufe.
         */
        private Duration requestTimeout = Duration.of(5, ChronoUnit.SECONDS);

        /**
         * Expression für Cron-Trigger zum periodischen Abruf der XRepo-Codeliste (Gemeindeverband).
         */
        private String periodicXoevArsRefreshCronExp = "0 30 2 * * 6";

        /**
         * Schalter für das Herausfiltern von BKG-Treffern, die keine Übereinstimmung mit der XRepo-Codeliste (Gemeindeverband) haben.
         */
        private boolean nonCompliantXoevArsFilterActive = true;

        public boolean isActive() {
            return StringUtils.isNotBlank(password);
        }
    }

    @Data
    public static class Importer {

        /**
         * Anzahl an Threads für gleichzeitig ausgeführte Tasks im {@link SudTaskMgr}
         */
        private int taskMgrThreadCount = 5;
        /**
         * Wartezeit bis zum erneuten Polling der persistenten Queue, wenn die InMemory-Queue leer gelaufen
         * ist.
         */
        private Duration delayBetweenQueuePolls = Duration.of(5, ChronoUnit.SECONDS);
        /**
         * Wartezeit bis zum erneuten Bereinigen der persistenten Queue.
         */
        private Duration delayBetweenQueueInvalidations = Duration.of(1, ChronoUnit.DAYS);
        /**
         * Initiale Wartezeit nach dem Neustart bis zur ersten Queue-Bereinigung
         */
        private Duration initialInvalidationDelay = Duration.of(30, ChronoUnit.SECONDS);
        /**
         * Alter eines Queue-Eintrags, ab dem er beim Aufräumen aus der persistenten Queue entfernt wird.
         */
        private Duration maxQueueStorageRetention = Duration.of(7, ChronoUnit.DAYS);
        /**
         * Schalter zum Aktivieren der Geo-Koordinaten-Bestimmung.
         */
        private boolean geoResolveEnabled = true;
        /**
         * Initiale Wartezeit nach dem Neustart bis zum ersten Lauf der Geo-Koordinaten-Bestimmung.
         */
        private Duration initialGeoResolveDelay = Duration.of(1, ChronoUnit.HOURS);
        /**
         * Wartezeit bis zum erneuten Lauf der Geo-Koordinaten-Bestimmung.
         */
        private Duration delayBetweenGeoResolves = Duration.of(1, ChronoUnit.DAYS);
        /**
         * Verzögerung zwischen dem Abarbeiten der Import-Queue und der Geo-Koordinaten-Bestimmung.
         */
        private Duration queueImportGeoResolveDelay = Duration.of(1, ChronoUnit.MINUTES);
        /**
         * Größe der OE-Chunks bei der Koordinaten-Bestimmung
         */
        private int geoResolveOeChunkSize = 50;
        /**
         * Zeitpunkt für die Neuberechnung der Verwaltungsleistungen in neue Indizes
         */
        private String cronTriggerVerwaltungsleistungReImport = "0 37 1 * * *";
        /**
         * Zeitpunkt für die Neuberechnung der Onlinedienste-Relationen in neuem Index
         */
        private String cronTriggerOnlineServiceRelationsImport = "0 0 1 * * *";
        /**
         * Maximale Gesamtanzahl an Import-Requests, die pro Polling-Zyklus über alle Redaktionssysteme
         * (SchemeAgencyIds) hinweg aus der persistenten Queue geladen werden.
         */
        private int queueGlobalMaxDocs = 3000;
        /**
         * Maximale Anzahl an Redaktionssystemen (SchemeAgencyIds), die pro Polling-Zyklus der
         * persistenten Queue berücksichtigt werden.
         */
        private int queueMaxBuckets = 20;
    }

    /**
     * Einstellungen für den Zugriff auf das XRepository (Codelisten-Download).
     */
    @Data
    public static class Codelist {

        /**
         * Basis-URL für den Zugriff auf das XRepository. Kann in Tests auf einen lokalen Mock-Server
         * umgebogen werden, um die externe Abhängigkeit von xrepository.de zu vermeiden.
         */
        private String baseUrl = "https://www.xrepository.de/api";

        /**
         * Timeout für das Aufbauen der TCP-Verbindung zum XRepository.
         */
        private Duration connectTimeout = Duration.of(5, ChronoUnit.SECONDS);

        /**
         * Timeout für das Warten auf die Antwort (Response) des XRepository, nachdem die Verbindung
         * steht.
         */
        private Duration requestTimeout = Duration.of(10, ChronoUnit.SECONDS);
    }

    /**
     * Die Dauer in Millisekunden, die das Prüfergebnis der XRepository-Verfügbarkeit gültig bleibt.
     */
    private long codelistCheckExpire = 300000L;

    /**
     * Intervall das gewartet werden soll während auf das Hochfahren von ElasticSearch gewartet wird.
     * Wert in Sekunden.
     */
    private int waitIntervallElasticSearch = 20;

    /**
     * Wie oft maximal gewartet werden soll bis ElasticSearch hochgefahren ist
     */
    private int maxRetriesWaitingForElasticSearch = 15;

    private Set<String> leikaTypingSet = Set.of(
        "1",
        "2",
        "2a",
        "2b",
        "3",
        "3a",
        "3b",
        "2/3",
        "4",
        "4a",
        "4b",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12"
    );

    /**
     * Steuert, ob Zugriffe auf öffentliche und Beta-Endpunkte (siehe {@link ApiAudience#PUBLIC},
     * {@link ApiAudience#BETA}) im SICHERHEIT-Log protokolliert werden (siehe {@link
     * RequestParameterFilter}).
     */
    private boolean publicApiAccessLogEnabled = true;

    private Evolution evolution = new Evolution();

    private Instance instance = new Instance();

    private BKG bkg = new BKG();

    private Elastic elastic = new Elastic();

    private Boost boost = new Boost();

    private Importer importer = new Importer();

    private Codelist codelist = new Codelist();

    /**
     * Eine Menge von Codelisten-Identifikatoren (in der Form {@literal <uri>_<version>}, die als
     * Dummy-Angaben gewertet und damit nicht aufgelöst werden. Betrifft insbesondere
     * TextModulIndividuell.
     */
    private Set<String> dummyCodeLists = Collections.singleton("individuell_unbestimmt");

    /**
     * Definiert Einstellungen der möglichen In-Memory-Caches. Die Schluessel entsprechen den Namen der
     * Caches, die Werte sind eine Caffeeine Cache-Spezifikation. Wird ein Schluessel mit einem leeren
     * String belegt, wird der entsprechende Cache deaktiviert.
     *
     * @see CaffeineSpec
     */
    private Map<String, String> inMemCache = new HashMap<>();

    public long getEvolutionRetries() {
        return evolution.getRetries();
    }

    public int getEvolutionRetriesTimeRange() {
        return evolution.getTimeRange();
    }

    public InstanceType getInstanceType() {
        return instance.getType();
    }
}
