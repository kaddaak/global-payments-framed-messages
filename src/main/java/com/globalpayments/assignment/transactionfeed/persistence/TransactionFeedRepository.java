package com.globalpayments.assignment.transactionfeed.persistence;

import com.globalpayments.assignment.common.validation.TextValues;
import com.globalpayments.assignment.transactionfeed.model.TransactionFeedMessage;
import com.globalpayments.assignment.transactionfeed.model.TransactionRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class TransactionFeedRepository {
    private static final String INSERT_SQL = """
            INSERT INTO received_transaction_messages (
                received_order,
                raw_message,
                transaction_type,
                transaction_id,
                amount,
                currency
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String NEXT_RECEIVED_ORDER_SQL = "SELECT COALESCE(MAX(received_order), 0) + 1 FROM received_transaction_messages";

    private final String databaseUrl;
    private final AtomicLong nextReceivedOrder;

    public TransactionFeedRepository(String databaseUrl) throws SQLException {
        this.databaseUrl = TextValues.requireNonBlank(databaseUrl, "databaseUrl");
        this.nextReceivedOrder = new AtomicLong(loadNextReceivedOrder());
    }

    public long save(TransactionFeedMessage message) throws SQLException {
        Objects.requireNonNull(message, "message");
        long receivedOrder = nextReceivedOrder.getAndIncrement();

        insertMessage(receivedOrder, message);

        return receivedOrder;
    }

    private void insertMessage(
            long receivedOrder,
            TransactionFeedMessage message
    ) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            bindInsertStatement(statement, receivedOrder, message);
            statement.executeUpdate();
        }
    }

    private static void bindInsertStatement(
            PreparedStatement statement,
            long receivedOrder,
            TransactionFeedMessage message
    ) throws SQLException {
        statement.setLong(1, receivedOrder);
        statement.setString(2, message.rawMessage());

        bindMessageColumns(statement, message);
    }

    private long loadNextReceivedOrder() throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(NEXT_RECEIVED_ORDER_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 1L;
        }
    }

    private static void bindMessageColumns(
            PreparedStatement statement,
            TransactionFeedMessage message
    ) throws SQLException {
        switch (message.kind()) {
            case TRANSACTION -> bindTransactionColumns(statement, requireTransactionRecord(message));
            case SENTINEL -> bindNullTransactionColumns(statement);
        }
    }

    private static TransactionRecord requireTransactionRecord(TransactionFeedMessage message) {
        if (message instanceof TransactionRecord transactionRecord) {
            return transactionRecord;
        }

        throw new IllegalArgumentException(
                "Expected transaction record but got: " + message.getClass().getName()
        );
    }

    private static void bindTransactionColumns(
            PreparedStatement statement,
            TransactionRecord message
    ) throws SQLException {
        statement.setString(3, message.transactionType());
        statement.setString(4, message.transactionId());
        statement.setBigDecimal(5, message.amount());
        statement.setString(6, message.currency());
    }

    private static void bindNullTransactionColumns(
            PreparedStatement statement
    ) throws SQLException {
        statement.setNull(3, Types.VARCHAR);
        statement.setNull(4, Types.VARCHAR);
        statement.setNull(5, Types.NUMERIC);
        statement.setNull(6, Types.VARCHAR);
    }
}
