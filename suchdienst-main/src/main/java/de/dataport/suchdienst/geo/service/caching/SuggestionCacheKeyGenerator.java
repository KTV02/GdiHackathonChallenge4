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
import java.util.Arrays;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service("suggestionCacheKeyGenerator")
public class SuggestionCacheKeyGenerator implements KeyGenerator {

    private final MessageSource messageSource;

    public SuggestionCacheKeyGenerator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length != 2) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "address_cache_key_generator.exception_multiple_params_error",
                    new String[] { Arrays.toString(params) },
                    Locale.GERMAN
                )
            );
        }
        var input = (String) params[0];
        var count = (int) params[1];

        return DigestUtils.md5Hex(StringUtils.join(input, "#", count));
    }
}
