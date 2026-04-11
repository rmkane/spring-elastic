package com.example.springelastic.controller;

import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<String> handleValidationExceptions(HandlerMethodValidationException ex) {
        String errors = ex.getAllErrors()
            .stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body("Validation errors: " + errors);
    }
}
