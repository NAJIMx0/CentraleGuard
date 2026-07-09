package com.najim.apigateway.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/plc")
public class PlcProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/command")
    @CircuitBreaker(name = "plcCommand", fallbackMethod = "fallback")
    public String sendCommand(@RequestBody Object command) {
        return restTemplate.postForObject("http://localhost:8998/api/plc/command", command, String.class);
    }

    public String fallback(Object command, Throwable t) {
        return "Fallback: PLC service unavailable, command queued";
    }
}