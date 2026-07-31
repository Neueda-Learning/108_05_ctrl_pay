# Phase 4: Docker & Infrastructure - IMPLEMENTATION GUIDE

**Status:** ✅ COMPLETE  
**Date:** July 31, 2026  
**Build:** ✅ SUCCESS

---

## Overview

Phase 4 containerizes the Ctrl-Pay application for seamless local development and production deployment. All services (MySQL + Spring Boot) can be started with a single command.

---

## What Was Delivered

### User Story 4.1: Dockerfile for Spring Boot Application
**File:** `backend/ctrl_pay/Dockerfile`

**Features:**
- ✅ Multi-stage build (Maven build → OpenJDK 17 runtime)
- ✅ Alpine Linux base image for minimal size (~300MB)
- ✅ Non-root user execution for security
- ✅ Health check endpoint
- ✅ Resource limits configurable via JAVA_OPTS
- ✅ Optimized layer caching

**Build Command:**
```bash
cd backend/ctrl_pay
docker build -t ctrl-pay:latest .
```

**Run Command:**
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ctrl_pay \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=admin123 \
  ctrl-pay:latest
```

### User Story 4.2: docker-compose.yml for Local Development
**File:** `docker-compose.yml`

**Services:**
1. **MySQL 8.0 Service**
   - Container: `ctrl-pay-mysql`
   - Port: 3306
   - Volume: `mysql_data` (persistent)
   - Health check: mysqladmin ping
   - Resource limits: 1 CPU, 512MB RAM

2. **Spring Boot Application Service**
   - Container: `ctrl-pay-app`
   - Port: 8080
   - Depends on: MySQL (waits for health check)
   - Health check: curl /actuator/health/liveness
   - Resource limits: 2 CPU, 1GB RAM

**Startup:**
```bash
docker-compose up -d
```

**Verify:**
```bash
docker-compose ps
docker logs ctrl-pay-mysql
docker logs ctrl-pay-app
```

**Stop:**
```bash
docker-compose down
```

**Cleanup (including data):**
```bash
docker-compose down -v
```

### User Story 4.3: Environment-Specific Configuration Profiles
**Files:**
- `src/main/resources/application.properties` (default/base)
- `src/main/resources/application-dev.properties` (local MySQL)
- `src/main/resources/application-docker.properties` (docker-compose MySQL service)
- `src/main/resources/application-prod.properties` (production MySQL)

**Profile Activation:**
```bash
# Development (local MySQL)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Docker (via docker-compose)
# Automatically set in Dockerfile and docker-compose.yml

# Production
java -jar app.jar -Dspring.profiles.active=prod
```

**Environment Variables (Production):**
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://prod-mysql:3306/ctrl_pay
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
```

### User Story 4.4: Health Check & Actuator Endpoints
**Endpoint:** `/actuator/health`

**Liveness Probe (is container running?):**
```bash
curl http://localhost:8080/actuator/health/liveness
# Response: {"status":"UP"}
```

**Readiness Probe (is app ready for traffic?):**
```bash
curl http://localhost:8080/actuator/health/readiness
# Response: {"status":"UP","components":{"db":{"status":"UP"}}}
```

**Info Endpoint (app metadata):**
```bash
curl http://localhost:8080/actuator/info
# Response: {"app":{"name":"Ctrl-Pay","version":"0.0.1","description":"Payment Processing System"}}
```

**Metrics Endpoint:**
```bash
curl http://localhost:8080/actuator/metrics
```

---

## Additional Files Created

### .dockerignore
Excludes unnecessary files from Docker build context:
- Git files (.git, .gitignore)
- IDE files (.idea, .vscode)
- Maven target directory
- Documentation (docs/, *.md)
- Test files (src/test/)

### .env.example
Template for environment variables:
```bash
SPRING_PROFILES_ACTIVE=docker
MYSQL_ROOT_PASSWORD=admin123
MYSQL_DATABASE=ctrl_pay
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=admin123
JAVA_OPTS=-Xmx512m -Xms256m
TZ=UTC
```

**Setup:**
```bash
cp .env.example .env
# Edit .env with your values
docker-compose up
```

### .gitignore
Prevents committing sensitive files:
- Environment files (.env)
- IDE files (.idea, .vscode)
- Build artifacts (target/, build/)
- Logs (*.log, logs/)
- Sensitive data

---

## Quick Start Guide

### Method 1: Docker Compose (Recommended)

```bash
# Navigate to project root
cd 108_05_ctrl_pay

# Copy environment template
cp .env.example .env

# Start services (MySQL + Spring Boot)
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f app

# Test API
curl http://localhost:8080/api/payments

# Stop services
docker-compose down
```

### Method 2: Manual Docker Run

```bash
# Start MySQL
docker run -d --name ctrl-pay-mysql \
  -e MYSQL_ROOT_PASSWORD=admin123 \
  -e MYSQL_DATABASE=ctrl_pay \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  mysql:8.0.35-alpine

# Build Ctrl-Pay image
cd backend/ctrl_pay
docker build -t ctrl-pay:latest .

# Run Ctrl-Pay
docker run -d --name ctrl-pay-app \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://ctrl-pay-mysql:3306/ctrl_pay \
  -p 8080:8080 \
  --link ctrl-pay-mysql \
  ctrl-pay:latest
```

### Method 3: Local Development (No Docker)

```bash
# Start local MySQL
mysql -u root -p < backend/ctrl_pay/src/main/resources/schema.sql

# Run Spring Boot
cd backend/ctrl_pay
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Test API
curl http://localhost:8080/api/payments
```

---

## Verification Checklist

```bash
# 1. Services running?
docker-compose ps

# 2. MySQL healthy?
docker-compose exec mysql mysqladmin ping -u root -p${MYSQL_ROOT_PASSWORD}

# 3. Application started?
curl http://localhost:8080/actuator/health

# 4. Database connected?
curl http://localhost:8080/actuator/health/db

# 5. API responsive?
curl http://localhost:8080/api/payments

# 6. Create test payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccount": "123456789012",
    "destinationAccount": "210987654321",
    "amount": 1000.00,
    "currency": "USD"
  }'

# 7. List payments with filter
curl "http://localhost:8080/api/payments?status=CREATED&limit=10"
```

---

## Troubleshooting

### Issue: Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
docker run -p 8081:8080 ctrl-pay:latest
```

### Issue: MySQL Connection Failed
```bash
# Check MySQL logs
docker logs ctrl-pay-mysql

# Check MySQL is running
docker-compose ps mysql

# Verify connection
mysql -h 127.0.0.1 -u root -p -e "SHOW DATABASES;"
```

### Issue: Application Won't Start
```bash
# Check application logs
docker logs ctrl-pay-app

# Check Spring profile active
docker-compose logs app | grep "profiles activeSet"

# Verify environment variables
docker-compose config
```

### Issue: Database Schema Not Initialized
```bash
# In docker-compose.yml, verify:
# 1. volumes mount includes schema.sql
# 2. spring.sql.init.mode=always is set
# 3. MySQL healthcheck passes before app starts

# Manually init schema if needed:
docker-compose exec mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} ctrl_pay < src/main/resources/schema.sql
```

---

## Configuration Reference

### Database Connection

**Development (local MySQL):**
```
jdbc:mysql://localhost:3306/ctrl_pay?serverTimezone=UTC&useSSL=false
Username: root
Password: admin123
```

**Docker (MySQL service):**
```
jdbc:mysql://mysql:3306/ctrl_pay?serverTimezone=UTC&useSSL=false
Username: root
Password: admin123
```

**Production (external MySQL):**
```
jdbc:mysql://<HOST>:<PORT>/ctrl_pay?serverTimezone=UTC&useSSL=true
Username: <from env variable>
Password: <from env variable>
```

### Logging Levels

| Profile | Root | App | SQL | Spring |
|---------|------|-----|-----|--------|
| dev | INFO | DEBUG | DEBUG | INFO |
| docker | INFO | INFO | INFO | WARN |
| prod | WARN | INFO | WARN | WARN |

### Actuator Endpoints

| Profile | Endpoints |
|---------|-----------|
| dev | health, info, metrics, prometheus |
| docker | health, info, metrics |
| prod | health, info |

### Resource Limits

| Service | CPU | Memory |
|---------|-----|--------|
| MySQL | 1.0 | 512MB |
| App | 2.0 | 1GB |

---

## Performance Tips

1. **Optimize Image Size**
   - Use Alpine base images
   - Multi-stage builds separate build from runtime
   - Exclude test files from image

2. **Database Performance**
   - Index optimization (already configured)
   - Connection pooling (HikariCP configured)
   - Query result pagination (implemented)

3. **Java Performance**
   - Heap size: -Xmx512m -Xms256m (adjust for your env)
   - Use production-grade settings in prod profile

4. **Container Performance**
   - Use resource limits to prevent resource exhaustion
   - Enable health checks for automated recovery

---

## DevOps Integration

### Kubernetes Deployment
To deploy to Kubernetes, replace docker-compose with:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ctrl-pay
spec:
  containers:
  - name: app
    image: ctrl-pay:latest
    ports:
    - containerPort: 8080
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 40
      periodSeconds: 30
```

### CI/CD Pipeline
```bash
# Build image
docker build -t ctrl-pay:${VERSION} .

# Push to registry
docker push registry.example.com/ctrl-pay:${VERSION}

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

### Monitoring & Logging
```bash
# View metrics
curl http://localhost:8080/actuator/metrics

# Stream logs
docker-compose logs -f

# Export logs
docker-compose logs > logs.txt
```

---

## Related Documentation

1. **[README.md](README.md)** - Project overview with Docker setup section
2. **[Dockerfile](Dockerfile)** - Multi-stage build configuration
3. **[docker-compose.yml](docker-compose.yml)** - Service orchestration
4. **[.env.example](.env.example)** - Environment template
5. **[.gitignore](.gitignore)** - Git configuration
6. **[PROJECT_ROADMAP.md](PROJECT_ROADMAP.md)** - Phase tracking

---

## What's Next (Phase 5)

Phase 5 focuses on:
- ✅ Comprehensive integration tests
- ✅ Swagger/OpenAPI documentation
- ✅ Deployment guides
- ✅ Postman collection
- ✅ Database migration strategy

---

**Completed By:** Development Team  
**Date:** July 31, 2026  
**Version:** Phase 4  
**Status:** ✅ COMPLETE

