# Linux Deployment with Jenkins

This project supports Linux server deployment using Docker Compose and Jenkins.

## What is included

- `Jenkinsfile` - CI + deploy pipeline
- `docker-compose.yml` - runtime orchestration

## Jenkins agent/server prerequisites

Install on the Jenkins Linux node that runs the job:

- Docker Engine
- Docker Compose plugin (`docker compose` command)
- Java 17+ (for backend build)
- Node.js 20+ (for frontend build)
- Python is validated inside Docker in the pipeline, so host `pip` is not required

Also ensure Jenkins user can run Docker commands.

## Required server files

No `.env` file is required. Runtime values are defined directly in `docker-compose.yml`.

## Public access rule

Deployment rule: only port `8080` is externally accessible.

Port mapping is defined directly in `docker-compose.yml` as `8080:80`.

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


