package com.example.ratelimiter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import com.example.ratelimiter.model.RateLimiterPolicy;

@ConfigurationProperties
public class RateLimiterPolicyStore {
    
    private final Map<String, RateLimiterPolicy> policies;

    @ConstructorBinding
    public RateLimiterPolicyStore(List<RateLimiterPolicy> policies) {
        this.policies = policies == null
                ? Map.of()
                : policies.stream().collect(Collectors.toMap(RateLimiterPolicy::getKey, Function.identity()));
    }

    public Optional<RateLimiterPolicy> getPolicy(String key) {
        return Optional.ofNullable(policies.get(key));
    }
}
