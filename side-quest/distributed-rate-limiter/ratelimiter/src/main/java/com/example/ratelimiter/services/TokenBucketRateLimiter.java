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
public class TokenBucketRateLimiter extends RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> tokenBucketScript;

    public TokenBucketRateLimiter(
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("tokenBucketScript") RedisScript<List> tokenBucketScript) {

        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    @Override
    public RateLimitResult isAllowed(RateLimiterPolicy rateLimitPolicy, String key) {
        double refillRate = ((double) rateLimitPolicy.getLimit() / getSeconds(rateLimitPolicy.getTimeUnit()));
        Long capacity = rateLimitPolicy.getLimit();

        List<?> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                capacity.toString(),
                Double.toString(refillRate)
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
        return Algorithm.TOKEN_BUCKET;
    }
    
}
