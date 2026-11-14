// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Löst die ApiAudience eines Handlers zentral für Security, Logging und OpenAPI auf.
package de.dataport.suchdienst.config.apiaudience;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Löst die {@link ApiAudience} eines Handlers anhand der {@link ApiAudienceMarker}-Annotation auf.
 *
 * <p>Aufrufer kennen je nach Kontext ein unterschiedliches Objekt und nutzen dafür einen von drei
 * unabhängigen Einstiegspunkten ("Strängen") dieser Klasse:
 *
 * <ol>
 *   <li><b>Request-Strang</b> ({@link #resolve(HttpServletRequest)}, {@link
 *       #hasAudience(HttpServletRequest, ApiAudience, ApiAudience...)}, {@link
 *       #isPubliclyAccessible(HttpServletRequest)}): Security-Filter und Logging kennen nur den
 *       eingehenden Request. Der zugehörige MVC-Handler wird darüber aufgelöst, das Ergebnis als
 *       Request-Attribut gecacht und die eigentliche Auflösung an den HandlerMethod-Strang
 *       delegiert.
 *   <li><b>HandlerMethod-Strang</b> ({@link #resolve(HandlerMethod)}): wird sowohl vom
 *       Request-Strang (sobald der Handler feststeht) als auch direkt von der Startup-Validierung
 *       genutzt, die über alle bei Spring MVC registrierten Handler-Methoden iteriert.
 *   <li><b>Method-Strang</b> ({@link #hasAudience(Method, ApiAudience)}): die OpenAPI-Gruppierung
 *       kennt nur die rohe Java-Methode ohne Handler- bzw. Bean-Kontext.
 * </ol>
 *
 * <p>Alle drei Stränge münden in dieselbe Annotations-Auflösung ({@link
 * #resolveAnnotatedAudience(Method, Class)}): HandlerMethod- und Method-Strang rufen sie direkt
 * auf, der Request-Strang indirekt über den HandlerMethod-Strang.
 */
@Slf4j
@Component
public class ApiAudienceResolver {

    // ---------------------------------------------------------------------------------
    // Request-Strang: HttpServletRequest -> MVC-Handler -> Audience, inkl. Request-Cache
    // Aufrufer: Security-Filter (ApiAudienceRequestMatcher, RequestParameterFilter)
    // ---------------------------------------------------------------------------------

    private static final String REQUEST_ATTRIBUTE_KEY =
        ApiAudienceResolver.class.getName() + ".audience";
    private static final String REQUEST_ATTRIBUTE_RESOLVED_KEY =
        ApiAudienceResolver.class.getName() + ".resolved";

    private final Optional<RequestMappingHandlerMapping> handlerMapping;

    /**
     * {@link RequestMappingHandlerMapping} ist optional, damit dieser Bean auch in Kontexten ohne
     * Spring-MVC (z.B. {@code webEnvironment = NONE}-Tests) angelegt werden kann. Ohne Handler-
     * Mapping liefert {@link #resolve(HttpServletRequest)} stets {@link Optional#empty()}.
     */
    public ApiAudienceResolver(
        @Qualifier(
            "requestMappingHandlerMapping"
        ) Optional<RequestMappingHandlerMapping> handlerMapping
    ) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Löst und cached die Audience des zu einem Request gehörenden MVC-Handlers.
     *
     * <p>Kann der Handler aus technischen Gründen nicht ermittelt werden (z.B. Spring MVC wirft
     * beim Auflösen eine Exception), wird das geloggt und {@link Optional#empty()} geliefert,
     * statt die Exception weiterzuwerfen. Damit bleiben alle Aufrufer (Security, Logging)
     * automatisch fail-closed, ohne die Exception selbst behandeln zu müssen.
     */
    public Optional<ApiAudience> resolve(HttpServletRequest request) {
        Object cachedAudience = request.getAttribute(REQUEST_ATTRIBUTE_KEY);
        if (cachedAudience instanceof ApiAudience audience) {
            return Optional.of(audience);
        }
        if (Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTRIBUTE_RESOLVED_KEY))) {
            return Optional.empty();
        }

        Object handler = resolveHandler(request);
        return resolveAndCache(request, handler);
    }

    /** Prüft, ob der zu einem Request gehörende Handler zu einer der erwarteten Audiences gehört. */
    public boolean hasAudience(
        HttpServletRequest request,
        ApiAudience firstExpectedAudience,
        ApiAudience... furtherExpectedAudiences
    ) {
        Set<ApiAudience> expectedAudiences = EnumSet.of(
            firstExpectedAudience,
            furtherExpectedAudiences
        );
        return resolve(request).filter(expectedAudiences::contains).isPresent();
    }

    /** Prüft, ob der zu einem Request gehörende Handler öffentlich (PUBLIC oder BETA) ist. */
    public boolean isPubliclyAccessible(HttpServletRequest request) {
        return hasAudience(request, ApiAudience.PUBLIC, ApiAudience.BETA);
    }

    private Object resolveHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        try {
            HandlerExecutionChain handlerExecutionChain = handlerMapping.get().getHandler(request);
            return handlerExecutionChain == null ? null : handlerExecutionChain.getHandler();
        } catch (Exception e) {
            log.warn(
                "API-Audience für Anfrage an '{}' konnte nicht aufgelöst werden, Request wird als nicht klassifiziert behandelt",
                request.getRequestURI(),
                e
            );
            return null;
        }
    }

    private Optional<ApiAudience> resolveAndCache(HttpServletRequest request, Object handler) {
        Optional<ApiAudience> audience =
            handler instanceof HandlerMethod handlerMethod
                ? resolve(handlerMethod)
                : Optional.empty();
        audience.ifPresent(value -> request.setAttribute(REQUEST_ATTRIBUTE_KEY, value));
        request.setAttribute(REQUEST_ATTRIBUTE_RESOLVED_KEY, true);
        return audience;
    }

    // ---------------------------------------------------------------------------------
    // HandlerMethod-Strang: intern vom Request-Strang genutzt (sobald der Handler feststeht),
    // extern von ApiAudienceStartupValidation, die beim Start alle registrierten MVC-Handler
    // gegen ihre Audience prüft
    // ---------------------------------------------------------------------------------

    /**
     * Löst die {@link ApiAudience} für einen {@link HandlerMethod} auf.
     *
     * @return die Audience, falls annotiert; sonst {@link Optional#empty()}
     */
    public Optional<ApiAudience> resolve(HandlerMethod handlerMethod) {
        return resolveAnnotatedAudience(handlerMethod.getMethod(), handlerMethod.getBeanType());
    }

    // ---------------------------------------------------------------------------------
    // Method-Strang: Aufrufer ist OpenApiConfig, die zur Gruppierung der OpenAPI-Doku nur
    // die rohe Java-Methode ohne Handler- bzw. Bean-Kontext kennt
    // ---------------------------------------------------------------------------------

    /** Prüft, ob eine Methode zur erwarteten Audience gehört. */
    public boolean hasAudience(Method method, ApiAudience expectedAudience) {
        return resolveAnnotatedAudience(method, method.getDeclaringClass())
            .filter(expectedAudience::equals)
            .isPresent();
    }

    // ---------------------------------------------------------------------------------
    // Gemeinsame Annotations-Auflösung, in der alle drei Stränge zusammenlaufen
    // ---------------------------------------------------------------------------------

    /**
     * Löst die {@link ApiAudience} auf: Methoden-Annotation hat Vorrang vor Klassen-Annotation.
     *
     * @return die Audience, falls annotiert; sonst {@link Optional#empty()}
     */
    private Optional<ApiAudience> resolveAnnotatedAudience(
        Method method,
        Class<?> controllerClass
    ) {
        ApiAudienceMarker methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
            method,
            ApiAudienceMarker.class
        );
        if (methodAnnotation != null) {
            return Optional.of(methodAnnotation.value());
        }

        ApiAudienceMarker classAnnotation = AnnotatedElementUtils.findMergedAnnotation(
            controllerClass,
            ApiAudienceMarker.class
        );
        if (classAnnotation != null) {
            return Optional.of(classAnnotation.value());
        }

        return Optional.empty();
    }
}
