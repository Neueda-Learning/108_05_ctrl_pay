-- Ctrl-Pay: Payment Processing System - MySQL Schema
-- Version: 1.0
-- Created: July 31, 2026

-- ========================================
-- TABLE: customers
-- Purpose: Customer profile master record keyed by PAN
-- ========================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique customer identifier',
    name VARCHAR(255) NOT NULL COMMENT 'Customer full name',
    dob DATE NOT NULL COMMENT 'Customer date of birth',
    phone_number VARCHAR(20) NOT NULL COMMENT 'Customer phone number',
    pan_number VARCHAR(10) NOT NULL UNIQUE COMMENT 'Permanent Account Number (unique customer key)',
    profile_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Profile creation timestamp',
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last profile update timestamp',
    country VARCHAR(100) NOT NULL COMMENT 'Customer country of residence',
    customer_account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Customer profile status: ACTIVE or PASSIVE',

    CONSTRAINT chk_customer_account_status CHECK (customer_account_status IN ('ACTIVE', 'PASSIVE')),
    CONSTRAINT chk_pan_number_format CHECK (pan_number REGEXP '^[A-Z]{5}[0-9]{4}[A-Z]{1}$'),
    CONSTRAINT chk_customer_phone_format CHECK (phone_number REGEXP '^[0-9+() -]{7,20}$') ,

    INDEX idx_customer_name (name) COMMENT 'Search customers by name',
    INDEX idx_customer_phone (phone_number) COMMENT 'Lookup customers by phone number',
    INDEX idx_customer_status (customer_account_status) COMMENT 'Filter customers by account status'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer profile master record keyed by PAN';

-- ========================================
-- TABLE: accounts
-- Purpose: Bank account records linked to a single customer profile
-- ========================================
CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique account identifier',
    customer_id BIGINT NOT NULL COMMENT 'Foreign key to customers.customer_id',
    account_number VARCHAR(12) NOT NULL UNIQUE COMMENT 'Unique 12-digit account number (business identifier)',
    account_name VARCHAR(255) NOT NULL COMMENT 'Account holder nickname or display name',
    account_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT 'Current account balance',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Account status: ACTIVE, PASSIVE, DORMANT, or SUSPICIOUS',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217 currency code',
    account_opening_date DATE NOT NULL COMMENT 'Date the account was opened',
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last account update timestamp',
    ifsc_code VARCHAR(11) NOT NULL COMMENT 'Bank IFSC code',
    account_location VARCHAR(255) NOT NULL COMMENT 'Branch or account location',
    bank_name VARCHAR(255) NOT NULL COMMENT 'Bank name',
    account_pin_hash VARCHAR(255) NOT NULL COMMENT 'Account PIN used for payment authentication (plain string storage)',

    CONSTRAINT fk_account_customer_id FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_account_status CHECK (account_status IN ('ACTIVE', 'PASSIVE', 'DORMANT', 'SUSPICIOUS')),
    CONSTRAINT chk_account_balance CHECK (account_balance >= 0),
    CONSTRAINT chk_account_currency CHECK (currency REGEXP '^[A-Z]{3}$'),
    CONSTRAINT chk_account_number_format CHECK (account_number REGEXP '^[0-9]{12}$'),
    CONSTRAINT chk_ifsc_code_format CHECK (ifsc_code REGEXP '^[A-Z]{4}0[A-Z0-9]{6}$'),

    INDEX idx_account_number (account_number) COMMENT 'Lookup account by account number',
    INDEX idx_account_customer_id (customer_id) COMMENT 'List all accounts for a customer',
    INDEX idx_account_status (account_status) COMMENT 'Filter by account status',
    INDEX idx_account_currency (currency) COMMENT 'Filter by currency',
    INDEX idx_account_bank_name (bank_name) COMMENT 'Search by bank name'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bank account records linked to customers';

SET @account_pin_hash_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'accounts'
      AND column_name = 'account_pin_hash'
);
SET @add_account_pin_hash_sql = IF(
    @account_pin_hash_exists = 0,
    'ALTER TABLE accounts ADD COLUMN account_pin_hash VARCHAR(255) NOT NULL COMMENT ''Account PIN used for payment authentication (plain string storage)''',
    'SELECT 1'
);
PREPARE add_account_pin_hash_stmt FROM @add_account_pin_hash_sql;
EXECUTE add_account_pin_hash_stmt;
DEALLOCATE PREPARE add_account_pin_hash_stmt;

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
    source_amount DECIMAL(19, 2) NULL COMMENT 'Amount debited from sender account (in sender currency)',
    destination_amount DECIMAL(19, 2) NULL COMMENT 'Amount credited to receiver account (in receiver currency)',
    exchange_rate DECIMAL(19, 6) NULL COMMENT 'Exchange rate applied (if cross-currency)',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'Payment lifecycle status: CREATED, VALIDATED, SUSPICIOUS, SENT, COMPLETED, FAILED',
    error_code VARCHAR(50) NULL COMMENT 'Error code if payment failed (e.g., VALIDATION_FAILED, INSUFFICIENT_FUNDS)',
    error_message TEXT NULL COMMENT 'Human-readable error message',
    settlement_attempt_count INT NOT NULL DEFAULT 0 COMMENT 'Number of settlement processing attempts',
    max_settlement_attempts INT NOT NULL DEFAULT 3 COMMENT 'Maximum allowed settlement attempts',
    last_settlement_attempt_time TIMESTAMP NULL COMMENT 'Timestamp of last settlement attempt',
    next_settlement_retry_time TIMESTAMP NULL COMMENT 'Scheduled time for next settlement retry',
    settled_at TIMESTAMP NULL COMMENT 'Timestamp when payment was successfully settled (accounts debited/credited)',
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
    INDEX idx_currency (currency) COMMENT 'Filter by currency',
    INDEX idx_next_retry_time (next_settlement_retry_time) COMMENT 'Find payments ready for retry settlement'
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
    name VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique rule name (e.g., AMOUNT_RANGE, ACCOUNT_FORMAT)',
    description TEXT NULL COMMENT 'Human-readable rule description',
    rule_type VARCHAR(50) NOT NULL COMMENT 'Rule type: AMOUNT_RANGE, ACCOUNT_FORMAT, ACCOUNT_DIFFERENCE, SUFFICIENT_FUNDS, etc.',
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

-- Rule 2: Account Format Validation
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
    2,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 3: Account Difference Validation
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
    3,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- Rule 4: Sufficient Funds Validation
INSERT INTO validation_rules (name, description, rule_type, rule_definition, severity, order_of_execution, is_active)
VALUES (
    'SUFFICIENT_FUNDS',
    'Validates that source account has sufficient balance for the payment',
    'SUFFICIENT_FUNDS',
    JSON_OBJECT(
        'type', 'SUFFICIENT_FUNDS',
        'message', 'Insufficient funds in source account'
    ),
    'HARD',
    4,
    TRUE
) ON DUPLICATE KEY UPDATE is_active = TRUE;

-- ========================================
-- TABLE: fraud_assessments
-- Purpose: Store fraud detection results for each payment
-- ========================================
CREATE TABLE IF NOT EXISTS fraud_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique fraud assessment identifier',
    payment_id BIGINT NOT NULL UNIQUE COMMENT 'Foreign key to payments table',
    hybrid_fraud_score DECIMAL(5, 2) NOT NULL COMMENT 'Final hybrid fraud score (0-100)',
    rule_engine_score DECIMAL(5, 2) NOT NULL COMMENT 'Aggregated score from fraud rules (0-100)',
    ml_fraud_probability DECIMAL(5, 2) NOT NULL COMMENT 'ML model fraud probability (0-100 scale)',
    triggered_rules_json JSON NULL COMMENT 'Array of triggered rule names',
    rule_scores_json JSON NOT NULL COMMENT 'Detailed breakdown of rule scores',
    decision VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT 'Final decision: APPROVED, SUSPICIOUS, REJECTED',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW' COMMENT 'Risk classification: LOW, MEDIUM, HIGH, CRITICAL',
    explanation TEXT NOT NULL COMMENT 'Explanation of the fraud assessment',
    reviewed_by VARCHAR(255) NULL COMMENT 'Admin user who reviewed (null if auto-decision)',
    reviewed_at TIMESTAMP NULL COMMENT 'Timestamp of admin review',
    reviewer_notes TEXT NULL COMMENT 'Admin reviewer notes/reasoning',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Assessment creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    CONSTRAINT fk_fraud_assessment_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT chk_fraud_score CHECK (hybrid_fraud_score >= 0 AND hybrid_fraud_score <= 100),
    CONSTRAINT chk_rule_score CHECK (rule_engine_score >= 0 AND rule_engine_score <= 100),
    CONSTRAINT chk_ml_probability CHECK (ml_fraud_probability >= 0 AND ml_fraud_probability <= 100),
    CONSTRAINT chk_fraud_decision CHECK (decision IN ('APPROVED', 'SUSPICIOUS', 'REJECTED')),
    CONSTRAINT chk_fraud_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

    -- Indexes for efficient querying
    INDEX idx_payment_id (payment_id) COMMENT 'Lookup assessment by payment',
    INDEX idx_decision (decision) COMMENT 'Filter by fraud decision',
    INDEX idx_decision_reviewed (decision, reviewed_by) COMMENT 'Find pending reviews',
    INDEX idx_created_at (created_at) COMMENT 'Order by creation date',
    INDEX idx_risk_level (risk_level) COMMENT 'Filter by risk level'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fraud assessment records with decision tracking';

-- ========================================
-- TABLE: fraud_account_risk
-- Purpose: Track account rejection history for risk escalation
-- ========================================
CREATE TABLE IF NOT EXISTS fraud_account_risk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique risk record identifier',
    account_number VARCHAR(12) NOT NULL COMMENT 'Account number',
    rejection_count INT NOT NULL DEFAULT 0 COMMENT 'Number of rejected/suspicious payments in window',
    window_start TIMESTAMP NOT NULL COMMENT 'Start of rolling 30-day window',
    window_end TIMESTAMP NOT NULL COMMENT 'End of rolling 30-day window',
    latest_rejection_at TIMESTAMP NULL COMMENT 'Timestamp of most recent rejection',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update',

    CONSTRAINT fk_fraud_risk_account FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE,

    INDEX idx_account_number (account_number) COMMENT 'Lookup risk record by account'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Account fraud risk tracking for escalation';

-- ========================================
-- TABLE: bulk_payment_batches
-- Purpose: Master record for bulk payment batch execution
-- ========================================
CREATE TABLE IF NOT EXISTS bulk_payment_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique batch identifier',
    batch_reference VARCHAR(50) NOT NULL UNIQUE COMMENT 'User-facing batch reference (e.g., BP202608051234)',
    idempotency_key VARCHAR(255) UNIQUE NULL COMMENT 'Client-provided idempotency key to prevent duplicate batch uploads',
    source_account VARCHAR(12) NOT NULL COMMENT 'Source account number (12 digits)',
    total_transactions INT NOT NULL DEFAULT 0 COMMENT 'Total number of transactions in batch',
    successful_transactions INT NOT NULL DEFAULT 0 COMMENT 'Count of successfully processed transactions',
    failed_transactions INT NOT NULL DEFAULT 0 COMMENT 'Count of failed transactions',
    total_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00 COMMENT 'Sum of all transaction amounts (informational)',
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED' COMMENT 'Batch status: CREATED, VALIDATING, VALIDATED, PROCESSING, COMPLETED, PARTIALLY_COMPLETED, FAILED, ROLLED_BACK',
    created_by VARCHAR(255) NOT NULL COMMENT 'User ID who created/uploaded the batch',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Batch creation timestamp',
    validation_started_at TIMESTAMP NULL COMMENT 'Timestamp when validation phase started',
    validation_completed_at TIMESTAMP NULL COMMENT 'Timestamp when validation phase completed',
    processing_started_at TIMESTAMP NULL COMMENT 'Timestamp when processing phase started',
    processing_completed_at TIMESTAMP NULL COMMENT 'Timestamp when processing phase completed (or last update)',
    completed_at TIMESTAMP NULL COMMENT 'Timestamp when batch processing fully completed',
    last_error_message TEXT NULL COMMENT 'Last error encountered during processing',

    CONSTRAINT fk_batch_source_account FOREIGN KEY (source_account) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    CONSTRAINT chk_batch_status CHECK (status IN ('CREATED', 'VALIDATING', 'VALIDATED', 'PROCESSING', 'COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED', 'ROLLED_BACK')),
    CONSTRAINT chk_batch_transactions CHECK (total_transactions >= 0),
    CONSTRAINT chk_batch_successful CHECK (successful_transactions >= 0),
    CONSTRAINT chk_batch_failed CHECK (failed_transactions >= 0),
    CONSTRAINT chk_batch_amount CHECK (total_amount >= 0),

    INDEX idx_batch_reference (batch_reference) COMMENT 'Lookup by batch reference',
    INDEX idx_batch_source_account (source_account) COMMENT 'Filter batches by source account',
    INDEX idx_batch_status (status) COMMENT 'Filter by batch status',
    INDEX idx_batch_created_by (created_by) COMMENT 'Filter by batch creator',
    INDEX idx_batch_created_at (created_at) COMMENT 'Order by creation date',
    INDEX idx_batch_idempotency_key (idempotency_key) COMMENT 'Duplicate batch detection'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bulk payment batch master records';

-- ========================================
-- TABLE: bulk_payment_items
-- Purpose: Individual transactions within a bulk payment batch
-- ========================================
CREATE TABLE IF NOT EXISTS bulk_payment_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique item identifier within batch',
    batch_id BIGINT NOT NULL COMMENT 'Foreign key to bulk_payment_batches.id',
    payment_id BIGINT NULL COMMENT 'Foreign key to payments.id (NULL until payment is created)',
    line_number INT NOT NULL COMMENT 'Line number in CSV (1-indexed)',
    destination_account VARCHAR(12) NOT NULL COMMENT 'Destination account number (12 digits)',
    amount DECIMAL(19, 2) NOT NULL COMMENT 'Transaction amount',
    currency CHAR(3) NOT NULL COMMENT 'ISO 4217 currency code',
    description VARCHAR(500) NULL COMMENT 'Transaction description/memo',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Item status: PENDING, VALIDATING, VALIDATED, PROCESSING, SUCCESS, FAILED, ROLLED_BACK',
    error_code VARCHAR(50) NULL COMMENT 'Error code if validation/processing failed',
    failure_reason TEXT NULL COMMENT 'Detailed failure reason',
    fraud_score DECIMAL(5, 2) NULL COMMENT 'Fraud score (0-100) assigned to this transaction',
    fraud_decision VARCHAR(20) NULL COMMENT 'Fraud decision: APPROVED, SUSPICIOUS, REJECTED',
    validation_errors JSON NULL COMMENT 'Array of validation error details',
    rollback_status VARCHAR(50) NULL COMMENT 'Rollback status if transaction was rolled back',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Item creation timestamp',
    validated_at TIMESTAMP NULL COMMENT 'Timestamp when validation completed',
    processing_started_at TIMESTAMP NULL COMMENT 'Timestamp when payment processing started',
    completed_at TIMESTAMP NULL COMMENT 'Timestamp when payment processing completed',

    CONSTRAINT fk_item_batch FOREIGN KEY (batch_id) REFERENCES bulk_payment_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT chk_item_status CHECK (status IN ('PENDING', 'VALIDATING', 'VALIDATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'ROLLED_BACK')),
    CONSTRAINT chk_item_amount CHECK (amount > 0),
    CONSTRAINT chk_item_currency CHECK (currency REGEXP '^[A-Z]{3}$'),

    INDEX idx_item_batch_id (batch_id) COMMENT 'Retrieve all items in a batch',
    INDEX idx_item_status (status) COMMENT 'Filter items by status',
    INDEX idx_item_payment_id (payment_id) COMMENT 'Link to individual payment',
    INDEX idx_item_destination_account (destination_account) COMMENT 'Filter by destination',
    INDEX idx_item_fraud_decision (fraud_decision) COMMENT 'Track fraud rejections',
    UNIQUE INDEX uk_batch_line (batch_id, line_number) COMMENT 'Unique line number per batch'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Individual transactions in bulk payment batches';

-- ========================================
-- TABLE: bulk_payment_error_log
-- Purpose: Append-only audit log of all errors encountered during batch processing
-- ========================================
CREATE TABLE IF NOT EXISTS bulk_payment_error_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Error log record ID',
    batch_id BIGINT NOT NULL COMMENT 'Foreign key to bulk_payment_batches.id',
    item_id BIGINT NULL COMMENT 'Foreign key to bulk_payment_items.id (if item-specific error)',
    line_number INT NULL COMMENT 'Line number in CSV where error occurred',
    error_type VARCHAR(50) NOT NULL COMMENT 'Error category: VALIDATION, FRAUD, INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND, PROCESSING, ROLLBACK',
    error_code VARCHAR(50) NOT NULL COMMENT 'Specific error code from business logic',
    error_message TEXT NOT NULL COMMENT 'Detailed error message',
    error_details JSON NULL COMMENT 'Additional error context (e.g., validation failures)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Error timestamp',

    CONSTRAINT fk_error_log_batch FOREIGN KEY (batch_id) REFERENCES bulk_payment_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_error_log_item FOREIGN KEY (item_id) REFERENCES bulk_payment_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_error_type CHECK (error_type IN ('VALIDATION', 'FRAUD', 'INSUFFICIENT_FUNDS', 'ACCOUNT_NOT_FOUND', 'PROCESSING', 'ROLLBACK')),

    INDEX idx_error_log_batch (batch_id) COMMENT 'Retrieve all errors for batch',
    INDEX idx_error_log_item (item_id) COMMENT 'Retrieve errors for specific item',
    INDEX idx_error_log_type (error_type) COMMENT 'Filter by error type',
    INDEX idx_error_log_created (created_at) COMMENT 'Order by timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Append-only error log for bulk payment processing';

-- ========================================
-- TABLE: bulk_payment_audit_events
-- Purpose: Audit trail for bulk payment lifecycle events (compliance requirement)
-- ========================================
CREATE TABLE IF NOT EXISTS bulk_payment_audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Audit event ID',
    batch_id BIGINT NOT NULL COMMENT 'Foreign key to bulk_payment_batches.id',
    event_type VARCHAR(100) NOT NULL COMMENT 'Event type: BATCH_CREATED, VALIDATION_STARTED, VALIDATION_COMPLETED, PROCESSING_STARTED, PROCESSING_COMPLETED, ROLLBACK_EXECUTED, etc.',
    event_timestamp TIMESTAMP NOT NULL COMMENT 'When event occurred',
    triggered_by VARCHAR(50) NOT NULL COMMENT 'Who/what triggered: USER, SYSTEM, SCHEDULER',
    triggered_by_user_id VARCHAR(255) NULL COMMENT 'User ID if triggered by user action',
    event_data JSON NULL COMMENT 'Additional event context (e.g., before/after state)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Log record creation timestamp',

    CONSTRAINT fk_audit_batch FOREIGN KEY (batch_id) REFERENCES bulk_payment_batches(id) ON DELETE CASCADE,
    CONSTRAINT chk_triggered_by CHECK (triggered_by IN ('USER', 'SYSTEM', 'SCHEDULER')),

    INDEX idx_audit_batch (batch_id) COMMENT 'Retrieve all events for batch',
    INDEX idx_audit_event_type (event_type) COMMENT 'Filter by event type',
    INDEX idx_audit_timestamp (event_timestamp) COMMENT 'Order by timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Compliance audit trail for bulk payment operations';

-- ========================================
-- TABLE: ml_models (Phase 1 - ML Model Versioning)
-- ========================================
CREATE TABLE IF NOT EXISTS ml_models (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    description TEXT NULL,
    model_type VARCHAR(50) NOT NULL DEFAULT 'XGBOOST',
    model_path VARCHAR(500) NOT NULL,
    training_date TIMESTAMP NULL,
    training_dataset_name VARCHAR(255) NOT NULL DEFAULT 'unknown',
    training_dataset_size INT UNSIGNED NULL,
    accuracy_score DECIMAL(5,2) NULL,
    precision_score DECIMAL(5,2) NULL,
    recall_score DECIMAL(5,2) NULL,
    f1_score DECIMAL(5,2) NULL,
    auc_score DECIMAL(5,2) NULL,
    false_positive_rate DECIMAL(5,2) NULL,
    false_negative_rate DECIMAL(5,2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    deployment_date TIMESTAMP NULL,
    retirement_date TIMESTAMP NULL,
    deployment_context VARCHAR(100) NULL,
    feature_columns_json JSON NULL,
    hyperparameters_json JSON NULL,
    created_by VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_model_name_version UNIQUE (model_name, model_version),
    INDEX idx_active_model (is_active, deployment_date),
    INDEX idx_model_name (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ML model versioning and deployment tracking';

-- ========================================
-- MIGRATION: Add idempotency_key column to bulk_payment_items
-- ========================================
SET @idempotency_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'bulk_payment_items'
      AND column_name = 'idempotency_key'
);
SET @add_idempotency_key_sql = IF(
    @idempotency_key_exists = 0,
    'ALTER TABLE bulk_payment_items ADD COLUMN idempotency_key VARCHAR(255) NULL COMMENT ''Idempotency key for this bulk payment item (generated during processing)'' AFTER payment_id',
    'SELECT 1'
);
PREPARE add_idempotency_key_stmt FROM @add_idempotency_key_sql;
EXECUTE add_idempotency_key_stmt;
DEALLOCATE PREPARE add_idempotency_key_stmt;

-- TABLE: ml_model_predictions (Phase 1 - Prediction Tracking)
CREATE TABLE IF NOT EXISTS ml_model_predictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ml_model_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    assessment_id BIGINT NULL,
    predicted_fraud_probability DECIMAL(5,2) NOT NULL,
    prediction_confidence DECIMAL(5,2) NULL,
    prediction_latency_ms INT UNSIGNED NULL,
    ground_truth_fraud BOOLEAN NULL,
    ground_truth_source VARCHAR(100) NULL,
    ground_truth_date TIMESTAMP NULL,
    is_correct_prediction BOOLEAN NULL,
    prediction_type VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ml_pred_model FOREIGN KEY (ml_model_id) REFERENCES ml_models(id),
    CONSTRAINT fk_ml_pred_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_ml_pred_model (ml_model_id),
    INDEX idx_ml_pred_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ML model prediction tracking';

-- TABLE: fraud_audit_events (Phase 1 - Audit Trail)
CREATE TABLE IF NOT EXISTS fraud_audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assessment_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    triggered_by VARCHAR(50) NOT NULL,
    triggered_by_user_id VARCHAR(255) NULL,
    event_data JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_assessment FOREIGN KEY (assessment_id) REFERENCES fraud_assessments(id) ON DELETE CASCADE,
    INDEX idx_audit_assessment (assessment_id),
    INDEX idx_audit_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fraud assessment audit trail';

-- Seed initial ML model record (active, represents the deployed XGBoost model)
INSERT IGNORE INTO ml_models (
    model_name, model_version, description, model_type, model_path,
    training_date, training_dataset_name, training_dataset_size,
    accuracy_score, precision_score, recall_score, f1_score, auc_score,
    is_active, deployment_date, deployment_context, created_by
) VALUES (
    'xgboost_paysim', '1.0.0',
    'XGBoost model trained on PaySim synthetic financial dataset',
    'XGBOOST', '/models/XGBoostModel.pkl',
    NOW(), 'paysim_v1', 6000000,
    99.96, 95.02, 76.13, 84.53, 99.46,  -- Accuracy/Precision/Recall/F1/AUC
    TRUE, NOW(), 'PROD', 'SYSTEM'
);

-- TABLE: fraud_rules (Configurable Fraud Detection Rules)
-- Purpose: Manage fraud detection rules dynamically without redeployment
CREATE TABLE IF NOT EXISTS fraud_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique rule identifier',
    rule_name VARCHAR(255) NOT NULL UNIQUE COMMENT 'Unique rule name (e.g., LARGE_TRANSACTION_RULE)',
    rule_type VARCHAR(100) NOT NULL COMMENT 'Rule type classifier (e.g., THRESHOLD, PATTERN, BEHAVIOR)',
    description TEXT NULL COMMENT 'Human-readable description of rule logic',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether rule is currently active',
    severity VARCHAR(50) DEFAULT 'MEDIUM' COMMENT 'Rule severity: LOW, MEDIUM, HIGH, CRITICAL',
    order_of_execution INT DEFAULT 100 COMMENT 'Execution order (lower first)',
    weight DECIMAL(5,2) DEFAULT 1.0 COMMENT 'Weight in fraud score calculation',
    rule_definition JSON NULL COMMENT 'Rule-specific configuration',
    triggering_conditions JSON NULL COMMENT 'Conditions that trigger this rule',
    mock_score INT DEFAULT 0 COMMENT 'Mock fraud score (for testing)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Rule creation date',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last updated date',
    created_by VARCHAR(255) NULL COMMENT 'Creator user',
    updated_by VARCHAR(255) NULL COMMENT 'Last updater user',

    CONSTRAINT chk_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    INDEX idx_rule_active (is_active),
    INDEX idx_rule_name (rule_name),
    INDEX idx_rule_type (rule_type),
    INDEX idx_rule_order (order_of_execution)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Configurable fraud detection rules';

-- ========================================
-- SEED DATA: Default Fraud Rules
-- ========================================
INSERT IGNORE INTO fraud_rules (rule_name, rule_type, description, is_active, severity, order_of_execution, weight, created_by, updated_by) VALUES
('LARGE_TRANSACTION_RULE', 'THRESHOLD', 'Detects unusually large transactions that exceed 80% of account balance', TRUE, 'HIGH', 10, 1.5, 'SYSTEM', 'SYSTEM'),
('EXTREMELY_LARGE_TRANSACTION_RULE', 'THRESHOLD', 'Auto-rejects transactions exceeding 10000 in any currency', TRUE, 'CRITICAL', 5, 1.2, 'SYSTEM', 'SYSTEM'),
('ACCOUNT_DRAIN_RULE', 'PATTERN', 'Detects attempts to drain account balance (leaving < 100)', TRUE, 'CRITICAL', 15, 1.5, 'SYSTEM', 'SYSTEM'),
('TRANSACTION_VELOCITY_RULE', 'VELOCITY', 'Monitors transaction frequency - flags >5 in 5 min or >20 in 24h', TRUE, 'HIGH', 20, 1.2, 'SYSTEM', 'SYSTEM'),
('BEHAVIOR_CHANGE_RULE', 'BEHAVIOR', 'Detects deviation from historical transaction patterns (3σ threshold)', TRUE, 'MEDIUM', 25, 1.0, 'SYSTEM', 'SYSTEM'),
('NEW_DESTINATION_RULE', 'ANOMALY', 'Flags transactions to new/untrusted destination accounts (< 30 days)', TRUE, 'MEDIUM', 30, 0.8, 'SYSTEM', 'SYSTEM'),
('MULTIPLE_FAILURE_RULE', 'PATTERN', 'Detects multiple failed transactions (>3 failures in 7 days)', TRUE, 'MEDIUM', 35, 0.9, 'SYSTEM', 'SYSTEM'),
('SUSPICIOUS_ACCOUNT_RULE', 'BEHAVIOR', 'Flags suspicious accounts with history of fraud-rejected payments', TRUE, 'HIGH', 40, 1.3, 'SYSTEM', 'SYSTEM'),
('CROSS_CURRENCY_RULE', 'THRESHOLD', 'Higher scrutiny for cross-currency transactions (>=5000)', TRUE, 'MEDIUM', 45, 0.8, 'SYSTEM', 'SYSTEM'),
('ML_FRAUD_RULE', 'ANOMALY', 'ML model prediction for fraud probability (XGBoost)', TRUE, 'HIGH', 50, 2.5, 'SYSTEM', 'SYSTEM'),
('UNUSUAL_TIME_PATTERN_RULE', 'BEHAVIOR', 'Detects off-hours or weekend transactions deviating from normal patterns', TRUE, 'LOW', 55, 0.7, 'SYSTEM', 'SYSTEM'),
('VELOCITY_ANOMALY_RULE', 'VELOCITY', 'High-velocity transaction detection with statistical analysis', TRUE, 'MEDIUM', 60, 1.1, 'SYSTEM', 'SYSTEM'),
('CYCLICAL_TRANSACTION_RULE', 'PATTERN', 'Detects circular fund flow patterns (A→B→C→A)', TRUE, 'HIGH', 65, 1.4, 'SYSTEM', 'SYSTEM'),
('BEHAVIORAL_BASELINE_RULE', 'BEHAVIOR', 'Deviation from baseline customer behavior profile', TRUE, 'MEDIUM', 70, 1.0, 'SYSTEM', 'SYSTEM'),
('CONTEXTUAL_RISK_AGGREGATION_RULE', 'PATTERN', 'Composite risk assessment combining multiple signals', TRUE, 'HIGH', 75, 1.2, 'SYSTEM', 'SYSTEM');

-- ========================================
-- VERIFICATION QUERIES (Run after schema creation)
-- ========================================
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'ctrl_pay';
-- SHOW CREATE TABLE fraud_assessments;
-- SELECT * FROM fraud_account_risk;
-- DESC fraud_assessments;
-- SELECT COUNT(*) FROM fraud_rules WHERE is_active = true;

-- Ctrl Pay Sample Data
-- Run after schema.sql
-- Contains only environment sample data

USE ctrl_pay;

SET FOREIGN_KEY_CHECKS = 0;


-- ========================================
-- CUSTOMERS
-- ========================================

INSERT IGNORE INTO customers
(customer_id,name,dob,phone_number,pan_number,country,customer_account_status)
VALUES
(1,'John Smith','1990-01-10','+353871111111','ABCDE1234F','Ireland','ACTIVE'),
(2,'Emma Watson','1988-04-15','+353872222222','FGHIJ2345K','Ireland','ACTIVE'),
(3,'Raj Kumar','1992-06-20','+919876543210','KLMNO3456P','India','ACTIVE'),
(4,'Sophia Brown','1985-11-05','+447700900111','PQRST4567L','UK','ACTIVE'),
(5,'Michael Lee','1979-09-12','+14155552671','UVWXY5678M','USA','PASSIVE');


-- ========================================
-- ACCOUNTS
-- PIN = 1234
-- ========================================

INSERT IGNORE INTO accounts
(account_id,customer_id,account_number,account_name,
account_balance,account_status,currency,
account_opening_date,ifsc_code,
account_location,bank_name,account_pin_hash)
VALUES
(1,1,'100000000001','John USD Account',
50000,'ACTIVE','USD',
'2020-01-01','HDFC0123456',
'Dublin','Ctrl Bank','1234'),

(2,1,'100000000002','John EUR Account',
25000,'ACTIVE','EUR',
'2021-01-01','HDFC0123457',
'Dublin','Ctrl Bank','1234'),

(3,2,'100000000003','Emma GBP Account',
30000,'ACTIVE','GBP',
'2019-05-10','HDFC0123458',
'London','Ctrl Bank','1234'),

(4,3,'100000000004','Raj INR Account',
500000,'ACTIVE','INR',
'2022-02-20','HDFC0123459',
'Mumbai','Ctrl Bank','1234'),

(5,4,'100000000005','Sophia USD Account',
100,'SUSPICIOUS','USD',
'2023-03-03','HDFC0123460',
'London','Ctrl Bank','1234'),

(6,5,'100000000006','Michael CAD Account',
1000,'ACTIVE','CAD',
'2020-08-08','HDFC0123461',
'Toronto','Ctrl Bank','1234');


-- ========================================
-- PAYMENTS
-- ========================================

INSERT IGNORE INTO payments
(id,idempotency_key,
source_account,destination_account,
amount,currency,
source_amount,destination_amount,
exchange_rate,status,
error_code,error_message,
settlement_attempt_count,
max_settlement_attempts,
settled_at)
VALUES

(1,'PAY-001',
'100000000001',
'100000000003',
500,'USD',
500,450,
0.9,
'COMPLETED',
NULL,NULL,
1,3,NOW()),


(2,'PAY-002',
'100000000003',
'100000000001',
1000,'GBP',
1000,1200,
1.2,
'COMPLETED',
NULL,NULL,
1,3,NOW()),


(3,'PAY-003',
'100000000001',
'100000000004',
100000,'USD',
100000,8300000,
83,
'FAILED',
'INSUFFICIENT_FUNDS',
'Insufficient funds after validation',
3,3,NULL),


(4,'PAY-004',
'100000000004',
'100000000005',
25000,'INR',
25000,300,
0.012,
'SUSPICIOUS',
'FRAUD_REVIEW',
'Transaction requires fraud review',
0,3,NULL),


(5,'PAY-005',
'100000000002',
'100000000006',
500,'EUR',
500,700,
1.4,
'SENT',
NULL,NULL,
0,3,NULL);



-- ========================================
-- PAYMENT STATUS HISTORY
-- ========================================

INSERT IGNORE INTO payment_status_history
(payment_id,old_status,new_status,triggered_by)
VALUES

(1,NULL,'CREATED','USER'),
(1,'CREATED','VALIDATED','SYSTEM'),
(1,'VALIDATED','SENT','SCHEDULER'),
(1,'SENT','COMPLETED','SCHEDULER'),

(2,NULL,'CREATED','USER'),
(2,'CREATED','VALIDATED','SYSTEM'),
(2,'VALIDATED','SENT','SCHEDULER'),
(2,'SENT','COMPLETED','SCHEDULER'),

(3,NULL,'CREATED','USER'),
(3,'CREATED','FAILED','SYSTEM'),

(4,NULL,'CREATED','USER'),
(4,'CREATED','VALIDATED','SYSTEM'),
(4,'VALIDATED','SUSPICIOUS','SYSTEM'),

(5,NULL,'CREATED','USER'),
(5,'CREATED','VALIDATED','SYSTEM'),
(5,'VALIDATED','SENT','SCHEDULER');



-- ========================================
-- FRAUD ASSESSMENTS
-- ========================================

INSERT IGNORE INTO fraud_assessments
(payment_id,
hybrid_fraud_score,
rule_engine_score,
ml_fraud_probability,
triggered_rules_json,
rule_scores_json,
decision,
risk_level,
explanation)
VALUES

(1,
10,
5,
8,
'[]',
'{"ML":8}',
'APPROVED',
'LOW',
'Normal payment'),


(3,
90,
85,
92,
'["LARGE_TRANSACTION_RULE","ML_FRAUD_RULE"]',
'{"LARGE_TRANSACTION_RULE":85,"ML_FRAUD_RULE":92}',
'REJECTED',
'CRITICAL',
'High fraud probability'),


(4,
60,
55,
70,
'["ML_FRAUD_RULE"]',
'{"ML_FRAUD_RULE":70}',
'SUSPICIOUS',
'HIGH',
'Manual review required');



-- ========================================
-- ML PREDICTIONS
-- ========================================

INSERT IGNORE INTO ml_model_predictions
(
ml_model_id,
payment_id,
assessment_id,
predicted_fraud_probability,
prediction_confidence,
prediction_latency_ms,
ground_truth_fraud,
ground_truth_source,
is_correct_prediction,
prediction_type
)
VALUES

(1,1,1,8,95,120,
FALSE,'AUDIT',TRUE,'TRUE_NEGATIVE'),

(1,3,2,92,96,150,
TRUE,'AUDIT',TRUE,'TRUE_POSITIVE'),

(1,4,3,70,85,NULL,
NULL,NULL,NULL,NULL);



-- ========================================
-- FRAUD ACCOUNT RISK
-- ========================================

INSERT IGNORE INTO fraud_account_risk
(account_number,
rejection_count,
window_start,
window_end,
latest_rejection_at)
VALUES

('100000000004',
2,
DATE_SUB(NOW(),INTERVAL 30 DAY),
NOW(),
NOW()),

('100000000005',
1,
DATE_SUB(NOW(),INTERVAL 30 DAY),
NOW(),
NOW());



-- ========================================
-- FRAUD AUDIT EVENTS
-- ========================================

INSERT IGNORE INTO fraud_audit_events
(assessment_id,event_type,event_timestamp,triggered_by,event_data)
VALUES

(1,'FRAUD_ASSESSMENT_CREATED',NOW(),'SYSTEM',
'{"decision":"APPROVED"}'),

(2,'FRAUD_ASSESSMENT_CREATED',NOW(),'SYSTEM',
'{"decision":"REJECTED"}'),

(3,'MANUAL_REVIEW_REQUIRED',NOW(),'SYSTEM',
'{"decision":"SUSPICIOUS"}');



-- ========================================
-- BULK PAYMENT DATA
-- ========================================

INSERT IGNORE INTO bulk_payment_batches
(
id,
batch_reference,
idempotency_key,
source_account,
total_transactions,
successful_transactions,
failed_transactions,
total_amount,
status,
created_by
)
VALUES

(
1,
'BP202608060001',
'BULK-001',
'100000000001',
3,
2,
1,
3000,
'PARTIALLY_COMPLETED',
'customer1'
);



INSERT IGNORE INTO bulk_payment_items
(
batch_id,
payment_id,
idempotency_key,
line_number,
destination_account,
amount,
currency,
description,
status,
error_code,
failure_reason,
fraud_score,
fraud_decision
)
VALUES

(
1,
1,
'BULK-ITEM-001',
1,
'100000000003',
1000,
'USD',
'Salary',
'SUCCESS',
NULL,
NULL,
10,
'APPROVED'
),


(
1,
2,
'BULK-ITEM-002',
2,
'100000000004',
1500,
'USD',
'Invoice',
'SUCCESS',
NULL,
NULL,
20,
'APPROVED'
),


(
1,
NULL,
'BULK-ITEM-003',
3,
'999999999999',
500,
'USD',
'Invalid account',
'FAILED',
'INVALID_ACCOUNT',
'Destination account not found',
80,
'REJECTED'
);



SET FOREIGN_KEY_CHECKS = 1;



-- ========================================
-- VERIFICATION
-- ========================================

SELECT COUNT(*) AS customers FROM customers;
SELECT COUNT(*) AS accounts FROM accounts;
SELECT COUNT(*) AS payments FROM payments;
SELECT COUNT(*) AS fraud_assessments FROM fraud_assessments;
SELECT COUNT(*) AS bulk_batches FROM bulk_payment_batches;