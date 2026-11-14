// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Validiert API-Audiences gegen die tatsächlich registrierten MVC-Pfade und deren
// Pfadkonventionen.
package de.dataport.suchdienst.config.apiaudience;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Stellt beim Start sicher, dass jeder Suchdienst-Endpunkt klassifiziert ist und seine Audience zur
 * vereinbarten Pfadstruktur passt.
 *
 * <p>Nur relevant in Servlet-Web-Kontexten, da {@link RequestMappingHandlerMapping} sonst nicht
 * existiert (z.B. in {@code webEnvironment = NONE}-Tests).
 */
@Component
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ApiAudienceStartupValidation implements SmartInitializingSingleton {

    private static final String APPLICATION_PACKAGE = "de.dataport.suchdienst.";

    private static final Pattern PUBLIC_PATH_PATTERN = Pattern.compile("^/v\\d{1,2}(?:/.*)?$");
    private static final Pattern BETA_PATH_PATTERN = Pattern.compile(
        "^/v\\d{1,2}beta\\d{1,2}(?:/.*)?$"
    );

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiAudienceResolver apiAudienceResolver;

    public ApiAudienceStartupValidation(
        @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
        ApiAudienceResolver apiAudienceResolver
    ) {
        this.handlerMapping = handlerMapping;
        this.apiAudienceResolver = apiAudienceResolver;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> violations = new ArrayList<>();
        handlerMapping
            .getHandlerMethods()
            .forEach((mapping, handlerMethod) -> validate(mapping, handlerMethod, violations));

        if (!violations.isEmpty()) {
            violations.sort(String::compareTo);
            throw new IllegalStateException(
                "Ungültige API-Audience-Konfiguration:\n - " + String.join("\n - ", violations)
            );
        }
    }

    private void validate(
        RequestMappingInfo mapping,
        HandlerMethod handlerMethod,
        List<String> violations
    ) {
        if (!handlerMethod.getBeanType().getName().startsWith(APPLICATION_PACKAGE)) {
            return;
        }

        apiAudienceResolver.resolve(handlerMethod).ifPresentOrElse(
            audience ->
                mapping
                    .getPatternValues()
                    .stream()
                    .filter(path -> !matchesAudience(path, audience))
                    .map(path -> violation(handlerMethod, path, audience))
                    .forEach(violations::add),
            () -> violations.add(missingAudienceViolation(handlerMethod))
        );
    }

    /**
     * Prüft, ob der registrierte Pfad zur annotierten Audience passt.
     *
     * <p>Die Muster sind bewusst auf Lesbarkeit ausgelegt: Sie erlauben ein- und zweistellige
     * Versionsnummern (auch {@code /v00}), ohne diese fachlich zu bewerten.
     */
    static boolean matchesAudience(String path, ApiAudience audience) {
        boolean publicPath = PUBLIC_PATH_PATTERN.matcher(path).matches();
        boolean betaPath = BETA_PATH_PATTERN.matcher(path).matches();
        return switch (audience) {
            case PUBLIC -> publicPath;
            case BETA -> betaPath;
            case INTERNAL -> !publicPath && !betaPath;
        };
    }

    private static String violation(
        HandlerMethod handlerMethod,
        String path,
        ApiAudience audience
    ) {
        return "%s ist als %s klassifiziert, der Pfad '%s' entspricht aber nicht der Konvention".formatted(
            handlerMethod.getShortLogMessage(),
            audience,
            path
        );
    }

    private static String missingAudienceViolation(HandlerMethod handlerMethod) {
        return "%s besitzt keine @ApiAudienceMarker-Annotation".formatted(
            handlerMethod.getShortLogMessage()
        );
    }
}
