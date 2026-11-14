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
package de.dataport.suchdienst.config.apiaudience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Verifiziert End-to-End (echter Anwendungskontext, echtes {@link
 * org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}), dass
 * {@link ApiAudienceStartupValidation} den Applikationsstart abbricht, sobald ein Endpunkt mit
 * einer {@link ApiAudience} annotiert ist, die nicht zu seinem tatsächlich registrierten Pfad
 * passt. Ergänzt {@link ApiAudienceStartupValidationTest}, der nur die reine
 * Pfad-Pattern-Prüfung isoliert testet.
 */
class ApiAudienceStartupValidationBootTest {

    private final WebApplicationContextRunner contextRunner =
        new WebApplicationContextRunner().withUserConfiguration(
            EnableWebMvcConfig.class,
            ApiAudienceResolver.class,
            ApiAudienceStartupValidation.class
        );

    @Test
    void contextFailsToStartWhenAudienceDoesNotMatchPathConvention() {
        contextRunner
            .withUserConfiguration(MisconfiguredInternalOnPublicPathController.class)
            .run(context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("entspricht aber nicht der Konvention")
            );
    }

    @Test
    void contextStartsSuccessfullyWhenAudienceMatchesPathConvention() {
        contextRunner
            .withUserConfiguration(CorrectlyConfiguredInternalController.class)
            .run(context -> assertThat(context).hasNotFailed());
    }

    /**
     * Der Actuator registriert ein zweites {@link RequestMappingHandlerMapping}. Ohne
     * {@code @Qualifier} auf dem Konstruktorparameter schlägt die Injektion dann mit
     * "required a single bean, but 2 were found" fehl.
     */
    @Test
    void contextStartsWhenAdditionalHandlerMappingBeanExists() {
        contextRunner
            .withUserConfiguration(
                CorrectlyConfiguredInternalController.class,
                AdditionalHandlerMappingConfig.class
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("controllerEndpointHandlerMapping");
            });
    }

    @EnableWebMvc
    static class EnableWebMvcConfig {}

    static class AdditionalHandlerMappingConfig {

        @Bean
        RequestMappingHandlerMapping controllerEndpointHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }
    }

    @RestController
    @ApiAudienceMarker(ApiAudience.INTERNAL)
    static class MisconfiguredInternalOnPublicPathController {

        @GetMapping(path = "/v1/misconfigured")
        String misconfigured() {
            return "A";
        }
    }

    @RestController
    @ApiAudienceMarker(ApiAudience.INTERNAL)
    static class CorrectlyConfiguredInternalController {

        @GetMapping(path = "/admin/correct")
        String correct() {
            return "A";
        }
    }
}
