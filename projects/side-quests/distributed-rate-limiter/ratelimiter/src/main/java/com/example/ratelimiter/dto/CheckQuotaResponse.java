package com.example.ratelimiter.dto;

import com.example.ratelimiter.model.RateLimitResult;

import lombok.Data;

@Data 
public class CheckQuotaResponse {
    private boolean isAllowed;
    private double remainingTokens;
    private long retryAfterMs;

    public static CheckQuotaResponse fromRateLimitResult(RateLimitResult rateLimitResult) {
        CheckQuotaResponse response = new CheckQuotaResponse();
        response.setAllowed(rateLimitResult.allowed());
        response.setRemainingTokens(rateLimitResult.remainingTokens());
        response.setRetryAfterMs(rateLimitResult.retryAfterMs());
        return response;
    }
}
