package org.acme.elastic.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("{} — {}", statusCode, ex.getMessage(), ex);
        } else if (log.isDebugEnabled()) {
            log.debug("{} — {}", statusCode, ex.toString());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    @Override
    protected ProblemDetail createProblemDetail(
            Exception ex,
            HttpStatusCode status,
            String defaultDetail,
            @Nullable String detailMessageCode,
            @Nullable Object[] detailMessageArguments,
            WebRequest request) {
        ProblemDetail detail = super.createProblemDetail(
                ex, status, defaultDetail, detailMessageCode, detailMessageArguments, request);
        detail.setType(URI.create("urn:problem:spring-elastic:" + status.value()));
        if (detail.getInstance() == null) {
            detail.setInstance(currentRequestUri(request));
        }
        return detail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorEntry).toList();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + Objects.toString(fe.getDefaultMessage(), "invalid"))
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        ProblemDetail problemDetail = createProblemDetail(ex, status, message, null, null, request);
        problemDetail.setTitle("Validation failed");
        problemDetail.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> parts = new ArrayList<>();
        for (ParameterValidationResult result : ex.getValueResults()) {
            collectMethodValidationMessages(parts, result, request);
        }
        for (ParameterErrors beanErrors : ex.getBeanResults()) {
            collectMethodValidationMessages(parts, beanErrors, request);
        }
        for (MessageSourceResolvable resolvable : ex.getCrossParameterValidationResults()) {
            String text = resolveResolvable(resolvable, request);
            if (text != null && !text.isBlank()) {
                parts.add(text);
            }
        }
        String message = parts.isEmpty() ? "Validation failed" : String.join("; ", parts);
        ProblemDetail problemDetail = createProblemDetail(ex, status, message, null, null, request);
        problemDetail.setTitle("Validation failed");
        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problemDetail = createProblemDetail(ex, HttpStatus.BAD_REQUEST, message, null, null, request);
        problemDetail.setTitle("Validation failed");
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problemDetail = createProblemDetail(
                ex, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null, null, request);
        problemDetail.setTitle("Internal error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private void collectMethodValidationMessages(
            List<String> parts, ParameterValidationResult result, WebRequest request) {
        for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
            String text = resolveMethodValidationMessage(result, resolvable, request);
            if (text != null && !text.isBlank()) {
                parts.add(text);
            }
        }
    }

    private String resolveMethodValidationMessage(
            ParameterValidationResult result, MessageSourceResolvable resolvable, WebRequest request) {
        ConstraintViolation<?> violation = result.unwrap(resolvable, ConstraintViolation.class);
        if (violation != null) {
            return violation.getMessage();
        }
        return resolveResolvable(resolvable, request);
    }

    private String resolveResolvable(MessageSourceResolvable resolvable, WebRequest request) {
        MessageSource messageSource = getMessageSource();
        if (messageSource != null) {
            try {
                return messageSource.getMessage(resolvable, request.getLocale());
            } catch (NoSuchMessageException ignored) {
                // fall through
            }
        }
        if (resolvable.getDefaultMessage() != null && !resolvable.getDefaultMessage().isBlank()) {
            return resolvable.getDefaultMessage();
        }
        String[] codes = resolvable.getCodes();
        if (codes != null && codes.length > 0) {
            return codes[0];
        }
        return null;
    }

    private Map<String, String> toFieldErrorEntry(FieldError fe) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("field", fe.getField());
        entry.put("message", Objects.toString(fe.getDefaultMessage(), ""));
        if (fe.getRejectedValue() != null) {
            entry.put("rejected", String.valueOf(fe.getRejectedValue()));
        }
        return entry;
    }

    private static URI currentRequestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            String uri = servletWebRequest.getRequest().getRequestURI();
            String query = servletWebRequest.getRequest().getQueryString();
            return URI.create(query == null ? uri : uri + "?" + query);
        }
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return URI.create(description.substring(4));
        }
        return URI.create("/");
    }
}
