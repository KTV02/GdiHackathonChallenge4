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
package de.dataport.suchdienst.elasticsearch.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexConfiguration {

    public static final String ALIAS_WRITE_PREFIX = "write_";
    public static final String ALIAS_READ_PREFIX = "read_";

    private final Map<IndexNames, String> currentIndexNames = new EnumMap<>(IndexNames.class);
    private final Map<AliasNames, String> currentAliasNames = new EnumMap<>(AliasNames.class);

    public void setCurrentIndexName(IndexNames index, String currentIndexName) {
        if (currentIndexName != null) {
            currentIndexNames.put(index, currentIndexName);
        }
    }

    public String getCurrentIndexName(IndexNames index) {
        return currentIndexNames.computeIfAbsent(index, IndexNames::getIndexName);
    }

    public void setCurrentAliasName(AliasNames alias, String currentAliasName) {
        if (currentAliasName != null) {
            currentAliasNames.put(alias, currentAliasName);
        }
    }

    public List<String> getCurrentReadAliasNames() {
        return AliasNames.VL_ALIASES.stream().map(this::getCurrentReadAliasName).toList();
    }

    public String getCurrentReadAliasName(AliasNames alias) {
        return ALIAS_READ_PREFIX + currentAliasNames.getOrDefault(alias, alias.getName());
    }

    public String getCurrentWriteAliasName(AliasNames alias) {
        return ALIAS_WRITE_PREFIX + currentAliasNames.getOrDefault(alias, alias.getName());
    }
}
