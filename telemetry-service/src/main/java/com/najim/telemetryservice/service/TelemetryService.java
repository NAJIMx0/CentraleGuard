package com.najim.telemetryservice.service;

import com.najim.telemetryservice.Model.TelemetryReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.Map;

@Service
public class TelemetryService {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

//    int number = random.nextInt(100); // 0 to 99
//    double value = random.nextDouble();

    public TelemetryReading getTelemetryJson() {
        double temperature = 70.00;
        double vibration = 2.0;
        int rotationSpeed = 3000;
        double pressure = 5.0;
        String machineId = "compressor-1";
        Instant timestamp = Instant.now();
        Random random = new Random();


            // Temperature: ±2 °C
            int valueT = random.nextInt(5) - 2;
            temperature += valueT;

            // Vibration: ±0.2 mm/s
            double valueV = (random.nextInt(41) - 20) / 100.0;
            vibration += valueV;

            // Rotation Speed: ±30 RPM
            int valueR = random.nextInt(61) - 30;
            rotationSpeed += valueR;

            // Pressure: ±0.1 bar
            double valueP = (random.nextInt(21) - 10) / 100.0;
            pressure += valueP;

            int num = random.nextInt(20);
            if (num % 20 == 0) {
                    temperature += random.nextInt(221) + 200;
                    vibration += (random.nextInt(201) + 100) / 100.0;
                    rotationSpeed += random.nextInt(1001) + 500;
                    pressure += (random.nextInt(201) + 100) / 100.0;

            }


//        return new TelemetryReading(machineId,temperature,vibration,rotationSpeed,pressure,timestamp);
        TelemetryReading reading = new TelemetryReading(machineId, temperature, vibration, rotationSpeed, pressure, timestamp);
        kafkaTemplate.send("sensor-readings", machineId, reading);
        return reading;
    }
}
