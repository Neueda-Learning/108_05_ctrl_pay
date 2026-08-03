-- Ctrl-Pay: Payment Processing System - MySQL Schema
-- Version: 1.0
-- Created: July 31, 2026

-- ========================================
-- TABLE: payments
-- Purpose: Core payment records with lifecycle status
-- ========================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique payment identifier',
    idempotency_key VARCHAR(255) UNIQUE NULL COMMENT 'Client-provided idempotency key for duplicate prevention',
    source_account VARCHAR(50) NOT NULL COMMENT 'Source account number (format: 12 digits)',
    destination_account VARCHAR(50) NOT NULL COMMENT 'Destination account number (format: 12 digits)',
    amount DECIMAL(19, 2) NOT NULL COMMENT 'Payment amount in minor units (cents/paise)',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217 currency code (e.g., USD, EUR)',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'Payment lifecycle status: CREATED, VALIDATED, SENT, COMPLETED, FAILED',
    error_code VARCHAR(50) NULL COMMENT 'Error code if payment failed (e.g., VALIDATION_FAILED, INSUFFICIENT_FUNDS)',
    error_message TEXT NULL COMMENT 'Human-readable error message',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Payment creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    -- Constraints: Business rules enforced at database level
    CONSTRAINT chk_different_accounts CHECK (source_account != destination_account) ,
    CONSTRAINT chk_valid_amount CHECK (amount > 0 AND amount <= 1000000.00) ,
    CONSTRAINT chk_valid_currency CHECK (currency REGEXP '^[A-Z]{3}$') ,

    -- Indexes for query optimization
    INDEX idx_status (status) COMMENT 'Filter by payment status',
    INDEX idx_created_at (created_at) COMMENT 'Order by creation date',
    INDEX idx_idempotency_key (idempotency_key) COMMENT 'Lookup by idempotency key for duplicate detection',
    INDEX idx_source_account (source_account) COMMENT 'Filter by source account',
    INDEX idx_destination_account (destination_account) COMMENT 'Filter by destination account',
    INDEX idx_currency (currency) COMMENT 'Filter by currency'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Core payment records';

-- ========================================
-- TABLE: payment_status_history
-- Purpose: Immutable audit trail of all status transitions
-- ========================================
CREATE TABLE IF NOT EXISTS payment_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'History record ID',
    payment_id BIGINT NOT NULL COMMENT 'Foreign key to payments table',
    old_status VARCHAR(20) NULL COMMENT 'Previous status (NULL for initial CREATED entry)',
    new_status VARCHAR(20) NOT NULL COMMENT 'New status after transition',
    error_code VARCHAR(50) NULL COMMENT 'Error code if transition to FAILED',
    error_message TEXT NULL COMMENT 'Error message for failed transitions',
    triggered_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT 'Who/what triggered this change: SYSTEM, USER, RETRY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Transition timestamp',

    CONSTRAINT fk_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE ,

    -- Indexes for audit trail retrieval
    INDEX idx_payment_id_created (payment_id, created_at) COMMENT 'Efficiently retrieve history for a payment'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable audit trail of payment status changes';

-- ========================================
-- TABLE: validation_rules
-- Purpose: Configurable validation rules stored in database
-- ========================================
CREATE TABLE IF NOT EXISTS validation_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique rule identifier',
    name VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique rule name (e.g., AMOUNT_RANGE, CURRENCY_WHITELIST)',
    description TEXT NULL COMMENT 'Human-readable rule description',
    rule_type VARCHAR(50) NOT NULL COMMENT 'Rule type: AMOUNT_RANGE, CURRENCY_WHITELIST, ACCOUNT_FORMAT, ACCOUNT_DIFFERENCE, MOCK_SUFFICIENT_FUNDS, etc.',
    rule_definition JSON NOT NULL COMMENT 'Rule parameters stored as JSON (e.g., min, max, allowed_values, pattern)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether rule is currently active',
    severity VARCHAR(20) NOT NULL DEFAULT 'HARD' COMMENT 'HARD (blocks payment) or SOFT (warning only)',
    order_of_execution INT NOT NULL DEFAULT 0 COMMENT 'Execution sequence for rule engine (lower = earlier)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Rule creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    -- Indexes for rule engine queries
    INDEX idx_is_active (is_active) COMMENT 'Fetch only active rules',
    INDEX idx_rule_type (rule_type) COMMENT 'Find rules by type',
    INDEX idx_order_of_execution (order_of_execution) COMMENT 'Sort by execution order'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Configurable validation rules (zero-downtime deployment)';

-- ========================================
-- TABLE: validation_results
-- Purpose: Immutable audit log of validation checks performed
-- ========================================
CREATE TABLE IF NOT EXISTS validation_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Result record ID',
    payment_id BIGINT NOT NULL COMMENT 'Foreign key to payments table',
    validation_rule_id BIGINT NOT NULL COMMENT 'Foreign key to validation_rules table',
    rule_name VARCHAR(255) NOT NULL COMMENT 'Name of rule (denormalized for readability)',
    rule_definition JSON NOT NULL COMMENT 'Snapshot of rule definition at time of execution',
    passed BOOLEAN NOT NULL COMMENT 'Whether validation passed (true) or failed (false)',
    error_code VARCHAR(50) NULL COMMENT 'Error code if validation failed',
    error_message TEXT NULL COMMENT 'Human-readable error message if failed',
    execution_time_ms INT NOT NULL DEFAULT 0 COMMENT 'Time taken to execute this rule in milliseconds',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Validation execution timestamp',

    CONSTRAINT fk_validation_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE ,
    CONSTRAINT fk_validation_rule_id FOREIGN KEY (validation_rule_id) REFERENCES validation_rules(id) ,

    -- Indexes for validation audit retrieval
    INDEX idx_payment_id_created (payment_id, created_at) COMMENT 'Retrieve validation history for payment',
    INDEX idx_validation_rule_id (validation_rule_id) COMMENT 'Find payments that failed a specific rule'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit log of validation rule execution and results';

-- ========================================
-- TABLE: payment_retry_attempts
-- Purpose: Track retry attempts for failed payments
-- ========================================
CREATE TABLE IF NOT EXISTS payment_retry_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Retry attempt record ID',
    payment_id BIGINT NOT NULL COMMENT 'Foreign key to payments table',
    attempt_number INT NOT NULL COMMENT 'Sequential retry attempt number (1 = first retry after initial failure)',
    status_before VARCHAR(20) NOT NULL COMMENT 'Status before this retry attempt',
    status_after VARCHAR(20) NOT NULL COMMENT 'Status after this retry attempt',
    error_code VARCHAR(50) NULL COMMENT 'Error code if retry failed',
    error_message TEXT NULL COMMENT 'Error message if retry failed',
    execution_time_ms INT NOT NULL DEFAULT 0 COMMENT 'Time taken for retry operation in milliseconds',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Retry attempt timestamp',

    CONSTRAINT fk_retry_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,

    -- Indexes for retry tracking
    INDEX idx_payment_id_attempt (payment_id, attempt_number) COMMENT 'Track retry history for a payment'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Retry attempt tracking for failed payments';

-- ========================================
-- SEED DATA: Initial Validation Rules
-- ========================================

-- Rule 1: Amount Range Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'AMOUNT_RANGE',
    'Validates that payment amount is between $0.01 and $1,000,000',
    'AMOUNT_RANGE',
    JSON_OBJECT(
        'type', 'AMOUNT_RANGE',
        'min', 0.01,
        'max', 1000000.00,
        'message', 'Amount must be between $0.01 and $1,000,000'
    ),
    'HARD',
    1,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 2: Currency Whitelist Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'CURRENCY_WHITELIST',
    'Validates that currency is in the supported list',
    'CURRENCY_WHITELIST',
    JSON_OBJECT(
        'type', 'CURRENCY_WHITELIST',
        'allowed_currencies', JSON_ARRAY('USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY', 'INR', 'MXN'),
        'message', 'Currency is not supported'
    ),
    'HARD',
    2,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 3: Account Format Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'ACCOUNT_FORMAT',
    'Validates that account numbers are in correct format (12 digits)',
    'ACCOUNT_FORMAT',
    JSON_OBJECT(
        'type', 'ACCOUNT_FORMAT',
        'pattern', '^[0-9]{12}$',
        'message', 'Account number must be exactly 12 digits'
    ),
    'HARD',
    3,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 4: Account Difference Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'ACCOUNT_DIFFERENCE',
    'Ensures source and destination accounts are different',
    'ACCOUNT_DIFFERENCE',
    JSON_OBJECT(
        'type', 'ACCOUNT_DIFFERENCE',
        'message', 'Source and destination accounts must be different'
    ),
    'HARD',
    4,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 5: Mock Sufficient Funds Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'MOCK_SUFFICIENT_FUNDS',
    'Simulates checking if source account has sufficient funds (mock implementation)',
    'MOCK_SUFFICIENT_FUNDS',
    JSON_OBJECT(
        'type', 'MOCK_SUFFICIENT_FUNDS',
        'failure_rate', 0.1,
        'message', 'Insufficient funds in source account'
    ),
    'HARD',
    5,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- ========================================
-- VERIFICATION QUERIES (Run after schema creation)
-- ========================================
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'ctrl_pay';
-- SHOW CREATE TABLE payments;
-- SELECT * FROM validation_rules;
-- DESC payment_status_history;

