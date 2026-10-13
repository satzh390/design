package com.example.ratelimiter.dto;

import lombok.Data;

@Data 
public class CheckQuotaRequest {
    private String policyKey;
    private String identifier;
}
