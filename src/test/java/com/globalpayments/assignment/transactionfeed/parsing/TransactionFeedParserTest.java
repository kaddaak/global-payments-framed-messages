package com.globalpayments.assignment.transactionfeed.parsing;

import com.globalpayments.assignment.transactionfeed.model.TransactionFeedSentinel;
import com.globalpayments.assignment.transactionfeed.model.TransactionRecord;
import com.globalpayments.assignment.transactionfeed.model.TransactionSentinel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFeedParserTest {
    private static final String MALFORMED_TRANSACTION_MESSAGE = "Expected 4 pipe-separated transaction fields or a sentinel message, got 3";

    private final TransactionFeedParser parser = new TransactionFeedParser();

    @Test
    void parsesPipeSeparatedTransactionIntoColumns() {
        TransactionRecord message = assertInstanceOf(
                TransactionRecord.class,
                parser.parse("UPLATA|ČĆŽŠĐ|100.50|HRK")
        );

        assertEquals("UPLATA", message.transactionType());
        assertEquals("ČĆŽŠĐ", message.transactionId());
        assertEquals(new BigDecimal("100.50"), message.amount());
        assertEquals("HRK", message.currency());
        assertEquals("UPLATA|ČĆŽŠĐ|100.50|HRK", message.rawMessage());
    }

    @Test
    void preservesSentinelMessageWithoutTransactionColumns() {
        TransactionFeedSentinel message = assertInstanceOf(
                TransactionFeedSentinel.class,
                parser.parse("KRAJ")
        );

        assertEquals("KRAJ", message.rawMessage());
        assertEquals(TransactionSentinel.KRAJ, message.sentinel());
    }

    @Test
    void rejectsMalformedPipeSeparatedMessage() {
        TransactionParseException exception = assertThrows(
                TransactionParseException.class,
                () -> parser.parse("PAYMENT|12345|HRK")
        );

        assertEquals(MALFORMED_TRANSACTION_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsInvalidAmount() {
        TransactionParseException exception = assertThrows(
                TransactionParseException.class,
                () -> parser.parse("PAYMENT|12345|not-a-decimal|HRK")
        );

        assertEquals("Amount is not a valid decimal: not-a-decimal", exception.getMessage());
    }

    @Test
    void rejectsTransactionPayloadWithLeadingWhitespace() {
        TransactionParseException exception = assertThrows(
                TransactionParseException.class,
                () -> parser.parse(" PAYMENT|12345|100.50|HRK")
        );

        assertEquals("transaction type must be alphanumeric:  PAYMENT", exception.getMessage());
    }

    @Test
    void rejectsTransactionPayloadWithTrailingWhitespace() {
        TransactionParseException exception = assertThrows(
                TransactionParseException.class,
                () -> parser.parse("PAYMENT|12345|100.50|HRK ")
        );

        assertEquals("currency must be alphanumeric: HRK ", exception.getMessage());
    }

    @Test
    void rejectsSentinelPayloadWithBoundaryWhitespace() {
        TransactionParseException leadingException = assertThrows(
                TransactionParseException.class,
                () -> parser.parse(" END")
        );
        TransactionParseException trailingException = assertThrows(
                TransactionParseException.class,
                () -> parser.parse("END ")
        );

        assertEquals("Unknown transaction sentinel:  END", leadingException.getMessage());
        assertEquals("Unknown transaction sentinel: END ", trailingException.getMessage());
    }

    @Test
    void rejectsUnknownSentinel() {
        TransactionParseException exception = assertThrows(
                TransactionParseException.class,
                () -> parser.parse("STOP")
        );

        assertEquals("Unknown transaction sentinel: STOP", exception.getMessage());
    }
}
