package org.acme.elastic.consumer.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Small string utilities for request parameters (consumer copy of the domain
 * helper).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringHelper {

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
