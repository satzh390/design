package com.example.ratelimiter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiter.dto.CheckQuotaRequest;
import com.example.ratelimiter.dto.CheckQuotaResponse;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.services.RateLimiterExecutor;

@RestController
@RequestMapping("/quota")
public class RateLimiterController {

    private final RateLimiterExecutor rateLimiterExecutor;

    public RateLimiterController(RateLimiterExecutor rateLimiterExecutor) {
        this.rateLimiterExecutor = rateLimiterExecutor;
    }

    @PostMapping("/check")
    public ResponseEntity<CheckQuotaResponse> check(@RequestBody CheckQuotaRequest request) {
        RateLimitResult result = rateLimiterExecutor.execute(request.getPolicyKey(), request.getIdentifier());

        CheckQuotaResponse response = CheckQuotaResponse.fromRateLimitResult(result);
        return ResponseEntity.ok(response);
    }
}
