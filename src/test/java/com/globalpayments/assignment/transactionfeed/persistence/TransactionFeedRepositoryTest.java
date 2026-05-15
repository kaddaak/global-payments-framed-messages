package com.globalpayments.assignment.transactionfeed.persistence;

import com.globalpayments.assignment.common.sql.SqliteDatabaseInitializer;
import com.globalpayments.assignment.transactionfeed.parsing.TransactionFeedParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionFeedRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsRawAndSplitColumnsInReceiveOrder() throws Exception {
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();

        TransactionFeedRepository repository = new TransactionFeedRepository(databaseUrl);
        TransactionFeedParser parser = new TransactionFeedParser();

        assertEquals(1L, repository.save(parser.parse("PAYMENT|12345|100.50|HRK")));
        assertEquals(2L, repository.save(parser.parse("END")));

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT received_order,
                            raw_message,
                            transaction_type,
                            transaction_id,
                            amount,
                            currency
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            assertTrue(resultSet.next());
            assertEquals(1L, resultSet.getLong("received_order"));
            assertEquals("PAYMENT|12345|100.50|HRK", resultSet.getString("raw_message"));
            assertEquals("PAYMENT", resultSet.getString("transaction_type"));
            assertEquals("12345", resultSet.getString("transaction_id"));
            assertEquals(
                    0,
                    new BigDecimal("100.50").compareTo(resultSet.getBigDecimal("amount"))
            );
            assertEquals("HRK", resultSet.getString("currency"));

            assertTrue(resultSet.next());
            assertEquals(2L, resultSet.getLong("received_order"));
            assertEquals("END", resultSet.getString("raw_message"));
            assertNull(resultSet.getString("transaction_type"));
        }
    }

    @Test
    void restartsReceivedOrderFromHighestPersistedValue() throws Exception {
        Path databaseFile = tempDir.resolve("messages.db");
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        new SqliteDatabaseInitializer(databaseUrl, "schema.sql").initialize();

        TransactionFeedParser parser = new TransactionFeedParser();
        TransactionFeedRepository firstRepository = new TransactionFeedRepository(databaseUrl);

        assertEquals(1L, firstRepository.save(parser.parse("PAYMENT|FIRST|100.50|HRK")));
        assertEquals(2L, firstRepository.save(parser.parse("END")));

        TransactionFeedRepository restartedRepository = new TransactionFeedRepository(databaseUrl);

        assertEquals(3L, restartedRepository.save(parser.parse("REFUND|SECOND|42.00|EUR")));

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT received_order,
                            raw_message
                     FROM received_transaction_messages
                     ORDER BY received_order
                     """)) {
            assertTrue(resultSet.next());
            assertEquals(1L, resultSet.getLong("received_order"));
            assertEquals("PAYMENT|FIRST|100.50|HRK", resultSet.getString("raw_message"));

            assertTrue(resultSet.next());
            assertEquals(2L, resultSet.getLong("received_order"));
            assertEquals("END", resultSet.getString("raw_message"));

            assertTrue(resultSet.next());
            assertEquals(3L, resultSet.getLong("received_order"));
            assertEquals("REFUND|SECOND|42.00|EUR", resultSet.getString("raw_message"));
        }
    }
}
