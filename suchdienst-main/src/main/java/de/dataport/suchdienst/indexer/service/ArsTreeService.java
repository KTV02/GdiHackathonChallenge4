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

import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.geo.service.CodelistDownloader;
import de.dataport.suchdienst.util.Exceptions;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

/**
 * Der bereitgestellte {@link ArsTree} wird aus der Destatis-ARS-Codeliste aufgebaut und im Speicher vorgehalten.
 *
 * @see ArsTree
 * @see XzufiParserService
 */
@Service
@Slf4j
public class ArsTreeService {

    private final CodelistDownloader codelistDownloader;
    private final TaskScheduler taskScheduler;
    private final SuchdienstProperties properties;
    private final boolean useSeedOnly;

    private Optional<ArsTree> arsTree = Optional.empty();

    public ArsTreeService(
        CodelistDownloader codelistDownloader,
        @Qualifier("singleTaskExecutor") TaskScheduler taskScheduler,
        SuchdienstProperties properties,
        @Value("${suchdienst.ars.use-seed-only:false}") boolean useSeedOnly
    ) {
        this.codelistDownloader = codelistDownloader;
        this.taskScheduler = taskScheduler;
        this.properties = properties;
        this.useSeedOnly = useSeedOnly;
    }

    public synchronized ArsTree getArsTree() {
        if (arsTree.isEmpty()) {
            refreshArsTree();
        }
        return arsTree.orElseThrow(() ->
            new IllegalStateException("ARS-Baum konnte nicht initialisiert werden")
        );
    }

    public synchronized void refreshArsTree() {
        log.info("Aktualisiere ARS-Liste und baue ARS Hierarchy Baum neu auf");
        try {
            resolveArsCodes()
                .map(this::calculateArsTree)
                .ifPresent(tree -> arsTree = Optional.of(tree));
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                Thread.currentThread().interrupt()
            );
            log.warn("Fehler beim Aktualisieren des ARS-Baums", e);
        }
    }

    /**
     * Liefert die zu verwendenden ARS-Schlüssel nach einer priorisierten Fallback-Kette:
     * <ol>
     *   <li>Im Testprofil ({@code useSeedOnly}): direkt aus dem Seed</li>
     *   <li>Frischer Download von Destatis</li>
     *   <li>Bestehenden Baum beibehalten (kein Update nötig)</li>
     *   <li>Gecachte Liste aus Redis</li>
     *   <li>Älterer Seed als letzter Ausweg</li>
     * </ol>
     */
    private Optional<Set<String>> resolveArsCodes() {
        if (useSeedOnly) {
            log.info("Seed-only Modus aktiv: Lade ARS direkt aus dem Seed");
            return toArsCodes(codelistDownloader.loadReducedArsSeedIntoCache());
        }

        Optional<Set<String>> freshCodes = toArsCodes(downloadFreshArsCodes());
        if (freshCodes.isPresent()) {
            return freshCodes;
        }

        if (arsTree.isPresent()) {
            log.warn(
                "Destatis ARS-Liste konnte nicht geladen werden, behalte bestehenden ARS-Baum"
            );
            return Optional.empty();
        }

        log.warn("Destatis ARS-Liste konnte nicht geladen werden, hole sie aus dem Cache");
        return toArsCodes(codelistDownloader.getCachedReducedDestatisArsCodelist())
            .or(() -> {
                log.warn(
                    "Destatis ARS-Liste nicht im Cache gefunden, hole sie aus einem älteren Seed"
                );
                return toArsCodes(codelistDownloader.loadReducedArsSeedIntoCache());
            })
            .or(() -> {
                log.warn("Keine ARS-Liste verfügbar, ARS-Baum wird nicht aktualisiert");
                return Optional.empty();
            });
    }

    private Map<String, String> downloadFreshArsCodes() {
        try {
            return codelistDownloader.updateDestatisArsCodelistCaches();
        } catch (Exception e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                Thread.currentThread().interrupt()
            );
            log.warn("Destatis ARS-Liste konnte nicht aktualisiert werden", e);
            return Map.of();
        }
    }

    private Optional<Set<String>> toArsCodes(Map<String, String> arss) {
        return arss.isEmpty() ? Optional.empty() : Optional.of(arss.keySet());
    }

    private ArsTree calculateArsTree(Set<String> arss) {
        if (arss.isEmpty()) {
            throw new IllegalStateException("ARS-Baum kann nicht ohne ARS-Liste berechnet werden");
        }
        log.info("Baue ARS Hierarchy Baum auf");
        return new ArsTree(arss);
    }

    public void schedulePeriodicArsRefresh() {
        taskScheduler.schedule(
            this::refreshArsTree,
            new CronTrigger(properties.getBkg().getPeriodicXoevArsRefreshCronExp())
        );
    }
}
