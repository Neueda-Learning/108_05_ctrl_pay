# Bulk Payments Feature - Installation & Deployment Guide

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 16+
- MySQL 5.7+ or 8.0+
- Existing Ctrl_Pay application running

---

## Installation Steps

### Step 1: Database Migration

1. **Stop any running application instances**

2. **Apply database schema changes:**
   ```bash
   cd backend/src/main/resources
   
   # Option A: Manual SQL execution in MySQL client
   mysql -u root -p ctrl_pay < schema.sql
   
   # Option B: Using Flyway (if preferred)
   # Copy migration file to src/main/resources/db/migration/
   cp db/migration/V1_1__create_bulk_payment_tables.sql \
      target/classes/db/migration/
   ```

3. **Verify tables created:**
   ```sql
   -- Connect to MySQL
   mysql -u root -p ctrl_pay
   
   -- Verify new tables exist
   SHOW TABLES LIKE 'bulk_payment%';
   
   -- Expected output:
   -- bulk_payment_audit_events
   -- bulk_payment_batches
   -- bulk_payment_error_log
   -- bulk_payment_items
   ```

### Step 2: Backend Build

1. **Navigate to backend directory:**
   ```bash
   cd backend
   ```

2. **Build the project:**
   ```bash
   # Full build with tests
   mvn clean install
   
   # Build without tests (faster)
   mvn clean install -DskipTests
   ```

3. **Verify build success:**
   ```bash
   # Should see: BUILD SUCCESS
   # JAR file: target/ctrl_pay-0.0.1-SNAPSHOT.jar
   ```

4. **[OPTIONAL] Run unit tests:**
   ```bash
   mvn test
   ```

### Step 3: Frontend Build

1. **Navigate to frontend directory:**
   ```bash
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Build production bundle:**
   ```bash
   npm run build
   ```

4. **Verify build output:**
   ```bash
   # Should create: build/ directory with output.txt
   ls -lh build/
   ```

### Step 4: Start Application Locally

**Option A: Maven (Development)**
```bash
# Terminal 1: Start backend
cd backend
./mvnw spring-boot:run

# Terminal 2: Start frontend (in separate terminal)
cd frontend
npm start

# Access application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

**Option B: Docker Compose (Production-like)**
```bash
# From root directory
docker-compose up --build

# Access application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
```

---

## Configuration

### Backend Application Properties

1. **Create/Edit:** `backend/src/main/resources/application.properties`

```properties
# Bulk Payments Configuration
bulk.payment.max-lines=10000
bulk.payment.batch-size=100
bulk.payment.max-file-size-mb=50
bulk.payment.processing.enabled=true
bulk.payment.async-processing=false

# Fraud Detection
fraud.detection.enabled=true

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ctrl_pay
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

### Frontend Environment

1. **Create:** `frontend/.env.local`

```properties
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_SOCKET_URL=ws://localhost:8080/ws
REACT_APP_DEBUG=false
```

---

## Verification Checklist

### Backend Verification
```bash
# 1. Check database connection
curl http://localhost:8080/actuator/health

# Expected: { "status": "UP" }

# 2. Verify bulk payment tables
curl http://localhost:8080/api/bulk-payments/history?limit=1

# Expected: 200 OK response (may be empty if no batches yet)

# 3. Check Swagger documentation
# Visit: http://localhost:8080/swagger-ui.html
# Should see all /api/bulk-payments endpoints listed
```

### Frontend Verification
```bash
# 1. Check application loads
# Visit: http://localhost:3000

# 2. Navigate to bulk payments
# Sidebar → Payments → Bulk Payments (or /payments/bulk)

# 3. Verify two modes available
# - CSV Upload tab
# - Manual Entry tab
```

---

## Deployment to Production

### Option 1: Traditional Deployment (VPS/On-Premise)

```bash
# 1. Build everything
mvn clean install -DskipTests
cd frontend && npm run build && cd ..

# 2. Copy JAR to server
scp backend/target/ctrl_pay-0.0.1-SNAPSHOT.jar \
    user@server:/opt/app/ctrl-pay/

# 3. Stop current application
ssh user@server 'systemctl stop ctrl-pay'

# 4. Update frontend build
scp -r frontend/build/* \
    user@server:/var/www/html/ctrl-pay/

# 5. Start application
ssh user@server 'systemctl start ctrl-pay'

# 6. Verify
curl https://your-domain.com/api/bulk-payments/history
```

### Option 2: Docker Container Deployment

```bash
# From root directory
docker build -f backend/Dockerfile -t ctrl-pay:latest .
docker push ctrl-pay:latest

# On server:
docker pull ctrl-pay:latest
docker-compose up -d

# Verify
docker ps | grep ctrl-pay
docker logs -f ctrl-pay_backend_1
```

### Option 3: Kubernetes Deployment

```bash
# 1. Build and push images
docker build -f backend/Dockerfile -t ctrl-pay-backend:latest .
docker build -f frontend/Dockerfile -t ctrl-pay-frontend:latest .
docker push ctrl-pay-backend:latest
docker push ctrl-pay-frontend:latest

# 2. Deploy to Kubernetes
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/mysql.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/frontend.yaml

# 3. Verify
kubectl get pods -n ctrl-pay
kubectl get svc -n ctrl-pay
```

---

## Troubleshooting Common Issues

### Issue 1: Database Connection Error

**Error:**
```
SQLException: Access denied for user 'root'@'localhost'
```

**Fix:**
1. Verify MySQL is running: `systemctl status mysql`
2. Check credentials in application.properties
3. Verify database exists: `mysql -u root -p -e "SHOW DATABASES;"`
4. Recreate if needed:
   ```sql
   CREATE DATABASE ctrl_pay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   GRANT ALL PRIVILEGES ON ctrl_pay.* TO 'root'@'localhost';
   ```

### Issue 2: React Module Not Found

**Error:**
```
Module not found: Can't resolve '@mui/material'
```

**Fix:**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm start
```

### Issue 3: Port Already in Use

**Error:**
```
Port 8080 is already in use
```

**Fix:**
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port (in application.properties)
server.port=8081
```

### Issue 4: Maven Build Failure

**Error:**
```
[ERROR] BUILD FAILURE
```

**Fix:**
```bash
# Clear cache and rebuild
mvn clean
rm -rf ~/.m2/repository/com/neueda
mvn install -DskipTests

# Check Java version
java -version # Ensure Java 17+
```

---

## Rollback Procedure

If issues arise after deployment:

```bash
# 1. Revert to previous version
docker-compose down
docker-compose up -d  # Uses previous version

# 2. Database rollback (if needed)
# Stop application
systemctl stop ctrl-pay

# Backup current tables
mysqldump ctrl_pay bulk_payment_batches > backup_batches.sql
mysqldump ctrl_pay bulk_payment_items > backup_items.sql

# Drop problem tables (if necessary)
mysql -u root -p ctrl_pay << 'EOF'
DROP TABLE bulk_payment_audit_events;
DROP TABLE bulk_payment_error_log;
DROP TABLE bulk_payment_items;
DROP TABLE bulk_payment_batches;
EOF

# 3. Restart with previous code
systemctl start ctrl-pay

# 4. Restore original functionality
# Application will work without bulk payment feature
```

---

## Performance Tuning

### Database Optimization
```sql
-- Add connection pooling
-- In application.properties:
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

-- Monitor slow queries
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- Analyze query execution
EXPLAIN SELECT * FROM bulk_payment_batches WHERE status = 'PROCESSING';
```

### Java Heap Memory
```bash
# In application startup:
java -Xmx2g -Xms1g -jar ctrl_pay-0.0.1-SNAPSHOT.jar

# Or in docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g
```

### Frontend Performance
```bash
# Analyze bundle size
npm run build
npm install -g analyze-bundle
analyze-bundle build/

# Consider code splitting for BulkPayments component
# In App.jsx use React.lazy() for lazy loading
```

---

## Monitoring & Logs

### Application Logs
```bash
# View logs (Docker)
docker logs -f ctrl-pay_backend_1

# View logs (File system)
tail -f /var/log/ctrl-pay/app.log

# Search for bulk payment errors
grep "BulkPayment" /var/log/ctrl-pay/app.log | tail -100
```

### Database Logs
```sql
-- Check for slow queries
SELECT * FROM mysql.slow_log LIMIT 20;

-- Monitor for locking issues
SHOW PROCESSLIST;

-- Check replication (if applicable)
SHOW SLAVE STATUS\G
```

### System Metrics
```bash
# Monitor resource usage
top -b -n 1 | grep java

# Check disk space
df -h

# Check memory
free -h

# Check network connections
netstat -an | grep 8080
```

---

## Backup & Recovery

### Daily Backup

```bash
#!/bin/bash
# backup.sh - Run via cron daily

BACKUP_DIR="/backups/ctrl-pay"
DB_NAME="ctrl_pay"
DATE=$(date +%Y%m%d_%H%M%S)

# Backup database
mysqldump -u root -p$DB_PASSWORD $DB_NAME \
  > $BACKUP_DIR/ctrl_pay_$DATE.sql

# Backup application configuration
tar -czf $BACKUP_DIR/config_$DATE.tar.gz \
  /opt/app/ctrl-pay/application.properties

# Cleanup old backups (keep 7 days)
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
```

### Restore from Backup

```bash
# Restore database
mysql -u root - p$DB_PASSWORD ctrl_pay < ctrl_pay_20260805_120000.sql

# Restore configuration
tar -xzf config_20260805_120000.tar.gz -C /

# Restart application
systemctl restart ctrl-pay
```

---

## Support & Troubleshooting

### Get Help
1. Check logs: `/var/log/ctrl-pay/app.log`
2. Check Swagger: `http://localhost:8080/swagger-ui.html`
3. Review implementation guide: `BULK_PAYMENTS_IMPLEMENTATION.md`
4. Review API testing: `BULK_PAYMENTS_API_TESTING.md`

### Report Issues
Include in issue report:
- Error message (full stack trace)
- Steps to reproduce
- Database state (output of SELECT queries)
- Application logs (250 lines around error)
- System info (Java version, MySQL version, OS)

---

## Maintenance Schedule

**Weekly:**
- Review slow query log
- Monitor disk space usage
- Check application memory consumption

**Monthly:**
- Analyze bulk payment performance metrics
- Review error logs for patterns
- Update dependencies (security patches)

**Quarterly:**
- Full database backup and restore test
- Performance load testing
- Update Swagger documentation

---

**Last Updated:** August 5, 2026  
**Installation Status:** Ready ✅

