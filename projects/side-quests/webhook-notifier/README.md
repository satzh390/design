# Webhook Delivery Implementation

This is the code workspace for the webhook-delivery exercise. The question, design, and architecture materials are maintained in the [webhook-delivery design folder](../../../system-design/exercises/webhook-delivery/README.md).

## Implementation status

The project is split into three Gradle subprojects:

| Module | Intended responsibility |
|---|---|
| `webhook` | Accept and persist webhook requests |
| `fanout` | Find delivery work and publish it |
| `worker` | Deliver webhooks and handle retries |

The modules currently contain Spring Boot application scaffolding and starter tests. The delivery flow, persistence model, queue behavior, retry policy, and dead-letter handling are not implemented yet.

## Build

From this directory, use the wrapper in the `webhook` module:

```powershell
.\webhook\gradlew.bat test
```

Each module currently has its own Gradle wrapper. Shared local dependencies for experiments are listed in [`../docker-compose.yml`](../docker-compose.yml).
