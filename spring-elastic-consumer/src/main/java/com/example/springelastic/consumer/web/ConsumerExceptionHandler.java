package com.example.springelastic.consumer.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ConsumerExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleUpstream(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        return ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(body);
    }
}
