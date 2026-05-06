from fastapi import FastAPI
import joblib
import pandas as pd
import numpy as np
from datetime import datetime

app = FastAPI()

# ==========================================
# LOAD MODELS
# ==========================================
model = joblib.load("fraud_model.pkl")

anomaly_model = joblib.load("anomaly_model.pkl")

scaler = joblib.load("scaler.pkl")

# ==========================================
# FEATURE COLUMNS
# MUST MATCH TRAINING ORDER
# ==========================================
FEATURE_COLUMNS = [
    "amount",
    "is_international",
    "is_night",
    "is_weekend",
    "velocity_5min",
    "is_new_merchant",
    "account_age_days",
    "failed_login_attempts"
]

# ==========================================
# HOME API
# ==========================================
@app.get("/")
def home():
    return {
        "message": "Fraud AI Model Running 🚀"
    }

# ==========================================
# FRAUD PREDICTION API
# ==========================================
@app.post("/predict")
def predict(data: dict):

    try:

        # ==========================================
        # INPUT VALIDATION
        # ==========================================
        features = data.get("features")

        if not features:
            return {
                "error": "Features missing"
            }

        if len(features) != len(FEATURE_COLUMNS):
            return {
                "error": f"Expected {len(FEATURE_COLUMNS)} features"
            }

        # ==========================================
        # CONVERT TO DATAFRAME
        # ==========================================
        df = pd.DataFrame(
            [features],
            columns=FEATURE_COLUMNS
        )

        # ==========================================
        # SCALE FEATURES
        # ==========================================
        scaled = scaler.transform(df)

        scaled_df = pd.DataFrame(
            scaled,
            columns=FEATURE_COLUMNS
        )

        # ==========================================
        # LIGHTGBM FRAUD SCORE
        # ==========================================
        ml_score = model.predict_proba(
            scaled_df
        )[0][1]

        # ==========================================
        # ISOLATION FOREST ANOMALY SCORE
        # ==========================================
        anomaly_raw = anomaly_model.decision_function(
            scaled
        )[0]

        # ------------------------------------------
        # positive = normal
        # negative = anomaly
        # ------------------------------------------
        anomaly_score = max(
            0,
            min(1, -anomaly_raw)
        )

        # ==========================================
        # HYBRID FINAL SCORE
        # ==========================================
        final_score = (
                (0.7 * ml_score)
                +
                (0.3 * anomaly_score)
        )

        # ==========================================
        # RISK LEVEL
        # ==========================================
        if final_score >= 0.85:
            risk_level = "HIGH"

        elif final_score >= 0.5:
            risk_level = "MEDIUM"

        else:
            risk_level = "LOW"

        # ==========================================
        # AI EXPLAINABILITY
        # ==========================================
        reasons = []

        amount = features[0]
        is_international = features[1]
        velocity = features[4]
        failed_logins = features[7]

        if amount > 100000:
            reasons.append(
                "Very high transaction amount"
            )

        if is_international == 1:
            reasons.append(
                "International transaction detected"
            )

        if velocity >= 8:
            reasons.append(
                "Rapid repeated transactions detected"
            )

        if failed_logins >= 3:
            reasons.append(
                "Multiple failed login attempts"
            )

        if anomaly_score > 0.6:
            reasons.append(
                "Behavior deviates from normal pattern"
            )

        if ml_score > 0.9:
            reasons.append(
                "ML model confidence extremely high"
            )

        if not reasons:
            reasons.append(
                "Transaction behavior appears normal"
            )

        # ==========================================
        # TERMINAL LOGS
        # ==========================================
        print("\n========== AI FRAUD ANALYSIS ==========")

        print(
            "Timestamp:",
            datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        )

        print("Input Features:", features)

        print(
            "ML Score:",
            round(ml_score, 4)
        )

        print(
            "Anomaly Score:",
            round(anomaly_score, 4)
        )

        print(
            "Final AI Score:",
            round(final_score, 4)
        )

        print(
            "Risk Level:",
            risk_level
        )

        print("Reasons:")

        for reason in reasons:
            print("-", reason)

        print("======================================\n")

        # ==========================================
        # RESPONSE
        # ==========================================
        return {

            "fraud_score":
                round(float(final_score), 4),

            "ml_score":
                round(float(ml_score), 4),

            "anomaly_score":
                round(float(anomaly_score), 4),

            "risk_level":
                risk_level,

            "reasons":
                reasons
        }

    except Exception as e:

        print("ERROR:", str(e))

        return {
            "error": str(e)
        }