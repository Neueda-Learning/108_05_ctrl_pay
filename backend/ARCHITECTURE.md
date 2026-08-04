# Ctrl-Pay Architecture

## High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    React Frontend (Port 3000)               │
│               (Phase 6: User interface for payments)        │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST (JSON)
                         │ CORS-enabled
┌────────────────────────▼────────────────────────────────────┐
│           Spring Boot REST API (Port 8080)                  │
│                (Phase 3: Controllers)                       │
│  POST /api/payments, GET /api/payments/{id}, etc.          │
├─────────────────────────────────────────────────────────────┤
│         Business Logic Layer (Phase 2: Services)            │
│  PaymentService, RuleEngine, ValidationLogic              │
│                                                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Validation Rule Engine:                         │       │
│  │ - Load active rules from DB (cached)            │       │
│  │ - Execute rules sequentially                    │       │
│  │ - Log results to validation_results table       │       │
│  │ - Aggregate: if ANY HARD rule fails → FAILED    │       │
│  └─────────────────────────────────────────────────┘       │
│                                                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Status Transition Manager:                      │       │
│  │ - Validate transition is allowed                │       │
│  │ - CREATED→VALIDATED→SENT→COMPLETED/FAILED      │       │
│  │ - Log transition to payment_status_history      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Idempotency Handler:                            │       │
│  │ - Check idempotency_key in DB                   │       │
│  │ - Return existing payment (200) if found        │       │
│  │ - Prevent duplicate submissions                 │       │
│  └─────────────────────────────────────────────────┘       │
├─────────────────────────────────────────────────────────────┤
│         Data Access Layer (Phase 2: Repositories)           │
│     JdbcTemplate-based queries, prepared statements        │
│  PaymentRepository, ValidationRuleRepository, etc.         │
├─────────────────────────────────────────────────────────────┤
│              MySQL 8.0 Database (Port 3306)                 │
│                    (Phase 1: Schema)                        │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │   payments       │  │ payment_status_  │               │
│  │   - id           │  │   history        │               │
│  │   - status       │  │ - id             │               │
│  │   - amount       │  │ - old_status     │               │
│  │   - currency     │  │ - new_status     │               │
│  │   - error_code   │  │ - timestamp      │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │validation_rules  │  │validation_       │               │
│  │- id              │  │results           │               │
│  │- name            │  │- id              │               │
│  │- rule_type       │  │- payment_id      │               │
│  │- is_active       │  │- passed/failed   │               │
│  │- severity        │  │- error_code      │               │
│  │- rule_definition │  │- execution_time  │               │
│  │  (JSON)          │  │- timestamp       │               │
│  └──────────────────┘  └──────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

---

## Layered Architecture Details

### 1. Controller Layer (REST API - Phase 3)

**Responsibility:** Accept HTTP requests, validate input, delegate to services, return responses

**Key Classes:**
- `PaymentController` - Payment lifecycle endpoints
- `ValidationRuleAdminController` - Rule management endpoints
- Global `@ControllerAdvice` - Exception handling → standardized error responses

**Request Flow:**
```
HTTP POST /api/payments
  ↓
PaymentController.createPayment(@RequestBody CreatePaymentRequest)
  ↓
Validates @Valid on request DTO
  ↓
Delegates to PaymentService.createPayment()
  ↓
Returns 201 Created + PaymentResponse
```

**Error Handling:**
- Validation errors (400 Bad Request)
- Not found (404 Not Found)
- Conflicts (409 Conflict)
- Internal errors (500 Internal Server Error)
- All responses use standardized `ErrorResponse` DTO

---

### 2. Service Layer (Business Logic - Phase 2)

**Responsibility:** Implement business rules, orchestrate repositories, manage transactions

**Key Classes:**
- `PaymentService` - Payment creation, transitions, queries
- `ValidationRuleService` - Rule management (create, update, activate/deactivate)
- `RuleEngine` - Orchestrates validation rule execution
- `StatusTransitionValidator` - Enforces state machine rules

**Example: Payment Creation Flow**

```java
public PaymentResponse createPayment(CreatePaymentRequest request) {
    // 1. Check idempotency
    Optional<PaymentRecord> existing = paymentRepo.findByIdempotencyKey(request.idempotencyKey());
    if (existing.isPresent()) {
        return toResponse(existing.get()); // Return 200 OK (idempotent)
    }
    
    // 2. Load active validation rules (cached)
    List<ValidationRuleRecord> rules = ruleRepo.findActiveRules();
    
    // 3. Create initial payment record
    PaymentRecord payment = PaymentRecord.create(
        request.idempotencyKey(),
        request.sourceAccount(),
        request.destinationAccount(),
        request.amount(),
        request.currency()
    );
    
    // 4. Execute validation rule engine
    List<ValidationResultRecord> results = ruleEngine.validatePayment(payment, rules);
    
    // 5. Check if any HARD rules failed
    boolean anyHardRuleFailed = results.stream()
        .filter(r -> rule.severity() == HARD)
        .anyMatch(r -> !r.passed());
    
    if (anyHardRuleFailed) {
        // Mark payment as FAILED with error
        payment = payment.withFailure(
            ErrorCode.VALIDATION_FAILED.getCode(),
            "Payment failed validation checks"
        );
    } else {
        // Mark as CREATED
        payment = payment.withStatus(PaymentStatus.CREATED);
    }
    
    // 6. Persist payment
    PaymentRecord saved = paymentRepo.save(payment);
    
    // 7. Log all validation results for audit
    results.forEach(result -> validationResultRepo.save(result));
    
    // 8. Log initial status history
    paymentStatusHistoryRepo.save(
        PaymentStatusHistoryRecord.createInitial(saved.id())
    );
    
    // 9. Return response with embedded validation results
    return toResponse(saved, results);
}
```

**Key Design Patterns:**
- **Dependency Injection:** Services injected into controllers
- **Transaction Management:** `@Transactional` ensures atomicity
- **Caching:** Active rules cached in memory, invalidated on rule changes
- **Factory Methods:** Records use static factory methods for convenience

---

### 3. Validation Rule Engine (Phase 2)

**Responsibility:** Execute configurable validation rules in sequence

**Architecture:**

```
Input: PaymentRecord + List<ValidationRuleRecord>
  ↓
RuleEngine.validatePayment()
  ↓
For each rule (ordered by order_of_execution):
  ├─ Measure execution time (start)
  ├─ Get rule implementation (AmountRangeRule, CurrencyWhitelistRule, etc.)
  ├─ Execute rule.validate(payment)
  ├─ Measure execution time (end)
  ├─ Create ValidationResultRecord (passed/failed)
  ├─ Log result to DB
  └─ Continue to next rule
  ↓
Output: List<ValidationResultRecord>
```

**Rule Types:**

| Rule Type | JSON Definition | Logic |
|-----------|-----------------|-------|
| AMOUNT_RANGE | `{min, max}` | Check 0.01 ≤ amount ≤ 1000000 |
| CURRENCY_WHITELIST | `{allowed_currencies}` | Check currency in [USD, EUR, ...] |
| ACCOUNT_FORMAT | `{pattern}` | Check account matches pattern (12 digits) |
| ACCOUNT_DIFFERENCE | `{}` | Check source ≠ destination |
| MOCK_SUFFICIENT_FUNDS | `{failure_rate}` | Simulate funds check with configurable failure rate |

**Zero-Downtime Deployment:**
1. Insert new rule into `validation_rules` table with `is_active=false`
2. Test rule via dry-run endpoint
3. Update rule: `is_active=true`
4. Rule engine loads active rules on next request (or from cache if invalidated)
5. No application restart required!

---

### 4. Status Transition State Machine (Phase 2)

**State Diagram:**

```
                    ┌─────────────┐
                    │   CREATED   │
                    └──────┬──────┘
                           │
                    ┌──────▼───────┐
                    │   VALIDATED  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │     SENT     │
                    └──────┬───────┘
                           │
                    ┌──────▼──────────┐
                    │   COMPLETED    │ ◄──┐ Terminal
                    └────────────────┘    │
                                          │
    ┌─────────────────────────────────────┘
    │
    ├──▶ FAILED ─────────────────────────┐
    │      (any stage)               Terminal
    └───────────────────────────────────────

Valid Transitions:
- CREATED → VALIDATED (explicit via API or automatic in service)
- CREATED → FAILED (validation fails)
- VALIDATED → SENT (explicit via API)
- VALIDATED → FAILED (explicit or automatic)
- SENT → COMPLETED (explicit via API)
- SENT → FAILED (explicit or automatic)
- COMPLETED (terminal)
- FAILED (terminal)

Invalid Transitions (rejected with 400 Bad Request):
- COMPLETED → anything
- FAILED → anything
- VALIDATED → CREATED (backward)
- SENT → VALIDATED (backward)
- CREATED → SENT (skip VALIDATED)
```

**Implementation:**

```java
public class StatusTransitionValidator {
    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.ofEntries(
        entry(CREATED, Set.of(VALIDATED, FAILED)),
        entry(VALIDATED, Set.of(SENT, FAILED)),
        entry(SENT, Set.of(COMPLETED, FAILED))
        // COMPLETED and FAILED have no valid transitions (terminal)
    );
    
    public static void validate(PaymentStatus from, PaymentStatus to) {
        Set<PaymentStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(
                "Cannot transition from " + from + " to " + to
            );
        }
    }
}
```

---

### 5. Repository Layer (Data Access - Phase 2)

**Responsibility:** Abstract database operations, execute SQL queries, map results to Records

**Technology:** Spring `JdbcTemplate` + Java `RowMapper`

**Example Repository Method:**

```java
@Override
public List<PaymentRecord> findAll(PaymentStatus status, int limit, int offset) {
    String sql = """
        SELECT id, idempotency_key, source_account, destination_account,
               amount, currency, status, error_code, error_message,
               created_at, updated_at
        FROM payments
        WHERE status = ? OR ? IS NULL
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        """;
    
    return jdbcTemplate.query(sql, new PaymentRowMapper(),
        status != null ? status.name() : null,
        status != null ? null : 1, // Trick for "OR ? IS NULL"
        limit,
        offset
    );
}

// RowMapper converts ResultSet row to PaymentRecord
private static class PaymentRowMapper implements RowMapper<PaymentRecord> {
    @Override
    public PaymentRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentRecord(
            rs.getLong("id"),
            rs.getString("idempotency_key"),
            rs.getString("source_account"),
            rs.getString("destination_account"),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            PaymentStatus.valueOf(rs.getString("status")),
            rs.getString("error_code"),
            rs.getString("error_message"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
```

**Key Features:**
- **Parameterized Queries:** Prevent SQL injection
- **Connection Pooling:** HikariCP manages connections efficiently
- **Type Safety:** Records use compile-time type checking
- **Transaction Support:** Spring `@Transactional` manages commit/rollback

---

### 6. Domain Models (Phase 1)

**Records (Immutable Data Carriers):**
- `PaymentRecord` - Main payment entity
- `PaymentStatusHistoryRecord` - Status transition audit log
- `ValidationRuleRecord` - Validation rule definition
- `ValidationResultRecord` - Validation execution result

**Enums:**
- `PaymentStatus` - CREATED, VALIDATED, SENT, COMPLETED, FAILED
- `ErrorCode` - 10 standardized error codes
- `RuleType` - 5 validation rule types
- `Severity` - HARD (blocking), SOFT (warning)

**DTOs (Data Transfer Objects):**
- `CreatePaymentRequest` - Client → Server
- `PaymentResponse` - Server → Client (with embedded validation results)
- `ErrorResponse` - Standardized error response
- `ValidationResultResponse` - Individual validation result

---

## Data Flow Examples

### Example 1: Successful Payment Creation

```
1. Client sends POST /api/payments with payment details + idempotency key
   
2. PaymentController.createPayment()
   ├─ @Valid validates CreatePaymentRequest
   └─ Calls PaymentService.createPayment()
   
3. PaymentService.createPayment()
   ├─ Checks idempotency key → NOT FOUND (first time)
   ├─ Loads active validation rules from DB (cached)
   ├─ Calls RuleEngine.validatePayment()
   │
   └─ RuleEngine executes rules in order:
      ├─ AmountRangeRule: 1000.00 > 0 AND <= 1000000 → PASS
      ├─ CurrencyWhitelistRule: USD in [USD, EUR, ...] → PASS
      ├─ AccountFormatRule: 123456789012 matches ^[0-9]{12}$ → PASS
      ├─ AccountDifferenceRule: 123456789012 ≠ 210987654321 → PASS
      └─ MockSufficientFundsRule: random(0,1) < 0.9 → PASS
   
   ├─ All rules passed → payment status = CREATED
   │
   ├─ PaymentRepository.save(payment) 
   │  └─ INSERT INTO payments (...) → returns id=1
   │
   ├─ Log all validation results:
   │  ValidationResultsRepository.save() × 5 times
   │
   ├─ Log initial status transition:
   │  PaymentStatusHistoryRepository.save(CREATED)
   │
   └─ Return PaymentResponse with:
      ├─ id: 1
      ├─ status: CREATED
      └─ validationResults: [5 results showing all PASS]

4. Controller returns 201 Created + PaymentResponse
```

### Example 2: Idempotent Payment Creation (Same Key)

```
1. Client sends same POST /api/payments with SAME idempotency key

2. PaymentService.createPayment()
   ├─ Checks idempotency key → FOUND (payment id=1)
   ├─ Verifies details match (same source, destination, amount, currency)
   └─ Returns existing payment (no new record created)

3. Controller returns 200 OK + PaymentResponse (NOT 201 Created)
   └─ Client understands this is idempotent response, not new creation
```

### Example 3: Validation Failure

```
1. Client sends POST /api/payments with negative amount

2. RuleEngine.validatePayment()
   ├─ AmountRangeRule: -100 > 0? → FAIL
   │  └─ Creates ValidationResultRecord (passed=false, errorCode=INVALID_AMOUNT)
   │
   ├─ Continue remaining rules anyway (collect all failures)
   │  ├─ CurrencyWhitelistRule: USD → PASS
   │  └─ [other rules...]

3. PaymentService detects HARD rule failure
   ├─ payment.status = FAILED
   ├─ payment.errorCode = VALIDATION_FAILED
   ├─ PaymentRepository.save()
   └─ ValidationResultsRepository.save() for all results

4. Controller returns 201 Created + PaymentResponse
   └─ Client sees status=FAILED with error details
```

### Example 4: Status Transition

```
1. Client sends POST /api/payments/1/send (while in VALIDATED status)

2. PaymentController.transitionPayment()
   ├─ Calls PaymentService.validateAndTransition(1, SENT)
   │
   └─ PaymentService.validateAndTransition()
      ├─ Retrieves current payment → status=VALIDATED ✓
      ├─ StatusTransitionValidator.validate(VALIDATED, SENT) ✓
      ├─ Simulation: "call payment gateway" → 80% success, 20% fail
      ├─ Success! → newStatus=SENT
      ├─ PaymentRepository.update(payment) → UPDATE payments SET status=SENT WHERE id=1
      ├─ PaymentStatusHistoryRepository.save() → INSERT with old_status=VALIDATED, new_status=SENT
      └─ Return PaymentResponse with status=SENT

3. Controller returns 200 OK + updated PaymentResponse
```

---

## Performance Considerations

### Database Indexes

All critical queries have indexes:
- **payments.status** - Filter by status
- **payments.idempotency_key** - Duplicate prevention
- **payment_status_history(payment_id, created_at)** - History retrieval
- **validation_results(payment_id, created_at)** - Validation audit
- **validation_rules(is_active, order_of_execution)** - Active rules fetch

### Caching

**In-Memory Cache:**
- Active validation rules cached (loaded once per app startup)
- Cache invalidated when rule is modified (is_active changed, definition updated)
- ~5-10 rules = minimal memory overhead

**Database Connection Pool:**
- HikariCP with 10 connections (dev), 20 (prod)
- Auto-recycles idle connections
- Prevents connection exhaustion

---

## Security Considerations

1. **SQL Injection Prevention** - All queries use parameterized statements
2. **Idempotency** - Prevents duplicate payment submissions
3. **State Machine Validation** - Prevents invalid status transitions
4. **Transaction Atomicity** - All-or-nothing operations via `@Transactional`
5. **Error Message Safety** - Generic error messages in production, detailed in logs
6. **No Authentication (Phase 1)** - Single user assumed (future: add auth in Phase 2+)

---

## Testing Strategy

**Unit Tests (Phase 5):**
- Service layer tests (mock repositories)
- Rule validation tests
- Status transition rules tests
- DTO serialization tests

**Integration Tests (Phase 5):**
- End-to-end with TestContainers MySQL
- Full payment lifecycle scenarios
- Duplicate idempotency key handling
- Invalid transition rejection

**Test Coverage Target:** 80%+ of business logic

---

## Deployment Architecture (Phase 4)

```
Developer (localhost:3000)
  ↓ HTTP
Docker Compose Network
  ├─ Spring Boot (port 8080) + application-docker.properties
  │  └─ Environment variables: SPRING_DATASOURCE_URL, etc.
  │
  └─ MySQL 8.0 (port 3306)
     └─ Persistent volume: mysql_data:/var/lib/mysql
```

**Production Deployment:**
- Spring Boot JAR deployed to container orchestration platform (Kubernetes, ECS, etc.)
- MySQL runs on managed database service (RDS, CloudSQL, etc.)
- All credentials via environment variables (no hardcoding)
- Health checks via `/actuator/health` endpoints

---

## Future Architecture Enhancements (Phase 6+)

- **Asynchronous Processing:** RabbitMQ/Kafka for payment processing queue
- **API Gateway:** Rate limiting, request logging, API versioning
- **Service Mesh:** Istio/Linkerd for service-to-service communication
- **Distributed Tracing:** Jaeger/Zipkin for request tracing
- **Analytics:** Apache Kafka + ELK stack for payment analytics
- **Multi-Region:** Payment replication, failover
- **micro-services:** Split into separate Payment, Validation, Notification services

---

**Last Updated:** July 31, 2026  
**Phase:** Phase 1 - Foundation Complete

