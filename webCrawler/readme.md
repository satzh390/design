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

## 📦 System Characteristics

| **Aspect** | **Description** |
|-------------|-----------------|
| **Scale** | 1 Billion Pages / Month |
| **Storage Retention** | 5 Years |
| **Content Type** | HTML |
| **Duplicate Handling** | Ignore Duplicate Pages |
| **Key Qualities** | Scalable, Robust, Polite, Extensible |

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

## 🧩 Challenges to Address

- Efficiently managing **billions of URLs**
- Ensuring **fault tolerance** across distributed systems
- Handling **duplication** and **spam pages**
- Maintaining **crawl politeness**
- Ensuring **data freshness** with periodic re-crawling

---

## 🧠 Key Takeaways

- A web crawler may look simple on paper, but real-world implementations must address:
  - **Distributed queue management**
  - **Deduplication**
  - **Rate control**
  - **Scalability and failure recovery**
- A well-designed crawler is **efficient**, **respectful**, and **resilient** to web unpredictability.

---

## 📘 Summary

| **Category** | **Details** |
|---------------|-------------|
| **Objective** | Search Engine Indexing |
| **Crawl Volume** | 1 Billion Pages / Month |
| **Storage Duration** | 5 Years |
| **Content Type** | HTML |
| **Duplicate Pages** | Ignored |
| **Core Qualities** | Scalability, Robustness, Politeness, Extensibility |

---

