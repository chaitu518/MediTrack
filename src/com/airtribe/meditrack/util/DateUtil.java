package com.airtribe.meditrack.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized date parsing/formatting so CSVUtil, the console UI, and
 * services all agree on one format string.
 */
public final class DateUtil {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtil() {
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(FORMAT);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }

    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(LocalDateTime.now());
    }
}