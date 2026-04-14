package org.acme.elastic.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Small string utilities for request parameters and user input. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringHelper {

    /**
     * Strips leading/trailing characters removed by {@link String#strip()} (Unicode
     * whitespace). Returns {@code null} if {@code value} is {@code null} or empty
     * after strip. No-break space (U+00A0) is not stripped and is not considered
     * blank by the JDK.
     */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
