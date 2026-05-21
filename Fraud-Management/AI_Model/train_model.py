import pandas as pd
import numpy as np
from lightgbm import LGBMClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.preprocessing import StandardScaler
import joblib
from sklearn.ensemble import IsolationForest

# ===============================
# STEP 1: LOAD DATA
# ===============================
np.random.seed(42)

data_size = 5000

df = pd.DataFrame({
    "amount": np.random.randint(100, 20000, data_size),
    "is_international": np.random.choice([0, 1], data_size),
    "hour": np.random.randint(0, 24, data_size),
    "day_of_week": np.random.randint(0, 7, data_size),  # NEW
    "velocity_5min": np.random.randint(1, 10, data_size),
    "is_new_merchant": np.random.choice([0, 1], data_size),
    "account_age_days": np.random.randint(1, 2000, data_size),
    "failed_login_attempts": np.random.randint(0, 5, data_size),
})

# ===============================
# STEP 2: FRAUD LABEL (simulate rules)
# ===============================
def generate_fraud_label(row):
    score = 0

    if row["amount"] > 10000:
        score += 2
    if row["is_international"] == 1:
        score += 2
    if row["hour"] < 5:
        score += 1
    if row["velocity_5min"] > 5:
        score += 2
    if row["is_new_merchant"] == 1:
        score += 1
    if row["failed_login_attempts"] > 2:
        score += 1
    if row["day_of_week"] >= 5:  # weekend
        score += 1

    return 1 if score >= 4 else 0

df["fraud"] = df.apply(generate_fraud_label, axis=1)

# ===============================
# STEP 3: FEATURE ENGINEERING
# ===============================

df["is_night"] = df["hour"].apply(lambda x: 1 if x < 5 else 0)
df["is_weekend"] = df["day_of_week"].apply(lambda x: 1 if x >= 5 else 0)

df.drop(columns=["hour", "day_of_week"], inplace=True)

# ===============================
# STEP 4: FEATURES (ALIGNED WITH RULES)
# ===============================
features = [
    "amount",
    "is_international",
    "is_night",
    "is_weekend",
    "velocity_5min",
    "is_new_merchant",
    "account_age_days",
    "failed_login_attempts"
]

X = df[features]
y = df["fraud"]

# ===============================
# STEP 5: SPLIT
# ===============================
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# ===============================
# STEP 6: SCALE
# ===============================
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# ===============================
# STEP 7:  LIGHTGBM MODEL
# ===============================
model = LGBMClassifier(
    n_estimators=120,
    learning_rate=0.08,
    max_depth=6
)

model.fit(X_train_scaled, y_train)

# ===============================
# STEP 7B:  ANOMALY MODEL
# ===============================
anomaly_model = IsolationForest(
    n_estimators=100,
    contamination=0.05,
    random_state=42
)

anomaly_model.fit(X_train_scaled)

# ===============================
# STEP 8: EVALUATION
# ===============================
y_pred = model.predict(X_test_scaled)
y_prob = model.predict_proba(X_test_scaled)[:, 1]

print("\nClassification Report:")
print(classification_report(y_test, y_pred))

print("ROC-AUC Score:", roc_auc_score(y_test, y_prob))

# ===============================
# STEP 9: SAVE
# ===============================
joblib.dump(model, "fraud_model.pkl")
joblib.dump(anomaly_model, "anomaly_model.pkl")
joblib.dump(scaler, "scaler.pkl")


print("\n Model saved as fraud_model.pkl")
print(" Scaler saved as scaler.pkl")

# ===============================

# STEP 9B: EXPORT TRAINED DATASET

# ===============================

df.to_csv("fraud_training_dataset.csv", index=False)

print(" Training dataset exported as fraud_training_dataset.csv")

# ===============================
# STEP 10: TEST
# ===============================
sample = np.array([[12000, 1, 1, 1, 7, 1, 200, 3]])
sample_scaled = scaler.transform(sample)

prob = model.predict_proba(sample_scaled)[0][1]

print("\n Sample Fraud Probability:", round(prob, 3))