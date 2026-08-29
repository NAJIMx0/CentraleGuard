package com.najim.apigateway;

import com.najim.apigateway.service.RateLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void allowsRequestsUpToCapacity() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowRequest("client1"), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void blocksRequestAfterCapacityExceeded() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.allowRequest("client1");
        }

        assertFalse(limiter.allowRequest("client1"), "6th request should be blocked");
    }

    @Test
    void refillAddsTokensBackUpToCapacity() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.allowRequest("client1");
        }
        assertFalse(limiter.allowRequest("client1"));

        limiter.refill();

        assertTrue(limiter.allowRequest("client1"), "After refill, a request should be allowed again");
    }

    @Test
    void differentClientsHaveIndependentBuckets() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.allowRequest("client1");
        }
        assertFalse(limiter.allowRequest("client1"));

        assertTrue(limiter.allowRequest("client2"), "A different client should have its own tokens");
    }
}