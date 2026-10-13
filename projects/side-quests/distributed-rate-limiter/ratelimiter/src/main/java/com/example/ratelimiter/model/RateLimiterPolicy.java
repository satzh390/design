package com.example.ratelimiter.model;

public class RateLimiterPolicy {
    private String key;
    private FailMode failMode;
    private Long limit;
    private TimeUnit timeUnit;
    private Algorithm algo;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public FailMode getFailMode() {
        return failMode;
    }

    public void setFailMode(FailMode failMode) {
        this.failMode = failMode;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Algorithm getAlgo() {
        return algo;
    }

    public void setAlgo(Algorithm algo) {
        this.algo = algo;
    }
}
