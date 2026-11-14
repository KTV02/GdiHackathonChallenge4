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
package de.dataport.suchdienst.geo.service;

import de.dataport.suchdienst.geo.model.internal.GeoLocation;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Service;

/**
 * Transformatiert Koordinaten von einer Systematik (z.B. EPSG:4326) in eine andere.
 */
@Service
@Slf4j
public class CoordinateTransformationService {

    /**
     * Schützt den crsCache vor Lost-Copy
     */
    private final Lock lock = new ReentrantLock();

    private final CRSFactory crsFactory = new CRSFactory();
    private final Map<String, CoordinateReferenceSystem> crsCache = new LinkedHashMap<>() {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CoordinateReferenceSystem> eldest) {
            return size() > 20;
        }
    };

    public Optional<GeoLocation> transform(GeoLocation from, String to) {
        if (Strings.CS.equals(from.getSystem(), to)) {
            return Optional.of(from);
        }
        ProjCoordinate src = new ProjCoordinate(
            Double.parseDouble(from.getLongitude()),
            Double.parseDouble(from.getLatitude())
        );
        try {
            ProjCoordinate transform = getTransformer(from.getSystem(), to).transform(
                src,
                new ProjCoordinate()
            );
            return Optional.of(
                new GeoLocation(Double.toString(transform.x), Double.toString(transform.y), to)
            );
        } catch (RuntimeException e) {
            // PV-8946 catch-log-skip
            log.warn("Fehler beim Transformieren von Koordinaten: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private CoordinateTransform getTransformer(String from, String to) {
        return new CoordinateTransformFactory().createTransform(getCrs(from), getCrs(to));
    }

    private CoordinateReferenceSystem getCrs(String name) {
        this.lock.lock();
        try {
            CoordinateReferenceSystem crs = crsCache.get(name);
            if (crs == null) {
                crs = crsFactory.createFromName(name);
                crsCache.put(name, crs);
            }
            return crs;
        } finally {
            this.lock.unlock();
        }
    }
}
