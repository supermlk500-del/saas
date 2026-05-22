package com.zhihuitong.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AuditRemarkUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditRemarkUtils() {
    }

    public static String append(String original, String tag, String content) {
        String normalizedOriginal = original == null ? "" : original.trim();
        String normalizedContent = content == null ? "" : content.trim();
        String entry = "[" + FORMATTER.format(LocalDateTime.now()) + "] " + tag + ": " + normalizedContent;
        if (normalizedOriginal.isEmpty()) {
            return entry;
        }
        return normalizedOriginal + System.lineSeparator() + entry;
    }
}
