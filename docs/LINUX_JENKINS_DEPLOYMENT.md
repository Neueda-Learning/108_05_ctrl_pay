# Linux Deployment with Jenkins

This project supports Linux server deployment using Docker Compose and Jenkins.

## What is included

- `Jenkinsfile` - CI + deploy pipeline
- `docker-compose.yml` - runtime orchestration
- `.env.example` - environment template (copy to `.env` on server)

## Jenkins agent/server prerequisites

Install on the Jenkins Linux node that runs the job:

- Docker Engine
- Docker Compose plugin (`docker compose` command)
- Java 17+ (for backend build)
- Node.js 20+ (for frontend build)
- Python 3.11+ and `pip` (for ML validation)

Also ensure Jenkins user can run Docker commands.

## Required server files

Create `.env` in repository root on the Linux server before deploy.

Minimum required variables:

```dotenv
TZ=UTC
NGINX_PORT=80
MYSQL_DATABASE=ctrl_pay
MYSQL_ROOT_PASSWORD=<strong-secret>
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<strong-secret>
JAVA_OPTS=-Xmx768m -Xms256m
FRAUD_API_BASE_URL=http://ml:5000
REACT_APP_API_URL=/api
REACT_APP_FRAUD_API_URL=/ml
```

## Public access rule

Deployment rule: only port `8080` is externally accessible.

Set in `.env`:

```dotenv
NGINX_PORT=8080
```

Access via VM IP:

```powershell
$env:VM_IP="10.9.71.48"
```

- Frontend: `http://${VM_IP}:8080`
- Swagger UI: `http://${VM_IP}:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://${VM_IP}:8080/v3/api-docs`

Jenkins exposure on the same endpoint depends on your Linux host/reverse-proxy/network configuration.

## Jenkins pipeline stages (same method as kk-04 reference)

1. Checkout source
2. Build/test backend (`mvn test`, then package)
3. Build/test frontend (`npm test`, then build)
4. Validate ML dependencies/model load
5. Stop existing containers (`docker-compose down || true`)
6. Build Docker images (`docker-compose build --no-cache`)
7. Deploy (`docker-compose up -d`)
8. Verify (`docker ps` and `docker-compose ps`)

## First deployment (manual baseline)

```bash
cd /opt/ctrl-pay
cp .env.example .env
# Edit .env with production secrets

docker-compose up -d --build
```

## Common operations

```bash
# Container status
docker-compose ps

# Logs by service
docker-compose logs -f frontend
docker-compose logs -f backend
docker-compose logs -f ml
docker-compose logs -f mysql

# Restart stack
docker-compose down
docker-compose up -d --build
```


