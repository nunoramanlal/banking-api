CREATE TABLE transactions (
    id VARCHAR(255) NOT NULL,
    account_number BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    reference VARCHAR(255),
    created_timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_number)
        REFERENCES bank_accounts(account_number)
        ON DELETE CASCADE,
    CONSTRAINT chk_transaction_amount CHECK (amount > 0.00 AND amount <= 10000.00),
    CONSTRAINT chk_transaction_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT chk_transaction_currency CHECK (currency IN ('GBP'))
);

CREATE INDEX idx_transactions_account_number ON transactions(account_number);