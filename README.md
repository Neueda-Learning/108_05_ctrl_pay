# Ctrl-Pay DevOps Setup

This repository now includes a production-ready container setup for the existing Ctrl-Pay stack without changing application logic, APIs, schemas, or package names.

## Architecture Overview

Runtime services are split by responsibility:

- `frontend`: Nginx container that serves the React production build and reverse proxies internal APIs
- `backend`: Spring Boot REST API (`/api`, `/actuator`)
- `ml`: Flask inference service (`/predict-json`)
- `mysql`: persistent MySQL database

Traffic flow:

1. Browser calls `http://<host>:<NGINX_PORT>`
2. `frontend` serves static frontend files
3. `frontend` proxies:
   - `/api/*` and `/actuator/*` -> `backend:8080`
   - `/ml/*` -> `ml:5000`
4. `backend` talks to `mysql:3306`
5. `backend` calls ML service via `FRAUD_API_BASE_URL` (default `http://ml:5000`)

## Service List

- `frontend` (public): host port `${NGINX_PORT:-80}` -> container `80`
- `backend` (internal): `8080`
- `ml` (internal): `5000`
- `mysql` (internal): `3306`

Only `frontend` is exposed publicly by default.

## Runtime Configuration

Runtime values are defined directly in `docker-compose.yml` (no `.env` file required).

## Docker Usage

### Build and run all services

```bash
docker compose up --build
```

### Run in detached mode

```bash
docker compose up -d --build
```

### Check status and logs

```bash
docker compose ps
docker compose logs -f frontend
docker compose logs -f backend
docker compose logs -f ml
docker compose logs -f mysql
```

### Stop services

```bash
docker compose down
```

### Stop and remove DB volume

```bash
docker compose down -v
```

## VM Access (Linux Host)

After deployment, access services through the VM IP. Example:

```powershell
$env:VM_IP="10.9.71.48"
```

Deployment rule: only port `8080` is externally accessible.

- Frontend: `http://${VM_IP}:8080`
- Swagger UI: `http://${VM_IP}:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://${VM_IP}:8080/v3/api-docs`

If Jenkins is also routed through the same external gateway, it should be exposed by your server/network team on that same public endpoint.

## Development Workflow

### Backend (without Docker)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Frontend (without Docker)

```bash
cd frontend
npm install
npm start
```

### ML service (without Docker)

```bash
cd ml_fraud-detection/payment-fraud-detection-main
pip install -r requirements.txt
flask --app app run --host=0.0.0.0 --port=5000
```

## CI Pipeline (GitHub Actions)

Workflow: `.github/workflows/ci.yml`

Pipeline stages:

1. Backend: install dependencies, run tests, build artifact
2. Frontend: install dependencies, run tests, build production bundle
3. ML: install dependencies, verify imports, validate model loading
4. Docker: build all images and validate compose configuration

No cloud deployment step is included.

## Jenkins Pipeline (Linux Server)

A Jenkins pipeline is included at `Jenkinsfile` for Linux-host deployment using Docker Compose.

It follows the same method pattern as the kk-04 reference pipeline:

1. Checkout source
2. Build and test backend/frontend/ML components
3. Stop existing containers (`docker-compose down || true`)
4. Build Docker images (`docker-compose build --no-cache`)
5. Deploy (`docker-compose up -d`)
6. Verify (`docker ps`, `docker-compose ps`)

Deployment setup details are in `docs/LINUX_JENKINS_DEPLOYMENT.md`.

## Infra File Map

- `docker-compose.yml` - orchestration for `mysql`, `backend`, `ml`, and `frontend`
- `backend/Dockerfile` - backend build/runtime image
- `frontend/Dockerfile` - frontend build + Nginx runtime image
- `frontend/nginx.conf` - SPA fallback and reverse proxy rules
- `ml_fraud-detection/payment-fraud-detection-main/Dockerfile` - ML service image
- `.github/workflows/ci.yml` - CI checks for backend/frontend/ML and Docker validation
- `Jenkinsfile` - Linux Jenkins CI/CD and deployment pipeline
- `docs/LINUX_JENKINS_DEPLOYMENT.md` - Linux server deployment instructions




