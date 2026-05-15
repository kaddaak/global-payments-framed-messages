package com.globalpayments.assignment.transactionfeed.parsing;

enum TransactionField {
    TYPE(0, "transaction type"),
    TRANSACTION_ID(1, "transaction ID"),
    AMOUNT(2, "amount"),
    CURRENCY(3, "currency");

    static final int EXPECTED_FIELD_COUNT = values().length;

    private final int index;
    private final String label;

    TransactionField(int index, String label) {
        this.index = index;
        this.label = label;
    }

    String readFrom(String[] fields) {
        return fields[index];
    }

    String label() {
        return label;
    }
}
