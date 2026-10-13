package com.example.ratelimiter.services;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.example.ratelimiter.model.Algorithm;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.model.RateLimiterPolicy;

@Service
public class FixedWindowRateLimiter extends RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> fixedWindowScript;

    public FixedWindowRateLimiter(
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("fixedWindowScript") RedisScript<List> fixedWindowScript) {
        this.redisTemplate = redisTemplate;
        this.fixedWindowScript = fixedWindowScript;
    }

    @Override
    public RateLimitResult isAllowed(RateLimiterPolicy rateLimitPolicy, String key) {
        long windowSeconds = getSeconds(rateLimitPolicy.getTimeUnit());

        List<?> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                rateLimitPolicy.getLimit().toString(),
                Long.toString(windowSeconds)
        );

        boolean allowed = ((Number) result.get(0)).intValue() == 1;
        double remainingTokens = ((Number) result.get(1)).doubleValue();
        long retryAfterMs = ((Number) result.get(2)).longValue();

        return new RateLimitResult(
                allowed,
                remainingTokens,
                retryAfterMs
        );
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithm.FIXED_WINDOW;
    }
}
