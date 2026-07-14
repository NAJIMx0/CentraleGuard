import json
import numpy as np
from kafka import KafkaConsumer
from sklearn.ensemble import IsolationForest
import joblib

consumer = KafkaConsumer(
    'sensor-readings',
    bootstrap_servers='localhost:9092',
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    auto_offset_reset='earliest'
)

data = []
print("Collecting readings... let this run while your telemetry service is sending data.")

for message in consumer:
    r = message.value
    data.append([r['temperature'], r['vibration'], r['rotationSpeed'], r['pressure']])
    print(f"Collected {len(data)} readings")

    if len(data) >= 8000:
        break

X = np.array(data)

# Remove obvious spikes before training (keep only realistic ranges)
mask = (X[:,0] < 100) & (X[:,2] < 3300)  # temp < 100, rpm < 3300
X = X[mask]

model = IsolationForest(contamination=0.05, random_state=42)
model.fit(X)

joblib.dump(model, "anomaly_model.pkl")
print("Model trained on real data and saved.")