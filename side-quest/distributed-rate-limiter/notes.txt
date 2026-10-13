FR Clarification:
    1. I need to store policy somewhere and client can send limit check request with policy key and identifier
    2. Service will always send 200 response code and client has to decide status code for user like 429. As disributed rate limiter is internal service and its not actually serving request, its just a internal validator.
    3. Response should include remaining quota. For failure case i can include next-try time window(optional)
    4. System should support fail-closed or fail-open[Configurable]. No fail-over for now.

NFR Clarification:
    1. Low latency
    2. Atomic decision
    3. Horizontal scaling
    4. Observable latency & rejection rate