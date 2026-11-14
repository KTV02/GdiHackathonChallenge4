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
package de.dataport.suchdienst.config;

import de.dataport.pvog.keycloak.KeycloakRoleAuthoritiesConverter;
import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceRequestMatcher;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import jakarta.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Security config: enables OAUTH CORS and CSRF when not explicitly deactivated
 */
@Profile("!" + Profiles.SECURITY_OFF)
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@Slf4j
public class WebSecurityConfig implements CorsConfig {

    private static final String SWAGGER_UI_PATH = "springdoc.swagger-ui.path";
    private static final String PATH_LIMITER = "/";

    private final Environment env;
    private final CustomCorsProperties corsProperties;
    private final KeycloakProperties keycloakProperties;
    private final WebEndpointProperties endpointProperties;
    private final ServletContext ctx;
    private final WebEndpointProperties actuator;
    private final RestErrorAuthenticationEntryPoint entryPoint;
    private final Optional<SpringDocConfigProperties> springDocConfigProperties;
    private final ObjectMapper objectMapper;
    private final SuchdienstProperties suchdienstProperties;

    public WebSecurityConfig(
        Environment env,
        ServletContext ctx,
        WebEndpointProperties actuator,
        CustomCorsProperties corsProperties,
        KeycloakProperties keycloakProperties,
        WebEndpointProperties endpointProperties,
        RestErrorAuthenticationEntryPoint entryPoint,
        Optional<SpringDocConfigProperties> springDocConfigProperties,
        ObjectMapper objectMapper,
        SuchdienstProperties suchdienstProperties
    ) {
        this.env = env;
        this.ctx = ctx;
        this.actuator = actuator;
        this.corsProperties = corsProperties;
        this.keycloakProperties = keycloakProperties;
        this.endpointProperties = endpointProperties;
        this.entryPoint = entryPoint;
        this.springDocConfigProperties = springDocConfigProperties;
        this.objectMapper = objectMapper;
        this.suchdienstProperties = suchdienstProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        ApiAudienceResolver apiAudienceResolver
    ) {
        // Konfigurierbar, Standard ist '/actuator'.
        final String actuatorBasePath = endpointProperties.getBasePath();
        final String actuatorBasePathSlash = actuatorBasePath.endsWith(PATH_LIMITER)
            ? actuatorBasePath
            : actuatorBasePath + PATH_LIMITER;

        List<String> unauthenticatedEndpoints = new ArrayList<>();
        unauthenticatedEndpoints.add(actuatorBasePathSlash + "health");
        unauthenticatedEndpoints.add(actuatorBasePathSlash + "health/readiness");
        unauthenticatedEndpoints.add(actuatorBasePathSlash + "health/liveness");
        unauthenticatedEndpoints.add(actuatorBasePathSlash + "prometheus");
        // API für XZuFI Download wird über eigene Tokens geschützt, um den Download im Browser zu ermöglichen
        // siehe DownloadTokenController und DownloadImportRequestTokenService
        unauthenticatedEndpoints.add("/admin/download/xzufi");

        springDocConfigProperties.ifPresent(config -> {
            if (config.getApiDocs() != null) {
                List<String> swaggerEndpointPatterns = getSwaggerEndpointPatterns(
                    config.getApiDocs().getPath(),
                    env.getProperty(SWAGGER_UI_PATH)
                );
                unauthenticatedEndpoints.addAll(swaggerEndpointPatterns);
            }
        });

        http.cors(Customizer.withDefaults())
            .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .csrf(AbstractHttpConfigurer::disable) // FIXME erstmal abschalten, um Push vom SaD zu ermoeglichen
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh ->
                eh.accessDeniedHandler(entryPoint).authenticationEntryPoint(entryPoint)
            )
            .addFilterBefore(
                new RequestParameterFilter(
                    ctx,
                    actuator,
                    objectMapper,
                    apiAudienceResolver,
                    suchdienstProperties.isPublicApiAccessLogEnabled()
                ),
                CsrfFilter.class
            )
            .oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                )
            );

        RequestMatcher publicOrBetaAudienceMatcher = new ApiAudienceRequestMatcher(
            apiAudienceResolver,
            ApiAudience.PUBLIC,
            ApiAudience.BETA
        );
        RequestMatcher internalAudienceMatcher = new ApiAudienceRequestMatcher(
            apiAudienceResolver,
            ApiAudience.INTERNAL
        );

        http.authorizeHttpRequests(authorizeRequests ->
            authorizeRequests
                // no authentication required
                .requestMatchers(unauthenticatedEndpoints.toArray(String[]::new))
                .permitAll()
                .requestMatchers(publicOrBetaAudienceMatcher)
                .permitAll()
                // authentication required
                .requestMatchers("/error/**", "/error")
                .authenticated()
                .requestMatchers(internalAudienceMatcher)
                .authenticated()
                // authentication and role required
                .requestMatchers(actuator.getBasePath() + "/info")
                .hasRole("o_sud_spring_actuator_info")
                .requestMatchers(actuator.getBasePath() + "/metrics/**")
                .hasRole("o_sud_spring_actuator_metrics")
                .requestMatchers(actuator.getBasePath() + "/env")
                .hasRole("o_sud_spring_actuator_env")
                .requestMatchers(actuator.getBasePath() + "/loggers")
                .hasRole("o_sud_spring_actuator_loggers")
                .requestMatchers(actuator.getBasePath() + "/logfile")
                .hasRole("o_sud_spring_actuator_logfile")
                .anyRequest()
                .denyAll()
        );

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(keycloakProperties.getJwkSetUri()).build();
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy(); // BSI Session-Mngmt (vgl. PV-1990)
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return getCorsConfiguration(corsProperties);
    }

    /**
     * Der Spring Boot default authentication converter kann die Rollen unserer Keycloak-JWT nicht lesen.
     * Wir brauchen daher einen eigenen. Sollten wir die Art, wie wir in Keycloak die Rollen spezifizieren,
     * mal ändern, muss dies hier angepasst werden, oder es reicht dann vielleicht der default converter.
     *
     * @return Ein für Keycloak-Rollen angepasster JwtAuthenticationConverter
     */
    private JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            KeycloakRoleAuthoritiesConverter roleConverter = new KeycloakRoleAuthoritiesConverter(
                keycloakProperties.getResource()
            );

            Collection<String> roleStrings = roleConverter.convert(jwt.getClaims());

            return roleStrings
                .stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        });

        return jwtAuthenticationConverter;
    }

    /**
     * Die Bean UserDetailsService wird definiert, um die UserDetailsServiceAutoConfiguration zu umgehen, die
     * andernfalls unerwünschte Sachen macht. Vor dem Wegfall des KeycloakAdapter wurde dazu der AuthenticationManager
     * konfiguriert. Jetzt führt das aber zu Stack Overflows bei der Validierung invalider bearer tokens, ausgelöst
     * von einer Konfigurationsschleife in Spring Security 5 bei Verwendung von http.oauth2ResourceServer().
     * Stattdessen definieren wir jetzt einen (inaktiven) UserDetailsService, der im Moment den gleich Zweck ohne die
     * Konfigurationsschleife erfüllt.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UnsupportedOperationException("unsupported");
        };
    }

    /**
     * Die Berechnung der Endpunkt-Patterns, die für Swagger freigeschaltet werden müssen, ist nicht trivial.
     * Um diese Berechnung testen zu können, ist sie in eine (protected) static-Methode der Config-Klasse
     * ausgelagert.
     *
     * @param apiDocPath    der Wert von springdoc.api-docs.path, default: '/v3/api-docs'
     * @param swaggerUIPath der Wert von springdoc.swagger-ui.path, default: '/swagger-ui.html'
     * @return Die drei Pattern '/v3/api-docs/**', '/swagger-ui.html/**' und '/swagger-ui/**', angepasst an die
     * vielleicht überschriebenen Werte der Eingangsparameter.
     */
    List<String> getSwaggerEndpointPatterns(String apiDocPath, String swaggerUIPath) {
        List<String> swaggerEndpointPatterns = new ArrayList<>();

        // Abfangen von Eingaben, die zu einer unerwünschten Freigabe von Nicht-Swagger-Endpunkten führen würden:
        if (StringUtils.isBlank(apiDocPath) || apiDocPath.equals("/") || apiDocPath.contains("*")) {
            MdcTemplate.runWith(MdcKeys.PROTOKOLLTYP, ProtocolType.SYSTEM.name(), () ->
                log.warn(
                    "Die Swagger Property springdoc.api-docs.path ('{}') ist so gesetzt, dass es zu unerwünschten Endpunktfreigaben kommen würde. Zur Sicherheit wird kein Swagger-Endpunkt freigegeben.",
                    apiDocPath
                )
            );

            // Liefere die leere Liste, dadurch werden keine Endpunkte freigegeben (Exception handling unnötig).
            return Collections.emptyList();
        }

        if (swaggerUIPath != null && (swaggerUIPath.equals("/") || swaggerUIPath.contains("*"))) {
            MdcTemplate.runWith(MdcKeys.PROTOKOLLTYP, ProtocolType.SYSTEM.name(), () ->
                log.warn(
                    "Die Swagger Property springdoc.swagger-ui.path ('{}') ist so gesetzt, dass es zu unerwünschten Endpunktfreigaben kommen würde. Zur Sicherheit wird kein Swagger-Endpunkt freigegeben.",
                    swaggerUIPath
                )
            );
            // Liefere die leere Liste, dadurch werden keine Endpunkte freigegeben (Exception handling unnötig).
            return Collections.emptyList();
        }

        final String apiDocPathSuffix = apiDocPath.endsWith(PATH_LIMITER) ? "**" : "/**";
        swaggerEndpointPatterns.add(apiDocPath + apiDocPathSuffix);

        // Der springdoc.swagger-ui.path Standard-Pfad '/swagger-ui.html' sieht nach einer Datei aus, ist aber
        // tatsächlich ein Pfad, der verlängert werden kann zu '/swagger-ui.html/xy'. Entsprechend muss das
        // Pattern '<path>/**' erzeugt werden. Außerdem baut Swagger immer Pfade mit ein konstanten Teil
        // '/swagger-ui', unabhängig davon, ob der Ausdruck 'swagger-ui.html' verwendet wird oder nicht.
        if (StringUtils.isBlank(swaggerUIPath) || "/swagger-ui.html".equals(swaggerUIPath)) {
            swaggerEndpointPatterns.add("/swagger-ui.html/**");
            swaggerEndpointPatterns.add("/swagger-ui/**");
        } else if (swaggerUIPath.endsWith(PATH_LIMITER)) {
            swaggerEndpointPatterns.add(swaggerUIPath + "**");
            // '/a/b/' -> /a/b/swagger-ui/**'
            swaggerEndpointPatterns.add(swaggerUIPath + "swagger-ui/**");
        } else {
            swaggerEndpointPatterns.add(swaggerUIPath + "/**");
            // '/a/b' -> /a/swagger-ui/**'
            // Erzeuge eine erweiterbare Liste aus den Teilen des Pfads. Eine mit Arrays.asList() erzeugte
            // Liste wäre nicht erweiterbar bzw. müsste dafür nochmal kopiert werden.
            List<String> pathItems = new ArrayList<>(20);
            pathItems.addAll(Arrays.asList(swaggerUIPath.split("/")));
            // Entferne den letzten Teil
            if (pathItems.size() > 1) {
                pathItems.removeLast();
            }
            // Füge die Konstante und den Wildcard dazu
            pathItems.add("swagger-ui");
            pathItems.add("**");
            swaggerEndpointPatterns.add(String.join("/", pathItems));
        }
        return swaggerEndpointPatterns;
    }

    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return new SuchdienstRequestRejectedHandler();
    }
}
