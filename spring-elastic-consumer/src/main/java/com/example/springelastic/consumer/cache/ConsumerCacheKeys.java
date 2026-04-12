package com.example.springelastic.consumer.cache;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import com.example.springelastic.consumer.util.StringHelper;

/** Stable SpEL keys for {@link org.springframework.cache.annotation.Cacheable}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConsumerCacheKeys {

    public static String categoryIds(List<String> categoryIds) {
        return categoryIds.stream()
                .map(StringHelper::trimToNull)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(","));
    }

    public static String searchDocuments(String fileName, String contentType) {
        return Objects.toString(fileName, "") + "|" + Objects.toString(contentType, "");
    }
}
