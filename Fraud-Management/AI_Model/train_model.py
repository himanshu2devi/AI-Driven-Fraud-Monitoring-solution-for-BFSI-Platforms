import pandas as pd
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.preprocessing import StandardScaler
import joblib

# ===============================
# 🔥 STEP 1: LOAD DATA (SIMULATE DB)
# ===============================

# In real case → load from PostgreSQL
# Example:
# df = pd.read_sql("SELECT * FROM transactions", connection)

np.random.seed(42)

data_size = 5000

df = pd.DataFrame({
    "amount": np.random.randint(100, 20000, data_size),
    "is_international": np.random.choice([0, 1], data_size),
    "hour": np.random.randint(0, 24, data_size),
    "velocity_5min": np.random.randint(1, 10, data_size),
    "is_new_merchant": np.random.choice([0, 1], data_size),
    "account_age_days": np.random.randint(1, 2000, data_size),
    "failed_login_attempts": np.random.randint(0, 5, data_size),
})

# ===============================
# 🔥 STEP 2: CREATE FRAUD LABEL (SIMULATION LOGIC)
# ===============================

def generate_fraud_label(row):
    score = 0

    if row["amount"] > 10000:
        score += 2
    if row["is_international"] == 1:
        score += 2
    if row["hour"] < 5:  # night
        score += 1
    if row["velocity_5min"] > 5:
        score += 2
    if row["is_new_merchant"] == 1:
        score += 1
    if row["failed_login_attempts"] > 2:
        score += 1

    return 1 if score >= 4 else 0

df["fraud"] = df.apply(generate_fraud_label, axis=1)

# ===============================
# 🔥 STEP 3: FEATURE ENGINEERING
# ===============================

# Convert hour → night flag
df["is_night"] = df["hour"].apply(lambda x: 1 if x < 5 else 0)

# Drop raw hour if needed
df.drop(columns=["hour"], inplace=True)

# ===============================
# 🔥 STEP 4: DEFINE FEATURES
# ===============================

features = [
    "amount",
    "is_international",
    "is_night",
    "velocity_5min",
    "is_new_merchant",
    "account_age_days",
    "failed_login_attempts"
]

X = df[features]
y = df["fraud"]

# ===============================
# 🔥 STEP 5: TRAIN TEST SPLIT
# ===============================

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# ===============================
# 🔥 STEP 6: SCALE FEATURES
# ===============================

scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# ===============================
# 🔥 STEP 7: TRAIN MODEL (REGRESSION)
# ===============================

model = LogisticRegression(max_iter=1000)
model.fit(X_train_scaled, y_train)

# ===============================
# 🔥 STEP 8: EVALUATE MODEL
# ===============================

y_pred = model.predict(X_test_scaled)
y_prob = model.predict_proba(X_test_scaled)[:, 1]

print("\n📊 Classification Report:")
print(classification_report(y_test, y_pred))

print("🔥 ROC-AUC Score:", roc_auc_score(y_test, y_prob))

# ===============================
# 🔥 STEP 9: SAVE MODEL + SCALER
# ===============================

joblib.dump(model, "fraud_model.pkl")
joblib.dump(scaler, "scaler.pkl")

print("\n✅ Model saved as fraud_model.pkl")
print("✅ Scaler saved as scaler.pkl")

# ===============================
# 🔥 STEP 10: SAMPLE PREDICTION
# ===============================

sample = np.array([[12000, 1, 1, 7, 1, 200, 3]])
sample_scaled = scaler.transform(sample)

prob = model.predict_proba(sample_scaled)[0][1]

print("\n🚨 Sample Fraud Probability:", round(prob, 3))