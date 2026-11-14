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

import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import jakarta.servlet.ServletContext;
import java.util.Optional;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Dev security Config, disables OAUTH and CSRF when keycloak is disabled
 */
@Profile(Profiles.SECURITY_OFF)
@Configuration
@EnableWebSecurity
public class DevSecurityConfig implements CorsConfig {

    private final CustomCorsProperties properties;
    private final ServletContext ctx;
    private final WebEndpointProperties actuator;
    private final ObjectMapper objectMapper;
    private final ApiAudienceResolver apiAudienceResolver;

    public DevSecurityConfig(
        CustomCorsProperties properties,
        ServletContext ctx,
        Optional<WebEndpointProperties> actuator,
        ObjectMapper objectMapper,
        ApiAudienceResolver apiAudienceResolver
    ) {
        this.properties = properties;
        this.ctx = ctx;
        // In manchen (schlanken) Test-Kontexten (z. B. @WebMvcTest ohne Actuator-Autoconfiguration) steht
        // kein WebEndpointProperties-Bean zur Verfügung. In diesem Fall wird auf die Default-Werte
        // zurückgefallen (Actuator-Basispfad '/actuator').
        this.actuator = actuator.orElseGet(WebEndpointProperties::new);
        this.objectMapper = objectMapper;
        this.apiAudienceResolver = apiAudienceResolver;
    }

    @Bean
    @SuppressWarnings("java:S4502") //im Profile security_off ist keine CSRF-Protection erforderlich
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(
                new RequestParameterFilter(ctx, actuator, objectMapper, apiAudienceResolver, true),
                CsrfFilter.class
            )
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests.requestMatchers("/**").permitAll()
            );
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        // Damit die UserDetailsServiceAutoConfiguration nicht greift und einen default User mit default Passwort
        // erstellt und das ins Log schreibt, definieren wir hier einen UserDetailsManager, der keine User kennt.
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return getCorsConfiguration(properties);
    }

    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return new SuchdienstRequestRejectedHandler();
    }
}
