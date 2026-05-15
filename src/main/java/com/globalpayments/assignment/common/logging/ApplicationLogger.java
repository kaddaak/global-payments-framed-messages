package com.globalpayments.assignment.common.logging;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ApplicationLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final String component;

    private ApplicationLogger(Class<?> componentType) {
        this.component = Objects.requireNonNull(componentType, "componentType").getSimpleName();
    }

    public static ApplicationLogger forClass(Class<?> componentType) {
        return new ApplicationLogger(componentType);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message, Throwable cause) {
        log(LogLevel.ERROR, message, cause);
    }

    private void log(
            LogLevel level,
            String message,
            Throwable cause
    ) {
        String timestamp = OffsetDateTime.now().format(TIMESTAMP_FORMATTER);
        PrintStream output = level.output();

        output.println(timestamp + " " + level.label() + " " + component + " - " + message);
        if (cause != null) {
            cause.printStackTrace(output);
        }
    }

    private enum LogLevel {
        INFO("INFO", System.out),
        WARN("WARN", System.err),
        ERROR("ERROR", System.err);

        private final String label;
        private final PrintStream output;

        LogLevel(String label, PrintStream output) {
            this.label = label;
            this.output = output;
        }

        private String label() {
            return label;
        }

        private PrintStream output() {
            return output;
        }
    }
}
