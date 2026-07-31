# Ctrl-Pay: Payment Processing System

## Overview

Ctrl-Pay is a **production-grade REST API** for managing the complete lifecycle of financial payments from creation through validation, processing, and completion (or failure). The system implements a **configurable validation rule engine** (zero-downtime deployment), comprehensive audit trail, and idempotency support for duplicate prevention.

### Key Features

✅ **Complete Payment Lifecycle** — CREATED → VALIDATED → SENT → COMPLETED/FAILED  
✅ **Configurable Validation Rules** — Rules stored in DB, deployable without code changes  
✅ **Comprehensive Audit Trail** — Every status change + validation result logged  
✅ **Idempotency Support** — Prevent duplicate payment submissions  
✅ **MySQL-Only Persistence** — JdbcTemplate-based data access  
✅ **Production-Ready** — Error handling, logging, health checks, Docker support  
✅ **React Frontend Ready** — CORS-enabled, comprehensive REST API  

---

## Quick Start

### Prerequisites

- **Java 17+** (OpenJDK or similar)
- **Maven 3.6+**
- **MySQL 8.0+** (or Docker)
- **Git**

### Local Development Setup (Option 1: Manual MySQL)

```bash
# 1. Clone repository
git clone <repo-url>
cd 108_05_ctrl_pay

# 2. Install MySQL locally
# On macOS: brew install mysql
# On Ubuntu: sudo apt-get install mysql-server
# On Windows: Download from https://dev.mysql.com/downloads/mysql/

# 3. Create database
mysql -u root -p
CREATE DATABASE ctrl_pay;
EXIT;

# 4. Build project
cd backend/ctrl_pay
mvn clean package

# 5. Run application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# 6. Test API
curl http://localhost:8080/actuator/health
```

### Local Development Setup (Option 2: Docker Compose - Recommended)

```bash
# 1. Clone repository
git clone <repo-url>
cd 108_05_ctrl_pay

# 2. Start everything with one command
docker-compose up -d

# 3. Verify services running
docker-compose ps

# 4. Test API
curl http://localhost:8080/actuator/health

# 5. Stop services
docker-compose down
```

---

## Project Structure

```
108_05_ctrl_pay/
├── backend/
│   └── ctrl_pay/                          # Spring Boot application
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/neueda/
│       │   │   │   ├── CtrlPayApplication.java       # Entry point
│       │   │   │   ├── controller/                   # REST endpoints (Phase 3)
│       │   │   │   ├── service/                      # Business logic (Phase 2)
│       │   │   │   ├── repository/                   # Data access (Phase 1 skeleton)
│       │   │   │   ├── domain/                       # Entity models (Phase 1)
│       │   │   │   ├── dto/                          # Request/response objects (Phase 1)
│       │   │   │   └── exception/                    # Custom exceptions (Phase 3)
│       │   │   └── resources/
│       │   │       ├── schema.sql                    # Database schema
│       │   │       └── application*.properties       # Configuration
│       │   └── test/java/com/neueda/                 # Tests (Phase 5)
│       ├── pom.xml                                   # Maven dependencies
│       ├── Dockerfile                                # Container image (Phase 4)
│       └── README.md                                 # This file
├── frontend/                               # React application (Phase 6)
├── docker-compose.yml                      # Local development stack (Phase 4)
└── PROJECT_ROADMAP.md                      # Feature tracking
```

---

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture diagram and data flow.

**High-level layering:**

```
┌─────────────────────────────────────┐
│    REST API Controllers (Phase 3)   │ ← HTTP endpoints
├─────────────────────────────────────┤
│    Business Logic Services (Phase 2)│ ← Rules, transitions, validation
├─────────────────────────────────────┤
│    JdbcTemplate Repositories        │ ← Data access (Phase 2 SQL)
├─────────────────────────────────────┤
│    MySQL 8.0 Database               │ ← Persistent storage
└─────────────────────────────────────┘
```

---

## Database Schema

See [docs/SCHEMA.md](docs/SCHEMA.md) for complete schema documentation.

**Core tables:**

- **`payments`** — Payment records with lifecycle status
- **`payment_status_history`** — Immutable audit trail of status changes
- **`validation_rules`** — Configurable validation rules (zero-downtime deployment)
- **`validation_results`** — Validation audit log (compliance required)
- **`payment_retry_attempts`** — Retry tracking for resilience

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 4.0.7 |
| Build Tool | Maven | 3.6+ |
| Database | MySQL | 8.0+ |
| Data Access | JdbcTemplate | (Spring framework) |
| JSON | Jackson | (Spring Boot default) |
| Validation | Jakarta Bean Validation | JSR-380 |
| Testing | JUnit 5 + TestContainers | (Spring Boot defaults) |
| Container | Docker | (Phase 4) |

---

## Running Tests

```bash
cd backend/ctrl_pay

# Run all tests with real MySQL (TestContainers)
mvn verify

# Run only unit tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceTest

# Generate test coverage report
mvn jacoco:report
```

---

## Building for Production

### Build JAR

```bash
cd backend/ctrl_pay
mvn clean package -DskipTests

# JAR location: target/ctrl_pay-0.0.1-SNAPSHOT.jar
```

### Build Docker Image

```bash
cd backend/ctrl_pay
docker build -t ctrl-pay:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/ctrl_pay \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  ctrl-pay:latest
```

---

## API Endpoints

See interactive API documentation at **http://localhost:8080/swagger-ui.html** (Phase 5)

### Payment Lifecycle Endpoints (Phase 3)

```
POST   /api/payments                    Create new payment
GET    /api/payments/{id}               Get payment details
GET    /api/payments                    List payments (with filtering/pagination)
POST   /api/payments/{id}/validate      Validate payment
POST   /api/payments/{id}/send          Send payment
POST   /api/payments/{id}/complete      Mark payment complete
POST   /api/payments/{id}/fail          Mark payment failed
```

### Advanced Filtering & Search (User Story 3.7)

The GET `/api/payments` endpoint supports powerful filtering for compliance and analytics:

```
GET /api/payments?status=COMPLETED&currency=USD&date-from=2026-07-01T00:00:00&date-to=2026-07-31T23:59:59&failed-rule=2&account=123456789012&limit=50&offset=0
```

**Query Parameters:**
- `status` — Filter by payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED)
- `currency` — Filter by ISO 4217 currency code (USD, EUR, GBP, etc.)
- `account` — Filter by source or destination account (12-digit format)
- `date-from` — Filter by created_at >= (ISO 8601 format, e.g., 2026-07-01T00:00:00)
- `date-to` — Filter by created_at <= (ISO 8601 format)
- `failed-rule` — Filter by failed validation rule ID
- `limit` — Max results (default 10, max 1000)
- `offset` — Pagination offset (default 0)

**Use Cases:**
- Daily reconciliation: `?date-from=2026-07-31T00:00:00&date-to=2026-07-31T23:59:59`
- Fraud investigation: `?account=123456789012` or `?failed-rule=2`
- Currency reporting: `?currency=USD&status=COMPLETED`
- Audit trails: `?date-from=2026-07-01T00:00:00`

See [FILTERING.md](docs/FILTERING.md) for complete documentation with examples.

### Audit Endpoints (Phase 3)

```
GET    /api/payments/{id}/history       Get status transition history
GET    /api/payments/{id}/validations   Get validation results
GET    /api/payments/{id}/audit         Get combined audit trail
```

### Admin Endpoints (Phase 3)

```
GET    /api/admin/validation-rules      List all validation rules
POST   /api/admin/validation-rules      Create new rule
PUT    /api/admin/validation-rules/{id} Update rule
PATCH  /api/admin/validation-rules/{id}/toggle Toggle rule active/inactive
POST   /api/admin/validation-rules/{id}/test-dry-run Test rule (no DB write)
```

---

## Handling Common Scenarios

### Create a Payment (Idempotent)

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: client-generated-uuid" \
  -d '{
    "sourceAccount": "123456789012",
    "destinationAccount": "210987654321",
    "amount": 1000.00,
    "currency": "USD",
    "idempotencyKey": "client-generated-uuid"
  }'
```

**Response (201 Created):**
```json
{
  "id": 1,
  "status": "CREATED",
  "validationResults": [
    {"ruleName": "AMOUNT_RANGE", "passed": true},
    {"ruleName": "CURRENCY_WHITELIST", "passed": true}
  ]
}
```

### Get Payment Audit Trail

```bash
curl http://localhost:8080/api/payments/1/audit
```

**Response:**
```json
{
  "history": [
    {"timestamp": "2026-07-31T10:00:00", "oldStatus": null, "newStatus": "CREATED"},
    {"timestamp": "2026-07-31T10:00:05", "oldStatus": "CREATED", "newStatus": "VALIDATED"}
  ],
  "validationResults": [...]
}
```

---

## Environment Configuration

### Development (Local MySQL)

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Uses `application-dev.properties`:
- MySQL: localhost:3306
- Verbose logging
- All actuator endpoints exposed
- Detailed error responses

### Docker Compose

```bash
docker-compose up -d
```

Uses `application-docker.properties`:
- MySQL: mysql:3306 (service name)
- Credentials from environment variables
- Production-appropriate logging

### Production

```bash
java -jar target/ctrl_pay-0.0.1-SNAPSHOT.jar \
  -Dspring.profiles.active=prod \
  -DSPRING_DATASOURCE_URL=jdbc:mysql://prod-mysql:3306/ctrl_pay \
  -DSPRING_DATASOURCE_USERNAME=root \
  -DSPRING_DATASOURCE_PASSWORD=<secret>
```

Uses `application-prod.properties`:
- Minimal logging (WARN level)
- Restricted actuator endpoints
- All credentials from environment variables

---

## Health Checks

```bash
# Liveness probe (is app running?)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (is app ready for traffic?)
curl http://localhost:8080/actuator/health/readiness

# Detailed health with database status
curl http://localhost:8080/actuator/health?show=when-authorized
```

---

## Troubleshooting

### MySQL Connection Failed

```
Error: Connection refused to host: localhost:3306
```

**Solution:**
- Ensure MySQL is running: `mysql -u root -p`
- Or use Docker: `docker-compose up -d mysql`
- Check `application-dev.properties` has correct URL

### Port 8080 Already in Use

```
Error: Address already in use: bind
```

**Solution:**
- Kill process on port 8080: `lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9`
- Or change port: `-Dserver.port=8081`

### Schema Initialization Failed

```
Error: Table 'ctrl_pay.payments' doesn't exist
```

**Solution:**
- Ensure database exists: `CREATE DATABASE ctrl_pay;`
- Or let Spring init schema: `spring.sql.init.mode=always` is enabled

---

## Contributing

1. Create feature branch: `git checkout -b phase1/feature-name`
2. Implement changes
3. Run tests: `mvn verify`
4. Commit with clear messages: `git commit -m "feat: description"`
5. Push and create Pull Request

---

## Development Roadmap

| Phase | Title | Status | ETA |
|-------|-------|--------|-----|
| 1 | Core Schema & Domain | ✅ Complete | Done |
| 2 | Rule Engine & Validation | 🔄 In Progress | Week 2 |
| 3 | REST API & Lifecycle | ⏸ Blocked on Phase 2 | Week 3 |
| 4 | Docker & Infrastructure | ⏸ Blocked on Phase 3 | Week 3 |
| 5 | Integration Testing & Docs | ⏸ Blocked on Phase 4 | Week 4 |
| 6 | Advanced Features | ⏸ Not Started | Week 5+ |

See [PROJECT_ROADMAP.md](../PROJECT_ROADMAP.md) for detailed sprint breakdown.

---

## Support

- **Issues:** Open GitHub issue
- **Questions:** Check [ARCHITECTURE.md](ARCHITECTURE.md) and [docs/SCHEMA.md](docs/SCHEMA.md)
- **Team Contact:** <team-email@example.com>

---

## License

Proprietary - Neueda Training Project

---

**Last Updated:** July 31, 2026  
**Version:** Phase 1 - Foundation Complete

