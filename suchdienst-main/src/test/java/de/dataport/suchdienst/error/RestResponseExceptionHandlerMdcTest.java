/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.util.MdcCleanupExtension;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import de.dataport.suchdienst.util.model.ProtocolType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MdcCleanupExtension.class)
class RestResponseExceptionHandlerMdcTest {

    @Test
    void loggt_mit_dem_mdc_der_exception_und_stellt_den_request_context_wieder_her() {
        ApiException exception;
        MDC.put(MdcKeys.REQUEST_ID, "request-1");
        try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
            exception = new ApiException(
                ErrorCodeEnum.BAD_REQUEST,
                "Fehler",
                ExceptionType.TECHNISCH
            );
        }
        MDC.put(MdcKeys.PROTOKOLLTYP, ProtocolType.ADMIN.name());

        Logger logger = (Logger) LoggerFactory.getLogger(RestResponseExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            var handler = new RestResponseExceptionHandler(
                mock(MessageSource.class),
                mock(PvogRequestContext.class)
            );
            handler.handleApiException(
                exception,
                new ServletWebRequest(new MockHttpServletRequest())
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getMDCPropertyMap())
            .containsEntry(MdcKeys.REQUEST_ID, "request-1")
            .containsEntry(MdcKeys.PROTOKOLLTYP, ProtocolType.EXTERN.name())
            .containsEntry(
                MdcKeys.ERROR_CODE,
                String.valueOf(ErrorCodeEnum.BAD_REQUEST.getNumber())
            );
        assertThat(MDC.get(MdcKeys.PROTOKOLLTYP)).isEqualTo(ProtocolType.ADMIN.name());
        assertThat(MDC.get(MdcKeys.ERROR_CODE)).isNull();
    }

    /**
     * Ziel: Der MDC-Schnappschuss vertraegt null-Werte. Ein Schluessel aus Nutzdaten kann null sein,
     * eine NPE im Exception-Konstruktor wuerde den eigentlichen Fehler verschlucken.
     */
    @Test
    void exception_kann_mit_einem_null_wert_im_mdc_erzeugt_werden() {
        try (var _ = MdcTemplate.scoped(MdcKeys.LBID, null)) {
            var exception = new ApiException(
                ErrorCodeEnum.BAD_REQUEST,
                "Fehler",
                ExceptionType.TECHNISCH
            );

            assertThat(exception.getMdcContext()).containsEntry(MdcKeys.LBID, null);
        }
    }
}
