# Fraud Detection ML Service

## 📁 Project Structure

```
ml_fraud-detection/
│
└── payment-fraud-detection-main/
    │
    ├── app.py                         # Flask REST API service
    │
    ├── XGBoostModel.pkl               # Trained XGBoost fraud detection model
    │
    ├── notebook/
    │   └── paysim.ipynb               # Model training and experimentation notebook
    │
    ├── templates/                     # Flask HTML templates
    │
    ├── requirements.txt               # Python package dependencies
    │
    ├── README.md                      # Documentation
    │
    └── venv/                          # Python virtual environment (not committed)
```

---

# 📌 Overview

The **Fraud Detection ML Service** is a Machine Learning microservice integrated with the **Ctrl+Pay Payment Processing System**.

The service uses a trained **XGBoost classification model** to analyze transaction details and predict the probability of fraudulent activity.

The ML service runs independently as a **Flask REST API** and provides real-time fraud risk scores after successful payment completion.

The generated fraud probability is used for:
- Risk monitoring
- Payment history visualization
- Suspicious transaction identification

The service does **not block or reject payments**.

---

# 🚀 Features

✅ Machine Learning based fraud prediction  
✅ XGBoost classification model  
✅ Flask REST API integration  
✅ JSON-based prediction endpoint  
✅ Real-time fraud probability calculation  
✅ Returns:

- 🔴 Fraud probability
- 🟢 Legitimate probability

---

# 🛠️ Tech Stack

## Machine Learning

| Technology | Purpose |
|------------|---------|
| Python | ML service development |
| XGBoost | Fraud classification model |
| Scikit-learn | Data preprocessing and ML utilities |
| Pandas | Data processing |
| NumPy | Numerical operations |

---

## API Service

| Technology | Purpose |
|------------|---------|
| Flask | REST API framework |
| Flask-CORS | Frontend API communication |

---

## Model Information

**Algorithm:**

```
XGBoost Classifier
```

**Training Dataset:**

```
PaySim Financial Transaction Dataset
```

The model predicts whether a transaction is potentially fraudulent based on transaction-level features.

---

# 📦 Requirements

## Python Version

```
Python 3.10+
```

---

## Install Dependencies

```bash
pip install -r requirements.txt
```

---

## Main Dependencies

```
Flask
flask-cors
pandas
numpy
scikit-learn
xgboost
joblib
```

---

# ⚙️ Setup Instructions

## 1. Clone Repository

```bash
git clone <repository-url>
```

Navigate to the ML service:

```bash
cd payment-fraud-detection-main
```

---

## 2. Create Virtual Environment

Windows:

```bash
python -m venv venv
```

Activate:

```powershell
.\venv\Scripts\Activate
```

After activation:

```
(venv)
```

---

## 3. Install Dependencies

```bash
pip install -r requirements.txt
```

---

# ▶️ Running the ML Service

Start Flask API:

```bash
python app.py
```

The service will start at:

```
http://127.0.0.1:5000
```

---

# 🔌 API Documentation

## Fraud Prediction API

### Endpoint

```
POST /predict-json
```


### Request Example

```json
{
    "amount": 250,
    "source_account": "123456789102",
    "destination_account": "123456789101",
    "currency": "INR",
    "transaction_type": "TRANSFER"
}
Response Example
{
    "fraud_probability": 2.5,
    "legitimate_probability": 97.5
}
```

# Integration Flow

Payment flow:

```
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
```

---

# Model Information

## Model

```
XGBoost Classifier
```

## Dataset

```
PaySim Financial Transaction Dataset
```

The model predicts whether a transaction is potentially fraudulent based on transaction features.

---

# Important Notes

- The ML service only provides fraud risk information.
- It does not block or reject payments.
- Payment workflow remains unchanged.
- The fraud score is used for risk visualization.

---

# Development

Run Flask in development mode:

```bash
python app.py
```

For production deployment, use a production WSGI server.

---

# Git Ignore Configuration

```gitignore
venv/
__pycache__/
*.pyc
.env
.ipynb_checkpoints/
```