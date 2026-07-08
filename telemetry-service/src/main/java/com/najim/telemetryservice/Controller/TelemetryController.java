package com.najim.telemetryservice.Controller;

import com.najim.telemetryservice.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class TelemetryController {

   private final TelemetryService service;

   @GetMapping("/telemetry")
    public String getTemperature() {
       return TelemetryService.TelemetryReading().toString();
   }


}
