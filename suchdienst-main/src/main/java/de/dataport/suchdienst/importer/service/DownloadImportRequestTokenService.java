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
package de.dataport.suchdienst.importer.service;

import static java.util.concurrent.TimeUnit.SECONDS;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DownloadImportRequestTokenService {

    private PassiveExpiringMap<UUID, String> downLoadTokenMap;

    @PostConstruct
    public void postConstruct() {
        var expirePeriod = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<UUID, String>(
            15,
            SECONDS
        );
        downLoadTokenMap = new PassiveExpiringMap<>(expirePeriod);
    }

    public String addAndGetToken(String importRequestsId) {
        UUID downloadToken = UUID.randomUUID();
        downLoadTokenMap.put(downloadToken, importRequestsId);
        return downloadToken.toString();
    }

    public String getAndRemoveToken(String token) {
        if (!isAToken(token)) {
            return null;
        }
        return downLoadTokenMap.remove(UUID.fromString(token));
    }

    public boolean isAToken(String token) {
        try {
            UUID.fromString(token);
        } catch (IllegalArgumentException _) {
            return false;
        }
        return true;
    }
}
