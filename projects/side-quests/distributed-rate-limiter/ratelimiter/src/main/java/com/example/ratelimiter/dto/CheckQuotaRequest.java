package com.example.ratelimiter.dto;

public class CheckQuotaRequest {
    private String policyKey;
    private String identifier;

    public String getPolicyKey() {
        return policyKey;
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
