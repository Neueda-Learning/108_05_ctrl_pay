# Database Schema Documentation

## Overview

Ctrl-Pay uses a **MySQL 8.0** relational database with 7 core tables and comprehensive constraints, indexes, and relationships. The schema enforces business rules at the database level and provides an immutable audit trail for compliance.

---

## Table: `customers`

### Purpose
Customer profile master record. PAN is the unique business key to ensure one profile per customer.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `customer_id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique customer identifier |
| `name` | VARCHAR(255) | NOT NULL | Customer full name |
| `dob` | DATE | NOT NULL | Date of birth |
| `phone_number` | VARCHAR(20) | NOT NULL | Customer phone number |
| `pan_number` | VARCHAR(10) | NOT NULL, UNIQUE | Permanent Account Number; unique customer key |
| `profile_created` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Profile creation timestamp |
| `last_updated` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last profile update timestamp |
| `country` | VARCHAR(100) | NOT NULL | Customer country |
| `customer_account_status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | Customer status (ACTIVE, PASSIVE) |

### Constraints

```sql
CONSTRAINT chk_customer_account_status CHECK (customer_account_status IN ('ACTIVE', 'PASSIVE'))
CONSTRAINT chk_pan_number_format CHECK (pan_number REGEXP '^[A-Z]{5}[0-9]{4}[A-Z]{1}$')
CONSTRAINT chk_customer_phone_format CHECK (phone_number REGEXP '^[0-9+() -]{7,20}$')
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `customer_id` | Fast lookup by customer ID |
| `UNIQUE` | `pan_number` | Enforce one profile per PAN |
| `idx_customer_name` | `name` | Search customers by name |
| `idx_customer_phone` | `phone_number` | Lookup by phone number |
| `idx_customer_status` | `customer_account_status` | Filter active/passive profiles |

### Sample Data

```sql
INSERT INTO customers (name, dob, phone_number, pan_number, country, customer_account_status)
VALUES ('Aarav Sharma', '1990-04-21', '9876543210', 'ABCDE1234F', 'India', 'ACTIVE');
```

---

## Table: `accounts`

### Purpose
Bank account records linked to a customer profile. A single customer can register multiple accounts.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `account_id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique account identifier |
| `customer_id` | BIGINT | NOT NULL, FK | Foreign key to `customers.customer_id` |
| `account_name` | VARCHAR(255) | NOT NULL | Account display name |
| `account_balance` | DECIMAL(19, 2) | NOT NULL, DEFAULT 0.00 | Current balance |
| `account_status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | Account status (ACTIVE, PASSIVE, DORMANT, SUSPICIOUS) |
| `currency` | CHAR(3) | NOT NULL | ISO 4217 currency code |
| `account_opening_date` | DATE | NOT NULL | Account opening date |
| `last_updated` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last account update timestamp |
| `ifsc_code` | VARCHAR(11) | NOT NULL | Bank IFSC code |
| `account_location` | VARCHAR(255) | NOT NULL | Account or branch location |
| `bank_name` | VARCHAR(255) | NOT NULL | Bank name |
| `account_pin_hash` | VARCHAR(255) | NOT NULL | Account PIN used for payment authentication (plain string storage) |

### Foreign Keys

```sql
CONSTRAINT fk_account_customer_id FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
```

### Constraints

```sql
CONSTRAINT chk_account_status CHECK (account_status IN ('ACTIVE', 'PASSIVE', 'DORMANT', 'SUSPICIOUS'))
CONSTRAINT chk_account_balance CHECK (account_balance >= 0)
CONSTRAINT chk_account_currency CHECK (currency REGEXP '^[A-Z]{3}$')
CONSTRAINT chk_ifsc_code_format CHECK (ifsc_code REGEXP '^[A-Z]{4}0[A-Z0-9]{6}$')
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `account_id` | Fast lookup by account ID |
| `idx_account_customer_id` | `customer_id` | List all accounts for a customer |
| `idx_account_status` | `account_status` | Filter by account status |
| `idx_account_currency` | `currency` | Filter by currency |
| `idx_account_bank_name` | `bank_name` | Search by bank name |

### Sample Data

```sql
INSERT INTO accounts (customer_id, account_name, account_balance, account_status, currency, account_opening_date, ifsc_code, account_location, bank_name, account_pin_hash)
VALUES (1, 'Primary Savings', 10000.00, 'ACTIVE', 'INR', '2026-07-31', 'HDFC0001234', 'Mumbai', 'HDFC Bank', '1234');
```

---

## Table: `payments`

### Purpose
Core payment records with lifecycle status tracking.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique payment identifier |
| `idempotency_key` | VARCHAR(255) | UNIQUE, NULL | Client-provided key for duplicate prevention |
| `source_account` | VARCHAR(50) | NOT NULL | Source account number (format: 12 digits) |
| `destination_account` | VARCHAR(50) | NOT NULL | Destination account number (format: 12 digits) |
| `amount` | DECIMAL(19, 2) | NOT NULL | Payment amount (supports up to ~9 quintillion with 2 decimals) |
| `currency` | CHAR(3) | NOT NULL | ISO 4217 currency code (e.g., USD, EUR, GBP) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'CREATED' | Payment lifecycle status (CREATED, VALIDATED, SENT, COMPLETED, FAILED) |
| `error_code` | VARCHAR(50) | NULL | Error code if payment failed (see ErrorCode enum) |
| `error_message` | TEXT | NULL | Human-readable error message |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Payment creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update timestamp |

### Constraints

```sql
-- Business Rule: Source and destination accounts must be different
CONSTRAINT chk_different_accounts CHECK (source_account != destination_account)

-- Business Rule: Amount must be in valid range
CONSTRAINT chk_valid_amount CHECK (amount > 0 AND amount <= 1000000.00)

-- Business Rule: Currency must be 3 uppercase letters (ISO 4217)
CONSTRAINT chk_valid_currency CHECK (currency REGEXP '^[A-Z]{3}$')

-- Foreign Key: Cascade delete if payment rules later depend on it
-- (Currently no fk_payments, but prepared for Phase 2+)
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `id` | Fast lookup by payment ID |
| `UNIQUE` | `idempotency_key` | Enforce uniqueness; NULL values allowed (multiple payments without key) |
| `idx_status` | `status` | Filter payments by status (e.g., find all VALIDATED) |
| `idx_created_at` | `created_at` | Order by creation date; sort latest first |
| `idx_idempotency_key` | `idempotency_key` | Fast duplicate detection |
| `idx_source_account` | `source_account` | Filter by source account |
| `idx_destination_account` | `destination_account` | Filter by destination account |
| `idx_currency` | `currency` | Filter by currency |

### Sample Data

```sql
INSERT INTO payments (idempotency_key, source_account, destination_account, amount, currency, status, created_at, updated_at)
VALUES ('uuid-1', '123456789012', '210987654321', 1000.00, 'USD', 'CREATED', NOW(), NOW());

INSERT INTO payments (idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at)
VALUES ('uuid-2', '111111111111', '222222222222', -100.00, 'EUR', 'FAILED', 'INVALID_AMOUNT', 'Amount must be greater than zero', NOW(), NOW());
```

---

## Table: `payment_status_history`

### Purpose
Immutable append-only audit trail of all payment status transitions. Used for compliance, debugging, and analytics.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique history record ID |
| `payment_id` | BIGINT | NOT NULL, FK | Foreign key to `payments.id` |
| `old_status` | VARCHAR(20) | NULL | Previous status (NULL for initial CREATED entry) |
| `new_status` | VARCHAR(20) | NOT NULL | New status after transition |
| `error_code` | VARCHAR(50) | NULL | Error code if transition to FAILED |
| `error_message` | TEXT | NULL | Error message for failed transitions |
| `triggered_by` | VARCHAR(50) | NOT NULL, DEFAULT 'SYSTEM' | Who triggered: SYSTEM, USER, RETRY, etc. |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Transition timestamp |

### Foreign Keys

```sql
CONSTRAINT fk_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
-- Cascade delete: if payment deleted, remove its history
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `id` | Fast lookup by history ID |
| `idx_payment_id_created` | `(payment_id, created_at)` | Retrieve all transitions for payment, ordered by time |

### Key Properties

- **Immutable:** Only INSERT allowed; UPDATE/DELETE never used
- **Append-Only:** New transitions are always added, never modified
- **Auditable:** Complete temporal record of payment lifecycle
- **Compliance-Ready:** Satisfies audit trail requirements

### Sample Data

```sql
-- Initial CREATED entry (old_status is NULL)
INSERT INTO payment_status_history (payment_id, old_status, new_status, triggered_by, created_at)
VALUES (1, NULL, 'CREATED', 'SYSTEM', '2026-07-31 10:00:00');

-- Transition to VALIDATED
INSERT INTO payment_status_history (payment_id, old_status, new_status, triggered_by, created_at)
VALUES (1, 'CREATED', 'VALIDATED', 'SYSTEM', '2026-07-31 10:00:05');

-- Failed transition with error details
INSERT INTO payment_status_history (payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at)
VALUES (2, 'VALIDATED', 'FAILED', 'NETWORK_ERROR', 'Connection timeout to payment gateway', 'RETRY', '2026-07-31 10:05:30');
```

---

## Table: `validation_rules`

### Purpose
Configurable validation rules stored in database. Enables zero-downtime rule deployment without code changes.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique rule identifier |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | Unique rule name (e.g., AMOUNT_RANGE, CURRENCY_WHITELIST) |
| `description` | TEXT | NULL | Human-readable rule description |
| `rule_type` | VARCHAR(50) | NOT NULL | Rule type (AMOUNT_RANGE, CURRENCY_WHITELIST, ACCOUNT_FORMAT, etc.) |
| `rule_definition` | JSON | NOT NULL | Rule configuration as JSON (structure varies by rule_type) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Whether rule is currently active |
| `severity` | VARCHAR(20) | NOT NULL, DEFAULT 'HARD' | HARD (blocks payment) or SOFT (warning only) |
| `order_of_execution` | INT | NOT NULL, DEFAULT 0 | Execution sequence (lower = earlier) for rule engine |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Rule creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last modification timestamp |

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `id` | Fast lookup by rule ID |
| `UNIQUE` | `name` | Prevent duplicate rule names |
| `idx_is_active` | `is_active` | Fetch only active rules (rule engine query) |
| `idx_rule_type` | `rule_type` | Find rules by type |
| `idx_order_of_execution` | `order_of_execution` | Sort by execution order |

### Rule Definition Examples

```json
// AMOUNT_RANGE: Validates payment amount is within acceptable range
{
  "type": "AMOUNT_RANGE",
  "min": 0.01,
  "max": 1000000.00,
  "message": "Amount must be between $0.01 and $1,000,000"
}

// CURRENCY_WHITELIST: Validates currency is supported
{
  "type": "CURRENCY_WHITELIST",
  "allowed_currencies": ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "MXN"],
  "message": "Currency is not supported"
}

// ACCOUNT_FORMAT: Validates account number format
{
  "type": "ACCOUNT_FORMAT",
  "pattern": "^[0-9]{12}$",
  "message": "Account number must be exactly 12 digits"
}

// ACCOUNT_DIFFERENCE: No parameters needed
{
  "type": "ACCOUNT_DIFFERENCE",
  "message": "Source and destination accounts must be different"
}

// MOCK_SUFFICIENT_FUNDS: Simulates funds check with configurable failure rate
{
  "type": "MOCK_SUFFICIENT_FUNDS",
  "failure_rate": 0.1,
  "message": "Insufficient funds in source account"
}
```

### Sample Data

```sql
-- Seed: Amount validation rule
INSERT INTO validation_rules (name, rule_type, rule_definition, is_active, severity, order_of_execution)
VALUES (
  'AMOUNT_RANGE',
  'AMOUNT_RANGE',
  JSON_OBJECT('type', 'AMOUNT_RANGE', 'min', 0.01, 'max', 1000000.00, 'message', 'Amount out of range'),
  TRUE,
  'HARD',
  1
);

-- Seed: Currency validation rule
INSERT INTO validation_rules (name, rule_type, rule_definition, is_active, severity, order_of_execution)
VALUES (
  'CURRENCY_WHITELIST',
  'CURRENCY_WHITELIST',
  JSON_OBJECT('type', 'CURRENCY_WHITELIST', 'allowed_currencies', JSON_ARRAY('USD', 'EUR', 'GBP')),
  TRUE,
  'HARD',
  2
);

-- Add new rule (zero-downtime deployment)
INSERT INTO validation_rules (name, rule_type, rule_definition, is_active, severity, order_of_execution)
VALUES (
  'NEW_GEOLOCATION_CHECK',
  'GEOLOCATION_CHECK',
  JSON_OBJECT('type', 'GEOLOCATION_CHECK', 'allowed_countries', JSON_ARRAY('US', 'CA', 'MX')),
  FALSE,  -- Start inactive
  'SOFT',
  6
);
-- Later: UPDATE validation_rules SET is_active=TRUE WHERE name='NEW_GEOLOCATION_CHECK';
```

### Zero-Downtime Deployment Process

1. **Insert new rule** with `is_active=false`
2. **Test rule** via dry-run endpoint (no DB writes)
3. **Activate rule** when ready: `UPDATE validation_rules SET is_active=TRUE WHERE id=?`
4. **Next payment request** loads updated active rules (cached in memory, invalidated on change)
5. **No application restart required!**

---

## Table: `validation_results`

### Purpose
Immutable audit log of validation rule execution. Provides compliance record of which rules were applied and how they evaluated.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique result record ID |
| `payment_id` | BIGINT | NOT NULL, FK | Foreign key to `payments.id` |
| `validation_rule_id` | BIGINT | NOT NULL, FK | Foreign key to `validation_rules.id` |
| `rule_name` | VARCHAR(255) | NOT NULL | Name of rule (denormalized for readability) |
| `rule_definition` | JSON | NOT NULL | Snapshot of rule definition at execution time (for audit) |
| `passed` | BOOLEAN | NOT NULL | Whether validation passed (true) or failed (false) |
| `error_code` | VARCHAR(50) | NULL | Error code if validation failed (e.g., INVALID_AMOUNT) |
| `error_message` | TEXT | NULL | Human-readable error message |
| `execution_time_ms` | INT | NOT NULL, DEFAULT 0 | Time taken to execute rule in milliseconds |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Validation execution timestamp |

### Foreign Keys

```sql
CONSTRAINT fk_validation_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
CONSTRAINT fk_validation_rule_id FOREIGN KEY (validation_rule_id) REFERENCES validation_rules(id)
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `id` | Fast lookup by result ID |
| `idx_payment_id_created` | `(payment_id, created_at)` | Retrieve all validations for a payment |
| `idx_validation_rule_id` | `validation_rule_id` | Find which payments failed a specific rule |

### Key Properties

- **Immutable:** Only INSERT allowed
- **Append-Only:** Every rule execution creates new entry
- **Compliance-Focused:** Snapshot rule definition at execution time
- **Performance-Tracked:** execution_time_ms enables bottleneck identification

### Sample Data

```sql
-- PASS result
INSERT INTO validation_results (payment_id, validation_rule_id, rule_name, rule_definition, passed, execution_time_ms)
VALUES (1, 1, 'AMOUNT_RANGE',
  JSON_OBJECT('type', 'AMOUNT_RANGE', 'min', 0.01, 'max', 1000000.00),
  TRUE, 2
);

-- FAIL result
INSERT INTO validation_results (payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms)
VALUES (2, 1, 'AMOUNT_RANGE',
  JSON_OBJECT('type', 'AMOUNT_RANGE', 'min', 0.01, 'max', 1000000.00),
  FALSE, 'INVALID_AMOUNT', 'Amount must be positive', 1
);

-- Compliance query: "Which payments failed CURRENCY_WHITELIST rule?"
SELECT p.id, p.amount, p.currency, vr.error_message, vr.created_at
FROM validation_results vr
JOIN payments p ON p.id = vr.payment_id
WHERE vr.validation_rule_id = 2 AND vr.passed = FALSE
ORDER BY vr.created_at DESC;
```

---

## Table: `payment_retry_attempts`

### Purpose
Track retry attempts for failed payments. Used for resilience and understanding failure patterns.

### Schema

| Column | Type | Attributes | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique retry attempt ID |
| `payment_id` | BIGINT | NOT NULL, FK | Foreign key to `payments.id` |
| `attempt_number` | INT | NOT NULL | Sequential attempt number (1 = first retry) |
| `status_before` | VARCHAR(20) | NOT NULL | Status before retry |
| `status_after` | VARCHAR(20) | NOT NULL | Status after retry (may be same if failed again) |
| `error_code` | VARCHAR(50) | NULL | Error code if retry failed |
| `error_message` | TEXT | NULL | Error message if retry failed |
| `execution_time_ms` | INT | NOT NULL, DEFAULT 0 | Time taken for retry operation |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Retry attempt timestamp |

### Foreign Keys

```sql
CONSTRAINT fk_retry_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
```

### Indexes

| Index | Columns | Purpose |
|-------|---------|---------|
| `PRIMARY` | `id` | Fast lookup by retry ID |
| `idx_payment_id_attempt` | `(payment_id, attempt_number)` | Track retry history for payment |

### Sample Data

```sql
-- First retry: status changed from SENT to COMPLETED
INSERT INTO payment_retry_attempts (payment_id, attempt_number, status_before, status_after, execution_time_ms)
VALUES (1, 1, 'SENT', 'COMPLETED', 150);

-- Second retry: status unchanged, still failed
INSERT INTO payment_retry_attempts (payment_id, attempt_number, status_before, status_after, error_code, error_message, execution_time_ms)
VALUES (2, 1, 'SENT', 'SENT', 'NETWORK_ERROR', 'Timeout after 2000ms', 2000);

-- Third retry: eventual success
INSERT INTO payment_retry_attempts (payment_id, attempt_number, status_before, status_after, execution_time_ms)
VALUES (2, 2, 'SENT', 'COMPLETED', 120);
```

---

## Relationships Diagram

```
customers
  └─ 1 → ∞ accounts (CASCADE DELETE)

payments
  ├─ 1 → ∞ payment_status_history (CASCADE DELETE)
  ├─ 1 → ∞ validation_results (CASCADE DELETE)
  └─ 1 → ∞ payment_retry_attempts (CASCADE DELETE)

validation_rules
  └─ 1 → ∞ validation_results
```

---

## Data Integrity Rules

### Level 1: Database Constraints

```sql
-- Customer profiles
UNIQUE (pan_number)                           -- Only one profile per PAN
CHECK (customer_account_status IN ('ACTIVE', 'PASSIVE'))

-- Accounts table
FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
CHECK (account_status IN ('ACTIVE', 'PASSIVE', 'DORMANT', 'SUSPICIOUS'))
CHECK (account_balance >= 0)

-- Payments table
CHECK (source_account != destination_account)  -- No self-transfers
CHECK (amount > 0 AND amount <= 1000000.00)    -- Amount validity
CHECK (currency REGEXP '^[A-Z]{3}$')           -- Currency format

-- Unique constraints
UNIQUE (idempotency_key)                       -- Prevent duplicates (NULL allowed)
UNIQUE (name)   on validation_rules            -- Unique rule names

-- Foreign keys with CASCADE DELETE
payments (id) ← payment_status_history (payment_id)
payments (id) ← validation_results (payment_id)
payments (id) ← payment_retry_attempts (payment_id)
```

### Level 2: Application Validation

- Status transitions enforced by StatusTransitionValidator
- Validation rules executed by RuleEngine
- Idempotency checked in PaymentService
- Customer uniqueness enforced by PAN number
- Account ownership enforced by `customers.customer_id` → `accounts.customer_id`

### Level 3: Transaction Atomicity

- All payment creation operations wrapped in `@Transactional`
- All-or-nothing semantics: either fully succeeds or fully rolls back
- Prevents partial updates

---

## Query Examples

### Get latest payment status (without joining to history)

```sql
SELECT status FROM payments WHERE id = 1;
-- Fast single row lookup via PRIMARY KEY
```

### Get payment status history (audit trail)

```sql
SELECT old_status, new_status, error_code, error_message, triggered_by, created_at
FROM payment_status_history
WHERE payment_id = 1
ORDER BY created_at ASC;
-- Uses index: idx_payment_id_created
```

### Get validation results for payment (which checks did it fail?)

```sql
SELECT rule_name, passed, error_code, error_message, execution_time_ms
FROM validation_results
WHERE payment_id = 1
ORDER BY created_at ASC;
-- Uses index: idx_payment_id_created
```

### Compliance query: Which payments failed CURRENCY_WHITELIST?

```sql
SELECT p.id, p.currency, vr.error_message, vr.created_at
FROM validation_results vr
JOIN validation_rules r ON r.id = vr.validation_rule_id
JOIN payments p ON p.id = vr.payment_id
WHERE r.name = 'CURRENCY_WHITELIST' AND vr.passed = FALSE
ORDER BY vr.created_at DESC;
-- Uses indexes: validation_results.idx_validation_rule_id, payments.PRIMARY KEY
```

### Idempotency check: Does this payment already exist?

```sql
SELECT id, status FROM payments WHERE idempotency_key = 'uuid-123';
-- Uses index: UNIQUE idempotency_key
```

### Analytics: How many payments reached COMPLETED?

```sql
SELECT COUNT(DISTINCT payment_id) as completed_count
FROM payment_status_history
WHERE new_status = 'COMPLETED';
-- Uses index: payment_status_history.idx_payment_id_created (partial scan)
```

---

## Performance Tuning Notes

### Indexes are optimized for:
- Fast single payment lookup (PRIMARY KEY)
- Duplicate prevention (UNIQUE idempotency_key)
- Status filtering (idx_status)
- Audit trail retrieval (idx_payment_id_created composite index)
- Compliance queries (idx_validation_rule_id)

### No N+1 problems:
- Denormalization of rule_name and rule_definition in validation_results
- Single JOIN queries instead of multiple roundtrips

### Connection pool sizing:
- Dev: 5 connections (low traffic)
- Prod: 20 connections (high traffic)

---

## Backup & Disaster Recovery

**Recommended Strategy:**
- Daily full backups of `ctrl_pay` database
- Point-in-time recovery via binary log
- Replicate to secondary MySQL instance for HA
- Test recovery procedures quarterly

**Sensitive Tables (priority backups):**
1. `customers` - Customer identity and KYC-style profile data
2. `accounts` - Customer bank account records
3. `payments` - Core business data
4. `payment_status_history` - Audit trail (compliance required)
5. `validation_results` - Audit trail (compliance required)

---

## Migration Strategy (Future)

When schema changes needed in Phase 2+:
1. Use Flyway or Liquibase for migrations
2. Keep schema versioning in migrations table
3. Support rollback for failed migrations
4. Blue-green deployment for zero-downtime migrations

Example migration:
```sql
-- V002__add_payment_reference.sql
ALTER TABLE payments ADD COLUMN reference_to_payment_id BIGINT NULL;
ALTER TABLE payments ADD CONSTRAINT fk_reference_payment
  FOREIGN KEY (reference_to_payment_id) REFERENCES payments(id);
```

---

**Last Updated:** August 4, 2026  
**Phase:** Phase 1 - Foundation Complete  
**Database:** MySQL 8.0  
**Schema Version:** 1.0

