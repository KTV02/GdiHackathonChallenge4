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
package de.dataport.suchdienst.config.parameterdiscovering;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Erzeugt bei Restcontrollern aus der @RequestParam Annotation den Parameternamen damit dieser später an der API ausgegeben werden kann
 */
public class RequestParameterDiscoverer implements ParameterNameDiscoverer {

    @Override
    public String[] getParameterNames(Method method) {
        return doGetParameterNames(method);
    }

    @Override
    public String[] getParameterNames(Constructor<?> constructor) {
        return doGetParameterNames(constructor);
    }

    @Nullable
    @SuppressWarnings("java:S1168") // Null-Return ist hier explizit zugelassen und gewollt.
    private String[] doGetParameterNames(Executable executable) {
        Parameter[] parameters = executable.getParameters();
        List<String> parameterNames = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            if (!parameter.isNamePresent()) {
                return null;
            }
            String parameterName = parameter.getName();
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = parameter.getAnnotation(RequestParam.class);
                if (StringUtils.isNotEmpty(annotation.value())) {
                    parameterName = annotation.value();
                } else if (StringUtils.isNotEmpty(annotation.name())) {
                    parameterName = annotation.name();
                }
            }
            parameterNames.add(parameterName);
        }
        return parameterNames.toArray(String[]::new);
    }
}
