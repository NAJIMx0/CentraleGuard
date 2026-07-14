import json
import joblib
import numpy as np
from kafka import KafkaConsumer
import psycopg2
from datetime import datetime

# Load the trained model once at startup
model = joblib.load("anomaly_model.pkl")

# Connect to Kafka
consumer = KafkaConsumer(
    'sensor-readings',
    bootstrap_servers='localhost:9092',
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    auto_offset_reset='earliest'
)

# Connect to TimescaleDB
conn = psycopg2.connect(
    host="localhost",
    port=5433,
    dbname="postgres",
    user="postgres",
    password="postgres"
)
cursor = conn.cursor()

print("Listening for sensor readings...")

for message in consumer:
    reading = message.value

    temperature = reading['temperature']
    vibration = reading['vibration']
    rotationSpeed = reading['rotationSpeed']
    pressure = reading['pressure']
    machineId = reading['machineId']

    # Build the same shape of data the model expects: [temp, vib, rpm, pressure]
    X_new = np.array([[temperature, vibration, rotationSpeed, pressure]])

    # predict returns -1 for anomaly, 1 for normal
    prediction = model.predict(X_new)[0]
    is_anomaly = bool(prediction == -1)

    print(f"{machineId} | temp={temperature:.1f} vib={vibration:.2f} rpm={rotationSpeed} pressure={pressure:.2f} | Anomaly: {is_anomaly}")

    # Save to TimescaleDB
    cursor.execute(
        "INSERT INTO sensor_readings (machine_id, temperature, vibration, rotation_speed, pressure, is_anomaly, recorded_at) VALUES (%s, %s, %s, %s, %s, %s, %s)",
        (machineId, temperature, vibration, rotationSpeed, pressure, is_anomaly, datetime.now())
    )
    conn.commit()