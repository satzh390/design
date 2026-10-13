# Distributed Rate Limiter Prototype

This Spring Boot project implements the [distributed-rate-limiter exercise](../../../../system-design/exercises/distributed-rate-limiter/README.md), based on the [question prompt](../../../../system-design/exercises/questions/01-distributed-rate-limiter.md). Keep architecture and design clarifications in the system-design exercise; this folder is for source code, tests, and run instructions.

The prototype currently contains fixed-window and token-bucket paths, Redis Lua scripts, a policy store, and unit tests. It is still in progress.

## Run locally

From this directory on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

The application uses Redis for distributed state. Start Redis from the [`side-quests` project directory](../../) before trying Redis-backed behavior:

```powershell
docker compose up -d redis
```

## Main areas

- `src/main/java/.../controller` — HTTP entry point
- `src/main/java/.../services` — rate-limiting algorithms and execution
- `src/main/java/.../model` and `dto` — policies, results, and API data
- `src/main/resources/scripts` — Redis Lua scripts
- `src/test/java` — policy-store and executor tests
