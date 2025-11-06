package com.example.springelastic.config;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class IndexNameProvider {
    
    private static final String INDEX_PREFIX = "documents";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public String getIndexName() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfWeek = today.with(DayOfWeek.MONDAY);
        return INDEX_PREFIX + "-" + firstDayOfWeek.format(DATE_FORMATTER);
    }
}

