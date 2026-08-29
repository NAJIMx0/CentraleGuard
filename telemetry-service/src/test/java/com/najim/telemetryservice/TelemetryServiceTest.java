package com.najim.telemetryservice;

import com.najim.telemetryservice.Model.TelemetryReading;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TelemetryServiceTest {

    @Test
    void generatedReadingHasValidMachineId() {
        // Adjust constructor call if TelemetryService needs KafkaTemplate injected
        TelemetryReading reading = new TelemetryReading(
                "compressor-1", 70.0, 2.0, 3000, 5.0, java.time.Instant.now()
        );

        assertEquals("compressor-1", reading.machineId());
        assertNotNull(reading.timestamp());
    }
}