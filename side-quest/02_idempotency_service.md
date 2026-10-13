# 02 — Idempotency & Request Deduplication Service

## Problem

Distributed systems retry requests.

A payment, order creation or message publication request may arrive twice.

Build a small service/pattern that ensures the same idempotency key does not execute the operation multiple times.

## Functional Requirements

1. Accept an idempotency key.
2. Associate it with a request fingerprint.
3. Store the result of the first successful execution.
4. Return the stored result for duplicate requests.
5. Reject reuse of the same key with a different request body.
6. Handle concurrent duplicate requests.

Example:

`POST /payments`

Header:

`Idempotency-Key: payment-123`

## Non-Functional Requirements

- Atomic under concurrency.
- Durable result storage.
- Safe across application restarts.
- Low overhead.
- Clear behavior for in-progress requests.

## Architecture

```text
Client
  |
  v
API
  |
  v
Idempotency Store
  |
  +--> PostgreSQL unique constraint
  |
  v
Business Operation
```

## Important Design

Use a unique constraint:

`UNIQUE(tenant_id, idempotency_key)`

Possible states:

```text
IN_PROGRESS
SUCCEEDED
FAILED
EXPIRED
```

## Key Concepts

- Idempotency
- duplicate delivery
- database constraints
- atomic insert
- race conditions
- exactly-once illusion
- retry safety
- TTL/retention

## Hard Question

What happens if the application crashes after performing the business operation but before storing the idempotency result?

Document at least two solutions and their trade-offs.

## Scope Limit

Do not build a payment system.

The project is about the idempotency mechanism.
