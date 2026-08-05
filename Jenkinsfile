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
  npm install --no-audit --no-fund
  CI=true npm test -- --watch=false --passWithNoTests
  npm run build
else
  docker run --rm -v "$PWD":/app -w /app node:20-alpine sh -lc "npm install --no-audit --no-fund && CI=true npm test -- --watch=false --passWithNoTests && npm run build"
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
                sh 'test -f .env || (echo ".env file is required on Jenkins/Linux host" && exit 1)'
                sh 'docker-compose down || true'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
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
}



