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
package de.dataport.suchdienst.util;

import de.dataport.suchdienst.elasticsearch.api.Elastic;
import de.dataport.suchdienst.error.ApiWarnException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import java.util.Locale;
import org.springframework.context.MessageSource;

/**
 * validation helper for Page Size and Page number
 */
public interface PageRequestCheck {
    default Boolean isValidElasticSearchPageableParameter(int page, int size) {
        return page * size + size <= Elastic.WINDOW_SIZE;
    }

    default void checkPageParameters(int page, int pageSize, MessageSource messageSource) {
        if (Boolean.FALSE.equals(isValidElasticSearchPageableParameter(page, pageSize))) {
            throw new ApiWarnException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "page_request_check.exception_bad_request_error",
                    null,
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH
            );
        }
    }
}
