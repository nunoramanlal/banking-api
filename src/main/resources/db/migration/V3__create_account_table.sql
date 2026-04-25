CREATE SEQUENCE account_number_seq
    START WITH 1000001
    INCREMENT BY 1
    MINVALUE 1000001
    MAXVALUE 1999999
    NO CYCLE;

CREATE TABLE bank_accounts (
    account_number BIGINT NOT NULL,
    sort_code VARCHAR(8) NOT NULL DEFAULT '10-10-10',
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    user_id VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_timestamp TIMESTAMP NOT NULL,
    updated_timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (account_number),
    CONSTRAINT fk_bank_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_balance_range CHECK (balance >= 0.00 AND balance <= 10000.00),
    CONSTRAINT chk_account_type CHECK (account_type IN ('personal')),
    CONSTRAINT chk_currency CHECK (currency IN ('GBP')),
    CONSTRAINT chk_account_number_range CHECK (account_number >= 01000000  AND account_number <= 01999999)
);