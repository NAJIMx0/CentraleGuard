import numpy as np
from sklearn.ensemble import IsolationForest
import joblib

# Generate 1000 fake "normal" readings, same wobble logic as your Java service
np.random.seed(42)
n_samples = 1000

temperature = 70 + np.random.normal(0, 2, n_samples)
vibration = 2.0 + np.random.normal(0, 0.2, n_samples)
rotation_speed = 3000 + np.random.normal(0, 80, n_samples)
pressure = 5.0 + np.random.normal(0, 0.1, n_samples)

# Combine into one array: each row is [temp, vib, rpm, pressure]
X = np.column_stack([temperature, vibration, rotation_speed, pressure])

# Train Isolation Forest
model = IsolationForest(contamination=0.05, random_state=42)
model.fit(X)

# Save the trained model to a file
joblib.dump(model, "anomaly_model.pkl")
print("Model trained and saved.")