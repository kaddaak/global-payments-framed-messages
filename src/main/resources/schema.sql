CREATE TABLE IF NOT EXISTS received_transaction_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    received_order INTEGER NOT NULL UNIQUE,
    raw_message TEXT NOT NULL,
    transaction_type TEXT,
    transaction_id TEXT,
    amount NUMERIC,
    currency TEXT,
    received_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (length(raw_message) > 0),
    CHECK (currency IS NULL OR length(currency) = 3),
    CHECK (
        (
            transaction_type IS NULL
            AND transaction_id IS NULL
            AND amount IS NULL
            AND currency IS NULL
        )
        OR
        (
            transaction_type IS NOT NULL
            AND transaction_id IS NOT NULL
            AND amount IS NOT NULL
            AND currency IS NOT NULL
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_received_transaction_messages_received_order
    ON received_transaction_messages (received_order);
