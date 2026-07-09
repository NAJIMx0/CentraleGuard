package com.najim.apigateway.filter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class RateLimiter {
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final int capacity = 5;

    public boolean allowRequest(String clientId) {
        tokens.putIfAbsent(clientId, capacity);
        synchronized (this) {
            int current = tokens.get(clientId);
            if (current <= 0) return false;
            tokens.put(clientId, current - 1);
            return true;
        }
    }

    public void refill() {
        tokens.replaceAll((k, v) -> Math.min(capacity, v + 1));
    }
    @Scheduled(fixedRate = 2000)
    public void scheduledRefill() {
        refill();
    }
}