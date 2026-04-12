package com.example.springelastic.consumer.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elastic-api")
public record ElasticApiProperties(URI baseUrl) {}
