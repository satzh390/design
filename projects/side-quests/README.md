# System-Design Implementations

This directory contains implementation code for selected system-design exercises. Question prompts are under [`system-design/exercises/questions/`](../../system-design/exercises/questions/). Design material lives in question-specific folders under [`system-design/exercises/`](../../system-design/exercises/) once created; implementation READMEs link to those folders when they exist.

| Project | Design workspace | Implementation |
|---|---|---|
| Distributed rate limiter | [Exercise](../../system-design/exercises/distributed-rate-limiter/), [question](../../system-design/exercises/questions/01-distributed-rate-limiter.md) | [Java/Spring prototype](./distributed-rate-limiter/ratelimiter/README.md); code and tests exist, in progress |
| Webhook delivery | [Exercise](../../system-design/exercises/webhook-delivery/), [question](../../system-design/exercises/questions/03-webhook-delivery.md) | [Webhook notifier](./webhook-notifier/README.md); scaffolding only, delivery behavior pending |
| URL shortener | [Exercise](../../system-design/exercises/url-shortener/), [question](../../system-design/exercises/questions/11-url-shortener.md) | [Java ID examples](./url-shortener/README.md); partial implementation |
| Payment gateway | [Question](../../system-design/exercises/questions/13-payment-gateway.md) | Not started |
| Restaurant booking | [Question](../../system-design/exercises/questions/14-restaurant-booking.md) | Not started |

## Local dependencies

[`docker-compose.yml`](./docker-compose.yml) provides Redis, PostgreSQL, and Kafka for local experiments. Start only the services needed by the project you are working on. These containers are for development and learning, not production deployment.

## Project completion checklist

- [ ] Working, deliberately scoped MVP
- [ ] Unit tests and at least one integration test
- [ ] At least one concurrency or failure-mode test
- [ ] Run instructions, limitations, and next implementation step
- [ ] Link to the design exercise and record implementation trade-offs
- [ ] Explain the system and its scaling limits
