package com.globalpayments.assignment.transactionfeed.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public record TransactionRecord(
        String rawMessage,
        String transactionType,
        String transactionId,
        BigDecimal amount,
        String currency
) implements TransactionFeedMessage {
    public TransactionRecord {
        rawMessage = requireNonBlank(rawMessage, "rawMessage");
        transactionType = requireNonBlank(transactionType, "transactionType");
        transactionId = requireNonBlank(transactionId, "transactionId");
        amount = Objects.requireNonNull(amount, "amount");
        currency = requireNonBlank(currency, "currency").toUpperCase(Locale.ROOT);

        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be 3 characters");
        }
    }

    @Override
    public TransactionFeedMessageKind kind() {
        return TransactionFeedMessageKind.TRANSACTION;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
