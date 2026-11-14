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
package de.dataport.suchdienst.geo.service.caching;

import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Service;

@Slf4j
@Service("locationCacheKeyGenerator")
public class LocationCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return DigestUtils.md5Hex(
            // Beim Joinen müssen wir darauf achten, dass Null-Werte berücksichtigt werden.
            // Ansonsten werden Key-Kollisionen möglich und der Cache könnte mit falschen Antworten geflutet werden
            StringUtils.joinWith(";", params)
        );
    }
}
