package com.najim.telemetryservice.Model;

import lombok.Builder;

import java.time.Instant;
//@Builder
public record TelemetryReading(
        String machineId,
        double temperature,
        double vibration,
        int rotationSpeed,
        double pressure,
        Instant timestamp
) {}
