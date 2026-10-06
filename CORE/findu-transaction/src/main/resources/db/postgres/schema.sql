-- =====================================================================
-- FINDU TRANSACTION - SCHEMA POSTGRESQL
-- =====================================================================

CREATE TABLE IF NOT EXISTS service_transactions (
    id VARCHAR(36) PRIMARY KEY,
    transaction_code VARCHAR(64) NOT NULL UNIQUE,
    service_request_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    service_amount NUMERIC(19, 2) NOT NULL,
    commission_rate NUMERIC(5, 4) NOT NULL,
    commission_amount NUMERIC(19, 2) NOT NULL,
    tip_amount NUMERIC(19, 2) NOT NULL,
    extra_amount NUMERIC(19, 2) NOT NULL,
    total_customer_amount NUMERIC(19, 2) NOT NULL,
    provider_amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    transaction_status VARCHAR(32) NOT NULL,
    service_completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reference VARCHAR(128) UNIQUE,
    provider_token VARCHAR(128),
    brand VARCHAR(32),
    last_four VARCHAR(4),
    expiration_month INT,
    expiration_year INT,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS platform_commissions (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    rate NUMERIC(5, 4) NOT NULL,
    base_amount NUMERIC(19, 2) NOT NULL,
    commission_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tips (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    provider_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS extra_expense_requests (
    id VARCHAR(36) PRIMARY KEY,
    service_request_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    description TEXT,
    requested_amount NUMERIC(19, 2) NOT NULL,
    approved_amount NUMERIC(19, 2),
    status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS extra_expense_items (
    id VARCHAR(36) PRIMARY KEY,
    extra_expense_request_id VARCHAR(36) NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_amount NUMERIC(19, 2) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS extra_expense_evidences (
    id VARCHAR(36) PRIMARY KEY,
    extra_expense_request_id VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    file_id VARCHAR(128),
    object_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128),
    uploaded_by VARCHAR(128),
    uploaded_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS extra_expense_approvals (
    id VARCHAR(36) PRIMARY KEY,
    extra_expense_request_id VARCHAR(36) NOT NULL,
    approved_amount NUMERIC(19, 2) NOT NULL,
    approved_by VARCHAR(128) NOT NULL,
    approval_status VARCHAR(32) NOT NULL,
    reason TEXT,
    approved_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_accounts (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL UNIQUE,
    provider_type VARCHAR(32) NOT NULL,
    payable_balance NUMERIC(19, 2) NOT NULL,
    receivable_balance NUMERIC(19, 2) NOT NULL,
    last_balance_date DATE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_ledger_entries (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    transaction_id VARCHAR(36),
    entry_type VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payable_balance_after NUMERIC(19, 2) NOT NULL,
    receivable_balance_after NUMERIC(19, 2) NOT NULL,
    reference VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS daily_balances (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    balance_date DATE NOT NULL,
    opening_balance NUMERIC(19, 2) NOT NULL,
    total_earnings NUMERIC(19, 2) NOT NULL,
    total_commissions NUMERIC(19, 2) NOT NULL,
    total_tips NUMERIC(19, 2) NOT NULL,
    total_extras NUMERIC(19, 2) NOT NULL,
    cash_payments NUMERIC(19, 2) NOT NULL,
    digital_payments NUMERIC(19, 2) NOT NULL,
    provider_debt NUMERIC(19, 2) NOT NULL,
    pending_payout NUMERIC(19, 2) NOT NULL,
    closing_balance NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_daily_balance_provider_date UNIQUE (provider_id, balance_date)
);

CREATE TABLE IF NOT EXISTS provider_settlement_profiles (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL UNIQUE,
    provider_type VARCHAR(32) NOT NULL,
    settlement_frequency VARCHAR(32) NOT NULL,
    settlement_day VARCHAR(32),
    settlement_time VARCHAR(16),
    negative_balance_limit NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_payment_accounts (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    key_type VARCHAR(32),
    encrypted_key VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_payment_accounts (
    id VARCHAR(36) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    provider_token VARCHAR(128) NOT NULL,
    brand VARCHAR(32),
    last_four VARCHAR(4),
    expiration_month INT,
    expiration_year INT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS settlements (
    id VARCHAR(36) PRIMARY KEY,
    settlement_code VARCHAR(64) NOT NULL UNIQUE,
    provider_id BIGINT NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    gross_amount NUMERIC(19, 2) NOT NULL,
    debt_compensation_amount NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    status VARCHAR(32),
    settlement_type VARCHAR(32),
    scheduled_at TIMESTAMP,
    executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS settlement_daily_balances (
    id VARCHAR(36) PRIMARY KEY,
    settlement_id VARCHAR(36) NOT NULL,
    daily_balance_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_settlement_daily_balance UNIQUE (settlement_id, daily_balance_id)
);

CREATE TABLE IF NOT EXISTS payouts (
    id VARCHAR(36) PRIMARY KEY,
    payout_code VARCHAR(64) NOT NULL UNIQUE,
    settlement_id VARCHAR(36),
    provider_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(32),
    destination VARCHAR(255),
    external_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_transfers (
    id VARCHAR(36) PRIMARY KEY,
    reference VARCHAR(128) NOT NULL UNIQUE,
    source_account VARCHAR(128),
    destination_account VARCHAR(128),
    amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
    id VARCHAR(36) PRIMARY KEY,
    refund_code VARCHAR(64) NOT NULL UNIQUE,
    transaction_id VARCHAR(36) NOT NULL,
    customer_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR(255),
    external_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS provider_debts (
    id VARCHAR(36) PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_debt_payments (
    id VARCHAR(36) PRIMARY KEY,
    payment_code VARCHAR(64) NOT NULL UNIQUE,
    provider_debt_id VARCHAR(36),
    provider_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    external_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- =====================================================================
-- ÍNDICES DE RENDIMIENTO
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_txn_provider_id ON service_transactions(provider_id);
CREATE INDEX IF NOT EXISTS idx_txn_customer_id ON service_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_txn_service_request ON service_transactions(service_request_id);

CREATE INDEX IF NOT EXISTS idx_ledger_provider_id ON provider_ledger_entries(provider_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created_at ON provider_ledger_entries(created_at);

CREATE INDEX IF NOT EXISTS idx_payout_provider_id ON payouts(provider_id);
CREATE INDEX IF NOT EXISTS idx_payout_status ON payouts(status);

CREATE INDEX IF NOT EXISTS idx_refund_customer_id ON refunds(customer_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON refunds(status);

CREATE INDEX IF NOT EXISTS idx_daily_balance_provider ON daily_balances(provider_id);
