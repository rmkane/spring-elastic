package com.example.springelastic.convert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;

/**
 * Reads date-only (yyyy-MM-dd), ISO instant, and offset datetimes into {@link Instant}. Spring Data's
 * default temporal converter does not accept date-only values for {@link Instant}.
 */
public class FlexibleInstantPropertyValueConverter implements PropertyValueConverter {

    @Override
    public Object read(Object from) {
        if (from == null) {
            return null;
        }
        if (from instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        String s = from.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(s).toInstant();
        }
    }

    @Override
    public Object write(Object to) {
        if (to == null) {
            return null;
        }
        // Full ISO-8601 instant so Elasticsearch stores wall-clock ordering (not date-only).
        return DateTimeFormatter.ISO_INSTANT.format((Instant) to);
    }
}
