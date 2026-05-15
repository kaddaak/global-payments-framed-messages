package com.globalpayments.assignment.transactionfeed.service;

import com.globalpayments.assignment.transactionfeed.model.TransactionFeedMessageKind;

public record TransactionFeedProcessingResult(
        long receivedOrder,
        TransactionFeedMessageKind messageKind
) {
}
