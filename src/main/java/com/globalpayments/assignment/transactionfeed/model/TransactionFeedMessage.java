package com.globalpayments.assignment.transactionfeed.model;

public sealed interface TransactionFeedMessage permits TransactionFeedSentinel, TransactionRecord {
    TransactionFeedMessageKind kind();

    String rawMessage();
}
