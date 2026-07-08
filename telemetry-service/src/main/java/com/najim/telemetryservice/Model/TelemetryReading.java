package com.najim.telemetryservice.Model;

import java.time.Instant;

public record TelemetryReading(
        String machineId,
        double temperature,
        double vibration,
        int rotationSpeed,
        double pressure,
        Instant timestamp
) {}
