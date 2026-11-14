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
package de.dataport.suchdienst.error;

import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.elasticsearch.api.exception.AdminElasticSearchException;
import de.dataport.suchdienst.elasticsearch.api.exception.ElasticSearchException;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import jakarta.annotation.Priority;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * custom error handling get registered here via ControllerAdvice
 */
@Slf4j
@Priority(1)
@ControllerAdvice(annotations = { RestController.class })
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    private final PvogRequestContext context;

    public RestResponseExceptionHandler(MessageSource messageSource, PvogRequestContext context) {
        this.messageSource = messageSource;
        this.context = context;
    }

    @ExceptionHandler({ SuchenException.class })
    protected ResponseEntity<ErrorModel> handleSuchenException(
        SuchenException ex,
        WebRequest request
    ) {
        withErrorCodeInMdcDo(ex, () -> {
            if (!ex.getCode().equals(ErrorCodeEnum.NOT_FOUND)) {
                ExceptionHandlingUtils.logWarn(log, ex, request);
            }
        });
        ApiErrorModel model = ex.getModel();
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ ElasticSearchException.class })
    protected ResponseEntity<ErrorModel> handleElasticSearchException(
        ElasticSearchException ex,
        WebRequest request
    ) {
        withErrorCodeInMdcDo(ex, () -> {
            if (!ex.getCode().equals(ErrorCodeEnum.NOT_FOUND)) {
                ExceptionHandlingUtils.logWarn(log, ex, request);
            }
        });
        // Bei ElasticSearch keine internen Details rausgeben, also allgemeine 500er Meldung:
        ApiErrorModel model = new ApiErrorModel(
            ErrorCodeEnum.UNKNOWN_ERROR,
            messageSource.getMessage("api.500_unknown_error", null, Locale.GERMAN),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ AdminElasticSearchException.class })
    protected ResponseEntity<ErrorModel> handleAdminElasticSearchException(
        AdminElasticSearchException ex,
        WebRequest request
    ) {
        withErrorCodeInMdcDo(ex, () -> {
            if (!ex.getCode().equals(ErrorCodeEnum.NOT_FOUND)) {
                ExceptionHandlingUtils.logWarn("ADMIN - FEHLER", log, ex, request);
            }
        });
        // Weitere Informationen mit rausgeben da es sich um eine interne Schnittstelle handelt:
        ApiErrorModel model = new ApiErrorModel(
            ex.getCode(),
            ex.getMessage(),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ ApiException.class })
    protected ResponseEntity<ErrorModel> handleApiException(ApiException ex, WebRequest request) {
        withErrorCodeInMdcDo(ex, () -> ExceptionHandlingUtils.logWarn(log, ex, request));
        ErrorModel model = ex.getModel();
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ ApiWarnException.class })
    protected ResponseEntity<ErrorModel> handleApiWarnException(
        ApiWarnException ex,
        WebRequest request
    ) {
        withErrorCodeInMdcDo(ex, () -> ExceptionHandlingUtils.logWarn(log, ex, request));
        ErrorModel model = ex.getModel();
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ ClientAbortException.class })
    protected ResponseEntity<ErrorModel> handleClientAbortException(
        ClientAbortException ex,
        WebRequest request
    ) {
        ErrorModel model = new ApiErrorModel(
            ErrorCodeEnum.CLIENT_ABORT,
            "Die Verbindung zum Client wurde unterbrochen. Die Anfrage kann nicht mehr beantwortet werden.",
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        withErrorCodeInMdcDo(model.getCode(), () ->
            ExceptionHandlingUtils.logWarn(log, ex, request)
        );
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ AccessDeniedException.class })
    protected ResponseEntity<Object> handleAccessDeniedException(
        AccessDeniedException ex,
        WebRequest request
    ) {
        // Logging im RequestParameterFilter
        ErrorModel model = new ApiErrorModel(
            ErrorCodeEnum.FORBIDDEN,
            messageSource.getMessage("api.403_access_denied", null, Locale.GERMAN),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ErrorModel> handleImportException(
        ImportException ex,
        WebRequest request
    ) {
        ErrorModel model = ex.getModel();
        withErrorCodeInMdcDo(ex, () -> ExceptionHandlingUtils.logWarn(log, ex, request));
        return new ResponseEntity<>(model, ErrorCodeEnum.mapToHttpStatus(model.getCode()));
    }

    @ExceptionHandler({ Exception.class })
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, WebRequest request) {
        ExceptionHandlingUtils.logWarn(
            "Handling unspecific exception of type " + ex.getClass().getSimpleName(),
            log,
            ex,
            request
        );

        ApiErrorModel model = new ApiErrorModel(
            ErrorCodeEnum.UNKNOWN_ERROR,
            messageSource.getMessage("api.500_unknown_error", null, Locale.GERMAN),
            ExceptionType.TECHNISCH,
            MdcTemplate.currentRequestId()
        );
        return handleExceptionInternal(
            ex,
            model,
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }

    /**
     * Handler für invalide Pfadparameter z.B. (javax)
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        @NonNull MissingServletRequestParameterException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        Map<String, String> headerMap = headers.toSingleValueMap();
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_request_params",
                    new String[] { ex.getParameterName(), ex.getParameterType() },
                    Locale.GERMAN
                ) +
                    " " +
                    messageSource.getMessage(
                        "api.400_missing_servlet_request_parameter",
                        new String[] { ex.getParameterName(), ex.getParameterType() },
                        Locale.GERMAN
                    ),
                ExceptionType.TECHNISCH,
                headerMap,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> headerMap = headers.toSingleValueMap();
        Locale locale = ObjectUtils.getIfNull(context.getLocale(), Locale.GERMAN);
        Class<?> requiredType = ex.getRequiredType();
        var typeName = requiredType != null ? requiredType.getSimpleName() : "";
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_request_params",
                    new String[] { ex.getPropertyName(), typeName },
                    locale
                ) +
                    " " +
                    messageSource.getMessage(
                        "api.400_type_mismatch_servlet_request_parameter",
                        new String[] { ex.getPropertyName(), typeName },
                        locale
                    ),
                ExceptionType.TECHNISCH,
                headerMap,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    private static void withErrorCodeInMdcDo(SuchdienstException sudEx, Runnable action) {
        var exCode = sudEx.getCode();
        MdcTemplate.runWith(sudEx.getMdcContext(), () ->
            MdcTemplate.runWith(MdcKeys.ERROR_CODE, String.valueOf(exCode.getNumber()), action)
        );
    }

    private static void withErrorCodeInMdcDo(ErrorCodeEnum errorCode, Runnable action) {
        MdcTemplate.runWith(MdcKeys.ERROR_CODE, String.valueOf(errorCode.getNumber()), action);
    }
}
