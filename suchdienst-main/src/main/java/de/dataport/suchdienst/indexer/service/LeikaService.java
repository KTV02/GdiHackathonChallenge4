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
package de.dataport.suchdienst.indexer.service;

import static de.dataport.suchdienst.config.CacheConfiguration.CACHE_NAME_CODELIST_ENTRIES;
import static de.dataport.suchdienst.elasticsearch.api.IndexNames.LEISTUNGSKATALOG;
import static java.util.stream.Collectors.toSet;

import com.google.common.base.Strings;
import de.dataport.pvog.pvexternaldownloader.service.ExternalDownloadException;
import de.dataport.pvog.pvexternaldownloader.service.PvExternalDownloaderLeistungskatalogService;
import de.dataport.pvog.pvexternaldownloader.service.ResourceNotFoundException;
import de.dataport.suchdienst.elasticsearch.api.ElasticBulkWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticReadConnector;
import de.dataport.suchdienst.elasticsearch.api.ElasticWriteConnector;
import de.dataport.suchdienst.elasticsearch.api.IndexConfiguration;
import de.dataport.suchdienst.elasticsearch.api.IndexNames;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorTypeCodelist;
import de.dataport.suchdienst.importer.model.GenericLeistung;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.indexer.model.CodelistEntry;
import de.dataport.suchdienst.indexer.model.CodelistKey;
import de.dataport.suchdienst.indexer.model.LeikaEntry;
import de.dataport.suchdienst.indexer.model.mapping.LifeSituation;
import de.dataport.suchdienst.indexer.model.verwaltungsleistung.LeistungsbeschreibungVL;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.ProtocolType;
import de.xoev.xzufi.v2_2_0.Code;
import de.xoev.xzufi.v2_2_0.Identifikator;
import de.xoev.xzufi.v2_2_0.Leistung;
import de.xoev.xzufi.v2_2_0.LeistungenAntwortLeistung040104;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * Importiert den Leistungskatalog und verknüpft ihn mit der Lebenslagen-Codeliste, siehe PV-8429.
 */
@Service("leikaServiceX22")
@Slf4j
public class LeikaService {

    private final PvExternalDownloaderLeistungskatalogService leistungskatalogService;
    private final ElasticWriteConnector writeConnector;
    private final ElasticBulkWriteConnector bulkWriteConnector;
    private final ElasticReadConnector readConnector;
    private final IndexConfiguration indexConfiguration;
    private final CodelistService codelistService;
    private final CacheManager cacheManager;
    private final MessageSource messageSource;

    // Caching des aufgebauten Leistungskatalogs
    private Leistungskatalog cachedLeistungskatalog;

    public LeikaService(
        CodelistService codelistService,
        PvExternalDownloaderLeistungskatalogService leistungskatalogService,
        ElasticWriteConnector writeConnector,
        ElasticBulkWriteConnector bulkWriteConnector,
        ElasticReadConnector readConnector,
        IndexConfiguration indexConfiguration,
        CacheManager cacheManager,
        MessageSource messageSource
    ) {
        this.leistungskatalogService = leistungskatalogService;
        this.writeConnector = writeConnector;
        this.bulkWriteConnector = bulkWriteConnector;
        this.readConnector = readConnector;
        this.indexConfiguration = indexConfiguration;
        this.codelistService = codelistService;
        this.cacheManager = cacheManager;
        this.messageSource = messageSource;
    }

    /**
     * Lädt den Leistungskatalog und die Lebenslagen-Codelisten, verknüpft diese und speichert das
     * Ergebnis im Index suchdienst_leistungskatalog.
     *
     * @return true, wenn der Katalog neu importiert wurde, false, wenn nichts heruntergeladen wurde oder ein Fehler aufgetreten ist
     */
    public boolean importLeistungskatalog() {
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            return downloadAndIndexLeistungskatalog();
        }
    }

    private boolean downloadAndIndexLeistungskatalog() {
        try {
            var stopWatch = new StopWatch();
            stopWatch.start();
            log.info("Importiere Leistungskatalog...");

            // Leistungskatalog herunterladen:
            List<GenericLeistung> leistungen = downloadLeistungskatalog()
                .stream()
                .map(l -> {
                    l.getId().setSchemeAgencyID("undefined");
                    return new GenericLeistung(l);
                })
                .toList();
            ArrayList<LeikaEntry> leikaEntries = createLeikaEntries(leistungen);

            // Den Index leeren und die neuen Einträge reinschreiben:
            writeConnector.clearIndex(
                indexConfiguration.getCurrentIndexName(IndexNames.LEISTUNGSKATALOG)
            );
            writeLeikaEntriesToIndex(leikaEntries);

            cachedLeistungskatalog = new Leistungskatalog(leikaEntries);

            stopWatch.stop();
            log.info(
                "{} ms Leistungskatalog importiert mit {} Leistungen",
                stopWatch.getTotalTimeMillis(),
                leikaEntries.size()
            );
            return true;
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(ex ->
                Thread.currentThread().interrupt()
            );
            log.warn("Fehler beim Neuberechnen der Lebenslagen", e);
            return false;
        }
    }

    private List<Leistung> downloadLeistungskatalog()
        throws ExternalDownloadException, ResourceNotFoundException {
        var leistungskatalog = leistungskatalogService.downloadLeistungskatalog();
        List<Leistung> leistungen = leistungskatalog
            .getAntwort()
            .stream()
            .flatMap(awl -> awl.getErgebnis().getLeistung().stream())
            .toList();
        if (leistungen.isEmpty()) {
            throw new ExternalDownloadException("Der Leistungskatalog enthält keine Leistungen.");
        }
        return leistungen;
    }

    private void writeLeikaEntriesToIndex(List<LeikaEntry> leikaEntries) {
        bulkWriteConnector.bulkIndexThrowIfError(leikaEntries, IndexNames.LEISTUNGSKATALOG);
    }

    protected ArrayList<LeikaEntry> createLeikaEntries(List<GenericLeistung> leistungen) {
        // Iterieren über den Leistungskatalog und jeder LeikaId die Codelisten-Einträge zuordnen:
        var leikaEntries = new ArrayList<LeikaEntry>();
        for (GenericLeistung genericLeistung : leistungen) {
            var codelistEntries = codelistEntriesFromLeistung(genericLeistung);
            idSekundaerFromLeistung(genericLeistung)
                .stream()
                .map(leikaId -> new LeikaEntry(leikaId, codelistKeysWithParents(codelistEntries)))
                .forEach(leikaEntries::add);
        }
        return leikaEntries;
    }

    protected Set<CodelistEntry> codelistEntriesFromLeistung(GenericLeistung genericLeistung) {
        return switch (genericLeistung.getVersion()) {
            case XZUFI22 -> {
                Leistung leistung = genericLeistung.getXZuFi22();
                yield leistung
                    .getKlassifizierung()
                    .stream()
                    .filter(kl -> kl.getListURI().equals(Leistungskatalog.PV_LAGEN_URI))
                    .map(kl ->
                        codelistService.getCodelistEntry(
                            Leistungskatalog.PV_LAGEN_URI,
                            kl.getListVersionID(),
                            kl.getCode()
                        )
                    )
                    .filter(CodelistEntry::isExistent)
                    .collect(toSet());
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Leistung leistung = genericLeistung.getXZuFi23();
                yield leistung
                    .getKlassifizierung()
                    .stream()
                    .filter(kl -> kl.getListURI().equals(Leistungskatalog.PV_LAGEN_URI))
                    .map(kl ->
                        codelistService.getCodelistEntry(
                            Leistungskatalog.PV_LAGEN_URI,
                            kl.getListVersionID(),
                            kl.getCode()
                        )
                    )
                    .filter(CodelistEntry::isExistent)
                    .collect(toSet());
            }
        };
    }

    private List<String> lifeSituationVersionsFromLeistung(GenericLeistung genericLeistung) {
        return switch (genericLeistung.getVersion()) {
            case XZUFI22 -> {
                Leistung leistung = genericLeistung.getXZuFi22();
                yield leistung.getKlassifizierung().stream().map(Code::getListVersionID).toList();
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Leistung leistung = genericLeistung.getXZuFi23();
                yield leistung
                    .getKlassifizierung()
                    .stream()
                    .map(de.xoev.xzufi.v2_3_1.Code::getListVersionID)
                    .toList();
            }
        };
    }

    private List<String> idSekundaerFromLeistung(GenericLeistung genericLeistung) {
        return switch (genericLeistung.getVersion()) {
            case XZUFI22 -> {
                Leistung leistung = genericLeistung.getXZuFi22();
                yield leistung.getIdSekundaer().stream().map(Identifikator::getValue).toList();
            }
            case XZUFI23 -> {
                de.xoev.xzufi.v2_3_1.Leistung leistung = genericLeistung.getXZuFi23();
                yield leistung
                    .getIdSekundaer()
                    .stream()
                    .map(de.xoev.xzufi.v2_3_1.Identifikator::getValue)
                    .toList();
            }
        };
    }

    /**
     * Liefert alle aktuellen Einträge aus dem Leistungskatalog-Index, gemappt nach LeikaKey.
     * Cacht den Leistungskatalog, damit er nicht bei jedem Import einer
     * einzelnen Leistungsbeschreibung neu von ES gelesen werden muss.
     * Falls das Laden des LK fehlschlägt, protokollieren wir dies als Warnung und wir fahren mit einem leeren LK fort.
     * Das hat zur Folge, dass API-Zugriffe und Importe dann noch funktionieren, aber möglicherweise Daten unvollständig liefern
     * bzw. importieren. Dieses Verhalten ist so gewollt!
     */
    public Leistungskatalog getLeistungskatalog() {
        if (cachedLeistungskatalog != null) {
            return cachedLeistungskatalog;
        }
        try {
            var leistungskatalog = readLeistungskatalogFromIndex();
            cachedLeistungskatalog = leistungskatalog;
            return leistungskatalog;
        } catch (Exception ex) {
            log.warn(
                "Fehler beim Laden des Leistungskatalogs. Setze fort mit leeren Leistungskatalog. APIs und Importe können keine Lebenslagen mehr liefern bzw. berechnen.",
                ex
            );
            return new Leistungskatalog(Collections.emptyList());
        }
    }

    public Leistungskatalog readLeistungskatalogFromIndex() {
        var stopWatch = new StopWatch();
        stopWatch.start();
        List<LeikaEntry> leikaEntriesFromEs = readConnector.getLeikaEntries();
        var leistungskatalog = new Leistungskatalog(leikaEntriesFromEs);
        stopWatch.stop();
        if (leistungskatalog.isEmpty()) {
            log.warn("Kein Leistungskatalog in ES vorhanden.");
        } else {
            log.debug(
                "{} ms Leistungskatalog mit {} Einträgen aus ElasticSearch eingelesen",
                stopWatch.getTotalTimeMillis(),
                leikaEntriesFromEs.size()
            );
        }
        return leistungskatalog;
    }

    /**
     * Sucht für jeden CodeListEntry die hierarchisch übergeordneten und fügt diese jeweils zu einer Liste zusammen.
     *
     * @param entries Die CodeListEntries
     * @return Die gebildeten Listen
     */
    protected Set<List<CodelistKey>> codelistKeysWithParents(Set<CodelistEntry> entries) {
        return entries
            .stream()
            .map(entry -> {
                var version = entry.getVersion();
                var codes = getLifeSituationCodeHierarchy(entry.getCode());
                return codes
                    .stream()
                    .map(c ->
                        codelistService.getCodelistEntry(Leistungskatalog.PV_LAGEN_URI, version, c)
                    )
                    .filter(CodelistEntry::isExistent)
                    .map(CodelistEntry::getKey)
                    .toList();
            })
            .collect(toSet());
    }

    /**
     * Berechnet die Hierarchie der Lebenslagen Codes.
     * Die gelieferte Liste enthält die Hiearchie-Ebenen in der Reihenfolge Sublage, Hauptlage, Toplage.
     */
    static List<String> getLifeSituationCodeHierarchy(String code) {
        var ret = new ArrayList<String>();
        if (code.length() != 7) {
            throw new IllegalArgumentException(
                String.format("Unerwarteter Code '%s': hat nicht die erwartete Länge von 7", code)
            );
        }
        if (!StringUtils.isNumeric(code)) {
            throw new IllegalArgumentException(
                String.format("Unerwarteter Code '%s': ist nicht numerisch", code)
            );
        }
        for (PvLagenHiearchyLevel hierLevel : List.of(
            PvLagenHiearchyLevel.SUB_LAGE,
            PvLagenHiearchyLevel.HAUPT_LAGE,
            PvLagenHiearchyLevel.TOP_LAGE
        )) {
            var endIndex = hierLevel.endIndex;
            var parentCode =
                code.substring(0, endIndex + 1) + Strings.repeat("0", 7 - endIndex - 1);
            assert parentCode.length() == 7;
            if (!ret.contains(parentCode)) {
                ret.add(parentCode);
            }
        }
        return ret;
    }

    /**
     * Diese Methode berechnet den Lebenslagen-Baum aus Einträgen aus dem Leistungskatalog.
     * Der so entstandene Baum wird dann an der Leistungsbeschreibung gespeichert.
     * Der entstehende Baum enthält alle Lebenslagen der CodelistKeys, inklusive der Oberlagen.
     * In den CodelistKeys sind die Lebenslagen-Codes inklusive Pfad zum Root in der Reihenfolge "Leaf first" in Listen gespeichert.
     */
    protected HashSet<LifeSituation> computeLifeSituationTree(
        Set<List<CodelistKey>> codelistKeyLists
    ) {
        var lifeSituations = new HashSet<LifeSituation>();
        for (var codelistKeys : codelistKeyLists) {
            var rootIndex = codelistKeys.size() - 1;
            mergeCodelistEntryIntoLifesituationTree(codelistKeys, rootIndex, lifeSituations);
        }
        return lifeSituations;
    }

    private void mergeCodelistEntryIntoLifesituationTree(
        List<CodelistKey> codelistKeys,
        int index,
        Set<LifeSituation> lifeSituations
    ) {
        var codelistKey = codelistKeys.get(index);
        var code = codelistKey.code();
        var lifeSit = lifeSituations
            .stream()
            .filter(liSu -> code.equals(liSu.getCode()))
            .findFirst()
            .orElse(null);
        if (lifeSit == null) {
            lifeSit = new LifeSituation(code);
            lifeSituations.add(lifeSit);
        }
        if (index > 0) {
            mergeCodelistEntryIntoLifesituationTree(codelistKeys, index - 1, lifeSit.getChildren());
        }
    }

    /**
     * Aktualisiert die Lebenslagen an der Leistungsbeschreibung durch Lookup in den Leistungskatalog.
     * Falls im Leistungskatalog keine Codelisten-Einträge für die LB definiert sind, die Klassifizierungen der Leistungen
     * als Fallback verwenden. Das sind die Codelisten-Einträge, die vom Redaktionssystem gekommen sind. Diesen trauen wir nur
     * in 2. Instanz, der Leistungskatalog soll Vorrang haben.
     * Der Umweg über den Leistungskatalog ist nur eine Übergangslösung bis
     * die Datenqualität der RS hinsichtlich der LL-Codes ausreichend ist.
     * Siehe PV-8656.
     */
    public void computeLifeSituations(LeistungsbeschreibungVL vl, GenericLeistung genericLeistung) {
        Leistungskatalog katalog = getLeistungskatalog();

        // Lookup im Katalog:
        var codelistKeyLists = vl
            .getLeikakeys()
            .stream()
            .map(katalog::getLeikaEntry)
            .filter(Objects::nonNull)
            .map(LeikaEntry::getCodelistKeys)
            .flatMap(Collection::stream)
            .collect(toSet());

        boolean catalogLookUpFailed = codelistKeyLists.isEmpty();
        // Setze die Versionen zur Codeliste 'urn:xoev-de:fim:codeliste:pvlagen'.
        // Nur wenn 'catalogLookUpFailed' werden die Versionen aus dem XML der Leistung extrahiert, sonst aus Katalog
        final Collection<String> lifeSituationVersions;

        // Fallback:
        if (catalogLookUpFailed) {
            // Es konnten keine Codelist Einträge zur Leistung aus dem Katalog aufgelöst werden, verwende Einträge aus dem XML
            var clESet = codelistEntriesFromLeistung(genericLeistung);
            codelistKeyLists = codelistKeysWithParents(clESet);

            // Speichere die Codelist-Version aus dem XML
            lifeSituationVersions = lifeSituationVersionsFromLeistung(genericLeistung);
        } else {
            // Speichere die Codelist-Version aus dem Catalog
            lifeSituationVersions = katalog.getCodelistVersions();
        }

        vl.getLifeSituationVersions().addAll(lifeSituationVersions);

        var lifeSituations = computeLifeSituationTree(codelistKeyLists);
        vl.setLifeSituations(lifeSituations);
        var lifeSituationCodes = codelistKeyLists
            .stream()
            .filter(clKeyList -> !clKeyList.isEmpty())
            .map(List::getFirst) // Nur die LL-Ebene selbst nehmen, nicht die Ober-Ebenen, siehe PV-9464
            .map(CodelistKey::code)
            .collect(toSet());
        vl.setLifeSituationCodes(lifeSituationCodes);
    }

    /**
     * Diese Methode wird beim Hochfahren des SuD vom SuchdienstIntitalizer aufgerufen.
     * Sie sorgt dafür, dass ein Leistungskatalog verfügbar ist. Falls der Index noch leer ist,
     * schreibt die Methode einen initialen Datenbestand in den Index.
     * Nachdem die Methode gelaufen ist, ist auf jeden Fall ein LK vorhanden und 'cachedLeistungskatalog'
     * ist besetzt, sodass der SuD im folgenden einfach diesen Cache verwenden kann und keine
     * kostspieligen ES- oder gar Import-Operationen während der Suche stattfinden.
     */
    public void intializeLeistungskatalog() throws IOException {
        if (!readConnector.isIndexEmpty(LEISTUNGSKATALOG)) {
            getLeistungskatalog(); // Falls schon etwas im Index steht, wird dies hiermit eingelesen und der Cache besetzt.
            return;
        }

        var stopWatch = new StopWatch();
        stopWatch.start();
        log.info("Importiere initialen Leistungskatalog...");
        // Unsere positive "out-of-the-box-experience" besteht darin, den Katalog aus einer mitgelieferten Datei
        // zu befüllen, in den Index zu schreiben,
        // alle Lebenslagen der bestehenden Leistungen neu zu berechnen und dann im Betrieb mit diesem Katalog zu arbeiten.
        Resource resource = new PathMatchingResourcePatternResolver().getResource(
            "classpath:/leistungskatalog/leistungskatalog_reduziert.xml"
        );
        try (InputStream inputStream = resource.getInputStream()) {
            var jaxbContext = JAXBContext.newInstance(LeistungenAntwortLeistung040104.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            LeistungenAntwortLeistung040104 root =
                (LeistungenAntwortLeistung040104) unmarshaller.unmarshal(inputStream);
            List<GenericLeistung> leistungen = root
                .getAntwort()
                .stream()
                .flatMap(awl -> awl.getErgebnis().getLeistung().stream())
                .map(GenericLeistung::new)
                .toList();
            List<LeikaEntry> leikaEntries = createLeikaEntries(leistungen);
            writeLeikaEntriesToIndex(leikaEntries);
            cachedLeistungskatalog = new Leistungskatalog(leikaEntries);
            stopWatch.stop();
            log.info(
                "{} ms Initialen Leistungskatalog importiert.",
                stopWatch.getTotalTimeMillis()
            );
        } catch (JAXBException e) {
            //PV-8946 catch-rethrow
            throw new ImportException(
                ErrorCodeEnum.INVALID_FILE,
                messageSource.getMessage(
                    "leika_service.exception_invalid_file_error",
                    null,
                    Locale.GERMAN
                ),
                e
            );
        }
    }

    public String getLatestText(SortedSet<String> versions, String code, Locale locale) {
        CodelistEntry latestEntry = getLatestEntry(versions, code);
        return latestEntry.getText(locale);
    }

    public String getLatestComment(SortedSet<String> versions, String code, Locale locale) {
        CodelistEntry latestEntry = getLatestEntry(versions, code);
        return latestEntry.getComment(locale);
    }

    private CodelistEntry getLatestEntry(SortedSet<String> versions, String code) {
        for (String version : versions) {
            CodelistEntry codelistEntry = readConnector.getCodelistEntry(
                new CodelistKey(Leistungskatalog.PV_LAGEN_URI, version, code)
            );
            if (Objects.nonNull(codelistEntry)) {
                return codelistEntry;
            }
        }
        return CodelistEntry.empty(
            code,
            Leistungskatalog.PV_LAGEN_URI,
            versions.first(),
            ErrorTypeCodelist.CODE
        );
    }

    public void updateLocalPvLagenCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME_CODELIST_ENTRIES);
        if (cache == null) {
            throw new IllegalStateException(
                "Cache %s nicht gefunden, bitte Konfiguration prüfen!".formatted(
                    CACHE_NAME_CODELIST_ENTRIES
                )
            );
        }

        StopWatch sw = new StopWatch();
        sw.start();
        List<String> versions = readConnector.aggregatePvLagenVersions();
        versions
            .stream()
            .map(version ->
                readConnector.getCodeListEntries(Leistungskatalog.PV_LAGEN_URI, version)
            )
            .flatMap(Collection::stream)
            .forEach(entry ->
                cache.put(
                    SimpleKeyGenerator.generateKey(
                        new CodelistKey(entry.getUri(), entry.getVersion(), entry.getCode())
                    ),
                    entry
                )
            );
        sw.stop();
        log.info(
            "{} ms Update des PV_Lagen Caches (Versionen: {})",
            sw.getTotalTimeMillis(),
            versions
        );
    }

    /**
     * Definition der Stellen des PVLagen Hierarchie Models. Wird zur Ermittlung der Codes für die einzelnen Ebenen benötigt.
     * Der Algorithmus braucht zwar nur die Information, an welcher Stelle (0-basiert) die Hierarchieebene aufhört.
     * Aus Dokumentationsgründen definieren wir hier aber trotzdem beiden Seiten des Intervalls.
     */
    private enum PvLagenHiearchyLevel {
        /**
         * Bürger/Unternehmen, verwendet die erste Stelle.
         */
        TOP_LAGE(0, 0),

        /**
         * Die Hauptlage verwendet die Stellen 1-2
         */
        HAUPT_LAGE(1, 2),

        /**
         * Die Sublage verwendet die Stellen 3-6
         */
        SUB_LAGE(3, 6);

        private final int startIndex;
        private final int endIndex; // inclusive

        PvLagenHiearchyLevel(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
