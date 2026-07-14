package com.najim.telemetryservice.service;

import com.najim.telemetryservice.Model.TelemetryReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class TelemetryService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final Random random = new Random();
    private final String machineId = "compressor-1";

    // Persistent sensor state — real sensors drift from their last value,
    // they don't reset to a fixed baseline every reading.
    private double temperature = 70.0;
    private double vibration = 2.0;
    private int rotationSpeed = 3000;
    private double pressure = 5.0;

    private long tickCount = 0;

    // Physical operating limits — a real PT100-monitored compressor
    // won't drift outside a sane envelope during normal operation.
    private static final double TEMP_MIN = 60.0;
    private static final double TEMP_MAX = 85.0;
    private static final int RPM_MIN = 2800;
    private static final int RPM_MAX = 3200;
    private static final double PRESSURE_MIN = 4.5;
    private static final double PRESSURE_MAX = 5.5;

    public TelemetryReading getTelemetryJson() {
        tickCount++;

        // ---- 1. Normal drift (random walk, not independent jumps) ----
        temperature += gaussianNoise(0, 0.15);
        temperature = clamp(temperature, TEMP_MIN, TEMP_MAX);

        rotationSpeed += (int) Math.round(gaussianNoise(0, 4));
        rotationSpeed = (int) clamp(rotationSpeed, RPM_MIN, RPM_MAX);

        pressure += gaussianNoise(0, 0.02);
        pressure = clamp(pressure, PRESSURE_MIN, PRESSURE_MAX);

        // ---- 2. Vibration coupled to RPM + periodic harmonic + noise ----
        double baseVibration = 1.5 + (rotationSpeed - 3000) * 0.001;
        double cyclicComponent = 0.05 * Math.sin(tickCount * 0.3);
        vibration = baseVibration + cyclicComponent + gaussianNoise(0, 0.08);
        vibration = Math.max(0, vibration);

        // ---- 3. PT100-style quantization (0.1°C resolution) ----
        double pt100Temp = Math.round(temperature * 10.0) / 10.0;
        double reportedVibration = Math.round(vibration * 100.0) / 100.0;
        double reportedPressure = Math.round(pressure * 100.0) / 100.0;

        // ---- 4. Anomaly injection (~10% chance per tick) ----
        if (random.nextInt(10) == 0) {
            pt100Temp += random.nextInt(221) + 200;                     // +200 to +420
            reportedVibration += (random.nextInt(201) + 100) / 100.0;   // +1.00 to +3.00
            rotationSpeed += random.nextInt(1001) + 500;                // +500 to +1500
            reportedPressure += (random.nextInt(201) + 100) / 100.0;    // +1.00 to +3.00
        }

        Instant timestamp = Instant.now();

        TelemetryReading reading = new TelemetryReading(
                machineId,
                pt100Temp,
                reportedVibration,
                rotationSpeed,
                reportedPressure,
                timestamp
        );

        kafkaTemplate.send("sensor-readings", machineId, reading);
        return reading;
    }

    private double gaussianNoise(double mean, double stdDev) {
        return mean + random.nextGaussian() * stdDev;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}