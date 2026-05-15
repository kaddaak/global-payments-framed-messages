package com.globalpayments.assignment.transactionfeed.parsing;

import com.globalpayments.assignment.transactionfeed.model.TransactionFeedMessage;
import com.globalpayments.assignment.transactionfeed.model.TransactionFeedSentinel;
import com.globalpayments.assignment.transactionfeed.model.TransactionRecord;
import com.globalpayments.assignment.transactionfeed.model.TransactionSentinel;

import java.math.BigDecimal;
import java.util.Locale;

public final class TransactionFeedParser {
    private static final String MALFORMED_TRANSACTION_MESSAGE_PREFIX = "Expected 4 pipe-separated transaction fields or a sentinel message, got ";

    public TransactionFeedMessage parse(String rawMessage) {
        String message = requireDecodedMessage(rawMessage);

        if (isSentinelMessage(message)) {
            return parseSentinel(message);
        }

        return parseTransactionRecord(message);
    }

    private static String requireDecodedMessage(String rawMessage) {
        return requireNonBlank(rawMessage, "Decoded message is blank");
    }

    private static boolean isSentinelMessage(String message) {
        return !message.contains("|");
    }

    private static TransactionRecord parseTransactionRecord(String message) {
        String[] fields = splitFields(message);

        String type = parseAlphaNumericField(TransactionField.TYPE, fields);
        String transactionId = parseAlphaNumericField(TransactionField.TRANSACTION_ID, fields);
        BigDecimal amount = parseAmount(TransactionField.AMOUNT.readFrom(fields));
        String currency = parseCurrency(fields);

        return new TransactionRecord(
                message,
                type,
                transactionId,
                amount,
                currency
        );
    }

    private static String[] splitFields(String message) {
        String[] fields = message.split("\\|", -1);
        if (fields.length != TransactionField.EXPECTED_FIELD_COUNT) {
            throw new TransactionParseException(
                    MALFORMED_TRANSACTION_MESSAGE_PREFIX + fields.length
            );
        }

        return fields;
    }

    private static String parseAlphaNumericField(
            TransactionField field,
            String[] fields
    ) {
        return requireAlphaNumeric(field.readFrom(fields), field.label());
    }

    private static String parseCurrency(String[] fields) {
        String currency = parseAlphaNumericField(TransactionField.CURRENCY, fields)
                .toUpperCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new TransactionParseException("Currency must be exactly 3 characters");
        }

        return currency;
    }

    private static TransactionFeedSentinel parseSentinel(String message) {
        TransactionSentinel sentinel = TransactionSentinel.find(message)
                .orElseThrow(() -> new TransactionParseException("Unknown transaction sentinel: " + message));

        return new TransactionFeedSentinel(message, sentinel);
    }

    private static BigDecimal parseAmount(String value) {
        String amount = requireNonBlank(value, "Amount must not be blank");

        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException ex) {
            throw new TransactionParseException("Amount is not a valid decimal: " + value);
        }
    }

    private static String requireAlphaNumeric(String value, String fieldName) {
        String fieldValue = requireNonBlank(value, fieldName + " must not be blank");

        boolean alphaNumeric = fieldValue.codePoints().allMatch(Character::isLetterOrDigit);
        if (!alphaNumeric) {
            throw new TransactionParseException(fieldName + " must be alphanumeric: " + value);
        }
        return fieldValue;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new TransactionParseException(message);
        }

        return value;
    }
}
