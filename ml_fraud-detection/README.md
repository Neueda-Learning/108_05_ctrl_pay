ml_fraud-detection/
└── payment-fraud-detection-main/
├── app.py              # Flask API
├── XGBoostModel.pkl    # trained model
├── notebook/
│   └── paysim.ipynb    # training notebook
├── templates/
├── requirements.txt
└── venv/



## Overview

This project provides a Machine Learning based fraud detection service integrated with the Ctrl+Pay payment processing system.

The service uses a trained XGBoost classification model to predict the probability of a transaction being fraudulent.

The ML service runs independently as a Flask REST API and provides fraud probability scores after successful payment completion.

---

# Features

- Fraud probability prediction using Machine Learning
- XGBoost classification model
- REST API using Flask
- JSON based prediction endpoint
- Returns:
    - Fraud probability
    - Legitimate probability

---

# Tech Stack

## Machine Learning
- Python
- XGBoost
- Scikit-learn
- Pandas
- NumPy

## Backend API
- Flask
- Flask-CORS

## Model
- XGBoost trained on PaySim transaction dataset

---

# Project Structure


payment-fraud-detection-main/

├── app.py
├── XGBoostModel.pkl
├── notebook/
│ └── paysim.ipynb
├── templates/
├── requirements.txt
└── README.md


---

# Requirements

Python version:


Python 3.10+


Install dependencies:


pip install -r requirements.txt


Main dependencies:


Flask
flask-cors
pandas
numpy
scikit-learn
xgboost
joblib


---

# Setup Instructions

## 1. Clone repository


git clone <repository-url>


Navigate:


cd payment-fraud-detection-main


---

## 2. Create virtual environment

Windows:


python -m venv venv


Activate:


.\venv\Scripts\Activate


---

## 3. Install dependencies


pip install -r requirements.txt


---

# Running the ML Service

Start Flask API:


python app.py


The service will start at:


http://127.0.0.1:5000


---

# API Documentation

## Fraud Prediction API

### Endpoint


POST /predict-json


### Request Example

```json
{
    "amount": 250,
    "transaction_type": "TRANSFER"
}
Response Example
{
    "fraud_probability": 2.5,
    "legitimate_probability": 97.5
}
Integration Flow

Payment flow:

User makes payment
        |
        |
Payment completed successfully
        |
        |
Frontend sends transaction details
        |
        |
ML Service (/predict-json)
        |
        |
Fraud probability returned
        |
        |
Displayed in Payment History
Model Information

Model:

XGBoost Classifier

Dataset:

PaySim Financial Transaction Dataset

The model predicts whether a transaction is potentially fraudulent based on transaction features.

Important Notes
The ML service only provides fraud risk information.
It does not block or reject payments.
Payment workflow remains unchanged.
The fraud score is used for risk visualization.
Development

Run Flask in development mode:

python app.py

For production deployment, use a production WSGI server.



 `.gitignore` contain:

```gitignore
venv/
__pycache__/
*.pyc
.env
.ipynb_checkpoints/