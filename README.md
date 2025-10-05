# 🎯 System Design & Coding Principles

This repository is my personal space for exploring **design problems, coding standards, and system design principles**.  
It contains small projects, design exercises, and implementations of key building blocks used in distributed systems and backend engineering.  

My goals with this repo:
- Showcase **clean code practices** (SOLID, design patterns, modular abstractions)  
- Explore **low-level design** (LLD) and **system design** topics in depth  
- Document trade-offs, edge cases, and performance considerations  
- Build a reference base that I (and others) can reuse for interviews, projects, and learning  

---

## 📂 Current Topics

### [Hashing](./Hashing)
- Implementation of **Consistent Hashing** with virtual nodes, server add/remove, and key redistribution.  
- Future extension: **Rendezvous Hashing (HRW)** and replication logic for fault tolerance.  

### [RateLimiter](./RateLimiter)
- Implementations of different **rate limiting algorithms** (Token Bucket, Leaky Bucket, Sliding Window, Fixed Window).  
- Clean abstractions to swap between strategies.  
- Future extension: distributed rate limiting (e.g. Redis-based).  

### [Filter](./Filter)
The `Filter` module provides a **Bloom Filter implementation** optimized for memory efficiency and fast lookups.  

---

## 📅 Upcoming Topics

I will be contributing designs and implementations for the following areas:  

- **Caching** (LRU/LFU cache, distributed cache strategies, cache invalidation patterns)  
- **Load Balancing** (round robin, least connections, weighted, consistent hashing based)  
- **Messaging & Queues** (Kafka internals, custom queue implementation, pub-sub patterns)  
- **Database Systems** (indexing, sharding, replication models)  
- **Concurrency & Synchronization** (thread-safe designs, lock-free structures, semaphores)  
- **Scheduling & Job Systems** (cron-like schedulers, task queues, priority jobs)  
- **Resiliency Patterns** (circuit breaker, retry with backoff, bulkhead, rate shaping)  

---

## 🚀 How to Use
- Browse each folder for design-specific code and explanations.  
- Each subfolder contains a `README.md` that explains the design, trade-offs, and usage examples.  
- I am continuously adding new designs, examples, and improvements.  

---

## 🧑‍💻 About Me
I’m **Sathish Soundararajan**, a Senior Software Engineer with 7+ years of backend & distributed system experience (Java, Spring Boot, Go, Kafka, AWS).  
I love solving design challenges, writing clean code, and sharing architectural insights.  

Connect with me:  
- [LinkedIn](https://www.linkedin.com/in/sathish-soundararajan-85a877227)  
- [LeetCode](https://leetcode.com/u/leetsatzh) | [CSES](https://cses.fi/user/259187)  

---
