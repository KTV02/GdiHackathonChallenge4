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

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcConfig.class);
    static final String DEPRECATED_HEADER = "X-PVOG-DEPRECATED";

    @Bean
    public WebMvcConfigurer initializeWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(deprecatedHeaderHandlerInterceptor());
            }
        };
    }

    /**
     * Abgekündigte Endpunkte sollen diese Eigenschaft in Form eines boolschen headers in ihren responses darstellen.
     * Um zusätzlichen Konfigurationsaufwand zu sparen, wird diese Eigenschaft aus den Annotationen der Endpunkte bzw
     * ihrer Controller-Klassen ausgelesen, spezifisch der Swagger-Operation-Annotation und der Java-Deprecated-Annotation
     * am Endpunkt oder der Klasse. Ist die deprecated-Eigenschaft an der Operation true oder eine Deprecated-Annotation
     * vorhanden, wird der PVOG-Header X-PVOG-DEPRECATED mit dem Wert 'true' an die response geschrieben.
     * <p>
     * Die (noch nicht) standardisierten Header Deprecated und Sunset werden hier nicht verwendet, da sie Datumsangaben
     * zur Abkündigung benötigen, die wir im Sourcecode nicht haben.
     */
    private static HandlerInterceptor deprecatedHeaderHandlerInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                HttpServletRequest request,
                HttpServletResponse response,
                Object handler
            ) {
                try {
                    if (handler instanceof HandlerMethod methodHandler) {
                        Class<?> controllerClass = methodHandler.getMethod().getDeclaringClass();
                        Operation operation = methodHandler.getMethodAnnotation(Operation.class);

                        boolean deprecatedOperation = operation != null && operation.deprecated();
                        boolean deprecatedMethod = methodHandler.hasMethodAnnotation(
                            Deprecated.class
                        );
                        boolean deprecatedClass =
                            controllerClass.getAnnotation(Deprecated.class) != null;
                        if (deprecatedOperation || deprecatedMethod || deprecatedClass) {
                            response.setHeader(DEPRECATED_HEADER, String.valueOf(true));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn(
                        "Der Deprecated-Status des Endpunkts '{}' konnte nicht ermittelt werden.",
                        request.getRequestURI(),
                        e
                    );
                }

                return true;
            }
        };
    }
}
