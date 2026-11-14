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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.testutil.RetryTestSupport;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Prüft den RetryAspect über echte Spring-Proxies.
 * Dadurch wird nicht nur die nackte Aspect-Methode, sondern auch die Proxy-Grenze validiert,
 * an der der Retry in der Produktion tatsächlich ausgelöst wird.
 */
@SpringJUnitConfig(RetryAspectTest.TestConfig.class)
class RetryAspectTest {

    @Autowired
    private BackgroundRetryableService backgroundRetryableService;

    @Autowired
    private MethodRetryableService methodRetryableService;

    @BeforeEach
    void resetServices() {
        backgroundRetryableService.reset();
        methodRetryableService.reset();
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void retriesClassAnnotatedBeanInBackgroundExecution() {
        backgroundRetryableService.failTimesThenReturn(2, RetryAspectTest::newRetryableFailure);

        String result = backgroundRetryableService.execute();

        assertThat(AopUtils.isAopProxy(backgroundRetryableService), is(true));
        assertThat(result, is("ok"));
        assertThat(backgroundRetryableService.invocations(), is(3));
    }

    @Test
    void retriesMethodAnnotatedBeanInBackgroundExecution() {
        methodRetryableService.failTimesThenReturn(1, RetryAspectTest::newRetryableFailure);

        String result = methodRetryableService.execute();

        assertThat(AopUtils.isAopProxy(methodRetryableService), is(true));
        assertThat(result, is("ok"));
        assertThat(methodRetryableService.invocations(), is(2));
    }

    @Test
    void doesNotRetryClassAnnotatedBeanDuringWebRequest() {
        RequestContextHolder.setRequestAttributes(
            new ServletRequestAttributes(new MockHttpServletRequest())
        );
        backgroundRetryableService.failTimesThenReturn(2, RetryAspectTest::newRetryableFailure);

        var exception = assertThrows(
            ElasticSearchException.class,
            backgroundRetryableService::execute
        );

        assertThat(exception.getCode(), is(ErrorCodeEnum.ELSE_IO_ERROR));
        assertThat(backgroundRetryableService.invocations(), is(1));
    }

    @Test
    void unwrapsRetryExceptionCauseAfterRetryExhaustion() {
        backgroundRetryableService.alwaysFail(RetryAspectTest::newRetryableFailure);

        var exception = assertThrows(
            ElasticSearchException.class,
            backgroundRetryableService::execute
        );

        assertThat(exception, instanceOf(ElasticSearchException.class));
        assertThat(exception.getCode(), is(ErrorCodeEnum.ELSE_IO_ERROR));
        assertThat(backgroundRetryableService.invocations(), is(4));
    }

    @Test
    void doesNotRetryNonRetryableExceptionInBackgroundExecution() {
        backgroundRetryableService.alwaysFail(() -> new IllegalStateException("unexpected"));

        var exception = assertThrows(
            IllegalStateException.class,
            backgroundRetryableService::execute
        );

        assertThat(exception.getMessage(), is("unexpected"));
        assertThat(backgroundRetryableService.invocations(), is(1));
    }

    private static ElasticSearchException newRetryableFailure() {
        return new ElasticSearchException(
            ErrorCodeEnum.ELSE_IO_ERROR,
            "IO failure",
            new SocketException("Connection reset")
        );
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean(name = "suchdienstRetryTemplate")
        RetryTemplate suchdienstRetryTemplate() {
            return RetryTestSupport.createRetryTemplate();
        }

        @Bean
        RetryAspect retryAspect(@Qualifier("suchdienstRetryTemplate") RetryTemplate retryTemplate) {
            return new RetryAspect(retryTemplate);
        }

        @Bean
        BackgroundRetryableService backgroundRetryableService() {
            return new BackgroundRetryableService();
        }

        @Bean
        MethodRetryableService methodRetryableService() {
            return new MethodRetryableService();
        }
    }

    @RetryableWhenBackgroundJob
    static class BackgroundRetryableService extends ConfigurableFailureService {

        public String execute() {
            return executeConfiguredBehaviour();
        }
    }

    static class MethodRetryableService extends ConfigurableFailureService {

        @RetryableWhenBackgroundJob
        public String execute() {
            return executeConfiguredBehaviour();
        }
    }

    abstract static class ConfigurableFailureService {

        private final AtomicInteger invocations = new AtomicInteger();
        private int failuresBeforeSuccess;
        private FailureFactory failureFactory = RetryAspectTest::newRetryableFailure;

        void failTimesThenReturn(int failuresBeforeSuccess, FailureFactory failureFactory) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.failureFactory = failureFactory;
            this.invocations.set(0);
        }

        void alwaysFail(FailureFactory failureFactory) {
            failTimesThenReturn(Integer.MAX_VALUE, failureFactory);
        }

        int invocations() {
            return invocations.get();
        }

        void reset() {
            failTimesThenReturn(0, RetryAspectTest::newRetryableFailure);
        }

        String executeConfiguredBehaviour() {
            int attempt = invocations.incrementAndGet();
            // Die Services modellieren nur das Fehlerverhalten; der eigentliche Retry wird durch den
            // Spring-Proxy um execute() herum ausgelöst.
            if (attempt <= failuresBeforeSuccess) {
                throw failureFactory.create();
            }
            return "ok";
        }
    }

    @FunctionalInterface
    interface FailureFactory {
        RuntimeException create();
    }
}
