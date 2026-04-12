package com.example.springelastic.consumer.util;

public final class StringHelper {

    private StringHelper() {}

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
