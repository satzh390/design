# 03 — Reliable Webhook Delivery Engine

## Problem

A project management platform's primary unit of work is a **Task**. A task has
attributes such as name, description, and start date, and can be created,
edited, or deleted. Tasks belong to Projects, which belong to customer
Accounts.

Customers want to receive HTTPS notifications when task changes happen so they
can run their own business logic. For example, a customer might subscribe to
task creation and only those task edits that change the start date.

Design a webhook system that lets each Account register HTTPS endpoints,
control which task events they receive, and reliably process deliveries even
when customer endpoints or cloud infrastructure fail.

## Scale and assumptions

- 100 Accounts
- 2,000 Projects
- 200,000 Tasks
- An average of 50% of Tasks undergo an operation each hour.
- For initial sizing, assume one event per changed Task: about **100,000 events
  per hour**, or **28 events/second on average**. Plan for bursts above the
  average; the burst factor is an open capacity-planning input.
- A single Task change can produce a separate delivery for each matching
  endpoint registered by its Account.

## Functional requirements

1. Register, update, and remove webhook endpoints for an Account.
2. Enable or disable an endpoint without deleting its configuration.
3. Subscribe to selected event types, such as:
   - `task.created`
   - `task.updated`, optionally filtered to changes in `startDate`
   - `task.deleted`
4. Deliver matching events to the Account's HTTPS endpoint.
5. Retry transient failures and expose permanently failing deliveries for
   investigation and replay.
6. Let customers verify endpoint ownership and send a test event.
7. Show delivery health and history so customers can tell whether a webhook is
   configured correctly and continues to work.

## Non-functional requirements

- Task writes must not wait for customer HTTP endpoints to respond.
- Do not lose an event after the corresponding task change is committed.
- Isolate slow or failing customer endpoints from one another.
- Bound connection time, response time, retries, and queued work.
- Support horizontal scaling of event publishers and delivery workers.
- Protect endpoint secrets and authenticate webhook payloads.
- Make delivery attempts, failures, backlog, and recovery observable.

## API sketch

Account identity is derived from the authenticated caller, not trusted from an
arbitrary request body.

```text
POST   /v1/webhooks
GET    /v1/webhooks
GET    /v1/webhooks/{webhookId}
PATCH  /v1/webhooks/{webhookId}          # endpoint, subscriptions, enabled
DELETE /v1/webhooks/{webhookId}
POST   /v1/webhooks/{webhookId}/test
GET    /v1/webhooks/{webhookId}/deliveries
POST   /v1/webhooks/{webhookId}/deliveries/{deliveryId}/replay
```

Example registration:

```json
{
  "url": "https://hooks.example.com/task-events",
  "enabled": true,
  "subscriptions": [
    { "eventType": "task.created" },
    { "eventType": "task.updated", "changedFields": ["startDate"] }
  ]
}
```

Validate HTTPS URLs, reject invalid destinations, and require a verification
challenge before enabling delivery to a newly registered endpoint.

## Proposed architecture

```text
Task API
   |
   | task change + outbox row in one database transaction
   v
Task Database + Transactional Outbox
   |
   v
Outbox Publisher
   |
   v
Durable Event Queue
   |
   v
Subscription Matcher / Delivery Scheduler
   |
   v
Durable Delivery Queue
   |
   v
Delivery Workers -----> Endpoint health / circuit breaker
   |                              |
   v                              v
Customer HTTPS endpoint     Pause / resume scheduling
   |
   v
Delivery status, metrics, logs, and replay
```

The outbox publisher and workers can run as independently scaled services.
Queue technology is an implementation choice; the design requires durable
messages, acknowledgements, retry visibility, and dead-letter handling.

## Event and delivery flow

1. The Task service changes task data and writes an outbox event in the same
   database transaction. If the transaction rolls back, neither is visible.
2. The publisher reads unprocessed outbox rows in batches, publishes them to
   the durable queue, and marks them published only after broker acknowledgement.
3. The matcher finds enabled webhook subscriptions for the event's Account
   and filters by event type and changed fields.
4. It creates a durable delivery record for each matching endpoint and queues
   its delivery. A uniqueness constraint on `(eventId, webhookId)` prevents
   duplicate records during publisher or matcher retries.
5. A worker signs the payload, sends an HTTPS POST with a bounded timeout, and
   records the attempt and response.
6. A 2xx response marks the delivery successful. Retryable failures are
   scheduled for another attempt; exhausted deliveries move to a dead-letter
   state that customers can inspect and replay.

## Event contract

Use a stable envelope with a unique event ID and versioned schema:

```json
{
  "id": "evt_...",
  "type": "task.updated",
  "version": 1,
  "occurredAt": "2026-10-10T10:00:00Z",
  "accountId": "acct_...",
  "projectId": "proj_...",
  "task": {
    "id": "task_...",
    "name": "Prepare launch",
    "description": "...",
    "startDate": "2026-10-15"
  },
  "changes": {
    "startDate": {
      "from": "2026-10-14",
      "to": "2026-10-15"
    }
  }
}
```

Include the relevant task snapshot and changed-field information in the event.
This avoids a database lookup for every delivery and ensures consumers receive
the data as it was when the event occurred. Define which fields are included,
how deleted-task data is represented, and how personally identifiable data is
handled.

## Data model

- **Webhook**: ID, Account ID, URL, enabled state, encrypted signing secret,
  created/updated timestamps.
- **Subscription**: Webhook ID, event type, optional changed-field filter.
- **OutboxEvent**: event ID, Account ID, event type, payload, creation time,
  publish state.
- **Delivery**: event ID, webhook ID, state, attempt count, next-attempt time,
  last status/error, timestamps.
- **DeliveryAttempt**: delivery ID, attempt number, start/end time, response
  status, sanitized error details.

Index webhooks and subscriptions by Account ID and enabled state. Index
deliveries by webhook and creation time for customer history, and by state and
next-attempt time for workers.

## Reliability and delivery semantics

- Promise **at-least-once delivery attempts**, not exactly-once HTTP delivery.
  A timeout can occur after the customer processed a request but before the
  sender received the response.
- Send `X-Webhook-Event-Id`, `X-Webhook-Delivery-Id`, and a timestamped HMAC
  signature. Customers should deduplicate using the event ID.
- Retry network errors, timeouts, and selected 5xx responses with exponential
  backoff and jitter. Honor `Retry-After` where appropriate. Do not retry
  ordinary permanent 4xx errors indefinitely.
- Set a maximum attempt count and delivery age. Move exhausted messages to a
  dead-letter state and expose a manual replay action.
- Apply per-endpoint concurrency and rate limits. Use circuit breaking or
  temporary suspension when an endpoint repeatedly fails, without affecting
  other Accounts.
- Make replay create a new delivery attempt while preserving the original
  event and delivery history.
- If ordering is required, define its scope explicitly (for example, per Task)
  and accept the throughput and retry head-of-line blocking trade-off. Do not
  imply global ordering.

## Customer onboarding and operations

Make setup low-friction:

1. Provide a UI or API to add an HTTPS URL and choose event subscriptions.
2. Verify endpoint ownership with a challenge request.
3. Generate and display a signing secret once; support secret rotation.
4. Send a test event and show its request ID, response, and signature
   verification guidance.
5. Provide copyable sample receiver code, retry/signature documentation, and
   an event schema catalog.
6. Show recent deliveries, response codes, attempt history, next retry, and
   dead-lettered events. Allow safe replay.

Measure:

- outbox age and unpublished row count
- queue depth and oldest event age
- delivery success rate, latency, timeout, and response-code distributions
- retries, dead-letter count, and per-endpoint failure rate
- worker utilization and queue processing rate

Alert on sustained queue growth, publisher lag, elevated failures, and
dead-letter spikes. Provide customers endpoint-level health state and last
successful delivery time. Avoid logging secrets or full sensitive payloads.

## Key design questions

- What burst factor and delivery-latency objective should capacity support?
- Are subscriptions configured per Account, per Project, or both?
- Which fields may appear in task snapshots, and for how long are events
  retained?
- Should update events be coalesced, or must every committed change be sent?
- What is the ordering guarantee and its scope?
- Which HTTP status codes are retryable, and what are the maximum retry count
  and retention window?
- How should endpoint verification, secret rotation, and URL security checks
  work?
- How long should delivery history and dead-letter events remain available?

## Scope limits

- HTTPS webhooks only; no email, SMS, or notification platform.
- No exactly-once delivery guarantee.
- Do not build a general-purpose workflow engine or customer receiver.
- Start with one region; multi-region active-active delivery is a stretch goal.

## Stretch goals

- Per-Account quotas and noisy-neighbor isolation.
- Per-Task ordering with partitioned queues.
- Multi-region failover and event replication.
- Delivery replay by event range or time window.
- Customer self-serve analytics and configurable retention.

## Key concepts

- transactional outbox
- durable queues
- at-least-once delivery
- idempotency and deduplication
- exponential backoff and jitter
- dead-letter queues and replay
- subscription filtering
- endpoint isolation and circuit breaking
- HMAC signatures and secret rotation
- backpressure and observability

## Interview questions

- Why use a transactional outbox instead of publishing directly from the Task
  service?
- Where can duplicates occur, and how does the customer deduplicate them?
- How does the system avoid one broken endpoint blocking all deliveries?
- How do task snapshots avoid expensive or inconsistent reads at delivery
  time?
- What happens if the queue, publisher, database, or customer endpoint is down?
- How would you support per-Task ordering?
- How would you scale from 100,000 events/hour to 100 million?
