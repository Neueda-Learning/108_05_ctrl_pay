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
                    sh 'npm install --no-audit --no-fund'
                    sh 'CI=true npm test -- --watch=false --passWithNoTests'
                    sh 'npm run build'
                }
            }
        }

        stage('ML Build and Validation') {
            steps {
                dir('ml_fraud-detection/payment-fraud-detection-main') {
                    sh 'python3 -m pip install -r requirements.txt'
                    sh 'python3 -u -c "import pickle, xgboost, sklearn, numpy, pandas"'
                    sh 'python3 -u -c "from pathlib import Path; p=Path(\"XGBoostModel.pkl\"); assert p.exists(), \"XGBoostModel.pkl not found\"; pickle.load(p.open(\"rb\")); print(\"Model loaded\")"'
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



