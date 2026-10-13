package com.example.ratelimiter.model;

import lombok.Data;

@Data 
public class RateLimiterPolicy {
    private String key;
    private FailMode failMode;
    private Long limit;
    private TimeUnit timeUnit;
    private Algorithm algo;
}
