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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles({ "default", "test" })
@SpringBootTest(
    classes = {
        WebSecurityConfig.class,
        ApiAudienceResolver.class,
        SuchdienstProperties.class,
        RestErrorAuthenticationEntryPoint.class,
        KeycloakProperties.class,
        WebSecurityConfigFilterChainTest.TestBeans.class,
        WebSecurityConfigFilterChainTest.PublicAudienceController.class,
        WebSecurityConfigFilterChainTest.ClassInternalAudienceController.class,
    },
    properties = {
        "keycloak.auth-server-url=https://example.test/auth",
        "keycloak.realm=pvog",
        "keycloak.resource=suchdienst",
    }
)
@EnableWebMvc
@AutoConfigureMockMvc
class WebSecurityConfigFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomCorsProperties corsProperties;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        when(corsProperties.isEnabled()).thenReturn(false);

        jwt = Jwt.withTokenValue("good-token")
            .header("alg", "none")
            .claim("sub", "test-user")
            .issuedAt(java.time.Instant.now().minusSeconds(3600))
            .expiresAt(java.time.Instant.now().plusSeconds(3600))
            .build();
        when(jwtDecoder.decode("good-token")).thenReturn(jwt);
    }

    @Test
    void publicAudienceShouldBePermitAllWithoutToken() throws Exception {
        mockMvc.perform(get("/audience/public")).andExpect(status().isOk());
    }

    @Test
    void betaAudienceShouldBePermitAllWithoutToken() throws Exception {
        mockMvc.perform(get("/audience/beta")).andExpect(status().isOk());
    }

    @Test
    void classLevelInternalAudienceShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/audience/class-internal")).andExpect(status().isUnauthorized());
    }

    @Test
    void classLevelInternalAudienceShouldAllowAuthenticatedRequest() throws Exception {
        mockMvc
            .perform(get("/audience/class-internal").header("Authorization", "Bearer good-token"))
            .andExpect(status().isOk());
    }

    @Test
    void methodLevelAudienceShouldOverrideClassLevelAudience() throws Exception {
        mockMvc.perform(get("/audience/override-public")).andExpect(status().isOk());
    }

    @Test
    void unclassifiedRequestShouldBeDeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/unknown")).andExpect(status().isUnauthorized());
    }

    @Test
    void unclassifiedRequestShouldBeDeniedWithToken() throws Exception {
        mockMvc
            .perform(get("/unknown").header("Authorization", "Bearer good-token"))
            .andExpect(status().isForbidden());
    }

    static class TestBeans {

        @Bean
        WebEndpointProperties webEndpointProperties() {
            WebEndpointProperties properties = new WebEndpointProperties();
            properties.setBasePath("/actuator");
            return properties;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        org.springframework.context.MessageSource messageSource() {
            org.springframework.context.support.ResourceBundleMessageSource ms =
                new org.springframework.context.support.ResourceBundleMessageSource();
            ms.setBasenames("static-text/static_text_resource");
            ms.setDefaultEncoding("UTF-8");
            ms.setUseCodeAsDefaultMessage(false);
            return ms;
        }
    }

    @RestController
    @RequestMapping("/audience")
    static class PublicAudienceController {

        @GetMapping("/public")
        @ApiAudienceMarker(ApiAudience.PUBLIC)
        String publicEndpoint() {
            return "ok";
        }

        @GetMapping("/beta")
        @ApiAudienceMarker(ApiAudience.BETA)
        String betaEndpoint() {
            return "ok";
        }
    }

    @RestController
    @ApiAudienceMarker(ApiAudience.INTERNAL)
    @RequestMapping("/audience")
    static class ClassInternalAudienceController {

        @GetMapping("/class-internal")
        String classInternalEndpoint() {
            return "ok";
        }

        @GetMapping("/override-public")
        @ApiAudienceMarker(ApiAudience.PUBLIC)
        String overridePublicEndpoint() {
            return "ok";
        }
    }
}
