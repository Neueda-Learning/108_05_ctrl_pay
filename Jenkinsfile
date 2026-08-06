pipeline {

    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout Source') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build and Test') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw'
                    sh './mvnw -B test'
                    sh './mvnw -B package -DskipTests'
                }
            }
        }

        stage('Frontend Build and Test') {
            steps {
                dir('frontend') {
                    sh '''#!/bin/sh
if command -v npm >/dev/null 2>&1; then
  npm ci --no-audit --no-fund
  CI=true npm test -- --watch=false --passWithNoTests
  npm run build
else
  docker run --rm -v "$PWD":/app -w /app node:20-alpine sh -lc "npm ci --no-audit --no-fund && CI=true npm test -- --watch=false --passWithNoTests && npm run build"
fi
'''
                }
            }
        }

        stage('ML Build and Validation') {
            steps {
                dir('ml_fraud-detection/payment-fraud-detection-main') {
                    sh '''#!/bin/sh
docker run --rm -v "$PWD":/app -w /app python:3.11-slim sh -lc '
pip install -r requirements.txt
python -u - <<"PY"
import pickle
import xgboost  # noqa: F401
import sklearn  # noqa: F401
import numpy    # noqa: F401
import pandas   # noqa: F401
from pathlib import Path

model_path = Path("XGBoostModel.pkl")
assert model_path.exists(), "XGBoostModel.pkl not found"

with model_path.open("rb") as f:
    pickle.load(f)

print("Model loaded")
PY
'
'''
                }
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        stage('Docker Cleanup') {
            steps {
                sh '''#!/bin/sh
echo "=== Docker usage before cleanup ==="
docker system df || true
echo ""
echo "=== Removing all unused build cache ==="
docker builder prune -af || true
echo ""
echo "=== Removing dangling images ==="
docker image prune -f || true
echo ""
echo "=== Docker usage after cleanup ==="
docker system df || true
'''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --pull'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
                sh 'docker-compose ps'
            }
        }
    }

    post {
        always {
            sh '''#!/bin/sh
echo "=== Post-build cleanup ==="
docker builder prune -af || true
docker image prune -f || true
echo "=== Final Docker usage ==="
docker system df || true
'''
        }
    }
}


