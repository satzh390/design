# 07 — Mini Workflow / DAG Engine

## Problem

Build a small workflow engine that executes tasks according to dependencies.

Example:

```text
extract
 /    v      v
clean  validate
 \    /
  v  v
   load
```

## Functional Requirements

1. Define a workflow.
2. Define tasks.
3. Define dependencies.
4. Validate DAGs.
5. Execute independent tasks in parallel.
6. Retry failed tasks.
7. Persist task state.
8. Resume after worker failure.
9. Show execution history.

## Example

```yaml
workflow: customer-import

tasks:
  extract:
    type: HTTP

  transform:
    dependsOn: [extract]

  validate:
    dependsOn: [extract]

  load:
    dependsOn: [transform, validate]
```

## Architecture

```text
Workflow API
     |
     v
Workflow Store
     |
     v
Scheduler
     |
     v
Queue
     |
     +---- Worker A
     +---- Worker B
     +---- Worker C
```

## NFRs

- Durable workflow state.
- No task executes before dependencies complete.
- Independent tasks can run concurrently.
- Worker crash must be recoverable.
- Duplicate execution must be detectable/controlled.
- Scheduler should scale horizontally.

## Key Concepts

- DAGs
- topological ordering
- state machines
- task scheduling
- worker pools
- retries
- leases
- idempotency
- concurrency
- backpressure
- persistent execution state

## Scope Limit

Do not support arbitrary Python execution, Spark clusters, Kubernetes jobs or complex operators.

Use simple task types such as HTTP or simulated Java tasks.

## Stretch Goal

Add Kafka as the task-dispatch layer.
