package com.example.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.ratelimiter.model.Algorithm;
import com.example.ratelimiter.model.FailMode;
import com.example.ratelimiter.model.TimeUnit;

@SpringBootTest
class RateLimiterPolicyStoreTest {

    @Autowired
    private RateLimiterPolicyStore policyStore;

    @Test
    void loadsPoliciesFromYamlConfiguration() {
        var userPolicy = policyStore.getPolicy("get-user-api");
        assertTrue(userPolicy.isPresent());
        assertEquals(3L, userPolicy.get().getLimit());
        assertEquals(TimeUnit.SECOND, userPolicy.get().getTimeUnit());
        assertEquals(FailMode.FAIL_OPEN, userPolicy.get().getFailMode());
        assertEquals(Algorithm.TOKEN_BUCKET, userPolicy.get().getAlgo());

        var subscriptionPolicy = policyStore.getPolicy("get-subscription-api");
        assertTrue(subscriptionPolicy.isPresent());
        assertEquals(6L, subscriptionPolicy.get().getLimit());
        assertEquals(TimeUnit.MINUTE, subscriptionPolicy.get().getTimeUnit());
        assertEquals(FailMode.FAIL_CLOSED, subscriptionPolicy.get().getFailMode());
        assertEquals(Algorithm.FIXED_WINDOW, subscriptionPolicy.get().getAlgo());
    }
}
