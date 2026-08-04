from flask import Flask, jsonify, render_template, request
import pickle
from pathlib import Path

app = Flask(__name__)

# Load trained model
MODEL_PATH = Path(__file__).resolve().parent / "XGBoostModel.pkl"
with MODEL_PATH.open("rb") as model_file:
    model = pickle.load(model_file)


@app.route("/")
def home():
    return render_template("index.html")


@app.route("/predict", methods=["POST"])
def predict():

    amount = float(request.form["amount"])
    oldbalanceOrg = float(request.form["oldbalanceOrg"])
    newbalanceOrig = float(request.form["newbalanceOrig"])
    oldbalanceDest = float(request.form["oldbalanceDest"])
    newbalanceDest = float(request.form["newbalanceDest"])

    transaction_type = request.form["transaction_type"]

    # One-Hot Encoding
    type_CASH_OUT = 0
    type_DEBIT = 0
    type_PAYMENT = 0
    type_TRANSFER = 0

    if transaction_type == "CASH_OUT":
        type_CASH_OUT = 1
    elif transaction_type == "DEBIT":
        type_DEBIT = 1
    elif transaction_type == "PAYMENT":
        type_PAYMENT = 1
    elif transaction_type == "TRANSFER":
        type_TRANSFER = 1
    # CASH_IN remains all zeros

    features = [[
        amount,
        oldbalanceOrg,
        newbalanceOrig,
        oldbalanceDest,
        newbalanceDest,
        type_CASH_OUT,
        type_DEBIT,
        type_PAYMENT,
        type_TRANSFER
    ]]

    probability = model.predict_proba(features)[0]

    fraud_probability = probability[1] * 100
    legitimate_probability = probability[0] * 100

    return render_template(
        "index.html",
        fraud=round(fraud_probability, 2),
        safe=round(legitimate_probability, 2)
    )


@app.route("/predict-json", methods=["POST"])
def predict_json():
    payload = request.get_json(silent=True) or {}

    amount = float(payload["amount"])
    oldbalanceOrg = float(payload["oldbalanceOrg"])
    newbalanceOrig = float(payload["newbalanceOrig"])
    oldbalanceDest = float(payload["oldbalanceDest"])
    newbalanceDest = float(payload["newbalanceDest"])

    transaction_type = payload["transaction_type"]

    # One-Hot Encoding (same logic as /predict)
    type_CASH_OUT = 0
    type_DEBIT = 0
    type_PAYMENT = 0
    type_TRANSFER = 0

    if transaction_type == "CASH_OUT":
        type_CASH_OUT = 1
    elif transaction_type == "DEBIT":
        type_DEBIT = 1
    elif transaction_type == "PAYMENT":
        type_PAYMENT = 1
    elif transaction_type == "TRANSFER":
        type_TRANSFER = 1

    features = [[
        amount,
        oldbalanceOrg,
        newbalanceOrig,
        oldbalanceDest,
        newbalanceDest,
        type_CASH_OUT,
        type_DEBIT,
        type_PAYMENT,
        type_TRANSFER
    ]]

    probability = model.predict_proba(features)[0]
    fraud_probability = round(float(probability[1]) * 100, 2)
    legitimate_probability = round(float(probability[0]) * 100, 2)

    return jsonify({
        "fraud_probability": fraud_probability,
        "legitimate_probability": legitimate_probability
    })


if __name__ == "__main__":
    app.run(debug=True)