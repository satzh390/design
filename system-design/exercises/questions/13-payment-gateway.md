# 13 — Payment Gateway / Payment Orchestration Service

## Problem

Build a small payment service used by an e-commerce application to initiate and track payments through an external payment provider.

The system must remain correct when requests are retried, the provider times out, webhooks are duplicated or arrive out of order, and the application crashes at inconvenient points.

This project is intentionally a payment **orchestration** system, not a card-processing or payment-provider implementation.

## Why this project

This project combines several distributed-systems problems already explored in the side quests:

- idempotency
- asynchronous webhooks
- retries and failure handling
- state machines
- eventual consistency
- reconciliation
- auditability

The goal is to understand how these ideas interact in a payment workflow.

## Functional Requirements

1. Create a payment for an order with an initial `PAYMENT_PENDING` state.
2. Accept an idempotency key for payment creation.
3. Create an external payment / checkout session with a provider.
4. Return a checkout URL to the client.
5. Track payment states such as `PENDING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `EXPIRED`, and `REFUNDED`.
6. Receive and verify signed provider webhooks.
7. Deduplicate webhook events.
8. Handle duplicate and out-of-order webhook events safely.
9. Provide an API to query payment status.
10. Support refunds for successful payments.
11. Maintain an auditable payment transaction history.
12. Reconcile payments that remain in an uncertain or pending state.
13. Use a provider sandbox/test environment; never process real card data.

## Non-Functional Requirements

- **No double charge:** retries of the same logical payment must not create multiple charges.
- **Idempotent APIs:** payment creation and refund operations must be safe to retry.
- **Durable state:** payment state and important events must survive application restarts.
- **Eventual consistency:** local payment state may temporarily differ from provider state.
- **Provider timeout safety:** a provider timeout must not automatically be treated as a payment failure because the provider may have accepted the payment.
- **At-least-once webhook processing:** duplicate webhook delivery is expected.
- **Out-of-order tolerance:** older provider events must not incorrectly overwrite a newer terminal state.
- **Horizontal scalability:** payment APIs should be stateless and support multiple application instances.
- **Security:** provider credentials and webhook signatures must be protected; raw card details must never be stored.
- **Observability:** payment lifecycle, failures, retries, webhook processing, and reconciliation should be traceable.
- **Low latency:** synchronous APIs should avoid waiting unnecessarily for asynchronous provider confirmation.

## High-Level Architecture

```text
                         +----------------------+
                         |   E-commerce Client   |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |   Payment Service    |
                         |----------------------|
                         | Payment API          |
                         | Idempotency          |
                         | State Machine        |
                         | Refund API           |
                         +----+------------+----+
                              |            |
                              v            v
                    +-------------+   +-------------------+
                    | PostgreSQL  |   | External Payment  |
                    |-------------|   | Provider          |
                    | Payments    |   | (Stripe-like)     |
                    | Events      |   +---------+---------+
                    | Idempotency|             |
                    +-------------+             |
                                                | Webhooks
                                                v
                                      +----------------------+
                                      | Webhook Handler      |
                                      |----------------------|
                                      | Signature validation |
                                      | Event deduplication  |
                                      | State transition     |
                                      +----------+-----------+
                                                 |
                                                 v
                                      +----------------------+
                                      | Payment State / DB   |
                                      +----------------------+

                                      +----------------------+
                                      | Reconciliation Worker|
                                      |----------------------|
                                      | Find uncertain      |
                                      | payments             |
                                      | Query provider       |
                                      | Repair local state  |
                                      +----------------------+
```

## Payment State Machine

Typical lifecycle:

```text
CREATED
   |
   v
PAYMENT_PENDING
   |       |        |         |
   |       |        |         +--> EXPIRED
   |       |        +------------> CANCELLED
   |       +---------------------> FAILED
   +-----------------------------> SUCCEEDED
                                      |
                                      +--> REFUNDED
                                      |
                                      +--> PARTIALLY_REFUNDED
```

A provider response or webhook should be treated as an input to the state machine, not as permission to blindly overwrite the current state.

## Important Failure Scenarios

### 1. Provider timeout after accepting the payment

The payment request times out, but the provider may have successfully created the payment.

**Do not mark the payment as FAILED just because the HTTP request timed out.**

Keep it in an uncertain/pending state and resolve it through a webhook or reconciliation.

### 2. Client retries payment creation

The same idempotency key must return the existing payment/session instead of creating another charge.

### 3. Duplicate webhook

The provider may deliver the same event more than once.

Store a provider event ID and process each event only once.

### 4. Out-of-order webhooks

A later event may arrive before an earlier event.

Use payment state-transition rules and provider event/version information where available so an older event cannot move a payment backwards incorrectly.

### 5. Application crash after provider charge

The provider has charged the customer, but the application crashes before persisting the local result.

The system must recover through webhook processing or reconciliation rather than charging again.

### 6. Refund retry

A refund request can also be retried. The refund operation therefore needs its own idempotency strategy and durable record.

## Key Distributed-System Concepts

- idempotency
- exactly-once illusion
- payment state machines
- at-least-once delivery
- duplicate event processing
- out-of-order events
- eventual consistency
- retries and backoff
- reconciliation
- audit logs
- provider timeout ambiguity
- crash consistency
- durable workflow state

## Hard Interview Questions

1. How do you prevent a double charge if the client retries?
2. What if the payment provider times out after accepting the request?
3. What if the webhook arrives twice?
4. What if a failure webhook arrives after a success webhook?
5. What happens if the application crashes immediately after the provider charges the customer?
6. Why is the success redirect from the provider not enough to mark a payment successful?
7. How do you reconcile payments stuck in `PENDING`?
8. How do you make refunds idempotent?
9. Which state transitions are legal, and which should be rejected?
10. How would the design change for multiple payment providers?

## Scope

### In scope

- One-time payments
- Provider-hosted checkout / payment session
- Payment status tracking
- Webhook processing
- Idempotency
- Refunds
- Reconciliation
- Audit history
- Provider sandbox/test mode

### Out of scope

- Storing raw card numbers or CVV
- Building a card vault
- PCI-compliant card processing
- Real-money production payments
- Multi-region active-active payments
- Complex tax/FX/currency handling
- Fraud detection
- Full accounting/ledger implementation

## Stretch Goals

- Multiple payment-provider abstraction
- Provider failover
- Outbox pattern
- Circuit breaker around provider calls
- Subscription / recurring payments
- Reconciliation dashboard
- Chaos tests for crash and timeout scenarios
- Metrics and distributed tracing
