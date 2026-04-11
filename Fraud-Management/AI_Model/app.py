from fastapi import FastAPI
import joblib
import numpy as np
import pandas as pd

app = FastAPI()

# 🔥 Load model + scaler
model = joblib.load("fraud_model.pkl")
scaler = joblib.load("scaler.pkl")

# 🔥 Feature names (MUST match training)
FEATURE_COLUMNS = [
    "amount",
    "is_international",
    "is_night",
    "velocity_5min",
    "is_new_merchant",
    "account_age_days",
    "failed_login_attempts"
]


@app.get("/")
def home():
    return {"message": "Fraud AI Model Running 🚀"}


@app.post("/predict")
def predict(data: dict):
    try:
        features = data.get("features")

        # 🔥 Basic validation
        if not features or len(features) != len(FEATURE_COLUMNS):
            return {
                "error": f"Expected {len(FEATURE_COLUMNS)} features"
            }

        # 🔥 Convert to DataFrame (removes warning)
        df = pd.DataFrame([features], columns=FEATURE_COLUMNS)

        # 🔥 Scale
        scaled = scaler.transform(df)

        # 🔥 Predict
        prob = model.predict_proba(scaled)[0][1]

        # 🔥 Optional debug log
        print("Input:", features)
        print("Fraud Score:", prob)

        return {
            "fraud_score": float(round(prob, 4))
        }

    except Exception as e:
        return {
            "error": str(e)
        }