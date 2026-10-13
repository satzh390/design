# IdGenerator

## Overview

The `IdGenerator` project implements a custom **Twitter Snowflake–style ID generator** in Java. This generator produces unique, time-ordered 64-bit IDs suitable for distributed systems. The design ensures high throughput, scalability, and uniqueness across multiple machines and data centers.

---
## Table of Contents

1. [System Design](#system-design)
2. [ID Structure](#id-structure)
---

## System Design

The ID generator is based on Twitter's Snowflake algorithm, which splits a 64-bit ID into several segments:

| **Field**     | **Bits** | **Description**                                      |
|---------------|----------|------------------------------------------------------|
| Sign bit      | 1        | Always 0, unused                                    |
| Timestamp     | 41       | Milliseconds since a custom epoch                   |
| Data Center ID| 5        | Unique ID for each data center                      |
| Machine ID    | 5        | Unique ID for each machine within a data center     |
| Sequence      | 12       | Incrementing sequence number per millisecond         |

This structure allows for approximately 69 years of unique IDs, with the ability to generate up to 4096 IDs per millisecond per machine.

---

## ID Structure

The 64-bit ID is constructed as follows:
- **Timestamp**: Represents the number of milliseconds since a custom epoch (e.g., 2025-01-01).
- **Data Center ID**: Identifies the data center where the ID was generated.
- **Machine ID**: Identifies the machine within the data center.
- **Sequence**: A 12-bit incrementing number to handle multiple IDs generated in the same millisecond.
  
---

