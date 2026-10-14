# System Design & Backend Engineering

A learning portfolio for distributed-systems fundamentals, low-level design, system-design exercises, and small Java prototypes. This repository is a collection of independent notes and examples, not a single application.

## Start here

| If you want to… | Go to |
|---|---|
| Explore a backend building block or distributed-systems algorithm | [`fundamentals/`](./fundamentals/) |
| Browse object-oriented design exercises | [`low-level-design/`](./low-level-design/) |
| Study end-to-end system-design problems | [`system-design/`](./system-design/) |
| Follow practical projects from design to code | [`projects/side-quests/README.md`](./projects/side-quests/README.md) |

## Fundamentals

| Topic | What is here |
|---|---|
| [Consensus](./fundamentals/consensus/) | Quorum, Paxos, and Raft study notes |
| [Failure detection](./fundamentals/failure-detection/) | Gossip implementation and failure-detection notes |
| [Hashing](./fundamentals/hashing/) | Consistent and rendezvous hashing implementations |
| [ID generation](./fundamentals/id-generation/) | Twitter Snowflake-style ID generator |
| [Rate limiting](./fundamentals/rate-limiting/) | Fixed-window, leaky-bucket, sliding-window, and token-bucket examples |
| [Replica consistency](./fundamentals/replica-consistency/) | Vector clock and Merkle tree examples, with other approaches documented |
| [Storage](./fundamentals/storage/) | Bloom filter and quadtree examples |
| [Numeric representation](./fundamentals/numeric-representation/floating-point-and-decimal-recall.md) | Binary fractions, IEEE 754 `double`, rounding, `BigDecimal`, and integer encodings |

## Low-level design

[`low-level-design/`](./low-level-design/) contains standalone Java exercises:

- [LRU cache](./low-level-design/lru-cache/)
- [Chess](./low-level-design/chess/)
- [Tic-tac-toe](./low-level-design/tic-tac-toe/)

## System design

See the [system-design index](./system-design/README.md) for study notes and exercises. Question prompts are in [system-design/exercises/questions/](./system-design/exercises/questions/). Add an exercise folder when you have design materials to store; implementation code is kept separately in [projects/side-quests/](./projects/side-quests/).

## Practical projects

[`projects/side-quests/README.md`](./projects/side-quests/README.md) tracks implementation locations and progress. The 15 system-design questions are collected in [`system-design/exercises/questions/`](./system-design/exercises/questions/); create design workspaces as you work on an exercise.

## Repository layout

```text
fundamentals/       Distributed-systems concepts and backend building blocks
low-level-design/   Object-oriented design exercises
projects/           Practical implementation prototypes
system-design/      Design notes and end-to-end design exercises
```

Most core Java examples are standalone and do not share a root build. Practical projects have their own build files; see each project README for its status and run instructions. The code and documentation are for learning, not production deployment.
