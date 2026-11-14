// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Spring-Security-Matcher für zentral aufgelöste API-Audiences.
package de.dataport.suchdienst.config.apiaudience;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Ordnet einen Request über den zugehörigen MVC-Handler einer oder mehreren API-Audiences zu.
 */
public final class ApiAudienceRequestMatcher implements RequestMatcher {

    private final ApiAudienceResolver apiAudienceResolver;
    private final ApiAudience firstExpectedAudience;
    private final ApiAudience[] furtherExpectedAudiences;

    public ApiAudienceRequestMatcher(
        ApiAudienceResolver apiAudienceResolver,
        ApiAudience firstExpectedAudience,
        ApiAudience... furtherExpectedAudiences
    ) {
        this.apiAudienceResolver = apiAudienceResolver;
        this.firstExpectedAudience = firstExpectedAudience;
        this.furtherExpectedAudiences = furtherExpectedAudiences;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return apiAudienceResolver.hasAudience(
            request,
            firstExpectedAudience,
            furtherExpectedAudiences
        );
    }
}
