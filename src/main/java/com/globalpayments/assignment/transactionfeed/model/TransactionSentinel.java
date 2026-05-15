package com.globalpayments.assignment.transactionfeed.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum TransactionSentinel {
    END("END"),
    KRAJ("KRAJ");

    private static final Map<String, TransactionSentinel> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toMap(TransactionSentinel::value, sentinel -> sentinel));

    private final String value;

    TransactionSentinel(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<TransactionSentinel> find(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(BY_VALUE.get(value.toUpperCase(Locale.ROOT)));
    }
}
