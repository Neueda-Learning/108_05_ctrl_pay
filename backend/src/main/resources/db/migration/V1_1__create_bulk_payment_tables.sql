-- Ctrl-Pay: Bulk Payment Processing - Database Schema Extension
-- Version: 2.0
-- Created: August 5, 2026
-- Purpose: Support bulk payment batch processing with independent transaction boundaries

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

    -- Indexes for efficient querying
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
    CONSTRAINT chk_fraud_score CHECK (fraud_score IS NULL OR (fraud_score >= 0 AND fraud_score <= 100)),

    -- Indexes for efficient querying
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

    -- Indexes for audit retrieval
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

    -- Indexes for audit retrieval
    INDEX idx_audit_batch (batch_id) COMMENT 'Retrieve all events for batch',
    INDEX idx_audit_event_type (event_type) COMMENT 'Filter by event type',
    INDEX idx_audit_timestamp (event_timestamp) COMMENT 'Order by timestamp'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Compliance audit trail for bulk payment operations';

-- ========================================
-- Verification Queries (Run after schema creation)
-- ========================================
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'ctrl_pay' AND table_name LIKE 'bulk_payment%';
-- DESCRIBE bulk_payment_batches;
-- DESCRIBE bulk_payment_items;
-- SHOW CREATE TABLE bulk_payment_items;

