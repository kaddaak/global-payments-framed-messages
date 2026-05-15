package com.globalpayments.assignment.transactionfeed.model;

import java.util.Locale;
import java.util.Objects;

public record TransactionFeedSentinel(
        String rawMessage,
        TransactionSentinel sentinel
) implements TransactionFeedMessage {
    public TransactionFeedSentinel(String rawMessage) {
        this(
                rawMessage,
                TransactionSentinel.find(rawMessage)
                        .orElseThrow(() -> new IllegalArgumentException("sentinel must be known: " + rawMessage))
        );
    }

    public TransactionFeedSentinel {
        rawMessage = requireNonBlank(rawMessage, "rawMessage");
        sentinel = Objects.requireNonNull(sentinel, "sentinel");

        if (!sentinel.value().equals(rawMessage.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("rawMessage does not match sentinel");
        }
    }

    @Override
    public TransactionFeedMessageKind kind() {
        return TransactionFeedMessageKind.SENTINEL;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
