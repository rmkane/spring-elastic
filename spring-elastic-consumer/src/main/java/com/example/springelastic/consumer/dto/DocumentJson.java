package com.example.springelastic.consumer.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Document as returned from the upstream API")
public record DocumentJson(
        String id,
        String fileName,
        String content,
        Long fileSize,
        String contentType,
        Instant uploadedAt) {}
