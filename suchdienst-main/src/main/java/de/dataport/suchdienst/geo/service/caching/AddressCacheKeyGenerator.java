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

import de.dataport.suchdienst.indexer.model.mapping.Address;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service("addressKeyGenerator")
public class AddressCacheKeyGenerator implements KeyGenerator {

    private final MessageSource messageSource;

    public AddressCacheKeyGenerator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params == null || params.length != 1) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "address_cache_key_generator.exception_multiple_params_error",
                    new String[] { Arrays.toString(params) },
                    Locale.GERMAN
                )
            );
        }
        Address a = (Address) params[0];
        return new SimpleKey(
            StringUtils.trimToEmpty(StringUtils.isNotBlank(a.getZip()) ? a.getZip() : a.getCity()),
            StringUtils.trimToEmpty(a.getStreet()),
            StringUtils.trimToEmpty(a.getHouseNumber())
        );
    }
}
