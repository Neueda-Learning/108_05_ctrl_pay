# Ctrl-Pay: Payments Processing System - Project Roadmap

**Project Start Date:** July 31, 2026  
**Status:** Planning Phase  
**Technology Stack:** Spring Boot (Java 17), MySQL 8.0, JdbcTemplate, React Frontend, Docker  
**Architecture:** Layered (Controller → Service → Repository → Database)  

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Phase Overview](#phase-overview)
3. [Phase 1: Core Schema & Domain Foundation](#phase-1-core-schema--domain-foundation)
4. [Phase 2: Rule Engine & Validation Framework](#phase-2-rule-engine--validation-framework)
5. [Phase 3: REST API & Payment Lifecycle](#phase-3-rest-api--payment-lifecycle)
6. [Phase 4: Docker & Infrastructure](#phase-4-docker--infrastructure)
7. [Phase 5: Integration Testing & Documentation](#phase-5-integration-testing--documentation)
8. [Phase 6: Advanced Features & Enhancements](#phase-6-advanced-features--enhancements)
9. [Git Branching Strategy](#git-branching-strategy)
10. [Progress Tracking](#progress-tracking)

---

## Executive Summary

**Goal:** Build a production-grade payment processing REST API with:
- ✅ Complete payment lifecycle management (CREATED → VALIDATED → SENT → COMPLETED/FAILED)
- ✅ Configurable validation rule engine (rules stored in DB, deployable without code changes)
- ✅ Comprehensive audit trail (every status change + validation result logged)
- ✅ Idempotency support (prevent duplicate submissions)
- ✅ MySQL-only persistence with JdbcTemplate
- ✅ Docker containerization for local + production deployment
- ✅ React frontend integration ready
- ✅ Complete API documentation (Swagger/OpenAPI)

**Timeline:** ~6-8 weeks for full implementation  
**Team Structure:** Backend focus first (Phases 1-5), then React frontend integration (Phase 6+)

---

## Phase Overview

| Phase | Title | Duration | Outcomes | Branch Prefix |
|-------|-------|----------|----------|----------------|
| 1 | Core Schema & Domain | 1 week | Locked DB schema, Java Records, Maven config | `phase1/` |
| 2 | Rule Engine & Validation | 1 week | Validation rules framework, rule execution, audit logging | `phase2/` |
| 3 | REST API & Lifecycle | 1.5 weeks | All endpoints, status transitions, error handling | `phase3/` |
| 4 | Docker & Infrastructure | 0.5 week | Dockerfile, docker-compose, environment profiles | `phase4/` |
| 5 | Integration Testing & Docs | 1 week | End-to-end tests, Swagger, README, deployment guide | `phase5/` |
| 6 | Advanced Features | 2+ weeks | Async processing, retry logic, analytics, React integration | `phase6/` |

---

# PHASE 1: Core Schema & Domain Foundation

**Duration:** 1 week  
**Goal:** Lock MySQL schema, configure Spring Boot + JdbcTemplate, define immutable Java Records  
**Outcomes:** Production-ready database and type-safe domain models  

## User Story 1.1: Design & Create MySQL Database Schema

**Description:** Define and implement the complete MySQL database schema with all core tables, constraints, indexes, and relationships.

**Acceptance Criteria:**
- [ ] Schema supports single payment record with all business fields
- [ ] Status history is append-only, immutable audit trail
- [ ] Validation results table records every rule execution
- [ ] Idempotency handled at database level (UNIQUE constraint)
- [ ] All constraints prevent invalid data states
- [ ] Indexes optimize query performance for common queries
- [ ] Foreign key relationships maintain referential integrity

**Tasks:**
- [ ] **Task 1.1.1:** Create `payments` table
  - Columns: id (BIGINT PK), idempotency_key (VARCHAR 255, UNIQUE NULL), source_account, destination_account, amount (DECIMAL 19,2), currency (CHAR 3), status (VARCHAR 20), error_code (VARCHAR 50), error_message (TEXT), created_at (TIMESTAMP), updated_at (TIMESTAMP)
  - Constraints: CHECK source_account ≠ destination_account, CHECK amount > 0 AND ≤ 1000000, CHECK currency matches [A-Z]{3}
  - Indexes: PK id, UNIQUE idempotency_key, INDEX status, INDEX created_at
  - File: `src/main/resources/schema.sql`

- [ ] **Task 1.1.2:** Create `payment_status_history` table
  - Columns: id (BIGINT PK AUTO_INCREMENT), payment_id (BIGINT FK), old_status (VARCHAR 20), new_status (VARCHAR 20), error_code (VARCHAR 50), error_message (TEXT), triggered_by (VARCHAR 50), created_at (TIMESTAMP)
  - Constraints: FK payment_id → payments.id ON DELETE CASCADE, NOT NULL new_status
  - Indexes: INDEX(payment_id, created_at) for efficient history retrieval
  - File: `src/main/resources/schema.sql`

- [ ] **Task 1.1.3:** Create `validation_rules` table
  - Columns: id (BIGINT PK AUTO_INCREMENT), name (VARCHAR 255 UNIQUE), description (TEXT), rule_type (VARCHAR 50), rule_definition (JSON), is_active (BOOLEAN), severity (VARCHAR 20), order_of_execution (INT), created_at (TIMESTAMP), updated_at (TIMESTAMP)
  - Indexes: INDEX is_active, INDEX rule_type, INDEX order_of_execution
  - File: `src/main/resources/schema.sql`

- [ ] **Task 1.1.4:** Create `validation_results` table
  - Columns: id (BIGINT PK AUTO_INCREMENT), payment_id (BIGINT FK), validation_rule_id (BIGINT FK), rule_name (VARCHAR 255), rule_definition (JSON), passed (BOOLEAN), error_code (VARCHAR 50), error_message (TEXT), execution_time_ms (INT), created_at (TIMESTAMP)
  - Constraints: FK payment_id → payments.id ON DELETE CASCADE, FK validation_rule_id → validation_rules.id
  - Indexes: INDEX(payment_id, created_at), INDEX validation_rule_id
  - File: `src/main/resources/schema.sql`

- [ ] **Task 1.1.5:** Create `payment_retry_attempts` table (optional, for Phase 3 retry logic)
  - Columns: id (BIGINT PK AUTO_INCREMENT), payment_id (BIGINT FK), attempt_number (INT), status_before (VARCHAR 20), status_after (VARCHAR 20), error_code (VARCHAR 50), created_at (TIMESTAMP)
  - File: `src/main/resources/schema.sql`

- [ ] **Task 1.1.6:** Validate schema with MySQL client
  - Connect to test MySQL instance
  - Run schema.sql
  - Verify all tables created with correct columns, types, constraints
  - Test CHECK constraints (e.g., amount ≤ 1000000)
  - Test UNIQUE constraint on idempotency_key

**Branch:** `phase1/feature-schema-design`

---

## User Story 1.2: Configure Spring Boot & Maven Dependencies

**Description:** Set up Maven dependencies for MySQL, JdbcTemplate, validation, testing, and build the project scaffold.

**Acceptance Criteria:**
- [ ] All required dependencies added to pom.xml
- [ ] MySQL connector 8.0.33 included
- [ ] JdbcTemplate available for data access
- [ ] Spring Data JDBC configured
- [ ] Test dependencies (JUnit 5, TestContainers, Mockito) included
- [ ] Jackson JSON library available for rule_definition JSON processing
- [ ] Maven build succeeds without errors

**Tasks:**
- [ ] **Task 1.2.1:** Add MySQL connector and JdbcTemplate dependencies to `pom.xml`
  - Dependency: `com.mysql:mysql-connector-java:8.0.33`
  - Verify Spring framework already includes spring-boot-starter-data-jdbc

- [ ] **Task 1.2.2:** Add validation & JSON processing dependencies
  - `org.springframework.boot:spring-boot-starter-validation`
  - `com.fasterxml.jackson.core:jackson-databind`

- [ ] **Task 1.2.3:** Add testing dependencies (if not present)
  - `org.junit.jupiter:junit-jupiter` (via parent)
  - `org.testcontainers:testcontainers:1.19.3` (test scope)
  - `org.testcontainers:mysql:1.19.3` (test scope)
  - `org.mockito:mockito-core` (test scope)

- [ ] **Task 1.2.4:** Verify Maven build
  - Run `mvn clean package` in `backend/ctrl_pay/`
  - Confirm build SUCCESS
  - Check `target/ctrl_pay-0.0.1-SNAPSHOT.jar` created

**Branch:** `phase1/feature-maven-dependencies`

---

## User Story 1.3: Configure application.properties for MySQL

**Description:** Set up Spring Boot application properties to connect to MySQL database, enable schema initialization, and configure logging.

**Acceptance Criteria:**
- [ ] MySQL connection URL configured
- [ ] Username/password for dev environment set
- [ ] Schema initialization enabled (schema.sql runs on startup)
- [ ] SQL logging enabled for development debugging
- [ ] JdbcTemplate configured
- [ ] Logging level set appropriately
- [ ] Environment profiles (dev, docker, prod) ready for future use

**Tasks:**
- [ ] **Task 1.3.1:** Configure `src/main/resources/application.properties`
  - Database URL: `jdbc:mysql://localhost:3306/ctrl_pay`
  - Username/Password: `root` / `admin123` (dev only, will use env vars in docker)
  - Schema init: `spring.sql.init.mode=always`
  - Platform: `spring.sql.init.platform=mysql`
  - JdbcTemplate connection pool settings

- [ ] **Task 1.3.2:** Create `application-docker.properties` (MySQL service via docker-compose)
  - Database URL: `jdbc:mysql://mysql:3306/ctrl_pay` (service name)
  - Username/Password: from environment variables

- [ ] **Task 1.3.3:** Enable SQL logging for debugging
  - Logging level for org.springframework.jdbc: DEBUG
  - Logging level for org.springframework.jdbc.core: DEBUG

- [ ] **Task 1.3.4:** Test connection (will verify in Phase 4 with Docker)

**Branch:** `phase1/feature-application-config`

---

## User Story 1.4: Define Java Record Models (Domain & DTOs)

**Description:** Create immutable Java Records for all domain entities and request/response DTOs.

**Acceptance Criteria:**
- [ ] PaymentRecord defined with all fields
- [ ] PaymentStatusHistoryRecord defined for audit trail
- [ ] ValidationRuleRecord defined with JSON rule_definition
- [ ] ValidationResultRecord defined for per-rule audit
- [ ] All request DTOs defined (CreatePaymentRequest, etc.)
- [ ] All response DTOs defined (PaymentResponse, ValidationResultResponse, etc.)
- [ ] Enums created: PaymentStatus, RuleType, ErrorCode, Severity
- [ ] Records use @NotNull, @Positive, @Pattern annotations for validation
- [ ] All records compile without errors

**Tasks:**
- [ ] **Task 1.4.1:** Create `PaymentStatus` enum
  - Values: CREATED, VALIDATED, SENT, COMPLETED, FAILED
  - Doc: status lifecycle definition
  - File: `src/main/java/com/neueda/domain/PaymentStatus.java`

- [ ] **Task 1.4.2:** Create `ErrorCode` enum
  - Values: VALIDATION_FAILED, INSUFFICIENT_FUNDS, INVALID_ACCOUNT, INVALID_CURRENCY, INVALID_AMOUNT, DUPLICATE_PAYMENT, INVALID_STATUS_TRANSITION, PAYMENT_NOT_FOUND, PROCESSING_ERROR, NETWORK_ERROR
  - File: `src/main/java/com/neueda/domain/ErrorCode.java`

- [ ] **Task 1.4.3:** Create `RuleType` enum
  - Values: AMOUNT_RANGE, CURRENCY_WHITELIST, ACCOUNT_FORMAT, ACCOUNT_DIFFERENCE, MOCK_SUFFICIENT_FUNDS, (extensible for future)
  - File: `src/main/java/com/neueda/domain/RuleType.java`

- [ ] **Task 1.4.4:** Create `Severity` enum
  - Values: HARD (blocks payment), SOFT (warning only)
  - File: `src/main/java/com/neueda/domain/Severity.java`

- [ ] **Task 1.4.5:** Create `PaymentRecord` (domain model)
  - Fields: id, idempotencyKey, sourceAccount, destinationAccount, amount, currency, status, errorCode, errorMessage, createdAt, updatedAt
  - Annotations: @NotNull on required fields, @Positive on amount, @Pattern for currency
  - File: `src/main/java/com/neueda/domain/PaymentRecord.java`

- [ ] **Task 1.4.6:** Create `PaymentStatusHistoryRecord`
  - Fields: id, paymentId, oldStatus, newStatus, errorCode, errorMessage, triggeredBy, createdAt
  - File: `src/main/java/com/neueda/domain/PaymentStatusHistoryRecord.java`

- [ ] **Task 1.4.7:** Create `ValidationRuleRecord`
  - Fields: id, name, description, ruleType, ruleDefinition (JsonNode), isActive, severity, orderOfExecution, createdAt, updatedAt
  - File: `src/main/java/com/neueda/domain/ValidationRuleRecord.java`

- [ ] **Task 1.4.8:** Create `ValidationResultRecord`
  - Fields: id, paymentId, validationRuleId, ruleName, ruleDefinition (JsonNode), passed, errorCode, errorMessage, executionTimeMs, createdAt
  - File: `src/main/java/com/neueda/domain/ValidationResultRecord.java`

- [ ] **Task 1.4.9:** Create request/response DTOs
  - `CreatePaymentRequest`: sourceAccount, destinationAccount, amount, currency, idempotencyKey (optional)
  - `PaymentResponse`: id, idempotencyKey, sourceAccount, destinationAccount, amount, currency, status, errorCode, errorMessage, createdAt, updatedAt, validationResults (embedded)
  - `ValidationResultResponse`: ruleId, ruleName, passed, errorCode, errorMessage, executionTimeMs
  - `ErrorResponse`: errorCode, message, timestamp, path
  - Files: `src/main/java/com/neueda/dto/`

- [ ] **Task 1.4.10:** Compile and verify no errors
  - Run `mvn clean compile`

**Branch:** `phase1/feature-domain-models`

---

## User Story 1.5: Set Up Repository/DAO Layer (No Implementation Yet)

**Description:** Define repository interfaces and skeleton structure for data access using JdbcTemplate.

**Acceptance Criteria:**
- [ ] PaymentRepository interface defined
- [ ] ValidationRuleRepository interface defined
- [ ] ValidationResultRepository interface defined
- [ ] PaymentStatusHistoryRepository interface defined
- [ ] Interface signatures match expected operations
- [ ] Skeleton implementations created (no SQL yet)
- [ ] Compiles without errors

**Tasks:**
- [ ] **Task 1.5.1:** Create `PaymentRepository` interface
  - Methods: save(PaymentRecord), findById(id), findAll(), findByIdempotencyKey(key), update(PaymentRecord)
  - File: `src/main/java/com/neueda/repository/PaymentRepository.java`

- [ ] **Task 1.5.2:** Create `ValidationRuleRepository` interface
  - Methods: save(ValidationRuleRecord), findById(id), findAll(), findActiveRules(), findByName(name), update(ValidationRuleRecord)
  - File: `src/main/java/com/neueda/repository/ValidationRuleRepository.java`

- [ ] **Task 1.5.3:** Create `ValidationResultRepository` interface
  - Methods: save(ValidationResultRecord), findByPaymentId(paymentId), appendResult(record)
  - File: `src/main/java/com/neueda/repository/ValidationResultRepository.java`

- [ ] **Task 1.5.4:** Create `PaymentStatusHistoryRepository` interface
  - Methods: save(PaymentStatusHistoryRecord), findByPaymentId(paymentId), appendHistory(record)
  - File: `src/main/java/com/neueda/repository/PaymentStatusHistoryRepository.java`

- [ ] **Task 1.5.5:** Create skeleton JdbcTemplate implementations (empty for now)
  - File: `src/main/java/com/neueda/repository/impl/PaymentRepositoryImpl.java` (etc.)
  - Will be filled in Phase 2

**Branch:** `phase1/feature-repository-skeleton`

---

## User Story 1.6: Create Project Documentation (README.md)

**Description:** Write comprehensive README covering project overview, setup, architecture, and development guide.

**Acceptance Criteria:**
- [ ] README explains project purpose and business context
- [ ] Setup instructions for running locally (MySQL + Spring Boot)
- [ ] Architecture overview with diagrams or descriptions
- [ ] Directory structure explained
- [ ] Database schema overview linked
- [ ] How to run tests documented
- [ ] How to build JAR documented
- [ ] Tech stack listed with versions
- [ ] Future phases mentioned

**Tasks:**
- [ ] **Task 1.6.1:** Create `backend/ctrl_pay/README.md`
  - Sections: Project Overview, Tech Stack, Prerequisites, Local Setup, Project Structure, Architecture, Database Schema, Running the App, Running Tests, Building for Production
  - File: `backend/ctrl_pay/README.md`

- [ ] **Task 1.6.2:** Create `ARCHITECTURE.md` (detailed architecture doc)
  - Layered architecture explanation
  - Data flow diagrams (text)
  - Rule engine overview
  - Lifecycle state machine diagram
  - File: `backend/ctrl_pay/ARCHITECTURE.md`

- [ ] **Task 1.6.3:** Create `SCHEMA.md` (database schema documentation)
  - Table definitions with column descriptions
  - Constraints and indexes explained
  - Rationale for design decisions
  - File: `backend/ctrl_pay/docs/SCHEMA.md`

**Branch:** `phase1/feature-documentation`

---

## Phase 1 Summary

**Outcome:** Project foundation locked in place.
- ✅ MySQL schema fully designed, no rework needed
- ✅ Maven/Spring Boot configured for MySQL + JdbcTemplate
- ✅ Java Records provide type-safe domain models
- ✅ Repository interfaces ready for implementation
- ✅ Documentation in place for team reference

**Merge to main:** After all Phase 1 features reviewed and tested

---

---

# PHASE 2: Rule Engine & Validation Framework

**Duration:** 1 week  
**Goal:** Implement validation rule engine, configurable rules, rule execution, and audit logging  
**Outcomes:** Pluggable validation system, rules stored in DB, zero-downtime rule updates  

## User Story 2.1: Implement JdbcTemplate Repositories for Core Data Access

**Description:** Implement all repository methods with raw SQL queries for payments, validation rules, validation results, and status history.

**Acceptance Criteria:**
- [ ] All repository methods implemented using JdbcTemplate
- [ ] INSERT operations return generated IDs
- [ ] UPDATE operations reflect in database
- [ ] SELECT operations return correctly mapped Records
- [ ] Exception handling for DB errors
- [ ] SQL queries are readable and maintainable
- [ ] Queries use parameterized statements (prevent SQL injection)
- [ ] Unit tests pass for each repository

**Tasks:**
- [ ] **Task 2.1.1:** Implement `PaymentRepositoryImpl`
  - save(PaymentRecord) → INSERT into payments
  - findById(id) → SELECT single payment
  - findAll() → SELECT all payments
  - findByIdempotencyKey(key) → UNIQUE index query
  - update(PaymentRecord) → UPDATE payment
  - File: `src/main/java/com/neueda/repository/impl/PaymentRepositoryImpl.java`

- [ ] **Task 2.1.2:** Implement `ValidationRuleRepositoryImpl`
  - save(ValidationRuleRecord) → INSERT
  - findById(id) → SELECT
  - findAll() → SELECT all
  - findActiveRules() → SELECT WHERE is_active=true ORDER BY order_of_execution
  - update(ValidationRuleRecord) → UPDATE
  - File: `src/main/java/com/neueda/repository/impl/ValidationRuleRepositoryImpl.java`

- [ ] **Task 2.1.3:** Implement `ValidationResultRepositoryImpl`
  - save(ValidationResultRecord) → INSERT
  - findByPaymentId(paymentId) → SELECT ORDER BY created_at
  - File: `src/main/java/com/neueda/repository/impl/ValidationResultRepositoryImpl.java`

- [ ] **Task 2.1.4:** Implement `PaymentStatusHistoryRepositoryImpl`
  - save(PaymentStatusHistoryRecord) → INSERT
  - findByPaymentId(paymentId) → SELECT ORDER BY created_at
  - File: `src/main/java/com/neueda/repository/impl/PaymentStatusHistoryRepositoryImpl.java`

- [ ] **Task 2.1.5:** Test repositories with unit tests
  - Test each CRUD operation
  - Verify generated IDs returned
  - Test uniqueness constraints
  - File: `src/test/java/com/neueda/repository/PaymentRepositoryTest.java` (etc.)

**Branch:** `phase2/feature-jdbc-repositories`

---

## User Story 2.2: Create Validation Rule Engine Core

**Description:** Build the rule execution engine that loads rules, executes them in order, and logs results.

**Acceptance Criteria:**
- [ ] RuleEngine interface defined
- [ ] Rule execution orchestrator implemented
- [ ] Rules execute in order_of_execution sequence
- [ ] Each rule execution captures: passed/failed, execution_time_ms, error_code, error_message
- [ ] All results logged to validation_results table
- [ ] Exceptions in rules caught gracefully (don't crash system)
- [ ] Rule caching for performance (reload only when rules change)
- [ ] Support for HARD (blocking) vs SOFT (warning) severity

**Tasks:**
- [ ] **Task 2.2.1:** Create `ValidationRule` interface
  - Method: validate(PaymentRecord) → ValidationResultRecord
  - Each implementation handles one rule type
  - File: `src/main/java/com/neueda/validation/ValidationRule.java`

- [ ] **Task 2.2.2:** Create `RuleEngine` orchestrator
  - Method: validatePayment(PaymentRecord, List<ValidationRuleRecord>) → List<ValidationResultRecord>
  - Loads rules
  - Executes in order_of_execution
  - Measures execution_time_ms
  - Catches exceptions
  - Separates HARD vs SOFT rules
  - File: `src/main/java/com/neueda/validation/RuleEngine.java`

- [ ] **Task 2.2.3:** Create `ValidationRuleRegistry`
  - Maps RuleType enum to ValidationRule implementations
  - Singleton or Spring singleton for performance
  - File: `src/main/java/com/neueda/validation/ValidationRuleRegistry.java`

- [ ] **Task 2.2.4:** Create exception classes
  - `ValidationException`
  - `RuleExecutionException`
  - `InvalidStatusTransitionException`
  - File: `src/main/java/com/neueda/exception/`

- [ ] **Task 2.2.5:** Test rule engine
  - Test with multiple rules in sequence
  - Verify execution_time_ms captured
  - Verify HARD rule blocks payment
  - Verify SOFT rule doesn't block
  - File: `src/test/java/com/neueda/validation/RuleEngineTest.java`

**Branch:** `phase2/feature-rule-engine-core`

---

## User Story 2.3: Implement Concrete Validation Rules

**Description:** Implement 5 core validation rules that cover MVP requirements.

**Acceptance Criteria:**
- [ ] AmountRangeRule validates 0.01 ≤ amount ≤ 1000000
- [ ] CurrencyWhitelistRule validates currency in allowed list
- [ ] AccountFormatRule validates 12-digit account format
- [ ] AccountDifferenceRule ensures source ≠ destination
- [ ] MockSufficientFundsRule simulates funds check with configurable failure rate
- [ ] Each rule returns correct error_code (INVALID_AMOUNT, INVALID_CURRENCY, etc.)
- [ ] Each rule includes human-readable error_message
- [ ] Rules are configurable via rule_definition JSON

**Tasks:**
- [ ] **Task 2.3.1:** Create `AmountRangeRule` implementation
  - Validates amount > 0 AND amount ≤ 1000000
  - Error code: INVALID_AMOUNT
  - File: `src/main/java/com/neueda/validation/rules/AmountRangeRule.java`

- [ ] **Task 2.3.2:** Create `CurrencyWhitelistRule` implementation
  - Whitelist: USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, INR, MXN
  - Error code: INVALID_CURRENCY
  - File: `src/main/java/com/neueda/validation/rules/CurrencyWhitelistRule.java`

- [ ] **Task 2.3.3:** Create `AccountFormatRule` implementation
  - Pattern: ^[0-9]{12}$
  - Validates both source and destination
  - Error code: INVALID_ACCOUNT
  - File: `src/main/java/com/neueda/validation/rules/AccountFormatRule.java`

- [ ] **Task 2.3.4:** Create `AccountDifferenceRule` implementation
  - Validates source_account ≠ destination_account
  - Error code: INVALID_ACCOUNT
  - File: `src/main/java/com/neueda/validation/rules/AccountDifferenceRule.java`

- [ ] **Task 2.3.5:** Create `MockSufficientFundsRule` implementation
  - Simulates funds check
  - Configuration in rule_definition: failure_rate (0.0-1.0)
  - Error code: INSUFFICIENT_FUNDS
  - File: `src/main/java/com/neueda/validation/rules/MockSufficientFundsRule.java`

- [ ] **Task 2.3.6:** Create seed data for validation_rules table
  - Prepopulate 5 rules with is_active=true
  - File: `src/main/resources/data.sql` (or initialization in code)

- [ ] **Task 2.3.7:** Test all concrete rules
  - Test passing conditions
  - Test failing conditions
  - Verify error codes and messages
  - File: `src/test/java/com/neueda/validation/rules/`

**Branch:** `phase2/feature-validation-rules`

---

## User Story 2.4: Implement PaymentService with Validation Integration

**Description:** Create core business logic service that validates payments and manages status transitions.

**Acceptance Criteria:**
- [ ] createPayment() applies validation rules, creates payment record
- [ ] Idempotency check: if idempotency_key exists, return existing payment (no duplicate)
- [ ] Validation results logged for audit
- [ ] Payment can enter FAILED state if validation fails (HARD rules)
- [ ] Payment enters CREATED if validations pass
- [ ] getPayment() retrieves payment + validation history
- [ ] listPayments() supports filtering by status, pagination
- [ ] validateAndTransition() enforces status transition rules
- [ ] All database operations transactional (atomicity)

**Tasks:**
- [ ] **Task 2.4.1:** Create `PaymentService` class
  - Inject PaymentRepository, ValidationRuleRepository, ValidationResultRepository, PaymentStatusHistoryRepository
  - File: `src/main/java/com/neueda/service/PaymentService.java`

- [ ] **Task 2.4.2:** Implement createPayment() method
  - Check idempotency_key: if exists, return existing payment (200 OK)
  - If no key, allow creation
  - Load active validation rules (cached)
  - Execute RuleEngine.validatePayment()
  - If any HARD rule fails → create payment with status=FAILED, error_code, error_message
  - If all pass → create payment with status=CREATED
  - Log all validation_results
  - Log initial payment_status_history (CREATED)
  - Return PaymentResponse with embedded validations

- [ ] **Task 2.4.3:** Implement getPayment() method
  - Retrieve payment by ID
  - Retrieve validation_results
  - Retrieve payment_status_history
  - Return enriched PaymentResponse
  - Throw PAYMENT_NOT_FOUND if not exists (404)

- [ ] **Task 2.4.4:** Implement listPayments() method
  - Filter by status (optional)
  - Support limit/offset pagination
  - Order by created_at DESC
  - Return List<PaymentResponse>

- [ ] **Task 2.4.5:** Implement validateAndTransition() method
  - Check current payment status
  - Validate new status is allowed (state machine rules)
  - If invalid → throw InvalidStatusTransitionException
  - Update payment.status
  - Log to payment_status_history
  - Return updated PaymentResponse

- [ ] **Task 2.4.6:** Create `StatusTransitionValidator` utility
  - Define valid transitions: CREATED→{VALIDATED,FAILED}, VALIDATED→{SENT,FAILED}, SENT→{COMPLETED,FAILED}
  - Terminal states: COMPLETED, FAILED (no exits)
  - File: `src/main/java/com/neueda/validation/StatusTransitionValidator.java`

- [ ] **Task 2.4.7:** Add @Transactional to service methods
  - Ensure all-or-nothing semantics
  - Verify Spring @Transactional configured

- [ ] **Task 2.4.8:** Test PaymentService
  - Test happy path: create → validate → send → complete
  - Test idempotency: same key returns existing payment
  - Test validation failure → FAILED status
  - Test invalid transitions
  - File: `src/test/java/com/neueda/service/PaymentServiceTest.java`

**Branch:** `phase2/feature-payment-service`

---

## User Story 2.5: Add Status History & Audit Trail Retrieval

**Description:** Implement audit trail retrieval showing complete history of payment status changes and validation results.

**Acceptance Criteria:**
- [ ] getPaymentStatusHistory() returns all status transitions for a payment
- [ ] Each entry shows old_status, new_status, triggered_by, timestamp
- [ ] getPaymentValidationResults() returns all validation checks performed
- [ ] Each validation result shows rule_name, rule_definition, passed, error details
- [ ] Audit trail is immutable (no updates/deletes)
- [ ] Results ordered by created_at chronologically
- [ ] Methods available via PaymentService

**Tasks:**
- [ ] **Task 2.5.1:** Add getPaymentStatusHistory() to PaymentService
  - Query payment_status_history by payment_id
  - Order by created_at ASC
  - Return List<PaymentStatusHistoryRecord>

- [ ] **Task 2.5.2:** Add getPaymentValidationResults() to PaymentService
  - Query validation_results by payment_id
  - Order by created_at ASC
  - Return List<ValidationResultRecord>

- [ ] **Task 2.5.3:** Create response DTOs for audit trails
  - `PaymentAuditResponse`: statuses, validations
  - `StatusHistoryItemResponse`: oldStatus, newStatus, triggeredBy, timestamp
  - File: `src/main/java/com/neueda/dto/`

- [ ] **Task 2.5.4:** Test audit trail retrieval
  - Create payment, validate, send, complete
  - Retrieve audit trail
  - Verify all transitions logged
  - Verify validation results captured
  - File: `src/test/java/com/neueda/service/PaymentAuditTest.java`

**Branch:** `phase2/feature-audit-trail`

---

## Phase 2 Summary

**Outcome:** Validation engine fully functional.
- ✅ Rule engine executes validations in order
- ✅ Every validation logged for audit
- ✅ Payments created with correct status based on validations
- ✅ Idempotency prevents duplicates
- ✅ Status transitions enforced by business rules
- ✅ Complete audit trail available

**Merge to main:** After all Phase 2 features reviewed and tested

---

---

# PHASE 3: REST API & Payment Lifecycle

**Duration:** 1.5 weeks  
**Goal:** Expose REST endpoints for all payment operations, implement complete lifecycle simulation, add error handling  
**Outcomes:** Full REST API, simulated payment processing, production error responses  

## User Story 3.1: Create PaymentController with Core CRUD Endpoints

**Description:** Implement REST controller with endpoints for creating, retrieving, and listing payments.

**Acceptance Criteria:**
- [ ] POST /api/payments creates payment, validates, returns 201
- [ ] GET /api/payments/{id} retrieves payment details, returns 200 or 404
- [ ] GET /api/payments lists all payments with pagination (query params: limit, offset)
- [ ] GET /api/payments?status=COMPLETED filters by status
- [ ] All endpoints return JSON with correct Content-Type
- [ ] Request validation (e.g., @Valid on DTOs)
- [ ] Error responses include error_code and message

**Tasks:**
- [ ] **Task 3.1.1:** Create `PaymentController` class
  - Inject PaymentService
  - @RestController annotation
  - @RequestMapping("/api/payments")
  - File: `src/main/java/com/neueda/controller/PaymentController.java`

- [ ] **Task 3.1.2:** Implement POST /api/payments
  - Accept CreatePaymentRequest in request body
  - Call PaymentService.createPayment()
  - Return 201 Created with Location header
  - Embed validation results in response
  - Handle ValidationException → 400 Bad Request

- [ ] **Task 3.1.3:** Implement GET /api/payments/{id}
  - Call PaymentService.getPayment(id)
  - Return 200 OK with PaymentResponse
  - Embed validation_results and status_history
  - Handle PAYMENT_NOT_FOUND → 404

- [ ] **Task 3.1.4:** Implement GET /api/payments
  - Accept query params: status (optional), limit (default 20), offset (default 0)
  - Call PaymentService.listPayments()
  - Return 200 OK with List<PaymentResponse>
  - Include pagination metadata (total, offset, limit)

- [ ] **Task 3.1.5:** Add request validation
  - @NotNull, @NotBlank, @Positive on DTOs
  - Validation errors return 400 with error details

- [ ] **Task 3.1.6:** Test controller endpoints
  - Test happy path for each endpoint
  - Test validation errors (negative amount, invalid currency)
  - Test 404 scenarios
  - File: `src/test/java/com/neueda/controller/PaymentControllerTest.java`

**Branch:** `phase3/feature-crud-endpoints`

---

## User Story 3.2: Create PaymentLifecycleController for Status Transitions

**Description:** Implement endpoints for advancing payment through its lifecycle (validate, send, complete/fail).

**Acceptance Criteria:**
- [ ] POST /api/payments/{id}/validate transitions CREATED → VALIDATED or FAILED
- [ ] POST /api/payments/{id}/send transitions VALIDATED → SENT or FAILED
- [ ] POST /api/payments/{id}/complete transitions SENT → COMPLETED or FAILED
- [ ] Transitions enforce state machine rules (reject invalid moves)
- [ ] Each transition is idempotent (can be retried safely)
- [ ] Detailed error message if transition fails
- [ ] Status history updated for each transition

**Tasks:**
- [ ] **Task 3.2.1:** Create `PaymentLifecycleController` class
  - Inject PaymentService
  - @RestController annotation
  - @RequestMapping("/api/payments/{id}/")
  - File: `src/main/java/com/neueda/controller/PaymentLifecycleController.java`

- [ ] **Task 3.2.2:** Implement POST /api/payments/{id}/validate
  - Accept StatusTransitionRequest (optional new error_code for manual failure)
  - Validate current status is CREATED
  - Perform additional validation checks (can add more rules here)
  - Transition to VALIDATED or FAILED
  - Return 200 OK with updated PaymentResponse

- [ ] **Task 3.2.3:** Implement POST /api/payments/{id}/send
  - Validate current status is VALIDATED
  - Simulate network call to payment gateway (mock: random 80% success)
  - Transition to SENT or FAILED
  - Return 200 OK

- [ ] **Task 3.2.4:** Implement POST /api/payments/{id}/complete
  - Validate current status is SENT
  - Simulate confirmation from gateway
  - Transition to COMPLETED or FAILED
  - Return 200 OK

- [ ] **Task 3.2.5:** Implement POST /api/payments/{id}/fail (manual failure)
  - Accept StatusTransitionRequest with error_code and message
  - Validate can fail from current status (any non-terminal state)
  - Transition to FAILED
  - Return 200 OK

- [ ] **Task 3.2.6:** Test lifecycle endpoints
  - Test happy path: created → validated → sent → completed
  - Test invalid transitions (e.g., completed → sent)
  - Test mock failures at each stage
  - File: `src/test/java/com/neueda/controller/PaymentLifecycleControllerTest.java`

**Branch:** `phase3/feature-lifecycle-endpoints`

---

## User Story 3.3: Implement Global Exception Handler & Error Responses

**Description:** Create standardized error response format and global exception handling for all API errors.

**Acceptance Criteria:**
- [ ] All errors return ErrorResponse with error_code, message, timestamp, path
- [ ] HTTP status codes follow REST conventions (400, 404, 409, 500, etc.)
- [ ] Validation errors (BindingException) return 400 with field details
- [ ] Domain exceptions (PAYMENT_NOT_FOUND) return 404
- [ ] Idempotency duplicate (same key) returns 200 (existing) or 409 (strict)
- [ ] Invalid transitions return 400
- [ ] Internal errors return 500 with generic message (not stack trace)
- [ ] Logging includes full context for debugging

**Tasks:**
- [ ] **Task 3.3.1:** Create `ErrorResponse` DTO
  - Fields: errorCode, message, timestamp, path
  - File: `src/main/java/com/neueda/dto/ErrorResponse.java`

- [ ] **Task 3.3.2:** Create `@ControllerAdvice` exception handler
  - Map domain exceptions to ErrorResponse + HTTP status
  - Handle validation exceptions (BindingException)
  - Handle generic Exception → 500
  - File: `src/main/java/com/neueda/exception/GlobalExceptionHandler.java`

- [ ] **Task 3.3.3:** Map error scenarios to HTTP status codes
  - VALIDATION_FAILED → 400
  - INVALID_ACCOUNT → 400
  - INVALID_CURRENCY → 400
  - INVALID_AMOUNT → 400
  - PAYMENT_NOT_FOUND → 404
  - DUPLICATE_PAYMENT → 200 or 409 (policy decision)
  - INVALID_STATUS_TRANSITION → 400
  - Processing errors → 500

- [ ] **Task 3.3.4:** Add logging to exception handler
  - Log full stack trace at ERROR level
  - Log request details (method, path, headers)

- [ ] **Task 3.3.5:** Test error handling
  - Test each error scenario
  - Verify correct HTTP status + error_code
  - Verify error messages are clear to client
  - File: `src/test/java/com/neueda/exception/GlobalExceptionHandlerTest.java`

**Branch:** `phase3/feature-error-handling`

---

## User Story 3.4: Implement Idempotency & Duplicate Detection

**Description:** Ensure same payment request (same idempotency_key) returns same response, preventing duplicates.

**Acceptance Criteria:**
- [ ] Client can provide Idempotency-Key header in request
- [ ] If same key submitted twice:
  - Same account/amount/currency → return 200 with existing payment (not 201)
  - Different details → return 422 Unprocessable Entity with explanation
- [ ] Idempotency key stored with payment for future lookups
- [ ] Idempotency keys optional (some requests may not include)
- [ ] Key validation: format, length constraints

**Tasks:**
- [ ] **Task 3.4.1:** Add idempotency key handling to CreatePaymentRequest DTO
  - Field: idempotencyKey (optional)
  - Validation: if present, must be non-empty string

- [ ] **Task 3.4.2:** Modify PaymentService.createPayment()
  - If idempotencyKey provided:
    - Query database for existing payment with same key
    - If found AND details match → return existing (not create new)
    - If found BUT details differ → throw ConflictException (422)
  - If no key → create payment normally

- [ ] **Task 3.4.3:** Implement idempotency key extraction from request header
  - In PaymentController, read Idempotency-Key header (if present)
  - Pass to createPayment()

- [ ] **Task 3.4.4:** Test idempotency
  - Submit same request twice with same key → verify 2nd returns 200
  - Verify only 1 payment created in DB
  - Submit similar request with different key → verify new payment created
  - Submit with different amount but same key → verify 422 Conflict
  - File: `src/test/java/com/neueda/service/IdempotencyTest.java`

**Branch:** `phase3/feature-idempotency`

---

## User Story 3.5: Add Audit Trail Endpoints

**Description:** Expose endpoints to retrieve payment history and validation audit trail for compliance/debugging.

**Acceptance Criteria:**
- [ ] GET /api/payments/{id}/history returns status transitions (timestamps, old→new status, triggered_by)
- [ ] GET /api/payments/{id}/validations returns validation results (rule_name, passed/failed, errors)
- [ ] GET /api/payments/{id}/audit returns combined audit (both status + validations)
- [ ] Results ordered by created_at (chronological)
- [ ] Timestamps in ISO 8601 format

**Tasks:**
- [ ] **Task 3.5.1:** Add endpoints to PaymentController
  - GET /api/payments/{id}/history
  - GET /api/payments/{id}/validations
  - GET /api/payments/{id}/audit

- [ ] **Task 3.5.2:** Implement history endpoint
  - Call PaymentService.getPaymentStatusHistory(id)
  - Return List<StatusHistoryItemResponse>
  - Include old_status, new_status, triggered_by, created_at

- [ ] **Task 3.5.3:** Implement validations endpoint
  - Call PaymentService.getPaymentValidationResults(id)
  - Return List<ValidationResultResponse>
  - Include rule_name, rule_definition, passed, error_code, error_message

- [ ] **Task 3.5.4:** Implement audit endpoint
  - Combine both histories into single response
  - Include both status transitions and validation results
  - Merge by timestamp (interleaved chronologically)

- [ ] **Task 3.5.5:** Test audit trail endpoints
  - Create payment, validate, send, complete
  - Retrieve history at each step
  - Verify all transitions logged
  - Verify validation results captured
  - File: `src/test/java/com/neueda/controller/AuditControllerTest.java`

**Branch:** `phase3/feature-audit-endpoints`

---

## User Story 3.6: Create Admin Endpoints for Validation Rules

**Description:** Expose API for managing validation rules (CRUD, enable/disable, test).

**Acceptance Criteria:**
- [ ] POST /api/admin/validation-rules creates new rule
- [ ] GET /api/admin/validation-rules lists all rules
- [ ] GET /api/admin/validation-rules/{id} retrieves single rule
- [ ] PUT /api/admin/validation-rules/{id} updates rule definition
- [ ] PATCH /api/admin/validation-rules/{id}/toggle toggles is_active
- [ ] POST /api/admin/validation-rules/{id}/test-dry-run runs rule against sample payment (no DB write)
- [ ] Rules changes apply to future payments immediately (caching)
- [ ] Previous rule versions kept for auditing (optional)

**Tasks:**
- [ ] **Task 3.6.1:** Create `ValidationRuleAdminController`
  - Inject ValidationRuleRepository, RuleEngine
  - @RestController
  - @RequestMapping("/api/admin/validation-rules")
  - File: `src/main/java/com/neueda/controller/ValidationRuleAdminController.java`

- [ ] **Task 3.6.2:** Implement POST /api/admin/validation-rules
  - Accept CreateValidationRuleRequest (name, description, rule_type, rule_definition, severity)
  - Call ValidationRuleRepository.save()
  - Invalidate rule cache
  - Return 201 Created

- [ ] **Task 3.6.3:** Implement GET endpoints
  - GET /api/admin/validation-rules → list all (with pagination)
  - GET /api/admin/validation-rules/{id} → single rule detail

- [ ] **Task 3.6.4:** Implement PUT /api/admin/validation-rules/{id}
  - Update rule_definition
  - Update order_of_execution if provided
  - Call ValidationRuleRepository.update()
  - Invalidate cache
  - Return 200 OK

- [ ] **Task 3.6.5:** Implement PATCH /api/admin/validation-rules/{id}/toggle
  - Toggle is_active boolean
  - Invalidate cache
  - Return 200 OK

- [ ] **Task 3.6.6:** Implement POST /api/admin/validation-rules/{id}/test-dry-run
  - Accept sample PaymentRecord in request
  - Execute rule against sample (no DB write)
  - Return result (passed/failed, execution_time_ms, error details)
  - Useful for testing new rules before activating

- [ ] **Task 3.6.7:** Add rule caching
  - Cache active rules in memory
  - Invalidate when rule modified (is_active changed, definition changed)
  - File: `src/main/java/com/neueda/cache/ValidationRuleCache.java`

- [ ] **Task 3.6.8:** Test admin endpoints
  - Create rule, verify in DB
  - Update rule, verify change applies to future payments
  - Toggle rule off, verify new payments skip it
  - Dry-run test, verify no DB write
  - File: `src/test/java/com/neueda/controller/ValidationRuleAdminControllerTest.java`

**Branch:** `phase3/feature-rule-admin-api`

---

## User Story 3.7: Add Filtering & Search Endpoints

**Description:** Implement rich filtering on payment list for compliance queries.

**Acceptance Criteria:**
- [ ] GET /api/payments?status=COMPLETED filters by status
- [ ] GET /api/payments?status=FAILED filters failures
- [ ] GET /api/payments?failed-rule=INSUFFICIENT_FUNDS finds payments failed specific rule
- [ ] GET /api/payments?account=123456789012 filters by account (source or destination)
- [ ] GET /api/payments?currency=USD filters by currency
- [ ] GET /api/payments?date-from=2026-07-01&date-to=2026-07-31 date range
- [ ] Results paginated (limit, offset)
- [ ] Results ordered by created_at DESC

**Tasks:**
- [ ] **Task 3.7.1:** Enhance GET /api/payments endpoint
  - Accept query params: status, failed_rule, account, currency, date_from, date_to, limit, offset
  - Build dynamic SQL WHERE clause based on provided filters

- [ ] **Task 3.7.2:** Add filters to PaymentService.listPayments()
  - Add parameters for each filter
  - Build WHERE conditions
  - Compose SQL dynamically

- [ ] **Task 3.7.3:** Add failed-rule filter
  - Query validation_results table joined with payments
  - Filter by validation_rule_id and passed=false

- [ ] **Task 3.7.4:** Test filtering
  - Create multiple payments with different statuses
  - Query by status, verify correct subset returned
  - Query by failed_rule, verify correct payments
  - Query date range, verify correct dates
  - File: `src/test/java/com/neueda/controller/PaymentFilteringTest.java`

**Branch:** `phase3/feature-filtering`

---

## Phase 3 Summary

**Outcome:** Full REST API operational.
- ✅ CRUD endpoints for payments
- ✅ Status transition endpoints (validate, send, complete, fail)
- ✅ Idempotency working (same key, same result)
- ✅ Global error handling (standardized responses)
- ✅ Audit trail endpoints (history + validations)
- ✅ Admin API for rule management
- ✅ Rich filtering for compliance queries

**Merge to main:** After all Phase 3 features reviewed and tested

---

---

# PHASE 4: Docker & Infrastructure

**Duration:** 0.5 week  
**Goal:** Containerize application, create docker-compose for local + production setup  
**Outcomes:** One-command startup (docker-compose up), environment-aware configuration  

## User Story 4.1: Create Dockerfile for Spring Boot Application

**Description:** Build multi-stage Dockerfile for production-ready container.

**Acceptance Criteria:**
- [ ] Dockerfile uses openjdk:17-alpine base image
- [ ] Multi-stage build: build JAR in one stage, runtime in another
- [ ] JAR compiled with optimizations
- [ ] Container size minimal (~300MB)
- [ ] Health check included
- [ ] Runs as non-root user (security)
- [ ] Environment variables configurable
- [ ] Docker image builds without errors

**Tasks:**
- [ ] **Task 4.1.1:** Create `Dockerfile` in `backend/ctrl_pay/`
  - Stage 1: Build with Maven
  - Stage 2: Runtime with openjdk:17-alpine
  - Copy JAR from build stage
  - Expose port 8080
  - Add healthcheck (GET /actuator/health)
  - Create non-root user (appuser)
  - File: `backend/ctrl_pay/Dockerfile`

- [ ] **Task 4.1.2:** Create `.dockerignore` file
  - Exclude: target/, .git/, .idea/, *.iml, node_modules/, etc.
  - File: `backend/ctrl_pay/.dockerignore`

- [ ] **Task 4.1.3:** Build Docker image locally
  - Run: `docker build -t ctrl-pay:latest backend/ctrl_pay/`
  - Verify image created
  - Check image size

- [ ] **Task 4.1.4:** Test Docker image standalone (if MySQL available)
  - Run: `docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=... ctrl-pay:latest`
  - Verify application starts
  - Test health check: curl http://localhost:8080/actuator/health

**Branch:** `phase4/feature-dockerfile`

---

## User Story 4.2: Create docker-compose.yml for Local Development

**Description:** Set up docker-compose with MySQL + Spring Boot for one-command local setup.

**Acceptance Criteria:**
- [ ] MySQL 8.0 service with persistent volume
- [ ] Spring Boot service depends_on MySQL
- [ ] Network for service-to-service communication
- [ ] Environment variables passed to Spring Boot
- [ ] Port 3306 for MySQL, 8080 for Spring Boot (mapped to host)
- [ ] Database created on startup
- [ ] Healthcheck waits for MySQL before starting app
- [ ] One command: docker-compose up -d starts everything

**Tasks:**
- [ ] **Task 4.2.1:** Create `docker-compose.yml` in project root
  - Services: mysql, app
  - MySQL config: root password, database name, port, volume
  - App config: environment variables for MySQL URL, username, password
  - Port mappings: 3306:3306, 8080:8080
  - Health checks for both services
  - File: `docker-compose.yml`

- [ ] **Task 4.2.2:** Create `.env` file for environment variables
  - MYSQL_ROOT_PASSWORD=...
  - MYSQL_DATABASE=ctrl_pay
  - SPRING_DATASOURCE_USERNAME=root
  - SPRING_DATASOURCE_PASSWORD=...
  - SPRING_PROFILES_ACTIVE=docker
  - File: `.env` (and add to .gitignore)

- [ ] **Task 4.2.3:** Create `.env.example` for template
  - Same keys, no sensitive values
  - File: `.env.example`

- [ ] **Task 4.2.4:** Test docker-compose locally
  - Run: `docker-compose up -d`
  - Verify MySQL started: `docker logs <mysql-container>`
  - Verify Spring Boot started: `docker logs <app-container>`
  - Test API: `curl http://localhost:8080/api/payments`
  - Run: `docker-compose down` to clean up

**Branch:** `phase4/feature-docker-compose`

---

## User Story 4.3: Create Environment-Specific Configuration Profiles

**Description:** Set up Spring profiles for dev (local MySQL), docker (docker-compose), and production.

**Acceptance Criteria:**
- [ ] application-dev.properties for local MySQL
- [ ] application-docker.properties for docker-compose (service name as host)
- [ ] application-prod.properties for production (placeholder)
- [ ] Default profile configurable via environment variable
- [ ] Database URL, username, password come from environment in docker/prod
- [ ] SQL logging enabled in dev, disabled in prod

**Tasks:**
- [ ] **Task 4.3.1:** Create `application-dev.properties`
  - Database URL: jdbc:mysql://localhost:3306/ctrl_pay
  - Username: root, Password: admin123
  - SQL logging enabled
  - File: `src/main/resources/application-dev.properties`

- [ ] **Task 4.3.2:** Create `application-docker.properties`
  - Database URL: jdbc:mysql://mysql:3306/ctrl_pay (service name)
  - Username/Password from environment variables
  - File: `src/main/resources/application-docker.properties`

- [ ] **Task 4.3.3:** Create `application-prod.properties`
  - Database URL from environment variable
  - Logging: WARN level only
  - Security: HTTPS, production settings
  - File: `src/main/resources/application-prod.properties`

- [ ] **Task 4.3.4:** Update `application.properties` (default)
  - spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
  - Default to dev if not specified

- [ ] **Task 4.3.5:** Test profile switching
  - Run app with -Dspring.profiles.active=dev
  - Run app with -Dspring.profiles.active=docker
  - Verify correct properties loaded

**Branch:** `phase4/feature-profiles`

---

## User Story 4.4: Add Health Check & Actuator Endpoints

**Description:** Implement Spring Boot Actuator for monitoring and health checks.

**Acceptance Criteria:**
- [ ] GET /actuator/health returns UP/DOWN status
- [ ] Includes database connectivity check
- [ ] Docker healthcheck uses this endpoint
- [ ] GET /actuator/info provides app version + build info
- [ ] GET /actuator/metrics available for monitoring

**Tasks:**
- [ ] **Task 4.4.1:** Add Spring Boot Actuator dependency
  - Already in starter-web, or add explicitly
  - File: `pom.xml`

- [ ] **Task 4.4.2:** Configure actuator endpoints in application.properties
  - management.endpoints.web.exposure.include=health,info,metrics
  - management.endpoint.health.show-details=when-authorized

- [ ] **Task 4.4.3:** Create custom health indicator for database
  - Implement HealthIndicator interface
  - Check database connectivity (simple query: SELECT 1)
  - File: `src/main/java/com/neueda/health/DatabaseHealthIndicator.java`

- [ ] **Task 4.4.4:** Add build info to application.properties
  - app.name=ctrl-pay
  - app.version=${project.version}
  - app.description=Payments Processing System

- [ ] **Task 4.4.5:** Test Actuator endpoints
  - curl http://localhost:8080/actuator/health
  - curl http://localhost:8080/actuator/info
  - Verify database health check works

**Branch:** `phase4/feature-actuator`

---

## Phase 4 Summary

**Outcome:** Application containerized and production-ready.
- ✅ Dockerfile builds optimized container
- ✅ docker-compose starts MySQL + app with one command
- ✅ Environment profiles for dev/docker/prod
- ✅ Health checks ensure service reliability
- ✅ Zero-downtime deployments possible

**Merge to main:** After all Phase 4 features reviewed and tested

---

---

# PHASE 5: Integration Testing & Documentation

**Duration:** 1 week  
**Goal:** Comprehensive integration tests, API documentation (Swagger), deployment guide  
**Outcomes:** Production-ready documentation, testable codebase  

## User Story 5.1: Create Comprehensive Integration Tests

**Description:** Write end-to-end tests covering complete payment lifecycle using TestContainers for real MySQL.

**Acceptance Criteria:**
- [ ] Tests use real MySQL via TestContainers (not in-memory)
- [ ] Happy path: create → validate → send → complete lifecycle
- [ ] Validation failures handled correctly (payment enters FAILED)
- [ ] Idempotency tested (same key returns 200, not 201)
- [ ] Invalid transitions rejected (400 error)
- [ ] Error scenarios tested (missing payment, invalid data)
- [ ] Concurrent operations tested (multiple payments simultaneously)
- [ ] Audit trail verified after each step
- [ ] All tests pass

**Tasks:**
- [ ] **Task 5.1.1:** Set up TestContainers for MySQL in test base class
  - Container lifecycle: start before tests, stop after
  - Dynamic port mapping
  - File: `src/test/java/com/neueda/integration/IntegrationTestBase.java`

- [ ] **Task 5.1.2:** Create integration test for full lifecycle
  - Test: create payment → validate → send → complete
  - Verify status at each step
  - Verify DB state
  - Verify validations logged
  - File: `src/test/java/com/neueda/integration/PaymentLifecycleIntegrationTest.java`

- [ ] **Task 5.1.3:** Create integration test for validation failures
  - Test: negative amount → FAILED status
  - Test: invalid currency → FAILED
  - Test: same source/destination → FAILED
  - Verify error codes and messages

- [ ] **Task 5.1.4:** Create integration test for idempotency
  - Submit same payment twice with same idempotency key
  - Verify 2nd returns 200 (not 201)
  - Verify only 1 payment in DB
  - Submit with different amount, same key → verify 422 Conflict

- [ ] **Task 5.1.5:** Create integration test for invalid transitions
  - Test COMPLETED → SENT (should fail)
  - Test CREATED → SENT (skip VALIDATED, should fail)
  - Verify 400 Bad Request with INVALID_STATUS_TRANSITION error code

- [ ] **Task 5.1.6:** Create integration test for audit trail
  - Create payment
  - Perform 3 transitions
  - Retrieve audit trail
  - Verify all steps logged with timestamps
  - Verify validation results captured

- [ ] **Task 5.1.7:** Create integration test for concurrent operations
  - Submit 10 payments simultaneously
  - Verify all created correctly
  - Verify no race conditions in DB

- [ ] **Task 5.1.8:** Run all integration tests locally
  - `mvn verify`
  - All tests must pass

**Branch:** `phase5/feature-integration-tests`

---

## User Story 5.2: Add Swagger/OpenAPI Documentation

**Description:** Generate interactive API documentation with Swagger UI.

**Acceptance Criteria:**
- [ ] Swagger UI accessible at /swagger-ui.html
- [ ] OpenAPI YAML/JSON spec available at /v3/api-docs
- [ ] All endpoints documented with descriptions
- [ ] Request/response schemas shown
- [ ] Error codes and HTTP status codes documented
- [ ] Example requests/responses provided
- [ ] Swagger UI allows testing endpoints (try-it-out)
- [ ] API version and info included

**Tasks:**
- [ ] **Task 5.2.1:** Add springdoc-openapi dependency to pom.xml
  - Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2`

- [ ] **Task 5.2.2:** Configure Swagger in Spring Boot
  - Add @Configuration class or application.properties settings
  - Set springdoc.swagger-ui.path=/swagger-ui.html
  - Set springdoc.api-docs.path=/v3/api-docs
  - File: `src/main/java/com/neueda/config/SwaggerConfig.java` or application.properties

- [ ] **Task 5.2.3:** Annotate PaymentController with OpenAPI/Swagger annotations
  - @Operation(summary="...", description="...")
  - @ApiResponse(responseCode="...", description="...")
  - @Parameter annotations for query/path params
  - @RequestBody documentation

- [ ] **Task 5.2.4:** Annotate all DTOs
  - @Schema annotations on fields
  - @Schema(example="...") for examples
  - Javadoc comments for clarity

- [ ] **Task 5.2.5:** Document error responses
  - Create ErrorResponse Swagger schema
  - Document all error codes
  - Include example error responses

- [ ] **Task 5.2.6:** Add API info to application.properties
  - springdoc.api-title=Ctrl-Pay API
  - springdoc.api-version=1.0.0
  - springdoc.api-description=Payment Processing System REST API

- [ ] **Task 5.2.7:** Test Swagger UI
  - Start application
  - Navigate to http://localhost:8080/swagger-ui.html
  - Verify all endpoints listed
  - Try out (POST) an endpoint
  - Verify request/response schemas shown

**Branch:** `phase5/feature-swagger`

---

## User Story 5.3: Create Comprehensive README & Developer Guide

**Description:** Write detailed documentation for developers and users.

**Acceptance Criteria:**
- [ ] README explains project purpose and features
- [ ] Prerequisites listed (Java 17, MySQL 8.0, Maven, Docker)
- [ ] Local setup instructions (manual + docker-compose)
- [ ] How to run tests documented
- [ ] How to build JAR documented
- [ ] How to deploy to production documented
- [ ] Architecture overview explained
- [ ] Database schema documented
- [ ] API endpoint summary with links to Swagger
- [ ] Troubleshooting section included
- [ ] Contributing guidelines

**Tasks:**
- [ ] **Task 5.3.1:** Create/update `backend/ctrl_pay/README.md`
  - Sections: Project Overview, Features, Tech Stack, Prerequisites, Local Setup (Manual), Local Setup (Docker), Running Tests, Building for Production, Architecture, Database Schema, API Documentation, Troubleshooting, Contributing
  - Include code examples for common tasks

- [ ] **Task 5.3.2:** Create `backend/ctrl_pay/docs/SETUP.md`
  - Detailed step-by-step local setup
  - MySQL installation for different OS
  - mvn clean package walk-through
  - Common errors and solutions

- [ ] **Task 5.3.3:** Create `backend/ctrl_pay/docs/SCHEMA.md`
  - Table-by-table schema documentation
  - Column descriptions
  - Constraints explained
  - Indexes and their purpose
  - Rationale for design decisions

- [ ] **Task 5.3.4:** Create `backend/ctrl_pay/docs/API.md`
  - API overview
  - Authentication (note: none in current version)
  - Request/response format
  - Error handling
  - Link to Swagger UI
  - cURL examples for each endpoint

- [ ] **Task 5.3.5:** Create `backend/ctrl_pay/docs/DEPLOYMENT.md`
  - How to deploy to production server
  - Docker image push to registry
  - Environment variables for production
  - Database migration strategy
  - Health checks for monitoring
  - Scaling considerations

- [ ] **Task 5.3.6:** Create `backend/ctrl_pay/docs/ARCHITECTURE.md`
  - High-level architecture diagram (text-based)
  - Layering explanation (Controller → Service → Repository → DB)
  - Request/response flow
  - Validation rule engine architecture
  - Status transition state machine
  - Data model relationships

- [ ] **Task 5.3.7:** Create `backend/ctrl_pay/CONTRIBUTING.md`
  - Git workflow (feature branches, PRs)
  - Coding standards
  - Testing requirements
  - Commit message format

**Branch:** `phase5/feature-documentation`

---

## User Story 5.4: Create Postman Collection for API Testing

**Description:** Export API endpoints as Postman collection for manual testing and sharing with team/frontend.

**Acceptance Criteria:**
- [ ] Postman collection includes all endpoints
- [ ] Environment variables for base URL, payment IDs
- [ ] Pre-request scripts for common setup (e.g., generate idempotency key)
- [ ] Tests/assertions in collection (verify response codes, fields)
- [ ] Example requests with realistic data
- [ ] Happy path workflow (create → validate → send → complete)
- [ ] Error scenario requests (invalid data, 404, etc.)
- [ ] Collection exportable for sharing

**Tasks:**
- [ ] **Task 5.4.1:** Export Swagger spec to Postman
  - Use Swagger UI: Download JSON
  - Import into Postman
  - Or manually create collection in Postman

- [ ] **Task 5.4.2:** Create Postman environment
  - baseUrl: http://localhost:8080
  - paymentId: placeholder
  - idempotencyKey: {{$guid}}
  - File: `docs/postman/Ctrl-Pay-Environment.json`

- [ ] **Task 5.4.3:** Create happy path workflow
  - Create payment (store id in variable)
  - Validate payment
  - Send payment
  - Complete payment
  - Retrieve payment (verify status)

- [ ] **Task 5.4.4:** Create error scenario requests
  - Create with negative amount (400)
  - Create with invalid currency (400)
  - Get non-existent payment (404)
  - Invalid transition (400)

- [ ] **Task 5.4.5:** Save collection JSON
  - File: `docs/postman/Ctrl-Pay-API-Collection.json`
  - Commit to git for team sharing

**Branch:** `phase5/feature-postman-collection`

---

## User Story 5.5: Write Database Migration Strategy (Future-Proofing)

**Description:** Plan for database schema changes as application evolves (using Liquibase or Flyway).

**Acceptance Criteria:**
- [ ] Migration tool decision (Liquibase or Flyway) documented
- [ ] Migration file structure defined
- [ ] Sample migration file created (not applied yet)
- [ ] Rollback strategy documented
- [ ] Version control for schema changes planned

**Tasks:**
- [ ] **Task 5.5.1:** Document migration strategy in `docs/MIGRATIONS.md`
  - Why migrations needed (schema evolution)
  - Tool selection rationale (Flyway chosen for simplicity)
  - How to create new migration
  - How to apply migrations
  - Rollback procedures

- [ ] **Task 5.5.2:** Create migration directory structure
  - `src/main/resources/db/migration/` directory
  - Naming convention: V001__initial_schema.sql

- [ ] **Task 5.5.3:** Create sample migration file (not applied)
  - Move schema.sql into first migration
  - File: `src/main/resources/db/migration/V001__initial_schema.sql`

- [ ] **Task 5.5.4:** Document future migrations
  - Example: V002__add_payment_reference_field.sql
  - Notes on schema backward compatibility

**Branch:** `phase5/feature-migrations-setup`

---

## Phase 5 Summary

**Outcome:** Application fully documented and tested.
- ✅ Integration tests with real MySQL
- ✅ Swagger documentation live (interactive)
- ✅ Comprehensive README and guides
- ✅ Postman collection for manual testing
- ✅ Production deployment guide ready

**Merge to main:** After all Phase 5 features reviewed and tested

---

---

# PHASE 6: Advanced Features & Enhancements

**Duration:** 2+ weeks  
**Goal:** Async processing, retry logic, analytics, React frontend integration  
**Outcomes:** Production-grade resilience and monitoring  

## User Story 6.1: Implement Async Status Progression (Scheduled Tasks)

**Description:** Automatically transition payments through VALIDATED → SENT → COMPLETED stages on a schedule (instead of manual API calls).

**Acceptance Criteria:**
- [ ] Scheduled task runs every N seconds
- [ ] Picks random VALIDATED payments, moves to SENT
- [ ] Picks random SENT payments, moves to COMPLETED or FAILED
- [ ] Simulates network latency (random delay)
- [ ] Failure rate configurable (e.g., 10% fail)
- [ ] Transitions logged to payment_status_history
- [ ] Can be disabled via configuration

**Tasks:**
- [ ] **Task 6.1.1:** Create `PaymentProcessorScheduler` class
  - @Scheduled(fixedRate=5000) methods
  - Method 1: move VALIDATED → SENT
  - Method 2: move SENT → COMPLETED/FAILED
  - File: `src/main/java/com/neueda/scheduler/PaymentProcessorScheduler.java`

- [ ] **Task 6.1.2:** Add configuration for scheduler
  - scheduler.enabled=true (toggle on/off)
  - scheduler.interval-ms=5000
  - scheduler.failure-rate=0.1
  - File: application.properties

- [ ] **Task 6.1.3:** Test scheduler
  - Create payment via API
  - Verify auto-transition to VALIDATED (or manual via API)
  - Wait for scheduler to run
  - Verify SENT transition in DB
  - Wait again for COMPLETED transition

**Branch:** `phase6/feature-async-processing`

---

## User Story 6.2: Implement Retry Logic for Failed Payments

**Description:** Automatically retry failed payments up to N times before marking permanently FAILED.

**Acceptance Criteria:**
- [ ] Retry count configurable (e.g., 3 times)
- [ ] Retry delay increasing (exponential backoff: 1s, 2s, 4s)
- [ ] Each retry logged to payment_retry_attempts table
- [ ] After N retries, mark FAILED with error code
- [ ] Manual retry endpoint available: POST /api/payments/{id}/retry

**Tasks:**
- [ ] **Task 6.2.1:** Create `PaymentRetryService`
  - Method: retryFailedPayment(paymentId)
  - Increment retry_count
  - Re-validate payment
  - Update status
  - File: `src/main/java/com/neueda/service/PaymentRetryService.java`

- [ ] **Task 6.2.2:** Add retry logic to scheduler
  - Pick failed payments (status=FAILED, retry_count < max)
  - Call PaymentRetryService.retryFailedPayment()
  - Exponential backoff calculation

- [ ] **Task 6.2.3:** Add retry configuration
  - payment.retry.max-attempts=3
  - payment.retry.initial-delay-ms=1000
  - File: application.properties

- [ ] **Task 6.2.4:** Add manual retry endpoint
  - POST /api/payments/{id}/retry
  - Allows manual operator intervention
  - File: `PaymentLifecycleController`

- [ ] **Task 6.2.5:** Test retry logic
  - Create payment that fails
  - Verify auto-retry runs
  - Verify retry_count increments
  - Verify exponential backoff

**Branch:** `phase6/feature-retry-logic`

---

## User Story 6.3: Add Analytics & Reporting Endpoints

**Description:** Expose endpoints for analytics: daily volumes, success/failure rates, average processing time.

**Acceptance Criteria:**
- [ ] GET /api/analytics/daily-volume returns payments per day (last 30 days)
- [ ] GET /api/analytics/success-rate returns % successful vs failed
- [ ] GET /api/analytics/processing-time returns average time per stage
- [ ] GET /api/analytics/error-summary returns top error codes
- [ ] Results cached (recompute every hour)
- [ ] Time ranges configurable

**Tasks:**
- [ ] **Task 6.3.1:** Create `AnalyticsRepository` with aggregate queries
  - Query: count payments per day
  - Query: count by status
  - Query: average time in each status
  - Query: count by error_code
  - File: `src/main/java/com/neueda/repository/AnalyticsRepository.java`

- [ ] **Task 6.3.2:** Create `AnalyticsService`
  - getDailyVolume(days=30)
  - getSuccessRate()
  - getProcessingTimeStats()
  - getErrorSummary()
  - File: `src/main/java/com/neueda/service/AnalyticsService.java`

- [ ] **Task 6.3.3:** Create `AnalyticsController`
  - GET /api/analytics/daily-volume
  - GET /api/analytics/success-rate
  - GET /api/analytics/processing-time
  - GET /api/analytics/error-summary
  - File: `src/main/java/com/neueda/controller/AnalyticsController.java`

- [ ] **Task 6.3.4:** Add caching
  - Cache results for 1 hour
  - Invalidate on new payment status change
  - File: `src/main/java/com/neueda/cache/AnalyticsCache.java`

- [ ] **Task 6.3.5:** Test analytics endpoints
  - Create diverse payment scenarios
  - Query analytics
  - Verify calculations correct

**Branch:** `phase6/feature-analytics`

---

## User Story 6.4: Add Payment Cancellation & Reversal

**Description:** Allow canceling payments before completion or reversing completed payments.

**Acceptance Criteria:**
- [ ] POST /api/payments/{id}/cancel transitions CREATED/VALIDATED/SENT → CANCELLED (new status)
- [ ] POST /api/payments/{id}/reverse creates new offsetting payment for COMPLETED payments
- [ ] Reversal creates mirror payment with opposite amount
- [ ] Original payment marked as reversed (reference to new payment)
- [ ] Reversals logged in payment_status_history

**Tasks:**
- [ ] **Task 6.4.1:** Add CANCELLED status to PaymentStatus enum
  - File: `src/main/java/com/neueda/domain/PaymentStatus.java`

- [ ] **Task 6.4.2:** Implement cancel endpoint
  - POST /api/payments/{id}/cancel
  - Only allowed from CREATED, VALIDATED, SENT
  - Transition to CANCELLED
  - File: `PaymentLifecycleController`

- [ ] **Task 6.4.3:** Implement reverse endpoint
  - POST /api/payments/{id}/reverse
  - Only allowed from COMPLETED
  - Create new payment with opposite amount
  - Link payments (reference field)
  - File: `PaymentLifecycleController`

- [ ] **Task 6.4.4:** Add reference_to_payment_id field to payments table
  - Schema migration: add field
  - Used to link reversed/cancellation payments

- [ ] **Task 6.4.5:** Test cancellation and reversal
  - Cancel payment, verify CANCELLED status
  - Reverse completed payment, verify new payment created
  - Verify amounts opposite

**Branch:** `phase6/feature-cancellation-reversal`

---

## User Story 6.5: React Frontend Integration Preparation

**Description:** Document and prepare backend for React frontend consumption.

**Acceptance Criteria:**
- [ ] CORS enabled for frontend URL
- [ ] Comprehensive Swagger/OpenAPI docs
- [ ] Frontend-ready error responses (structured error_code, message)
- [ ] Timestamps in ISO 8601 format
- [ ] Pagination metadata clear
- [ ] Rate limiting implemented (optional but good)
- [ ] Frontend team can test against API without modifications

**Tasks:**
- [ ] **Task 6.5.1:** Configure CORS in Spring Boot
  - Allow frontend origin (localhost:3000 in dev, production URL in prod)
  - File: `src/main/java/com/neueda/config/CorsConfig.java`

- [ ] **Task 6.5.2:** Create Frontend Integration Guide
  - Document all endpoints from React perspective
  - Show example fetch calls in JavaScript
  - Error handling patterns
  - Authentication placeholder (for future)
  - File: `docs/FRONTEND_INTEGRATION.md`

- [ ] **Task 6.5.3:** Ensure timestamp format
  - All timestamps in ISO 8601 (2026-07-31T10:30:00Z)
  - Use Jackson @JsonFormat on Records
  - File: Update all Record definitions

- [ ] **Task 6.5.4:** Test frontend can reach backend
  - Run frontend on localhost:3000
  - Run backend on localhost:8080
  - Test CORS by making fetch request
  - Verify Swagger URL accessible from frontend tools

- [ ] **Task 6.5.5:** Create sample .env for frontend
  - REACT_APP_API_URL=http://localhost:8080/api
  - File: `frontend/.env.example`

**Branch:** `phase6/feature-frontend-integration`

---

## Phase 6 Summary

**Outcome:** Production-grade features and monitoring.
- ✅ Async payment processing
- ✅ Automatic retry logic with backoff
- ✅ Analytics dashboard data via API
- ✅ Payment cancellation/reversal
- ✅ Frontend integration ready

**Merge to main:** After all Phase 6 features reviewed and tested

---

---

# GIT BRANCHING STRATEGY

## Branch Naming Convention

```
main                           # Production-ready, always stable
  ├── phase1/feature-schema-design
  ├── phase1/feature-maven-dependencies
  ├── phase1/feature-application-config
  ├── phase1/feature-domain-models
  ├── phase1/feature-repository-skeleton
  ├── phase1/feature-documentation
  │
  ├── phase2/feature-jdbc-repositories
  ├── phase2/feature-rule-engine-core
  ├── phase2/feature-validation-rules
  ├── phase2/feature-payment-service
  ├── phase2/feature-audit-trail
  │
  ├── phase3/feature-crud-endpoints
  ├── phase3/feature-lifecycle-endpoints
  ├── phase3/feature-error-handling
  ├── phase3/feature-idempotency
  ├── phase3/feature-audit-endpoints
  ├── phase3/feature-rule-admin-api
  ├── phase3/feature-filtering
  │
  ├── phase4/feature-dockerfile
  ├── phase4/feature-docker-compose
  ├── phase4/feature-profiles
  ├── phase4/feature-actuator
  │
  ├── phase5/feature-integration-tests
  ├── phase5/feature-swagger
  ├── phase5/feature-documentation
  ├── phase5/feature-postman-collection
  ├── phase5/feature-migrations-setup
  │
  ├── phase6/feature-async-processing
  ├── phase6/feature-retry-logic
  ├── phase6/feature-analytics
  ├── phase6/feature-cancellation-reversal
  └── phase6/feature-frontend-integration
```

## Git Workflow

1. **Create Feature Branch**
   ```bash
   git checkout main
   git pull origin main
   git checkout -b phase1/feature-schema-design
   ```

2. **Develop Feature**
   - Commit frequently with descriptive messages
   - Keep commits atomic (one logical change per commit)

3. **Push Branch**
   ```bash
   git push origin phase1/feature-schema-design
   ```

4. **Create Pull Request**
   - Title: `Phase 1: Schema Design - Database Foundation`
   - Description: Link to this roadmap, list tasks completed
   - Request code review

5. **Merge to Main**
   - Require approval before merge
   - Delete feature branch after merge
   - ```bash
     git checkout main
     git pull origin main
     ```

---

# PROGRESS TRACKING

## Overall Progress

| Phase | Status | Completion % | Notes |
|-------|--------|-------------|-------|
| Phase 1 | 🔄 IN PROGRESS | 40% | US 1.1-1.4 Complete; US 1.5-1.6 Pending |
| Phase 2 | ⏸ NOT STARTED | 0% | Dependent on Phase 1 completion |
| Phase 3 | ⏸ NOT STARTED | 0% | Dependent on Phase 2 |
| Phase 4 | ⏸ NOT STARTED | 0% | Parallel with Phase 3 |
| Phase 5 | ⏸ NOT STARTED | 0% | Dependent on Phase 3/4 |
| Phase 6 | ⏸ NOT STARTED | 0% | Dependent on Phase 5 |

---

## Phase 1 Progress

### User Story 1.1: Database Schema Design
- [ ] Task 1.1.1: Create payments table — 0%
- [ ] Task 1.1.2: Create payment_status_history table — 0%
- [ ] Task 1.1.3: Create validation_rules table — 0%
- [ ] Task 1.1.4: Create validation_results table — 0%
- [ ] Task 1.1.5: Create payment_retry_attempts table — 0%
- [ ] Task 1.1.6: Validate schema — 0%

**Status:** 🔄 PLANNING | **ETA:** -

**Blockers/Notes:** Awaiting team approval on schema design decisions

---

### User Story 1.2: Maven Dependencies
- [ ] Task 1.2.1: Add MySQL + JdbcTemplate deps — 0%
- [ ] Task 1.2.2: Add validation & JSON deps — 0%
- [ ] Task 1.2.3: Add test deps — 0%
- [ ] Task 1.2.4: Verify Maven build — 0%

**Status:** 🔄 PLANNING | **ETA:** -

**Blockers/Notes:** Ready to start after Phase 1.1 approval

---

### User Story 1.3: Application Configuration
- [ ] Task 1.3.1: Configure application.properties — 0%
- [ ] Task 1.3.2: Create application-docker.properties — 0%
- [ ] Task 1.3.3: Enable SQL logging — 0%
- [ ] Task 1.3.4: Test connection — 0%

**Status:** 🔄 PLANNING | **ETA:** -

---

### User Story 1.4: Domain Models (Java Records)
- [x] Task 1.4.1: Create PaymentStatus enum — 100%
- [x] Task 1.4.2: Create ErrorCode enum — 100%
- [x] Task 1.4.3: Create RuleType enum — 100%
- [x] Task 1.4.4: Create Severity enum — 100%
- [x] Task 1.4.5: Create PaymentRecord — 100%
- [x] Task 1.4.6: Create PaymentStatusHistoryRecord — 100%
- [x] Task 1.4.7: Create ValidationRuleRecord — 100%
- [x] Task 1.4.8: Create ValidationResultRecord — 100%
- [x] Task 1.4.9: Create request/response DTOs — 100%
- [x] Task 1.4.10: Compile & verify — 100%

**Status:** ✅ COMPLETE | **Verification:** mvn clean compile = BUILD SUCCESS

---

### User Story 1.5: Repository/DAO Layer Skeleton
- [ ] Task 1.5.1: Create PaymentRepository interface — 0%
- [ ] Task 1.5.2: Create ValidationRuleRepository interface — 0%
- [ ] Task 1.5.3: Create ValidationResultRepository interface — 0%
- [ ] Task 1.5.4: Create PaymentStatusHistoryRepository interface — 0%
- [ ] Task 1.5.5: Create skeleton implementations — 0%

**Status:** 🔄 PLANNING | **ETA:** -

---

### User Story 1.6: Project Documentation
- [ ] Task 1.6.1: Create README.md — 0%
- [ ] Task 1.6.2: Create ARCHITECTURE.md — 0%
- [ ] Task 1.6.3: Create SCHEMA.md — 0%

**Status:** 🔄 PLANNING | **ETA:** -

---

## Phase 2 Progress

*To be completed as Phase 2 begins*

---

## Phase 3 Progress

*To be completed as Phase 3 begins*

---

## Phase 4 Progress

*To be completed as Phase 4 begins*

---

## Phase 5 Progress

*To be completed as Phase 5 begins*

---

## Phase 6 Progress

*To be completed as Phase 6 begins*

---

## Key Decision Points (Requires Input)

1. **Payment ID Format:** BIGINT (recommended) or UUID?
   - [ ] Decided: ___
   - [ ] Rationale: ___

2. **Idempotency Key Behavior:** Return 200 (existing) or 409 (conflict) on duplicate?
   - [ ] Decided: ___
   - [ ] Rationale: ___

3. **Status Progression:** Synchronous API-driven (Phase 3) or async scheduled (Phase 6)?
   - [ ] Decided: ___
   - [ ] Rationale: ___

4. **Rule Severity Levels:** HARD/SOFT only, or add MEDIUM?
   - [ ] Decided: ___
   - [ ] Rationale: ___

---

## Lessons Learned & Retrospective

*(To be updated as project progresses)*

- What went well?
- What could be improved?
- Technical challenges encountered?
- Team collaboration notes?

---

## Appendix: Useful Commands

### Git
```bash
# Start new feature
git checkout -b phase1/feature-schema-design

# View changes
git status

# Commit changes
git commit -m "feat: Create MySQL schema for payments and validation_rules tables"

# Push to remote
git push origin phase1/feature-schema-design

# Create pull request (via GitHub/GitLab web UI)

# Merge feature (after approval)
git checkout main && git pull origin main && git merge phase1/feature-schema-design

# Delete feature branch
git branch -d phase1/feature-schema-design
```

### Maven
```bash
# Build project
mvn clean package

# Run tests only
mvn test

# Run integration tests
mvn verify

# Build Docker image
cd backend/ctrl_pay && docker build -t ctrl-pay:latest .

# Run with Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Docker
```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Test app
curl http://localhost:8080/actuator/health
```

---

**Last Updated:** July 31, 2026  
**Next Review:** (To be scheduled)  
**Owner:** Development Team


