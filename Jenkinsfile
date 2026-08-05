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
docker run --rm -v "$PWD":/app -w /app python:3.11-slim sh -lc "pip install -r requirements.txt && python -u -c 'import pickle, xgboost, sklearn, numpy, pandas' && python -u -c 'from pathlib import Path; p=Path(\"XGBoostModel.pkl\"); assert p.exists(), \"XGBoostModel.pkl not found\"; pickle.load(p.open(\"rb\")); print(\"Model loaded\")'"
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



