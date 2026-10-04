package com.ticketmanagement.api.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.ticketmanagement.api.common.ErrorResponse;
import com.ticketmanagement.api.common.ErrorResponse.ErrorBody;
import com.ticketmanagement.api.common.ErrorResponse.ErrorDetail;
import com.ticketmanagement.domain.EmptyPatchException;
import com.ticketmanagement.domain.IllegalTicketTransitionException;
import com.ticketmanagement.domain.InvalidSortException;
import com.ticketmanagement.domain.TicketNotFoundException;
import com.ticketmanagement.domain.TicketValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorDetail(fieldName(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorDetail(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", details, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String field = ex.getName();
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed.",
                List.of(new ErrorDetail(field, "invalid value")),
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getMostSpecificCause() instanceof InvalidFormatException invalid) {
            String field = invalid.getPath().isEmpty() ? "body" : invalid.getPath().getLast().getFieldName();
            return error(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    List.of(new ErrorDetail(field, "invalid value")),
                    request);
        }
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed JSON request.", List.of(), request);
    }

    @ExceptionHandler(InvalidSortException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(InvalidSortException ex, HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Invalid sort parameter.",
                List.of(new ErrorDetail("sort", ex.getMessage())),
                request);
    }

    @ExceptionHandler(EmptyPatchException.class)
    public ResponseEntity<ErrorResponse> handleEmptyPatch(EmptyPatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(TicketValidationException.class)
    public ResponseEntity<ErrorResponse> handleTicketValidation(
            TicketValidationException ex, HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed.",
                List.of(new ErrorDetail(ex.field(), ex.getMessage())),
                request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(IllegalTicketTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(
            IllegalTicketTransitionException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION", ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                List.of(),
                request);
    }

    private ErrorDetail toDetail(FieldError fieldError) {
        return new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private static String fieldName(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }

    private static ResponseEntity<ErrorResponse> error(
            HttpStatus status, String code, String message, List<ErrorDetail> details, HttpServletRequest request) {
        ErrorBody body = new ErrorBody(
                status.value(),
                code,
                message,
                details,
                Instant.now(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(new ErrorResponse(body));
    }
}
