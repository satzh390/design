package com.example.ratelimiter.dto;

import com.example.ratelimiter.model.RateLimitResult;

public class CheckQuotaResponse {
    private boolean isAllowed;
    private double remainingTokens;
    private long retryAfterMs;

    public boolean isAllowed() {
        return isAllowed;
    }

    public void setAllowed(boolean allowed) {
        isAllowed = allowed;
    }

    public double getRemainingTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(double remainingTokens) {
        this.remainingTokens = remainingTokens;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    public void setRetryAfterMs(long retryAfterMs) {
        this.retryAfterMs = retryAfterMs;
    }

    public static CheckQuotaResponse fromRateLimitResult(RateLimitResult rateLimitResult) {
        CheckQuotaResponse response = new CheckQuotaResponse();
        response.setAllowed(rateLimitResult.allowed());
        response.setRemainingTokens(rateLimitResult.remainingTokens());
        response.setRetryAfterMs(rateLimitResult.retryAfterMs());
        return response;
    }
}
