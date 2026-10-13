package com.example.ratelimiter.services;

import java.util.Map;

import com.example.ratelimiter.model.Algorithm;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.model.RateLimiterPolicy;
import com.example.ratelimiter.model.TimeUnit;

public abstract class RateLimiter {

    private final Map<TimeUnit, Long> timeUnitToSeconds = Map.of(
            TimeUnit.SECOND, 1L,
            TimeUnit.MINUTE, 60L,
            TimeUnit.HOUR, 60L * 60
    );

    protected long getSeconds(TimeUnit timeUnit) {
        return timeUnitToSeconds.get(timeUnit);
    }

    public abstract RateLimitResult isAllowed(RateLimiterPolicy rateLimitPolicy, String key);

    public abstract Algorithm getAlgorithm();
}
