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
package de.dataport.suchdienst;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@MockitoBean(types = SuchdienstInitializer.class)
@ActiveProfiles({ "default", "dev", "openapi_show_internal" })
class MappingTest {

    private static final String REQUEST_MAPPING_NO_DEFAULT;

    static {
        try {
            var defaultValueMethod = RequestParam.class.getMethod("defaultValue");
            REQUEST_MAPPING_NO_DEFAULT = (String) defaultValueMethod.getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    @SuppressWarnings("java:S2178")
    void testMappings() {
        AtomicBoolean hasErrors = new AtomicBoolean(false);
        requestMappingHandlerMapping
            .getHandlerMethods()
            .forEach((requestMappingInfo, handlerMethod) ->
                hasErrors
                    // Hinweis: Der "|" ist hier gewünscht, er hat im Gegensatz zum "||" keine Kurzschluss-Evaluation
                    // und inspectHandlerMethod wird somit für alle HandlerMethoden aufgerufen, das ist gewünscht, um
                    // alle Fehler zu protokollieren und nicht nur den ersten.
                    .set(hasErrors.get() | inspectHandlerMethod(handlerMethod))
            );
        assertThat("ist fehlerfrei", hasErrors.get(), is(false));
    }

    private boolean inspectHandlerMethod(HandlerMethod handlerMethod) {
        boolean isErroneousMethod = false;
        boolean isMethodNameLogged = false;
        Method method = handlerMethod.getMethod();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            io.swagger.v3.oas.annotations.Parameter openApiParameter = parameter.getAnnotation(
                io.swagger.v3.oas.annotations.Parameter.class
            );
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (openApiParameter == null || requestParam == null) {
                continue;
            }
            boolean requiredAsOfOpenApi = openApiParameter.required();
            boolean requiredAsOfSpring =
                requestParam.required() &&
                REQUEST_MAPPING_NO_DEFAULT.equals(requestParam.defaultValue());
            if (requiredAsOfOpenApi && !requiredAsOfSpring) {
                if (!isMethodNameLogged) {
                    log.warn("Method: {}", method);
                    isMethodNameLogged = true;
                }
                log.warn("  Parameter: {}", parameter);
                log.warn("    OpenApi: {}", requiredAsOfOpenApi);
                log.warn("    Spring: {}", requiredAsOfSpring);
            } else if (requiredAsOfSpring && !requiredAsOfOpenApi) {
                isErroneousMethod = true;
                if (!isMethodNameLogged) {
                    log.error("Method: {}", method);
                    isMethodNameLogged = true;
                }
                log.error("  Parameter: {}", parameter);
                log.error("    OpenApi: {}", requiredAsOfOpenApi);
                log.error("    Spring: {}", requiredAsOfSpring);
            }
        }
        return isErroneousMethod;
    }
}
