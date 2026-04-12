package com.example.springelastic.consumer.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    private Duration defaultTtl = Duration.ofMinutes(5);

    /** Keys match {@link com.example.springelastic.consumer.cache.ConsumerCacheNames} values. */
    private Map<String, Duration> ttlByName = new LinkedHashMap<>();
}
