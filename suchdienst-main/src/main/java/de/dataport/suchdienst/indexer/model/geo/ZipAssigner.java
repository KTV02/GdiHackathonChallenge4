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
package de.dataport.suchdienst.indexer.model.geo;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import de.dataport.suchdienst.indexer.model.ArsObject;
import de.dataport.suchdienst.indexer.model.RegionalKeyKind;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Diese Klasse enthält den eigentlichen Algorithmus, um bestehenden Ars-Datensätzen (wie sie im Index gespeichert sind)
 * die aus den HKDE-Daten stammenden Postleitzahlen zuzuordnen, d.h. die PLZ zu den bereits am ArsObject vorhandenen
 * PLZ zu ersetzen.
 */
@Slf4j
public class ZipAssigner {

    /**
     * Ermittelt zu jeder in den geoItems auftretenden PLZ denjenigen regionalKey mit den meisten Vorkommen.
     * Diese PLZ tragen wir dann als PLZ des regionalKey in die arsObjects ein.
     * Auf diese Weise werden Grenzfälle eliminiert, also solche, bei denen zu einer PLZ nur wenige Gebiets-Zuordnungen in den
     * GeoDaten sind, diese resultieren aus Häusern, die zwar im Gemeindegebiet einer stehen, aber die PLZ einer Nachbargemeinde
     * haben. Siehe PV-9580.
     * <p>
     * Diese Methode liesse sich nach Bundesländern partitioniert aufrufen, also mit den arsObjects und geoItems eines Bundeslandes.
     * Der gesamte Vorgang wäre damit parallelisierbar.
     *
     * @param arsObjects Objekte aus dem ars-Index, diese werden hier geupdatet werden
     * @param geoItems   Enthält die PLZ-RS-Paarungen aus den HKDE-Daten
     * @param keyKind    Bestimmt, ob die Zuordnung auf AGS oder ARS-Basis erfolgt
     * @return Diejenigen arsObject, deren PLZ modifiziert wurden.
     */
    public List<ArsObject> assignZipCodes(
        List<ArsObject> arsObjects,
        Stream<GeoItem> geoItems,
        RegionalKeyKind keyKind
    ) {
        var startMillis = System.currentTimeMillis();
        // Bilde Map von PLZ -> {regioKey -> Anzahl regioKeys} ("Histogramm"):
        AtomicInteger itemCount = new AtomicInteger(0);
        var groups = geoItems
            .parallel()
            .peek(i -> {
                if (itemCount.incrementAndGet() % 500000 == 0) {
                    log.debug("{} geoItems eingelesen", itemCount.get());
                }
            })
            .collect(groupingBy(GeoItem::zip, groupingBy(GeoItem::regioKey, counting())));

        // Jetzt die Umkehrung bilden, also eine Abbildung von regioKey -> Set of PLZ, dabei wählen wir den zur PLZ
        // am häufigsten auftretenden regioKey.
        Map<String, Set<String>> regioKeyToZipMapping = new HashMap<>();

        groups.forEach((zip, regioKeyHistogram) -> {
            final Set<String> regioKeys; // Könnte ggf mehrere Einträge enthalten, wenn wir mehr als einen Key aus dem Histogramm auswählen wollen
            if (regioKeyHistogram.size() == 1) {
                // Wenn es für die PLZ einen eindeutigen Key gibt, nehmen wir diesen:
                regioKeys = regioKeyHistogram.keySet();
            } else {
                // Wenn der regioKey für die PLZ nicht eindeutig ist nehmen wir denjenigen mit den meisten Vorkommen,
                // dadurch fallen "Grenzgebiete" aus der Betrachtung heraus:
                regioKeys = choseRegioKeysFromHistogram(zip, regioKeyHistogram);
            }
            regioKeys.forEach(r -> {
                var zipBucketOfRegioKey = regioKeyToZipMapping.computeIfAbsent(r, x ->
                    new HashSet<>()
                );
                zipBucketOfRegioKey.add(zip);
            });
        });
        var stopMillis = System.currentTimeMillis();
        log.debug(
            "{} ms {} PLZ-Zuordnungen aus {} GeoItems bestimmt",
            stopMillis - startMillis,
            regioKeyToZipMapping.size(),
            itemCount.get()
        );
        // Jetzt die arsObjects updaten:
        return arsObjects
            .stream()
            .filter(arsObject ->
                arsObject.replaceZipcodes(
                    regioKeyToZipMapping.getOrDefault(keyKind.get(arsObject), Set.of())
                )
            )
            .toList();
    }

    /**
     * Wählt den regioKey mit dem maximalen Counter aus und gibt diesen zurück.
     * Diese Methode könnten wir leicht in ein Interface auslagern für den Fall, dass wir unterschiedliche
     * Verfahren ausprobieren wollen und evtl. bei eng aneinanderliegenden Zählern mehrere keys liefern.
     *
     * @param histogram Mappt regioKey auf Anzahl seiner Vorkommen
     */
    private Set<String> choseRegioKeysFromHistogram(String zip, Map<String, Long> histogram) {
        var maxEntry = histogram.entrySet().stream().max(Map.Entry.comparingByValue());
        assert maxEntry.isPresent();
        var regioKey = maxEntry.get().getKey();
        if (log.isDebugEnabled()) {
            log.debug(
                "RegioKey {} ausgewählt für PLZ {} aus regioKeyHistogram: {}",
                regioKey,
                zip,
                histogram
            );
        }
        return Set.of(regioKey);
    }
}
