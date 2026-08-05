# Ctrl-Pay Payment Processing System

A full-stack payment processing platform with fraud detection, built with React, Spring Boot, MySQL, a Flask ML service, Docker Compose, Nginx, and Jenkins.

- **Backend:** Spring Boot 4.0.7 (Java 17), Spring Web, Spring JDBC, MySQL
- **Frontend:** React 18 + CRA
- **API Docs:** springdoc OpenAPI / Swagger UI
- **Fraud Detection:** Flask + XGBoost model inference service
- **Containerization:** Docker + Docker Compose

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start (Docker Compose)](#quick-start-docker-compose)
- [Deployment Access (Linux VM)](#deployment-access-linux-vm)
- [Local Development (Without Docker)](#local-development-without-docker)
- [Configuration](#configuration)
- [Database Schema and Seed Data](#database-schema-and-seed-data)
- [API Reference](#api-reference)
- [Frontend Notes](#frontend-notes)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

## Overview

Ctrl-Pay manages the lifecycle of payment transactions:

1. Create a payment
2. Validate it against business rules
3. Process it through the settlement flow
4. Complete or fail the payment
5. Record an audit trail for compliance and traceability

The frontend provides the user interface for payment operations, the backend exposes REST APIs, the MySQL database stores transactional state, and the ML service provides fraud probability scoring that the backend can consult during processing.

## Architecture

```text
+--------------+--------------------------+------+----------------------------------------------+
| Layer        | Component                | Port | Responsibility                               |
+--------------+--------------------------+------+----------------------------------------------+
| Frontend     | ctrl-pay-frontend        | 8081 | Serves React UI through Nginx                |
| Proxy        | Nginx (frontend)         | 80   | Proxies /api, /ml, /actuator, Swagger paths  |
| Backend API  | ctrl-pay-backend         | 8082 | Exposes payment REST endpoints               |
| ML Service   | ctrl-pay-ml              | 8083 | Fraud prediction inference                   |
| Database     | ctrl-pay-mysql (MySQL 8) | 3306 | Persists payments, history, rules, audits    |
| Jenkins      | Jenkins                   | 8080 | CI/CD pipeline execution                     |
+--------------+--------------------------+------+----------------------------------------------+
```

Request flow:

```text
[ Browser :8081 ]
        |
        | GET /api/payments
        v
[ Nginx (ctrl-pay-frontend) ]
        |
        | proxy_pass /api/*
        v
[ Spring Boot API :8082 ] -----> [ MySQL :3306 ]
        |
        | fraud scoring request
        v
[ Flask ML Service :8083 ]
```

## Tech Stack

```text
+-----------+--------------------------------------+---------+--------------------------------------+
| Layer     | Technology                           | Version | Notes                                |
+-----------+--------------------------------------+---------+--------------------------------------+
| Backend   | Java                                 | 17      | Runtime language                     |
| Backend   | Spring Boot                          | 4.0.7   | Application framework                |
| Backend   | Spring Web                           | Managed | REST controllers                     |
| Backend   | Spring JDBC                          | Managed | Data access layer                    |
| Backend   | MySQL Connector/J                    | Managed | MySQL JDBC driver                    |
| Backend   | springdoc-openapi-starter-webmvc-ui  | 2.8.14  | OpenAPI + Swagger UI                 |
| Frontend  | React                                | 18      | UI framework                         |
| Frontend  | CRA / react-scripts                  | 5.x     | Build tooling                        |
| ML        | Flask                                | 3.x     | Inference API                        |
| ML        | XGBoost / scikit-learn / pandas      | 2.x/1.x | Fraud model runtime dependencies     |
| Container | Docker / Docker Compose              | N/A     | Multi-service local environment      |
+-----------+--------------------------------------+---------+--------------------------------------+
```

## Prerequisites

Install these tools:

- Java 17+
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- Docker Desktop or Docker Engine
- Docker Compose

## Quick Start (Docker Compose)

This is the easiest way to run the entire platform.

```bash
docker compose up --build
```

Services started:

- `ctrl-pay-mysql` (MySQL): `3306` internal
- `ctrl-pay-backend` (API): `8082` internal
- `ctrl-pay-ml` (fraud scoring): `8083` internal
- `ctrl-pay-frontend` (UI via Nginx): `8081` public
- Jenkins (separate VM service): `8080`

Open:

- Frontend: `http://localhost:8081`
- API base: `http://localhost:8081/api/payments`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Stop services:

```bash
docker compose down
```

Stop and remove volumes (fresh DB):

```bash
docker compose down -v
```

## Deployment Access (Linux VM)

After deployment, access services through the VM IP:

```powershell
$env:VM_IP="10.9.72.215"
```

Access URLs:

- Frontend: `http://10.9.72.215:8081`
- Swagger UI: `http://10.9.72.215:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://10.9.72.215:8081/v3/api-docs`
- Jenkins: `http://10.9.72.215:8080`

## Local Development (Without Docker)

### 1) Start MySQL and create database

Create the `ctrl_pay` database locally if you are not using Docker.

### 2) Run backend

From `backend`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 3) Run frontend

From `frontend`:

```bash
npm install
npm start
```

### 4) Run ML service

From `ml_fraud-detection/payment-fraud-detection-main`:

```bash
pip install -r requirements.txt
flask --app app run --host=0.0.0.0 --port=8083
```

## Configuration

Main config files:

- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-dev.properties`
- `backend/src/main/resources/application-docker.properties`
- `backend/src/main/resources/application-prod.properties`
- `frontend/src/services/api.js`
- `frontend/nginx.conf`
- `docker-compose.yml`

Important settings:

- `spring.sql.init.mode=always` loads `schema.sql` at startup.
- Docker backend port is `8082`.
- Docker ML service port is `8083`.
- Nginx serves the UI on `8081` and proxies `/api`, `/ml`, `/actuator`, and Swagger routes.

## Database Schema and Seed Data

Schema file:

- `backend/src/main/resources/schema.sql`

Core tables:

- `customers`
- `accounts`
- `payments`
- `payment_status_history`
- `validation_rules`
- `validation_results`
- `payment_retry_attempts`

Additional Phase 1 tables:

- `ml_models`
- `ml_model_predictions`
- `fraud_audit_events`

The backend seeds validation rules and other bootstrap data during startup when required.

## API Reference

Base URL: `/api`

Common endpoints:

```text
POST   /api/payments                    Create new payment
GET    /api/payments/{id}               Get payment details
GET    /api/payments                    List payments with filtering/pagination
POST   /api/payments/{id}/validate      Validate payment
POST   /api/payments/{id}/send          Send payment
POST   /api/payments/{id}/complete      Mark payment complete
POST   /api/payments/{id}/fail          Mark payment failed
GET    /api/payments/{id}/audit         Get audit trail
GET    /api/payments/{id}/history       Get status history
GET    /api/payments/{id}/validations   Get validation results
```

Admin and analytics endpoints:

```text
GET    /api/admin/validation-rules
POST   /api/admin/validation-rules
PUT    /api/admin/validation-rules/{id}
PATCH  /api/admin/validation-rules/{id}/toggle
POST   /api/admin/validation-rules/{id}/test-dry-run
GET    /api/analytics/success-rate
GET    /api/analytics/status-distribution
GET    /api/analytics/volume
GET    /api/analytics/trends
```

ML inference endpoint:

```text
POST   /predict-json
```

Swagger UI is available through the frontend Nginx route:

```text
/swagger-ui/index.html
```

## Frontend Notes

- API helper is in `frontend/src/services/api.js`.
- The app uses Nginx to proxy API requests to the backend.
- The frontend routes include dashboard, payments, payment details, create payment, rules management, analytics, customer profile, and fraud dashboard pages.
- For containerized runs, the frontend should call relative paths like `/api` and `/ml`.

## Testing

Run backend tests:

```bash
cd backend
./mvnw test
```

Build backend artifact:

```bash
cd backend
./mvnw clean package -DskipTests
```

Run frontend tests/build:

```bash
cd frontend
npm test
npm run build
```

Validate ML service imports/model loading:

```bash
cd ml_fraud-detection/payment-fraud-detection-main
pip install -r requirements.txt
python -u -c "import pickle, xgboost, sklearn, numpy, pandas"
```

Docker Compose validation:

```bash
docker compose config -q
```

## Troubleshooting

- **Frontend cannot reach backend:** confirm nginx is proxying `/api` to `backend:8082` and the frontend bundle uses `/api` as the base URL.
- **CORS errors:** nginx handles preflight and strips the `Origin` header before forwarding to the backend.
- **Backend connection failed:** ensure the backend is running on `8082` and MySQL is healthy.
- **ML service errors:** ensure `XGBoostModel.pkl` exists and the ML service is running on `8083`.
- **Port conflict:** frontend uses `8081`, Jenkins uses `8080`, backend/ML stay internal.
- **Database schema issues:** confirm `schema.sql` is mounted and MySQL volume is clean if you need a fresh start.

## CI/CD

### GitHub Actions

Workflow: `.github/workflows/ci.yml`

Pipeline stages:

1. Backend: install dependencies, run tests, build artifact
2. Frontend: install dependencies, run tests, build production bundle
3. ML: install dependencies, verify imports, validate model loading
4. Docker: build images and validate compose configuration

### Jenkins (Linux Server)

`Jenkinsfile` runs the Linux deployment pipeline:

1. Checkout source
2. Build and test backend/frontend/ML components
3. Stop existing containers
4. Build Docker images
5. Deploy with Docker Compose
6. Verify running containers

Deployment notes are in `docs/LINUX_JENKINS_DEPLOYMENT.md`.

## Project Structure

```text
+--------------------------------------------------+------------------------------------------+
| Path                                             | Purpose                                  |
+--------------------------------------------------+------------------------------------------+
| backend/src/main/java/com/neueda/controller/     | REST endpoints                           |
| backend/src/main/java/com/neueda/service/        | Business logic                           |
| backend/src/main/java/com/neueda/repository/     | JDBC repositories                        |
| backend/src/main/java/com/neueda/domain/         | Domain models                            |
| backend/src/main/java/com/neueda/fraud/          | ML model registry / fraud logic          |
| backend/src/main/resources/schema.sql             | DB schema init                           |
| frontend/src/                                     | React application source                 |
| frontend/src/services/api.js                      | API client                               |
| frontend/nginx.conf                               | Nginx reverse proxy                      |
| ml_fraud-detection/payment-fraud-detection-main/  | Flask ML inference service               |
| docker-compose.yml                                | Multi-service container orchestration     |
| backend/Dockerfile                                | Backend container image                  |
| frontend/Dockerfile                               | Frontend container image                 |
| Jenkinsfile                                       | Jenkins CI/CD pipeline                   |
| docs/LINUX_JENKINS_DEPLOYMENT.md                  | Linux VM deployment guide                |
+--------------------------------------------------+------------------------------------------+
```
