# 🧠 Design a Search Autocomplete System

### 📌 Overview

Autocomplete (also known as **typeahead**, **search-as-you-type**, or **incremental search**) is a core feature in modern applications such as Google, Amazon, and YouTube.  
It predicts user queries as they type and returns relevant, ranked suggestions within milliseconds.

This project explores the **system design** of a large-scale **Search Autocomplete System**, commonly asked in system design interviews as **“Design Top-K Most Searched Queries”**.

---

## 🎯 Problem Statement

When a user types into a search box (e.g., “dinner”), the system should:

- Suggest up to **5 most popular search queries** that begin with the user’s input.
- Display results **within 100 ms** (industry-standard latency threshold).
- Handle **10M+ Daily Active Users (DAU)** with scalability, relevance, and high availability.

---

## 🧾 Requirements

### ✅ Functional Requirements
- Support **prefix-based matching** (not substring).
- Return **top 5 most frequent** queries.
- Maintain **relevance ranking** by query frequency.
- Update data periodically (e.g., weekly aggregation).
- Case-insensitive and no special characters.

### 🚫 Non-Functional Requirements
- **Low latency:** <100ms for suggestions.
- **High availability** even under partial failure.
- **Scalable** to tens of millions of users.
- **Efficient storage** and incremental updates.

---

## ⚙️ High-Level Design

### 1. Data Collection
- All user searches are logged into an **append-only log** or **analytics database**.
- Periodic batch jobs aggregate query frequency (e.g., every 15 minutes using Spark, or daily using MapReduce).

### 2. Trie Construction
- A **Trie (prefix tree)** is built from aggregated query data.
- Each node stores:
  - The prefix.
  - Top-K most frequent completions under that prefix.

### 3. Trie Updates
There are two strategies for updating the Trie:

#### **Option 1: Full Rebuild (Recommended)**
- Rebuild the entire Trie weekly using batch jobs.
- Once ready, atomically swap the new Trie into production memory.
- ✅ Efficient and avoids partial update complexity.

#### **Option 2: Incremental Update**
- Update Trie nodes directly in memory.
- Slower, but viable if Trie size is small.
- When a node (e.g., “beer”) updates, all its ancestors (“b”, “be”, “bee”) must update top-K lists.

<pre>
package com.example.trie;

import java.util.*;

/**
 * Minimal Trie with top-K suggestions stored at each node.
 */
public class Trie {
    private static final int K = 5;

    private static class Entry implements Comparable<Entry> {
        final int freq;
        final String query;

        Entry(int freq, String query) {
            this.freq = freq;
            this.query = query;
        }

        @Override
        public int compareTo(Entry o) {
            // sort descending by freq, then lexicographically
            if (this.freq != o.freq) return Integer.compare(o.freq, this.freq);
            return this.query.compareTo(o.query);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Entry)) return false;
            return query.equals(((Entry) obj).query);
        }

        @Override
        public int hashCode() {
            return query.hashCode();
        }
    }

    private static class Node {
        final Map<Character, Node> children = new HashMap<>();
        final List<Entry> topk = new ArrayList<>();
    }

    private final Node root = new Node();

    private void addToTopK(List<Entry> topk, int freq, String query) {
        // replace if exists
        for (int i = 0; i < topk.size(); i++) {
            if (topk.get(i).query.equals(query)) {
                topk.set(i, new Entry(freq, query));
                sortAndTrim(topk);
                return;
            }
        }
        // otherwise add
        topk.add(new Entry(freq, query));
        sortAndTrim(topk);
    }

    private void sortAndTrim(List<Entry> topk) {
        Collections.sort(topk);
        if (topk.size() > K) {
            topk.subList(K, topk.size()).clear();
        }
    }

    public void insert(String query, int frequency) {
        if (query == null || query.isEmpty()) return;
        Node node = root;
        addToTopK(node.topk, frequency, query);
        for (char ch : query.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new Node());
            addToTopK(node.topk, frequency, query);
        }
    }

    public List<String> suggest(String prefix) {
        if (prefix == null) return Collections.emptyList();
        Node node = root;
        for (char ch : prefix.toCharArray()) {
            node = node.children.get(ch);
            if (node == null) return Collections.emptyList();
        }
        List<String> res = new ArrayList<>();
        for (Entry e : node.topk) res.add(e.query);
        return res;
    }

    // Simple example loader
    public static Trie buildSample() {
        Trie t = new Trie();
        t.insert("twitter", 35);
        t.insert("twitch", 29);
        t.insert("twilight", 25);
        t.insert("twin peak", 21);
        t.insert("twitch prime", 18);
        t.insert("twitter search", 14);
        t.insert("twillo", 10);
        t.insert("twin peak sf", 8);
        return t;
    }
}
</pre>

---

## 🧠 Example: Trie Node Update

Suppose the query frequency for “beer” increases from **10 → 30**.

- “beer” node is updated.
- Its ancestors (“b”, “be”, “bee”) also refresh their top queries.
- This ensures that “beer” correctly appears in their top-K ranking.

---

## 🧮 Data Aggregation Layer

Depending on the time scale and dataset size:

| Scenario | Technology | Interval | Purpose |
|-----------|-------------|-----------|----------|
| Near-real-time | **Apache Spark Streaming** | Every 15 min | For fast-changing trends (e.g., Twitter, YouTube) |
| Batch | **MapReduce / Hadoop** | Daily / Weekly | For long-term aggregation and deduplication |

Spark provides better latency and incremental aggregation, while MapReduce handles larger, less frequent workloads efficiently.

---

## 🚀 Scalability Considerations

### 1. **Sharding**
- Partition data by prefix hash (`prefix[0:2]`) or region.
- Each shard builds and serves a subset of Trie.

### 2. **Caching**
- Use Redis / Memcached for frequently accessed prefixes.
- Warm popular prefixes during off-peak hours.

### 3. **Load Balancing**
- Distribute requests using consistent hashing or load balancers.

### 4. **Politeness and Throttling**
- Limit frequency of updates and avoid overloading dependent systems.

---

## 💾 Storage

- Store raw query logs in **S3 / HDFS** for long-term retention.
- Keep last 5 years of data for historical analytics.
- Deduplicate queries and ignore duplicate content before aggregation.

---

## 🧩 Characteristics of a Good Autocomplete System

| Property | Description |
|-----------|--------------|
| **Scalability** | Efficiently handle billions of search queries. |
| **Robustness** | Recover from node failure, bad data, or malformed logs. |
| **Politeness** | Avoid excessive hits to dependent systems. |
| **Extensibility** | Easily extend to support new data sources or content types (images, videos). |

---

## 📈 Performance Optimization

1. **Distributed Trie Construction**
   - Build Trie shards in parallel on multiple servers.

2. **Cached DNS Resolution**
   - Cache frequent hostname lookups to reduce latency.

3. **Locality**
   - Deploy Trie servers closer to target users.

4. **Short Timeout**
   - Limit autocomplete query timeout to 100ms.

---

## 🧰 Tech Stack

| Layer | Technologies |
|--------|---------------|
| Data Storage | HDFS / S3 |
| Batch Processing | MapReduce / Spark |
| Stream Processing | Kafka / Spark Streaming |
| In-Memory Trie | Java / C++ / Rust |
| Cache | Redis / Memcached |
| API Layer | REST / gRPC |

---

## 📚 References

- [Prefixy: Scalable Prefix Search Service](https://medium.com/@prefixyteam/how-we-built-prefixy-a-scalable-prefix-search-service-for-powering-autocomplete-c20f98e2eff1)
- [Designing a Search Autocomplete System - Grokking the System Design Interview](https://www.educative.io/courses/grokking-modern-system-design-interview-for-engineers-managers/mE2Jjv6R8nY)
- [Google Research: Efficient Trie-based Autocomplete Systems](https://research.google.com/pubs/archive.html)

---

## 🧩 Summary

| Concept | Description |
|----------|-------------|
| Core Data Structure | Trie (Prefix Tree) |
| Update Strategy | Weekly rebuild or incremental update |
| Aggregation | Spark (fast) or MapReduce (large-scale) |
| Latency Goal | <100ms |
| Key Qualities | Scalable, fault-tolerant, extensible |

---

