package com.globalpayments.assignment.transactionfeed.service;

import com.globalpayments.assignment.transactionfeed.model.TransactionFeedMessage;
import com.globalpayments.assignment.transactionfeed.parsing.TransactionFeedParser;
import com.globalpayments.assignment.transactionfeed.persistence.TransactionFeedRepository;

import java.sql.SQLException;
import java.util.Objects;

public final class TransactionFeedProcessor {
    private final TransactionFeedParser parser;
    private final TransactionFeedRepository repository;

    public TransactionFeedProcessor(
            TransactionFeedParser parser,
            TransactionFeedRepository repository
    ) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public TransactionFeedProcessingResult process(String decodedMessage) throws SQLException {
        TransactionFeedMessage message = parse(decodedMessage);
        long receivedOrder = save(message);

        return new TransactionFeedProcessingResult(receivedOrder, message.kind());
    }

    private TransactionFeedMessage parse(String decodedMessage) {
        return parser.parse(decodedMessage);
    }

    private long save(TransactionFeedMessage message) throws SQLException {
        return repository.save(message);
    }
}
