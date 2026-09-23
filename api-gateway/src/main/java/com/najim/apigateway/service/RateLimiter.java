package com.najim.apigateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final int capacity = 5;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String clientId) {
        String key = "ratelimit:" + clientId;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(capacity));
        if (Boolean.TRUE.equals(isNew)) {
            return capacity > 0;
        }

        Long current = redisTemplate.opsForValue().decrement(key);
        if (current == null || current < 0) {
            redisTemplate.opsForValue().set(key, "0");
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 2000)
    public void scheduledRefill() {
        var keys = redisTemplate.keys("ratelimit:*");
        if (keys == null) return;
        for (String key : keys) {
            redisTemplate.opsForValue().set(key, String.valueOf(capacity));
        }
    }
}