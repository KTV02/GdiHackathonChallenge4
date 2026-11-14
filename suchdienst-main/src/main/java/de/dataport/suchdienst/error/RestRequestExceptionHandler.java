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

import com.fasterxml.jackson.databind.JsonMappingException;
import de.dataport.suchdienst.config.PvogRequestContext;
import de.dataport.suchdienst.error.model.ApiErrorModel;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.model.MdcKeys;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.DatabindException;

@Priority(0)
@ControllerAdvice(annotations = { RestController.class })
@Slf4j
public class RestRequestExceptionHandler {

    private final MessageSource messageSource;
    private final PvogRequestContext pvogRequestContext;

    public RestRequestExceptionHandler(
        MessageSource messageSource,
        PvogRequestContext pvogRequestContext
    ) {
        this.messageSource = messageSource;
        this.pvogRequestContext = pvogRequestContext;
    }

    /**
     * Handler für ConstraintViolations (javax)
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorModel> handleConstraintViolationException(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        Map<String, String> violations = ex
            .getConstraintViolations()
            .stream()
            .collect(
                Collectors.toMap(
                    k -> getQueryParameterNameFromPath(String.valueOf(k.getPropertyPath())),
                    ConstraintViolation::getMessage,
                    (existing, replacement) -> StringUtil.join("; ", existing, replacement)
                )
            );
        String messageSourceMessage = messageSource.getMessage(
            "api.400_bad_request_params",
            null,
            pvogRequestContext.getLocale()
        );

        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                new ApiErrorModel(
                    ErrorCodeEnum.BAD_REQUEST,
                    messageSourceMessage,
                    ExceptionType.TECHNISCH,
                    violations,
                    MdcTemplate.currentRequestId()
                )
            );
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorModel> handleMissingRequestHeaderException(
        MissingRequestHeaderException ex
    ) {
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.MISSING_HEADER,
                "Erforderlicher Header Parameter '%s' wurde nicht übermittelt.".formatted(
                    ex.getHeaderName()
                ),
                ExceptionType.TECHNISCH,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorModel> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex
    ) {
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.MESSAGE_NOT_READABLE,
                "Fehler beim Lesen der Anfrage.",
                ExceptionType.TECHNISCH,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    private String getQueryParameterNameFromPath(String path) {
        String queryParameter = path.contains(".") ? path.substring(path.indexOf(".") + 1) : path;
        queryParameter = queryParameter.contains(".")
            ? queryParameter.substring(0, queryParameter.indexOf("."))
            : queryParameter;
        return queryParameter;
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorModel> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldMap = ExceptionHandlingUtils.getBindingErrorsAsMap(
            ex.getBindingResult()
        );
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_request_params",
                    null,
                    pvogRequestContext.getLocale()
                ),
                ExceptionType.TECHNISCH,
                fieldMap,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorModel> handleBindException(BindException ex) {
        Map<String, String> fieldMap = ExceptionHandlingUtils.getBindingErrorsAsMap(
            ex.getBindingResult()
        );
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.BAD_JSON_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_payload",
                    null,
                    pvogRequestContext.getLocale()
                ),
                ExceptionType.TECHNISCH,
                fieldMap,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(value = { JsonMappingException.class, DatabindException.class })
    public ResponseEntity<ErrorModel> handleJsonMappingException(
        JsonMappingException ex,
        WebRequest request
    ) {
        MdcTemplate.runWith(
            MdcKeys.ERROR_CODE,
            String.valueOf(ErrorCodeEnum.BAD_JSON_PROP_REQUEST.getNumber()),
            () ->
                ExceptionHandlingUtils.logInfo(
                    "Fehler beim Konvertieren nach JSON",
                    log,
                    ex,
                    request
                )
        );
        Map<String, String> fieldMap = ex
            .getPath()
            .stream()
            .collect(
                Collectors.toMap(
                    JsonMappingException.Reference::getDescription,
                    JsonMappingException.Reference::getFieldName
                )
            );
        return new ResponseEntity<>(
            new ApiErrorModel(
                ErrorCodeEnum.BAD_JSON_PROP_REQUEST,
                messageSource.getMessage(
                    "api.400_bad_payload",
                    null,
                    pvogRequestContext.getLocale()
                ),
                ExceptionType.TECHNISCH,
                fieldMap,
                MdcTemplate.currentRequestId()
            ),
            HttpStatus.BAD_REQUEST
        );
    }
}
