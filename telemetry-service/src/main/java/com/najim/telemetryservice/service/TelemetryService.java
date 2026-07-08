package com.najim.telemetryservice.service;

import com.najim.telemetryservice.Model.TelemetryReading;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.Map;

@Service
public class TelemetryService {
    double temperature = 70.00;
    double vibration=2.0;
    int rotationSpeed=3000;
    double pressure=5.0;
    String machineId= "turbine-1";
    Instant timestamp = Instant.now();




    Random random = new Random();

//    int number = random.nextInt(100); // 0 to 99
//    double value = random.nextDouble();

    public TelemetryReading getTelemetryJson() {
        for (int i = 0; i < 20; i++) {
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


            double x = random.nextInt(101) / 100.0;
            if(x >0.05) {
                temperature += random.nextInt(41) + 60;              // +60 to +100 °C
                vibration += (random.nextInt(201) + 100) / 100.0;    // +1.00 to +3.00 mm/s
                rotationSpeed += random.nextInt(1001) + 500;         // +500 to +1500 RPM
                pressure += (random.nextInt(201) + 100) / 100.0;     // +1.00 to +3.00 bar

                return new TelemetryReading(machineId,temperature,vibration,rotationSpeed,pressure,timestamp);
            }
        }
        return new TelemetryReading(machineId,temperature,vibration,rotationSpeed,pressure,timestamp);
    }
}
