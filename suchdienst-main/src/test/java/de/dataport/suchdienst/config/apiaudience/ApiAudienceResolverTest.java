// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Tests für zentrale Audience-Auflösung und Request-Caching.
package de.dataport.suchdienst.config.apiaudience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class ApiAudienceResolverTest {

    private final RequestMappingHandlerMapping handlerMapping = Mockito.mock(
        RequestMappingHandlerMapping.class
    );
    private final ApiAudienceResolver resolver = new ApiAudienceResolver(
        Optional.of(handlerMapping)
    );

    @Test
    void methodAudienceOverridesClassAudience() throws Exception {
        HandlerMethod handlerMethod = handlerMethod("publicEndpoint");

        assertThat(resolver.resolve(handlerMethod)).contains(ApiAudience.PUBLIC);
    }

    @Test
    void requestAudienceIsResolvedOnlyOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/test");
        when(handlerMapping.getHandler(request)).thenReturn(
            new HandlerExecutionChain(handlerMethod("publicEndpoint"))
        );

        assertThat(resolver.resolve(request)).contains(ApiAudience.PUBLIC);
        assertThat(resolver.resolve(request)).contains(ApiAudience.PUBLIC); // Wiederholung prüft den Request-Cache.

        verify(handlerMapping, times(1)).getHandler(request);
    }

    @Test
    void missingHandlerIsCachedAsMissingAudience() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unknown");
        when(handlerMapping.getHandler(request)).thenReturn(null);

        assertThat(resolver.resolve(request)).isEmpty();
        assertThat(resolver.resolve(request)).isEmpty(); // Auch ein fehlendes Ergebnis muss gecacht werden.

        verify(handlerMapping, times(1)).getHandler(request);
    }

    @Test
    void exceptionDuringHandlerResolutionIsTreatedAsMissingAudience() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/broken");
        when(handlerMapping.getHandler(request)).thenThrow(new IllegalArgumentException("boom"));

        assertThat(resolver.resolve(request)).isEmpty();
        // Exception wird nur einmal ausgelöst, das (leere) Ergebnis wird danach gecached
        assertThat(resolver.resolve(request)).isEmpty();

        verify(handlerMapping, times(1)).getHandler(request);
    }

    @Test
    void missingHandlerMappingIsTreatedAsMissingAudience() {
        ApiAudienceResolver resolverWithoutMvc = new ApiAudienceResolver(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/test");

        assertThat(resolverWithoutMvc.resolve(request)).isEmpty();
    }

    private static HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(
            new InternalController(),
            InternalController.class.getDeclaredMethod(methodName)
        );
    }

    @ApiAudienceMarker(ApiAudience.INTERNAL)
    private static class InternalController {

        @ApiAudienceMarker(ApiAudience.PUBLIC)
        void publicEndpoint() {
            // Bewusst ohne Rumpf: Die Methode dient allein als Träger der Annotationen und wird
            // nur per Reflection über HandlerMethod ausgelesen, aber nie aufgerufen.
        }
    }
}
