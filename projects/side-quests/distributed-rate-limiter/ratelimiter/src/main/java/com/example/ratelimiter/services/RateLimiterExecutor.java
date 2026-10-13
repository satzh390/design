package com.example.ratelimiter.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.ratelimiter.RateLimiterPolicyStore;
import com.example.ratelimiter.model.Algorithm;
import com.example.ratelimiter.model.FailMode;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.model.RateLimiterPolicy;

@Service 
public class RateLimiterExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterExecutor.class);
    
    private final Map<Algorithm, RateLimiter> rateLimiters;
    private final RateLimiterPolicyStore rateLimitPolicyStore;

    public RateLimiterExecutor(List<RateLimiter> rateLimiters, RateLimiterPolicyStore rateLimitPolicyStore) {
        this.rateLimiters = rateLimiters.stream().collect(Collectors.toMap(RateLimiter::getAlgorithm, Function.identity()));
        this.rateLimitPolicyStore = rateLimitPolicyStore;
    }

    public RateLimitResult execute(String policyKey, String identifier){
        Optional<RateLimiterPolicy> policyOptional = rateLimitPolicyStore.getPolicy(policyKey);
        if (policyOptional.isEmpty()) {
            return new RateLimitResult(true, 0, 0);
        }

        RateLimiterPolicy rateLimiterPolicy = policyOptional.get();
        RateLimiter rateLimiter = rateLimiters.get(rateLimiterPolicy.getAlgo());
        if (rateLimiter == null) {
            return failOpenOrClosed(rateLimiterPolicy);
        }

        try {
            return rateLimiter.isAllowed(rateLimiterPolicy, identifier);
        } catch (Exception ex) {
            logger.error("Rate limiter failed for policy '{}'; applying configured fail mode.", policyKey, ex);
            return failOpenOrClosed(rateLimiterPolicy);
        }
    }

    private RateLimitResult failOpenOrClosed(RateLimiterPolicy policy) {
        if (policy.getFailMode() == null || policy.getFailMode() == FailMode.FAIL_OPEN) {
            return new RateLimitResult(true, 0, 0);
        }

        return new RateLimitResult(false, 0, 0);
    }
}
