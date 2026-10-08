package com.example.corebanking.common.api;

import com.example.corebanking.common.domain.DomainException;
import com.example.corebanking.common.domain.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler producing RFC 9457 ProblemDetail payloads with stable error codes,
 * correlation request IDs, and security-safe error sanitization.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String PROPERTY_CODE = "code";
    private static final String PROPERTY_REQUEST_ID = "requestId";
    private static final String PROPERTY_TIMESTAMP = "timestamp";
    private static final String PROPERTY_ERRORS = "errors";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** Handles all banking domain business rule exceptions. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            DomainException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = HttpStatus.valueOf(errorCode.getHttpStatus());
        String requestId = resolveRequestId(request);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(errorCode.getDefaultTitle());
        problemDetail.setType(URI.create(errorCode.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, errorCode.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());

        log.warn("Domain exception [{}] handled: {}", errorCode.name(), ex.getMessage());
        return ResponseEntity.status(status).body(problemDetail);
    }

    /** Handles body validation errors triggered by @Valid annotations. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String requestId = resolveRequestId(request);
        List<ValidationError> validationErrors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(
                    new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(
                        globalError -> {
                            validationErrors.add(
                                    new ValidationError(
                                            globalError.getObjectName(),
                                            globalError.getDefaultMessage()));
                        });

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Validation failed for request payload");
        problemDetail.setTitle(ErrorCode.VALIDATION_FAILED.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.VALIDATION_FAILED.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.VALIDATION_FAILED.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());
        problemDetail.setProperty(PROPERTY_ERRORS, validationErrors);

        log.warn(
                "Validation failed [requestId={}]: {} error(s)",
                requestId,
                validationErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
    }

    /** Handles method parameter validation errors in Spring 6.1+. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String requestId = resolveRequestId(request);
        List<ValidationError> validationErrors = new ArrayList<>();

        ex.getAllValidationResults()
                .forEach(
                        result -> {
                            String paramName = result.getMethodParameter().getParameterName();
                            result.getResolvableErrors()
                                    .forEach(
                                            err -> {
                                                validationErrors.add(
                                                        new ValidationError(
                                                                paramName,
                                                                err.getDefaultMessage()));
                                            });
                        });

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Validation failed for request parameters");
        problemDetail.setTitle(ErrorCode.VALIDATION_FAILED.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.VALIDATION_FAILED.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.VALIDATION_FAILED.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());
        problemDetail.setProperty(PROPERTY_ERRORS, validationErrors);

        log.warn(
                "Handler method validation failed [requestId={}]: {} error(s)",
                requestId,
                validationErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
    }

    /** Handles constraint violation exceptions. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        String requestId = resolveRequestId(request);
        List<ValidationError> validationErrors = new ArrayList<>();

        ex.getConstraintViolations()
                .forEach(
                        violation -> {
                            String field =
                                    violation.getPropertyPath() != null
                                            ? violation.getPropertyPath().toString()
                                            : "param";
                            validationErrors.add(
                                    new ValidationError(field, violation.getMessage()));
                        });

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Validation failed for constraints");
        problemDetail.setTitle(ErrorCode.VALIDATION_FAILED.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.VALIDATION_FAILED.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.VALIDATION_FAILED.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());
        problemDetail.setProperty(PROPERTY_ERRORS, validationErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /** Handles unreadable or malformed JSON payloads. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String requestId = resolveRequestId(request);
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Malformed JSON request body");
        problemDetail.setTitle(ErrorCode.VALIDATION_FAILED.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.VALIDATION_FAILED.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.VALIDATION_FAILED.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());

        log.warn("Malformed JSON request [requestId={}]: {}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
    }

    /** Handles 404 for missing Spring MVC static or endpoint resources. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String requestId = resolveRequestId(request);
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle(ErrorCode.RESOURCE_NOT_FOUND.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.RESOURCE_NOT_FOUND.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.RESOURCE_NOT_FOUND.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(problemDetail);
    }

    /** Catches and secures all unexpected exceptions, preventing stack trace leaks. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllUncaughtExceptions(
            Exception ex, WebRequest request) {
        String requestId = resolveRequestId(request);

        log.error(
                "Unhandled internal server error [requestId={}]: {}",
                requestId,
                ex.getMessage(),
                ex);

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please contact support with request ID: "
                                + requestId);
        problemDetail.setTitle(ErrorCode.INTERNAL_ERROR.getDefaultTitle());
        problemDetail.setType(URI.create(ErrorCode.INTERNAL_ERROR.getProblemTypeUri()));
        problemDetail.setInstance(resolveInstanceUri(request));
        problemDetail.setProperty(PROPERTY_CODE, ErrorCode.INTERNAL_ERROR.name());
        problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
        problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            String requestId = resolveRequestId(request);
            if (problemDetail.getProperties() == null
                    || !problemDetail.getProperties().containsKey(PROPERTY_CODE)) {
                problemDetail.setProperty(PROPERTY_CODE, "HTTP_" + statusCode.value());
            }
            problemDetail.setProperty(PROPERTY_REQUEST_ID, requestId);
            problemDetail.setProperty(PROPERTY_TIMESTAMP, clock.instant());
            return ResponseEntity.status(statusCode).headers(headers).body(problemDetail);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private String resolveRequestId(WebRequest request) {
        String mdcId = RequestIdFilter.getCurrentRequestId();
        if (mdcId != null && !mdcId.isBlank()) {
            return mdcId;
        }
        if (request instanceof ServletWebRequest servletWebRequest) {
            String headerId =
                    servletWebRequest.getRequest().getHeader(RequestIdFilter.REQUEST_ID_HEADER);
            if (headerId != null && !headerId.isBlank()) {
                return headerId;
            }
        }
        return UUID.randomUUID().toString();
    }

    private URI resolveInstanceUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        String description = request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            return URI.create(description.substring(4));
        }
        return URI.create("about:blank");
    }
}
