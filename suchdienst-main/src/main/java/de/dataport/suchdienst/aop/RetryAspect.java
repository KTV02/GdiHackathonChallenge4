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
package de.dataport.suchdienst.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@Aspect
@Configuration
@Slf4j
public class RetryAspect {

    private final RetryTemplate retry;

    public RetryAspect(@Qualifier("suchdienstRetryTemplate") RetryTemplate retry) {
        this.retry = retry;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}

    /**
     * {@link RetryableWhenBackgroundJob} an der Klasse oder der Methode
     */
    @SuppressWarnings("EmptyMethod")
    @Pointcut("@within(retryable) || @annotation(retryable)")
    public void hasRetryable(RetryableWhenBackgroundJob retryable) {}

    @SuppressWarnings("unused")
    @Around(value = "publicMethod() && hasRetryable(retryable)", argNames = "jp,retryable")
    public Object retry(
        ProceedingJoinPoint jp,
        @SuppressWarnings("unused") RetryableWhenBackgroundJob retryable
    ) throws Throwable {
        if (RequestContextHolder.getRequestAttributes() == null) {
            log.debug(
                "Führe {} mit Retry aus, da kein Webrequest, also Hintergrundverarbeitung",
                jp.getSignature()
            );
            try {
                return retry.execute(jp::proceed);
            } catch (RetryException e) {
                throw e.getCause();
            }
        } else {
            log.debug("Führe {} ohne Retry aus, da Webrequest", jp.getSignature());
            return jp.proceed();
        }
    }
}
