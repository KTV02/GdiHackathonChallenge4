// SPDX-License-Identifier: EUPL-1.2
// Copyright (c) 2026 Dataport AöR.
// Beschreibung: Regressionstest, der alle real registrierten Endpunkte gegen die Security-Chain prüft.
package de.dataport.suchdienst.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.dataport.suchdienst.SuchdienstInitializer;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceRequestMatcher;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Sicherheits-Regressionstest über <b>alle</b> tatsächlich registrierten Endpunkte der Anwendung.
 *
 * <p>Die übrigen Audience-Tests arbeiten mit eigens angelegten Test-Controllern und prüfen damit die
 * Mechanik ({@link ApiAudienceResolver}, {@link ApiAudienceRequestMatcher}). Dieser Test schließt die
 * verbleibende Lücke und prüft das Ergebnis am Produktivcode: Er startet den vollständigen
 * Anwendungskontext, liest alle bei Spring MVC registrierten Handler aus und schickt gegen jeden
 * einzelnen echte Requests durch die vollständige Security-Filterchain.
 *
 * <p>Damit ist die Zusicherung unabhängig davon regressionssicher, ob ein Endpunkt künftig über
 * Annotation, Pfad oder einen ganz anderen Mechanismus klassifiziert wird:
 *
 * <ul>
 *   <li>Jeder nicht öffentliche Endpunkt (insbesondere alle {@code /admin}-Endpunkte) beantwortet
 *       einen Request ohne Token mit {@code 401}.
 *   <li>Ein interner Endpunkt ist mit gültigem Token nicht mehr {@code 401}, die Annotation wirkt
 *       also tatsächlich als Freigabe und nicht nur als Sperre.
 *   <li>Öffentliche Endpunkte ({@code PUBLIC}/{@code BETA}) sind ohne Token erreichbar.
 * </ul>
 *
 * <p>Neue Endpunkte werden automatisch mitgeprüft, weil die Testfälle aus dem Handler-Mapping
 * erzeugt werden. Bewusste Ausnahmen müssen in {@link #INTENTIONALLY_UNAUTHENTICATED_PATH_PREFIXES}
 * dokumentiert werden.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@MockitoBean(types = SuchdienstInitializer.class)
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class EndpointSecurityRegressionTest {

    /** Endpunkte des Anwendungscodes, die in {@link WebSecurityConfig} bewusst freigegeben sind. */
    private static final List<String> INTENTIONALLY_UNAUTHENTICATED_PATH_PREFIXES = List.of(
        // wird über eigene Download-Tokens geschützt, siehe DownloadTokenController
        "/admin/download/xzufi"
    );

    private static final String APPLICATION_PACKAGE = "de.dataport.suchdienst.";
    private static final String VALID_TOKEN = "valid-test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private ApiAudienceResolver apiAudienceResolver;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpToken() {
        Jwt jwt = Jwt.withTokenValue(VALID_TOKEN)
            .header("alg", "none")
            .claim("sub", "regression-test-user")
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(jwt);
    }

    // ------------------------------------------------------------------
    // Absicherung der Testdatenbasis
    // ------------------------------------------------------------------

    /**
     * Stellt sicher, dass die {@link TestFactory}-Tests nicht stillschweigend leer laufen, falls sich
     * die Ermittlung der Endpunkte einmal ändert.
     */
    @Test
    void endpointsUnderTestShouldCoverAdminAndPublicApi() {
        List<Endpoint> endpoints = applicationEndpoints();

        assertThat(endpoints)
            .as("registrierte Anwendungs-Endpunkte")
            .isNotEmpty()
            .as("es müssen /admin-Endpunkte geprüft werden")
            .anyMatch(endpoint -> endpoint.path().startsWith("/admin"))
            .as("es muss mindestens ein INTERNAL-Endpunkt geprüft werden")
            .anyMatch(endpoint -> endpoint.audience().orElse(null) == ApiAudience.INTERNAL)
            .as("es muss mindestens ein öffentlicher Endpunkt geprüft werden")
            .anyMatch(Endpoint::isPubliclyAccessible);
    }

    // ------------------------------------------------------------------
    // Kern-Zusicherung: nichts Nicht-Öffentliches ohne Token erreichbar
    // ------------------------------------------------------------------

    /**
     * Prüft die zentrale Sicherheitszusicherung: Jeder Endpunkt, der nicht ausdrücklich als
     * {@code PUBLIC} oder {@code BETA} klassifiziert ist, wird ohne Token mit {@code 401} abgelehnt.
     *
     * <p>Die Prüfung ist bewusst unabhängig vom Klassifizierungsmechanismus: Sie erwartet {@code 401}
     * auch für Endpunkte, die überhaupt keine Audience haben, weil die Filterchain nicht
     * klassifizierte Requests per {@code denyAll} ablehnen muss (fail closed).
     */
    @TestFactory
    Stream<DynamicTest> nonPublicEndpointsShouldRequireAuthentication() {
        return applicationEndpoints()
            .stream()
            .filter(endpoint -> !endpoint.isPubliclyAccessible())
            .filter(endpoint -> !endpoint.isIntentionallyUnauthenticated())
            .map(endpoint ->
                DynamicTest.dynamicTest(endpoint.displayName(), () -> {
                    int status = mockMvc
                        .perform(endpoint.toRequestWithoutToken())
                        .andReturn()
                        .getResponse()
                        .getStatus();

                    assertThat(status)
                        .as(
                            "%s ist %s und muss ohne Token mit 401 abgelehnt werden",
                            endpoint.displayName(),
                            endpoint.audienceDescription()
                        )
                        .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
                })
            );
    }

    /**
     * Ergänzt {@link #nonPublicEndpointsShouldRequireAuthentication()} um die Gegenprobe: Mit gültigem
     * Token darf ein interner Endpunkt nicht mehr an der Authentifizierung scheitern.
     *
     * <p>Ein {@code 403} ist zulässig, weil viele interne Endpunkte zusätzlich eine Rolle verlangen,
     * die das Test-Token bewusst nicht mitbringt. Fachliche Fehler (z.B. {@code 400}/{@code 500}
     * mangels Elasticsearch) sind ebenfalls zulässig, da hier ausschließlich die Security-Entscheidung
     * geprüft wird.
     */
    @TestFactory
    Stream<DynamicTest> internalEndpointsShouldBeReachableWithValidToken() {
        return applicationEndpoints()
            .stream()
            .filter(endpoint -> endpoint.audience().orElse(null) == ApiAudience.INTERNAL)
            .filter(endpoint -> !endpoint.isIntentionallyUnauthenticated())
            .map(endpoint ->
                DynamicTest.dynamicTest(endpoint.displayName(), () -> {
                    int status = performIgnoringHandlerException(endpoint.toRequestWithToken());

                    assertThat(status)
                        .as(
                            "%s ist INTERNAL und darf mit gültigem Token nicht mehr an der Authentifizierung scheitern",
                            endpoint.displayName()
                        )
                        .isNotEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
                })
            );
    }

    // ------------------------------------------------------------------
    // Gegenprobe: öffentliche Endpunkte bleiben ohne Token erreichbar
    // ------------------------------------------------------------------

    /**
     * Stellt sicher, dass die Zentralisierung der Audience-Auflösung öffentliche Endpunkte nicht
     * versehentlich sperrt. Geprüft wird ausschließlich, dass die Security-Chain den Request
     * durchlässt; das fachliche Ergebnis ist hier ohne Belang.
     */
    @TestFactory
    Stream<DynamicTest> publicEndpointsShouldBeReachableWithoutToken() {
        return applicationEndpoints()
            .stream()
            .filter(Endpoint::isPubliclyAccessible)
            .map(endpoint ->
                DynamicTest.dynamicTest(endpoint.displayName(), () -> {
                    int status = performIgnoringHandlerException(endpoint.toRequestWithoutToken());

                    assertThat(status)
                        .as(
                            "%s ist %s und muss ohne Token erreichbar sein",
                            endpoint.displayName(),
                            endpoint.audienceDescription()
                        )
                        .isNotIn(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            HttpServletResponse.SC_FORBIDDEN
                        );
                })
            );
    }

    // ------------------------------------------------------------------
    // Explizite, gut lesbare Stichprobe auf /admin
    // ------------------------------------------------------------------

    /**
     * Bewusst redundante, aber unmittelbar lesbare Stichprobe auf den Admin-Bereich: Sie dokumentiert
     * die Erwartung auch dann, wenn die generierten Testfälle einmal nicht mehr greifen sollten.
     */
    @Test
    void adminEndpointsShouldRejectRequestsWithoutToken() throws Exception {
        List<Endpoint> adminEndpoints = applicationEndpoints()
            .stream()
            .filter(endpoint -> endpoint.path().startsWith("/admin"))
            .filter(endpoint -> !endpoint.isIntentionallyUnauthenticated())
            .toList();

        assertThat(adminEndpoints).as("/admin-Endpunkte").isNotEmpty();

        for (Endpoint endpoint : adminEndpoints) {
            mockMvc
                .perform(endpoint.toRequestWithoutToken())
                .andExpect(result ->
                    assertThat(result.getResponse().getStatus())
                        .as("%s ohne Token", endpoint.displayName())
                        .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED)
                );
        }
    }

    /** Ein unbekannter Pfad wird von {@code anyRequest().denyAll()} abgelehnt (fail closed). */
    @Test
    void unknownPathShouldBeRejectedWithoutToken() throws Exception {
        int status = mockMvc
            .perform(MockMvcRequestBuilders.get("/gibt-es-nicht"))
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(status).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // ------------------------------------------------------------------
    // Hilfsmittel
    // ------------------------------------------------------------------

    /**
     * Führt den Request aus und liefert den Status. Schlägt der Handler fachlich mit einer nicht
     * behandelten Exception fehl (z.B. weil im Test kein Elasticsearch erreichbar ist), hat die
     * Security-Chain den Request bereits durchgelassen; das wird als "nicht abgelehnt" gewertet.
     */
    private int performIgnoringHandlerException(MockHttpServletRequestBuilder request) {
        try {
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        } catch (Exception _) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }

    /** Liest alle Endpunkte des Anwendungscodes aus dem Handler-Mapping aus. */
    private List<Endpoint> applicationEndpoints() {
        return handlerMapping
            .getHandlerMethods()
            .entrySet()
            .stream()
            .filter(entry -> isApplicationHandler(entry.getValue()))
            .flatMap(this::toEndpoints)
            .sorted(Comparator.comparing(Endpoint::displayName))
            .toList();
    }

    private static boolean isApplicationHandler(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getName().startsWith(APPLICATION_PACKAGE);
    }

    private Stream<Endpoint> toEndpoints(
        Map.Entry<RequestMappingInfo, HandlerMethod> handlerMappingEntry
    ) {
        RequestMappingInfo mapping = handlerMappingEntry.getKey();
        Optional<ApiAudience> audience = apiAudienceResolver.resolve(
            handlerMappingEntry.getValue()
        );
        return new TreeSet<>(mapping.getPatternValues())
            .stream()
            .map(pattern -> new Endpoint(mapping, pattern, audience));
    }

    /** Ein konkret aufrufbarer Endpunkt: Pfadmuster, HTTP-Methode und erwartete Audience. */
    private record Endpoint(
        RequestMappingInfo mapping,
        String pattern,
        Optional<ApiAudience> audience
    ) {
        String path() {
            // Pfadvariablen wie '{id}' oder '{id:.+}' durch einen aufrufbaren Wert ersetzen
            return pattern.replaceAll("\\{[^/]+?}", "1");
        }

        HttpMethod httpMethod() {
            return mapping
                .getMethodsCondition()
                .getMethods()
                .stream()
                .findFirst()
                .map(requestMethod -> HttpMethod.valueOf(requestMethod.name()))
                .orElse(HttpMethod.GET);
        }

        boolean isPubliclyAccessible() {
            return audience
                .filter(value -> value == ApiAudience.PUBLIC || value == ApiAudience.BETA)
                .isPresent();
        }

        boolean isIntentionallyUnauthenticated() {
            return INTENTIONALLY_UNAUTHENTICATED_PATH_PREFIXES.stream().anyMatch(
                pattern::startsWith
            );
        }

        String audienceDescription() {
            return audience.map(ApiAudience::name).orElse("nicht klassifiziert");
        }

        String displayName() {
            return "%s %s [%s]".formatted(httpMethod(), pattern, audienceDescription());
        }

        MockHttpServletRequestBuilder toRequestWithoutToken() {
            return buildRequest();
        }

        MockHttpServletRequestBuilder toRequestWithToken() {
            return buildRequest().header("Authorization", "Bearer " + VALID_TOKEN);
        }

        /**
         * Baut einen Request, der die Bedingungen des Mappings (Methode, Content-Type, Accept,
         * geforderte Parameter und Header) erfüllt. Nur so wird derselbe Handler aufgelöst, den auch
         * die Security-Chain bewerten muss.
         */
        private MockHttpServletRequestBuilder buildRequest() {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(
                httpMethod(),
                path()
            );

            Set<MediaType> consumable = mapping.getConsumesCondition().getConsumableMediaTypes();
            if (!consumable.isEmpty()) {
                MediaType contentType = consumable.iterator().next();
                request.contentType(contentType).content(exampleBody(contentType));
            }

            Set<MediaType> producible = mapping.getProducesCondition().getProducibleMediaTypes();
            MediaType[] acceptedMediaTypes = producible.isEmpty()
                ? new MediaType[] { MediaType.ALL }
                : producible.toArray(MediaType[]::new);
            request.accept(acceptedMediaTypes);

            mapping
                .getParamsCondition()
                .getExpressions()
                .forEach(expression -> applyParam(request, expression));
            mapping
                .getHeadersCondition()
                .getExpressions()
                .forEach(expression -> applyHeader(request, expression));

            return request;
        }

        private static String exampleBody(MediaType contentType) {
            if (MediaType.APPLICATION_JSON.includes(contentType)) {
                return "{}";
            }
            if (
                MediaType.APPLICATION_XML.includes(contentType) ||
                MediaType.TEXT_XML.includes(contentType)
            ) {
                return "<root/>";
            }
            return "";
        }

        private static void applyParam(
            MockHttpServletRequestBuilder request,
            NameValueExpression<String> expression
        ) {
            if (!expression.isNegated()) {
                request.param(
                    expression.getName(),
                    Optional.ofNullable(expression.getValue()).orElse("1")
                );
            }
        }

        private static void applyHeader(
            MockHttpServletRequestBuilder request,
            NameValueExpression<String> expression
        ) {
            if (!expression.isNegated()) {
                request.header(
                    expression.getName(),
                    Optional.ofNullable(expression.getValue()).orElse("1")
                );
            }
        }
    }
}
