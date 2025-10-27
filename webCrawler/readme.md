# 🕷️ Web Crawler Design

> **Topic:** System Design — Web Crawler  
> **Purpose:** Scalable, polite, and extensible system for large-scale web crawling  
> **Goal:** Crawl and store up to **1 billion HTML pages per month** for **search engine indexing**

---

## 📘 Problem Statement

The **basic algorithm** of a web crawler appears simple:

1. Given a set of URLs, **download** all the web pages addressed by those URLs.  
2. **Extract** URLs from these web pages.  
3. **Add** new URLs to the list of URLs to be downloaded.  
4. **Repeat** these steps continuously.

However, building a **real-world scalable web crawler** is **far more complex**. Designing a distributed, robust, and polite crawler that can operate at search-engine scale involves many system design considerations.

---

## 🧭 Understanding Requirements

Before diving into architecture, we must clarify the **requirements and design scope**.

| **Candidate Question** | **Interviewer Response** |
|--------------------------|---------------------------|
| What is the main purpose of the crawler? | Search engine indexing |
| How many web pages will be collected per month? | 1 billion pages |
| What content types should be supported? | HTML only |
| Should newly added or updated pages be considered? | Yes |
| Do we need to store crawled HTML pages? | Yes, for up to 5 years |
| How should we handle duplicate content? | Ignore duplicate pages |

These answers help define both **functional** and **non-functional** requirements.

---

## ⚙️ Key Design Goals

A good web crawler must satisfy the following core characteristics:

### 🧩 **1. Scalability**
- The web contains **billions of pages**.  
- Crawling must be **distributed** and **parallelized** across multiple servers.  
- The system should support horizontal scaling.

### 🛡️ **2. Robustness**
- Must handle:
  - Invalid HTML
  - Unresponsive or slow servers
  - Crashes
  - Malicious links  
- The crawler should **gracefully recover** from errors.

### 🤝 **3. Politeness**
- Respect **robots.txt** and domain-specific crawling rules.  
- Implement **rate limiting** to avoid overloading target servers.  
- Support **retry and backoff** strategies.

### 🧱 **4. Extensibility**
- Designed to support **new content types** (e.g., images, PDFs) without major redesign.  
- Modular architecture — easy to integrate new parsers or filters.

---

## 🧠 Design Components (High-Level)

![HLD](https://github.com/satzh390/design-diagram/blob/main/WebCrawler-HighLevelFlow.drawio.png)

| **Component** | **Responsibility** |
|----------------|--------------------|
| **URL Frontier** | Manages URLs to be crawled, prioritizes new vs. revisited URLs |
| **Fetcher** | Downloads HTML content efficiently using multiple threads |
| **Parser** | Extracts metadata, hyperlinks, and relevant page data |
| **Duplicate Eliminator** | Detects and ignores duplicate pages using hashing (e.g., MD5, SHA) |
| **Storage** | Stores HTML pages for up to 5 years |
| **Scheduler** | Decides crawl timing, ensures politeness, and handles recrawling |
| **Indexer** | Processes and indexes content for search engine use |

---

## 🧰 Example Architecture Flow

1. **Seed URLs** are added to the **URL Frontier**.  
2. **Fetcher** retrieves HTML pages.  
3. **Parser** extracts outbound links and content.  
4. **Duplicate Eliminator** filters known or similar pages.  
5. **Storage** saves raw HTML and metadata.  
6. **Scheduler** adds newly discovered links back to the **URL Frontier**.  
7. **Indexer** updates the search index.

---

# 🕸️ Step 3 - Design Deep Dive

In this chapter, we go beyond the high-level overview and explore the **core design components** and **technical decisions** behind a scalable and efficient web crawler.

---

## 📚 Table of Contents
- [DFS vs BFS](#-dfs-vs-bfs)
- [URL Frontier](#-url-frontier)
  - [Politeness](#politeness)
  - [Priority](#priority)
  - [Freshness](#freshness)
  - [Storage for URL Frontier](#storage-for-url-frontier)
- [HTML Downloader](#-html-downloader)
  - [Robots.txt](#robotstxt)
  - [Performance Optimization](#performance-optimization)
- [Robustness](#-robustness)
- [Extensibility](#-extensibility)
- [Detect and Avoid Problematic Content](#-detect-and-avoid-problematic-content)
- [References](#-references)

---

## 🔍 DFS vs BFS

You can think of the **web as a directed graph**:
- **Nodes** → Web pages  
- **Edges** → Hyperlinks (URLs)

The crawl process is equivalent to traversing this graph.

### Why BFS is preferred over DFS
- **DFS** may go too deep within a single domain.
- **BFS** ensures balanced, broad coverage across multiple sites.

BFS uses a **FIFO (First-In-First-Out)** queue, but naive BFS has two key issues:
1. Many links belong to the same host, flooding that domain with requests.  
2. BFS treats all URLs equally — it ignores priority or quality.

To address this, we introduce the **URL Frontier**.

---

## 🌐 URL Frontier

A **URL Frontier** is a data structure responsible for managing URLs to be crawled.

### Goals
- Enforce **politeness** (avoid overloading servers)
- Support **prioritization** (important pages first)
- Maintain **freshness** (recrawl updated pages efficiently)

---

### 🕊️ Politeness

Politeness prevents flooding the same host with multiple simultaneous requests.

> **Key principle:** Only download one page at a time per host, with delays between downloads.

#### Design Overview

| Component | Description |
|------------|-------------|
| **Queue Router** | Ensures each queue holds URLs from a single host. |
| **Mapping Table** | Maps hosts to queues. |
| **FIFO Queues (b1...bn)** | Each queue processes URLs from the same host. |
| **Queue Selector** | Maps queues to worker threads. |
| **Worker Threads** | Each thread sequentially downloads from one queue. |

**Example Mapping Table**

| Host | Queue |
|------|--------|
| wikipedia.com | b1 |
| apple.com | b2 |
| nike.com | bn |

---

### 🎯 Priority

Different URLs have different levels of importance.

Example:  
- `apple.com` homepage > random discussion forum post about Apple.

| Component | Role |
|------------|------|
| **Prioritizer** | Calculates importance metrics (PageRank, update frequency, traffic). |
| **Priority Queues (f1...fn)** | Queues ordered by priority. |
| **Queue Selector** | Picks URLs with bias toward higher priority. |

---

### 🕰️ Freshness

Web content changes frequently.  
The crawler must **recrawl** periodically to stay up to date.

**Optimization Strategies**
- Recrawl based on **update history**.
- Prioritize and **recrawl important pages** more frequently.
- Avoid full re-crawls to save bandwidth and time.

---

### 💾 Storage for URL Frontier

Real-world crawlers handle **hundreds of millions of URLs**, so storage must balance **speed and durability**.

| Approach | Advantage |
|-----------|------------|
| **In-memory only** | Fast but not scalable or durable. |
| **Disk only** | Durable but slow. |
| **Hybrid (recommended)** | Stores bulk on disk, uses in-memory buffers for I/O. |

---

## 🌍 HTML Downloader

The **HTML Downloader** is responsible for fetching web pages via HTTP.  
Before downloading, it checks the site’s **robots.txt** rules.

---

### 🤖 Robots.txt

The **Robots Exclusion Protocol** specifies which pages are crawlable.

Example from [Amazon’s robots.txt](https://www.amazon.com/robots.txt):

```text
User-agent: Googlebot
Disallow: /creatorhub/*
Disallow: /rss/people/*/reviews
Disallow: /gp/pdp/rss/*/reviews
Disallow: /gp/cdp/member-reviews/
Disallow: /gp/aw/cr/
```
Crawlers should cache robots.txt results and refresh them periodically to reduce overhead.

---

## ⚡ Performance Optimization

### 1. Distributed Crawl
- Crawl jobs are **partitioned and distributed** across multiple servers.  
- Each server handles a **subset of URLs** independently.


---

### 2. Cached DNS Resolver
- DNS lookups are **slow (10–200ms)**.  
- Maintain a **local DNS cache** to avoid repetitive resolutions.

---

### 3. Locality
- Deploy crawl servers **geographically closer** to target sites to improve latency.

---

### 4. Short Timeout
- Set a **maximum wait time** for slow or unresponsive servers.  
- Prevents threads from blocking and ensures timely job rotation.

---

## 🧱 Robustness

A large-scale crawler must gracefully handle **failures, crashes, and data corruption**.

| Technique | Description |
|------------|-------------|
| **Consistent Hashing** | Evenly distributes load among crawler nodes. |
| **Persist Crawl State** | Saves progress to resume after failure. |
| **Exception Handling** | Prevents thread crashes. |
| **Data Validation** | Detects malformed or duplicate data. |

---

## 🧩 Extensibility

The system should support **plug-and-play modules** for new content types.

**Example extensions:**
- 🖼️ **PNG Downloader** → Fetch image files  
- 🔍 **Web Monitor** → Detect copyright or trademark violations  

---

## 🚫 Detect and Avoid Problematic Content

Web crawlers must detect and filter **redundant, harmful, or useless content**.

### 1. Redundant Content
- ~30% of the web consists of **duplicate content**.  
- Use **hashes or checksums** to detect identical pages.

---

### 2. Spider Traps
Infinite loops such as:
http://trap.com/foo/bar/foo/bar/foo/bar/...
**Prevention Techniques:**
- Limit maximum URL depth.  
- Detect unusually large URL counts per domain.  
- Apply **custom domain-specific filters**.

---

### 3. Data Noise
- Filter out **ads, spam, and irrelevant code snippets** to improve data quality.

---

## 📘 References

- [Managing URL Frontiers in Large-Scale Crawling](https://research.google.com/archive/url_frontier.html)  
- [PageRank Algorithm – Wikipedia](https://en.wikipedia.org/wiki/PageRank)  
- [Robots Exclusion Protocol](https://en.wikipedia.org/wiki/Robots_exclusion_standard)  
- [Consistent Hashing – Wikipedia](https://en.wikipedia.org/wiki/Consistent_hashing)

---

## 🧠 Key Takeaways

- A web crawler may look simple on paper, but real-world implementations must address:
  - **Distributed queue management**
  - **Deduplication**
  - **Rate control**
  - **Scalability and failure recovery**
- A well-designed crawler is **efficient**, **respectful**, and **resilient** to web unpredictability.

---

