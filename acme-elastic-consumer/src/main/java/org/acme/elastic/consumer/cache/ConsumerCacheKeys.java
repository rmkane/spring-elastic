package org.acme.elastic.consumer.cache;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.elastic.consumer.util.StringHelper;

/**
 * Stable SpEL keys for {@link org.springframework.cache.annotation.Cacheable}.
 */
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
